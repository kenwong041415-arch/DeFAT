package com.defat.core.data.db

/**
 * SQL fragments shared by the DAOs and by the migration test, so the test
 * checks the predicate the app really runs.
 */
object SyncSql {
    /**
     * A row is pending when it was never uploaded or has been written since
     * (phase1.5 plan §6.1). Used for every table that has `updatedAtMillis` and
     * `syncedAtMillis` as non-null / nullable respectively.
     */
    const val PENDING = "(syncedAtMillis IS NULL OR syncedAtMillis < updatedAtMillis)"

    /**
     * `food_items.updatedAtMillis` is nullable (seed rows have none), so it is
     * coalesced; only owner-edited rows are ever candidates.
     */
    const val FOOD_PENDING =
        "editedByOwner = 1 AND (syncedAtMillis IS NULL OR syncedAtMillis < COALESCE(updatedAtMillis, 0))"
}
