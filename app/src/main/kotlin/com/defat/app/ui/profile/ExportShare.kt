package com.defat.app.ui.profile

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Hands the export JSON to the system share sheet as a file.
 *
 * The file is the user's health data, so it lives only in the app's private
 * cache (served through FileProvider, see res/xml/file_paths.xml), only one copy
 * exists at a time, and [purge] removes it on the next app start.
 */
object ExportShare {
    private const val DIR = "exports"

    fun purge(context: Context) {
        File(context.cacheDir, DIR).deleteRecursively()
    }

    suspend fun share(context: Context, json: String, chooserTitle: String) {
        val uri = withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, DIR)
            dir.deleteRecursively()
            dir.mkdirs()
            val file = File(dir, "defat-export-${LocalDate.now()}.json")
            file.writeText(json, Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/json"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}
