package com.defat.core.domain.sync

/**
 * What to do the first time (or any time) a signed-in session meets this
 * phone's local data. Phase 1.5 plan §6.3.
 *
 * The owner already has real logged meals on their phone that have never been
 * uploaded anywhere. The worst outcome in this phase is an implementation that
 * downloads first and treats an empty cloud as the truth: that erases them.
 * So this decision is a pure function, tested against every combination.
 */
enum class SignInPlan {
    /**
     * Case A: local rows exist and no account has ever signed in on this
     * device. The local data becomes this account's cloud data.
     *
     * Local rows are pushed **first**, and nothing local is ever removed or
     * overwritten by the cloud during the adopt. The one thing the adopt will
     * not do is clobber a *remote* document that is newer than the local row
     * with the same id: weights are keyed by date, water by day and the
     * profile is a single fixed document, so a phone that was used offline and
     * only signed in later could otherwise overwrite newer records made on a
     * phone that was already signed in. Those documents are left pending and
     * the pull that follows reconciles them.
     */
    ADOPT_LOCAL,

    /** Case B: same uid as last time. Pull what changed, then push. */
    SYNC_SAME_ACCOUNT,

    /**
     * Nothing local to protect and nothing to confirm (a second device, or a
     * phone whose previous account left no rows): fetch everything, then push.
     */
    DOWNLOAD_ONLY,

    /**
     * Case C: local rows belong to a different uid. Touch nothing until the
     * user explicitly chooses to replace them (§6.4).
     */
    CONFIRM_REPLACE,
}

object SyncDecision {

    /**
     * @param hasLocalRows true if the device holds *anything* the user logged
     *   or entered: meals, weights, exercise, water, owner-edited foods or a
     *   completed profile. Tombstoned rows count; they are still rows.
     * @param lastSignedInUid the uid stored in DataStore at the end of the
     *   last completed sync pass, or null if no account has ever finished one.
     * @param newUid the uid that has just signed in.
     */
    fun decide(hasLocalRows: Boolean, lastSignedInUid: String?, newUid: String): SignInPlan {
        require(newUid.isNotBlank()) { "newUid must not be blank" }
        return when {
            lastSignedInUid == newUid -> SignInPlan.SYNC_SAME_ACCOUNT
            // Different (or no) previous account, and nothing local to lose.
            !hasLocalRows -> SignInPlan.DOWNLOAD_ONLY
            // Rows exist and nobody has ever signed in here: they are this user's.
            lastSignedInUid == null -> SignInPlan.ADOPT_LOCAL
            // Rows exist and they belonged to someone else.
            else -> SignInPlan.CONFIRM_REPLACE
        }
    }
}
