package com.defat.core.domain.auth

/** Client-side checks, run before a network call is spent on an obviously bad input. */
object AuthValidation {

    /** Firebase Auth's own minimum. */
    const val MIN_PASSWORD_LENGTH = 6

    private val emailShape = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")

    fun normaliseEmail(raw: String): String = raw.trim()

    fun isEmailValid(raw: String): Boolean = emailShape.matches(normaliseEmail(raw))

    fun isPasswordValid(password: String): Boolean = password.length >= MIN_PASSWORD_LENGTH
}

/**
 * Maps a `FirebaseAuthException.errorCode` string to an [AuthError]. Lives here,
 * not next to the Firebase call, so it is unit-tested without Android.
 */
object AuthErrorMapper {
    fun fromFirebaseCode(code: String?): AuthError = when (code) {
        "ERROR_INVALID_EMAIL", "ERROR_MISSING_EMAIL" -> AuthError.INVALID_EMAIL
        "ERROR_WEAK_PASSWORD" -> AuthError.WEAK_PASSWORD
        "ERROR_EMAIL_ALREADY_IN_USE", "ERROR_CREDENTIAL_ALREADY_IN_USE" -> AuthError.EMAIL_IN_USE
        "ERROR_WRONG_PASSWORD",
        "ERROR_INVALID_CREDENTIAL",
        "ERROR_USER_NOT_FOUND",
        "ERROR_INVALID_LOGIN_CREDENTIALS",
        "ERROR_USER_DISABLED",
        -> AuthError.WRONG_CREDENTIALS
        "ERROR_TOO_MANY_REQUESTS" -> AuthError.TOO_MANY_REQUESTS
        "ERROR_NETWORK_REQUEST_FAILED" -> AuthError.NETWORK
        "ERROR_USER_TOKEN_EXPIRED", "ERROR_USER_MISMATCH" -> AuthError.NOT_SIGNED_IN
        else -> AuthError.UNKNOWN
    }
}
