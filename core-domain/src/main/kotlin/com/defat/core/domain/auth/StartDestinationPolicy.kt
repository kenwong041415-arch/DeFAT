package com.defat.core.domain.auth

/** Where the app opens. Pure so the sign-in gate is tested without Android. */
enum class StartScreen { SIGN_IN, FINISH_SIGN_IN, ONBOARDING, HOME }

/** How the signed-in uid relates to the account whose first sync last completed on this phone (§6.3). */
enum class SyncAccountState {
    /** `lastSignedInUid == uid`: this account has synced here before. */
    SAME_ACCOUNT,

    /** No account has ever completed a first sync on this phone (Case A territory, or a fresh install). */
    NEVER_SYNCED,

    /** `lastSignedInUid` is a different uid: the records on this phone belong to someone else (Case C). */
    OTHER_ACCOUNT,
}

object StartDestinationPolicy {

    /**
     * @param signedIn a Firebase user exists.
     * @param onboardingComplete a complete profile is on this phone.
     * @param account see [SyncAccountState].
     *
     * Two rows are subtle, and both exist so a signed-in user is never dropped
     * into the wrong person's data:
     *
     *  - **OTHER_ACCOUNT always finishes sign-in.** If the app is killed while the
     *    "this phone has records from another account" dialog is up, the new
     *    account is already authenticated and the old account's profile is still
     *    on the phone. Going to Home would show one person's records under
     *    another's session. Finishing sign-in re-runs the decision and re-asks.
     *  - **No local profile and no first sync finishes sign-in** instead of
     *    onboarding, because a freshly typed profile would then win last-write-wins
     *    over the one already in the cloud. A user who *has* a local profile
     *    (the owner's existing records, Case A) goes straight to Home and the
     *    sync adopts it in the background.
     */
    fun decide(signedIn: Boolean, onboardingComplete: Boolean, account: SyncAccountState): StartScreen = when {
        !signedIn -> StartScreen.SIGN_IN
        account == SyncAccountState.OTHER_ACCOUNT -> StartScreen.FINISH_SIGN_IN
        onboardingComplete -> StartScreen.HOME
        account == SyncAccountState.NEVER_SYNCED -> StartScreen.FINISH_SIGN_IN
        else -> StartScreen.ONBOARDING
    }
}
