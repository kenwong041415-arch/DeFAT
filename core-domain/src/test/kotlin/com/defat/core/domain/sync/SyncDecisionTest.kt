package com.defat.core.domain.sync

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

/** The A/B/C table of phase1.5 plan §6.3, exhaustively. */
class SyncDecisionTest {

    @Test
    fun `case A - local rows and nobody ever signed in adopts and pushes only`() {
        assertEquals(
            SignInPlan.ADOPT_LOCAL,
            SyncDecision.decide(hasLocalRows = true, lastSignedInUid = null, newUid = "owner"),
        )
    }

    @Test
    fun `case B - same uid as last time is a normal sync whether or not rows exist`() {
        assertEquals(SignInPlan.SYNC_SAME_ACCOUNT, SyncDecision.decide(true, "owner", "owner"))
        assertEquals(SignInPlan.SYNC_SAME_ACCOUNT, SyncDecision.decide(false, "owner", "owner"))
    }

    @Test
    fun `case C - a different uid with local rows requires confirmation`() {
        assertEquals(SignInPlan.CONFIRM_REPLACE, SyncDecision.decide(true, "someone-else", "owner"))
    }

    @Test
    fun `a different uid with nothing local needs no confirmation and just downloads`() {
        assertEquals(SignInPlan.DOWNLOAD_ONLY, SyncDecision.decide(false, "someone-else", "owner"))
    }

    @Test
    fun `a second device - nobody signed in and nothing local - just downloads`() {
        assertEquals(SignInPlan.DOWNLOAD_ONLY, SyncDecision.decide(false, null, "owner"))
    }

    @Test
    fun `every combination maps to exactly the documented plan`() {
        val expected = mapOf(
            Triple(true, null, "a") to SignInPlan.ADOPT_LOCAL,
            Triple(false, null, "a") to SignInPlan.DOWNLOAD_ONLY,
            Triple(true, "a", "a") to SignInPlan.SYNC_SAME_ACCOUNT,
            Triple(false, "a", "a") to SignInPlan.SYNC_SAME_ACCOUNT,
            Triple(true, "b", "a") to SignInPlan.CONFIRM_REPLACE,
            Triple(false, "b", "a") to SignInPlan.DOWNLOAD_ONLY,
        )
        for ((input, plan) in expected) {
            assertEquals(plan, SyncDecision.decide(input.first, input.second, input.third), "input $input")
        }
    }

    @Test
    fun `existing local rows are never routed to a download-first plan unless they belong to the same account`() {
        // The R1 invariant: with local rows, the only plans are adopt-and-push,
        // same-account sync, or ask. Never DOWNLOAD_ONLY.
        for (last in listOf(null, "a", "b")) {
            val plan = SyncDecision.decide(hasLocalRows = true, lastSignedInUid = last, newUid = "a")
            assertEquals(false, plan == SignInPlan.DOWNLOAD_ONLY, "last=$last")
        }
    }

    @Test
    fun `a blank uid is rejected`() {
        assertFailsWith<IllegalArgumentException> { SyncDecision.decide(true, null, " ") }
    }
}
