package com.defat.core.domain.auth

import kotlinx.coroutines.flow.Flow

data class AuthUser(val uid: String, val email: String?)

/** Why an auth call failed, in terms the UI can word. Never carries a raw Firebase message. */
enum class AuthError {
    INVALID_EMAIL,
    WEAK_PASSWORD,
    EMAIL_IN_USE,

    /**
     * Wrong password *or* no such account. Firebase's email-enumeration
     * protection reports both the same way, and the UI must too, so a stranger
     * cannot probe which emails are registered.
     */
    WRONG_CREDENTIALS,
    TOO_MANY_REQUESTS,
    NETWORK,
    NOT_SIGNED_IN,
    UNKNOWN,
}

sealed interface AuthOutcome {
    data class Success(val user: AuthUser) : AuthOutcome
    data class Failure(val error: AuthError) : AuthOutcome
}

interface AuthRepository {
    /** The signed-in user, or null. Emits on every change. */
    val currentUser: Flow<AuthUser?>

    fun currentUserNow(): AuthUser?

    suspend fun signUp(email: String, password: String): AuthOutcome
    suspend fun signIn(email: String, password: String): AuthOutcome

    /** Null on success. Reports success even for an unknown email, by design. */
    suspend fun sendPasswordReset(email: String): AuthError?

    /** Clears the session only. Local records stay (phase1.5 plan D7). */
    suspend fun signOut()

    /** Re-checks the password of the signed-in user. Null on success. */
    suspend fun reauthenticate(password: String): AuthError?

    /** Deletes the Firebase Auth user. Needs a recent sign-in. Null on success. */
    suspend fun deleteAccount(): AuthError?
}
