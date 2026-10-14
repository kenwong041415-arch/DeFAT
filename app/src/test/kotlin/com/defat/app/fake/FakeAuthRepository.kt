package com.defat.app.fake

import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthOutcome
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.auth.AuthUser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Signing in with a successful result also makes that user current, like the real thing. */
class FakeAuthRepository(initial: AuthUser? = null) : AuthRepository {

    private val user = MutableStateFlow(initial)

    var signInResult: AuthOutcome = AuthOutcome.Failure(AuthError.UNKNOWN)
    var signUpResult: AuthOutcome = AuthOutcome.Failure(AuthError.UNKNOWN)
    var resetResult: AuthError? = null
    var reauthResult: AuthError? = null
    var deleteResult: AuthError? = null

    /** Every call, in order, so a test can assert what did NOT happen as well. */
    val calls = mutableListOf<String>()

    override val currentUser: Flow<AuthUser?> = user

    override fun currentUserNow(): AuthUser? = user.value

    override suspend fun signUp(email: String, password: String): AuthOutcome {
        calls += "signUp:$email"
        return signUpResult.also { if (it is AuthOutcome.Success) user.value = it.user }
    }

    override suspend fun signIn(email: String, password: String): AuthOutcome {
        calls += "signIn:$email"
        return signInResult.also { if (it is AuthOutcome.Success) user.value = it.user }
    }

    override suspend fun sendPasswordReset(email: String): AuthError? {
        calls += "reset:$email"
        return resetResult
    }

    override suspend fun signOut() {
        calls += "signOut"
        user.value = null
    }

    override suspend fun reauthenticate(password: String): AuthError? {
        calls += "reauth"
        return reauthResult
    }

    override suspend fun deleteAccount(): AuthError? {
        calls += "deleteAccount"
        if (deleteResult == null) user.value = null
        return deleteResult
    }
}
