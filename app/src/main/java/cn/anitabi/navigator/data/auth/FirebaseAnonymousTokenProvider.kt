package cn.anitabi.navigator.data.auth

import cn.anitabi.navigator.data.network.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface IdTokenProvider {
    suspend fun idToken(): String
}

class FirebaseAnonymousTokenProvider(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) : IdTokenProvider {
    private val signInMutex = Mutex()

    override suspend fun idToken(): String = try {
        val user = currentAnonymousUser()
        user.getIdToken(false).awaitResult().token
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("Firebase returned no ID token")
    } catch (exception: Exception) {
        throw ApiException.Unauthenticated(exception)
    }

    private suspend fun currentAnonymousUser(): FirebaseUser = signInMutex.withLock {
        auth.currentUser?.takeIf(FirebaseUser::isAnonymous)?.let { return@withLock it }
        if (auth.currentUser != null) auth.signOut()
        auth.signInAnonymously().awaitResult().user
            ?: throw IllegalStateException("Firebase returned no anonymous user")
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(
                task.exception ?: IllegalStateException("Firebase task failed"),
            )
        }
    }
}
