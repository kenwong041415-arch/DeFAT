package com.defat.core.domain.auth

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `plausible emails pass and obvious junk fails`() {
        assertTrue(AuthValidation.isEmailValid("0625cheung@gmail.com"))
        assertTrue(AuthValidation.isEmailValid("  a.b+c@d.co.uk  "))
        assertFalse(AuthValidation.isEmailValid(""))
        assertFalse(AuthValidation.isEmailValid("no-at-sign"))
        assertFalse(AuthValidation.isEmailValid("a@b"))
        assertFalse(AuthValidation.isEmailValid("a b@c.com"))
        assertFalse(AuthValidation.isEmailValid("@c.com"))
    }

    @Test
    fun `passwords need Firebase's minimum of six characters`() {
        assertFalse(AuthValidation.isPasswordValid("12345"))
        assertTrue(AuthValidation.isPasswordValid("123456"))
    }

    @Test
    fun `the email is trimmed before use`() {
        assertEquals("a@b.com", AuthValidation.normaliseEmail("  a@b.com "))
    }
}

class AuthErrorMapperTest {

    @Test
    fun `wrong password and unknown account are indistinguishable`() {
        val codes = listOf("ERROR_WRONG_PASSWORD", "ERROR_USER_NOT_FOUND", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS")
        codes.forEach { assertEquals(AuthError.WRONG_CREDENTIALS, AuthErrorMapper.fromFirebaseCode(it), it) }
    }

    @Test
    fun `the other common codes map to their own errors`() {
        assertEquals(AuthError.EMAIL_IN_USE, AuthErrorMapper.fromFirebaseCode("ERROR_EMAIL_ALREADY_IN_USE"))
        assertEquals(AuthError.WEAK_PASSWORD, AuthErrorMapper.fromFirebaseCode("ERROR_WEAK_PASSWORD"))
        assertEquals(AuthError.INVALID_EMAIL, AuthErrorMapper.fromFirebaseCode("ERROR_INVALID_EMAIL"))
        assertEquals(AuthError.TOO_MANY_REQUESTS, AuthErrorMapper.fromFirebaseCode("ERROR_TOO_MANY_REQUESTS"))
        assertEquals(AuthError.NETWORK, AuthErrorMapper.fromFirebaseCode("ERROR_NETWORK_REQUEST_FAILED"))
    }

    @Test
    fun `an unknown or missing code is UNKNOWN, never a crash`() {
        assertEquals(AuthError.UNKNOWN, AuthErrorMapper.fromFirebaseCode("ERROR_SOMETHING_NEW"))
        assertEquals(AuthError.UNKNOWN, AuthErrorMapper.fromFirebaseCode(null))
    }
}

class StartDestinationPolicyTest {

    private val same = SyncAccountState.SAME_ACCOUNT
    private val never = SyncAccountState.NEVER_SYNCED
    private val other = SyncAccountState.OTHER_ACCOUNT

    private fun decide(signedIn: Boolean, onboarded: Boolean, account: SyncAccountState) =
        StartDestinationPolicy.decide(signedIn, onboarded, account)

    @Test
    fun `signed out always goes to sign-in, even with a local profile`() {
        SyncAccountState.entries.forEach { account ->
            assertEquals(StartScreen.SIGN_IN, decide(false, true, account), "$account")
            assertEquals(StartScreen.SIGN_IN, decide(false, false, account), "$account")
        }
    }

    @Test
    fun `the owner with an existing local profile skips onboarding`() {
        assertEquals(StartScreen.HOME, decide(true, true, never))
        assertEquals(StartScreen.HOME, decide(true, true, same))
    }

    @Test
    fun `a new account with no profile goes to onboarding once its first sync has run`() {
        assertEquals(StartScreen.ONBOARDING, decide(true, false, same))
    }

    @Test
    fun `no local profile and no first sync finishes sign-in instead of overwriting the cloud profile`() {
        assertEquals(StartScreen.FINISH_SIGN_IN, decide(true, false, never))
    }

    @Test
    fun `another account's records on the phone never open Home, with or without a profile`() {
        // Killed while the case C dialog was up: B is signed in, A's profile is still here.
        assertEquals(StartScreen.FINISH_SIGN_IN, decide(true, true, other))
        assertEquals(StartScreen.FINISH_SIGN_IN, decide(true, false, other))
    }
}
