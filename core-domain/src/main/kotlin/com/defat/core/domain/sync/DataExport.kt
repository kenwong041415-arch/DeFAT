package com.defat.core.domain.sync

import java.time.Instant

/**
 * "Export my data" (docs/06 principle 3, phase1.5 plan §7.1): one JSON document
 * with every live local row. Built from the same [SyncDoc] maps the sync engine
 * uploads, so the file contains exactly what the cloud holds for this user and
 * nothing more. Tombstoned (deleted) rows are not exported; the user deleted
 * them.
 *
 * Local-only on purpose: it works offline and needs no Cloud Function.
 */
object DataExport {

    const val FORMAT = "defat-export"
    const val FORMAT_VERSION = 1

    /** One entry per source, in a stable order: the profile first, then [SYNCED_SUBCOLLECTIONS]. */
    suspend fun build(
        sources: List<SyncableCollection>,
        exportedAt: Instant,
        appVersion: String,
        accountEmail: String?,
    ): String {
        val byName = sources.associateBy { it.name }
        val root = linkedMapOf<String, Any?>(
            "format" to FORMAT,
            "formatVersion" to FORMAT_VERSION,
            "exportedAt" to exportedAt.toString(),
            "appVersion" to appVersion,
            "account" to accountEmail,
        )
        root["profile"] = byName[SyncCollections.PROFILE]?.liveDocs()?.firstOrNull()?.let(::docToMap)
        for (name in SYNCED_SUBCOLLECTIONS) {
            val source = byName[name] ?: continue
            root[name] = source.liveDocs().map(::docToMap)
        }
        return JsonWriter.write(root)
    }

    private fun docToMap(doc: SyncDoc): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>("id" to doc.id)
        map.putAll(doc.fields)
        map["updatedAt"] = doc.updatedAt
        return map
    }
}

/**
 * A small deterministic JSON serializer. Hand-written because :core-domain has
 * no JSON library and must stay free of Android (org.json is Android's). Keys
 * keep insertion order; non-finite doubles become null, since JSON cannot
 * represent them.
 */
object JsonWriter {

    fun write(value: Any?): String = StringBuilder().also { append(it, value, 0) }.toString()

    private fun append(out: StringBuilder, value: Any?, depth: Int) {
        when (value) {
            null -> out.append("null")
            is Boolean -> out.append(value.toString())
            is Int, is Long, is Short, is Byte -> out.append(value.toString())
            is Float -> appendDouble(out, value.toDouble())
            is Double -> appendDouble(out, value)
            is Enum<*> -> appendString(out, value.name)
            is CharSequence -> appendString(out, value.toString())
            is Map<*, *> -> appendMap(out, value, depth)
            is Iterable<*> -> appendList(out, value, depth)
            else -> appendString(out, value.toString())
        }
    }

    private fun appendDouble(out: StringBuilder, value: Double) {
        if (value.isNaN() || value.isInfinite()) {
            out.append("null")
        } else if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            // 520.0 -> 520.0 (keep it recognisably a decimal) without exponent noise.
            out.append(value.toLong()).append(".0")
        } else {
            out.append(value.toString())
        }
    }

    private fun appendMap(out: StringBuilder, map: Map<*, *>, depth: Int) {
        if (map.isEmpty()) {
            out.append("{}")
            return
        }
        out.append("{\n")
        var first = true
        for ((key, v) in map) {
            if (!first) out.append(",\n")
            first = false
            indent(out, depth + 1)
            appendString(out, key.toString())
            out.append(": ")
            append(out, v, depth + 1)
        }
        out.append('\n')
        indent(out, depth)
        out.append('}')
    }

    private fun appendList(out: StringBuilder, list: Iterable<*>, depth: Int) {
        val items = list.toList()
        if (items.isEmpty()) {
            out.append("[]")
            return
        }
        out.append("[\n")
        items.forEachIndexed { index, item ->
            if (index > 0) out.append(",\n")
            indent(out, depth + 1)
            append(out, item, depth + 1)
        }
        out.append('\n')
        indent(out, depth)
        out.append(']')
    }

    private fun indent(out: StringBuilder, depth: Int) {
        repeat(depth) { out.append("  ") }
    }

    private fun appendString(out: StringBuilder, s: String) {
        out.append('"')
        for (c in s) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c < ' ' -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        out.append('"')
    }
}
