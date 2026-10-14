package com.defat.core.domain.sync

import com.defat.core.domain.auth.AuthRepository
import java.time.Instant

/**
 * "Export my data" (docs/06 principle 3). Reads every live local row and renders
 * one JSON document. It never touches the network, so it works offline and a
 * sync failure can never block a user from getting their data out.
 */
class ExportMyDataUseCase(
    private val sources: List<SyncableCollection>,
    private val auth: AuthRepository,
    private val now: () -> Instant,
) {
    suspend operator fun invoke(appVersion: String): String =
        DataExport.build(
            sources = sources,
            exportedAt = now(),
            appVersion = appVersion,
            accountEmail = auth.currentUserNow()?.email,
        )
}
