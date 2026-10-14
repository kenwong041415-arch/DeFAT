package com.defat.core.data.auth

import com.defat.core.domain.auth.AuthError
import com.defat.core.domain.auth.AuthErrorMapper
import com.defat.core.domain.auth.AuthOutcome
import com.defat.core.domain.auth.AuthRepository
import com.defat.core.domain.auth.AuthUser
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await

/**
 * Email + password (phase1.5 plan D1). Every failure is turned into an
 * [AuthError]; no Firebase exception type or message ever reaches the UI.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) : AuthRepository {

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        // The listener fires once immediately with the current state.
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override fun currentUserNow(): AuthUser? = auth.currentUser?.toAuthUser()

    override suspend fun signUp(email: String, password: String): AuthOutcome = outcome {
        auth.createUserWithEmailAndPassword(email, password).await()
    }

    override suspend fun signIn(email: String, password: String): AuthOutcome = outcome {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun sendPasswordReset(email: String): AuthError? {
        val error = failure { auth.sendPasswordResetEmail(email).await() }
        // "No such account" is reported as success so the screen cannot be used
        // to find out which emails are registered.
        return if (error == AuthError.WRONG_CREDENTIALS) null else error
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun reauthenticate(password: String): AuthError? {
        val user = auth.currentUser ?: return AuthError.NOT_SIGNED_IN
        val email = user.email ?: return AuthError.UNKNOWN
        return failure { user.reauthenticate(EmailAuthProvider.getCredential(email, password)).await() }
    }

    override suspend fun deleteAccount(): AuthError? {
        val user = auth.currentUser ?: return AuthError.NOT_SIGNED_IN
        return failure { user.delete().await() }
    }

    private suspend fun outcome(block: suspend () -> com.google.firebase.auth.AuthResult): AuthOutcome =
        try {
            val user = block().user
            if (user != null) AuthOutcome.Success(user.toAuthUser()) else AuthOutcome.Failure(AuthError.UNKNOWN)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthOutcome.Failure(e.toAuthError())
        }

    private suspend fun failure(block: suspend () -> Unit): AuthError? =
        try {
            block()
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            e.toAuthError()
        }

    private fun Throwable.toAuthError(): AuthError = when (this) {
        is FirebaseAuthException -> AuthErrorMapper.fromFirebaseCode(errorCode)
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }

    private fun FirebaseUser.toAuthUser() = AuthUser(uid = uid, email = email)
}
