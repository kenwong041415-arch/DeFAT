package com.defat.core.domain.sync

/**
 * Every name under `users/{uid}/` that the app can write health data into.
 *
 * **This is the one list** that both halves of Phase 1.5 read (phase1.5 plan
 * §7.1 / risk R3):
 *
 *  - [SyncEngine] builds its per-collection sources from it and refuses to
 *    start if a source is missing or unknown, and
 *  - [RemoteTreeDeleter] walks it to empty the cloud tree when an account is
 *    deleted.
 *
 * Firestore has no client-side recursive delete, so a collection that is
 * written but missing from this list would silently survive account deletion
 * and leave health data in the cloud after the user asked for it to be
 * destroyed. Adding a collection here is therefore the *only* step needed to
 * make both sync and delete cover it. The rules test also reads this file, so
 * a new name is covered by the ownership tests automatically.
 *
 * Keep this file to plain `const val` / `listOf` literals: the Node rules
 * tests (`firestore-tests/`) parse it with a regular expression.
 */
object SyncCollections {
    /** `users/{uid}` itself. Not a subcollection, so deliberately not in [SYNCED_SUBCOLLECTIONS]. */
    const val PROFILE = "profile"

    const val MEALS = "meals"
    const val WEIGHTS = "weights"
    const val EXERCISE = "exercise"
    const val WATER = "water"
    const val FOOD_OVERRIDES = "foodOverrides"
    const val DAYS = "days"
}

/** See [SyncCollections]. Order is the order sync pushes in. */
val SYNCED_SUBCOLLECTIONS: List<String> = listOf(
    SyncCollections.MEALS,
    SyncCollections.WEIGHTS,
    SyncCollections.EXERCISE,
    SyncCollections.WATER,
    SyncCollections.FOOD_OVERRIDES,
    SyncCollections.DAYS,
)

/**
 * Names in [SYNCED_SUBCOLLECTIONS] that this build does not yet write.
 *
 * `days` is a *derived* rollup. Phase 1 decision D2 is "no caching", so there
 * is no local `days` table to track pending state against; the delete routine
 * still walks it so that whichever build later starts writing it can never
 * leave a rollup behind.
 */
val SUBCOLLECTIONS_NOT_YET_WRITTEN: Set<String> = setOf(SyncCollections.DAYS)
