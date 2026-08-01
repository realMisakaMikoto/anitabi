package cn.anitabi.navigator.navigation

import com.google.android.libraries.navigation.ListenableResultFuture
import java.io.IOException
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GoogleRoadNavigationSessionTest {
    @Test
    fun `successful result is read without registering the SDK listener`() = runBlocking {
        val future = FakeNavigationFuture<String>().apply { complete("ready") }

        assertEquals("ready", future.awaitNavigationResult())
        assertEquals(0, future.listenerRegistrations)
        assertEquals(0, future.cancelCalls)
    }

    @Test
    fun `cancelling coroutine interrupts wait without cancelling SDK future`() = runBlocking {
        val future = FakeNavigationFuture<String>()
        var continuedAfterAwait = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            future.awaitNavigationResult()
            continuedAfterAwait = true
        }
        assertTrue(future.getStarted.await(2, TimeUnit.SECONDS))

        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertFalse(continuedAfterAwait)
        assertEquals(0, future.listenerRegistrations)
        assertEquals(0, future.cancelCalls)
    }

    @Test
    fun `SDK cancellation while caller is active becomes recoverable failure`() = runBlocking {
        val future = FakeNavigationFuture<String>()
        future.completeCancelled()

        try {
            future.awaitNavigationResult()
            fail("Expected a recoverable navigation cancellation")
        } catch (exception: NavigationRouteRequestCancelledException) {
            assertEquals("Google 导航路线请求已取消", exception.message)
            assertTrue(exception.cause is CancellationException)
        }
        assertEquals(0, future.listenerRegistrations)
        assertEquals(0, future.cancelCalls)
    }

    @Test
    fun `failed future unwraps its original cause for normal error handling`() = runBlocking {
        val future = FakeNavigationFuture<String>().apply {
            completeExceptionally(IOException("route failed"))
        }

        try {
            future.awaitNavigationResult()
            fail("Expected the original future failure")
        } catch (exception: IOException) {
            assertEquals("route failed", exception.message)
        }
        assertEquals(0, future.listenerRegistrations)
        assertEquals(0, future.cancelCalls)
    }
}

private class FakeNavigationFuture<T> : ListenableResultFuture<T> {
    private val completed = CountDownLatch(1)
    val getStarted = CountDownLatch(1)
    var listenerRegistrations = 0
        private set
    var cancelCalls = 0
        private set
    @Volatile
    private var cancelled = false
    @Volatile
    private var value: T? = null
    @Volatile
    private var failure: Throwable? = null

    override fun setOnResultListener(listener: ListenableResultFuture.OnResultListener<T>) {
        listenerRegistrations += 1
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        cancelCalls += 1
        cancelled = true
        completed.countDown()
        return true
    }

    override fun isCancelled(): Boolean = cancelled

    override fun isDone(): Boolean = completed.count == 0L

    override fun get(): T {
        getStarted.countDown()
        completed.await()
        return completedValue()
    }

    override fun get(timeout: Long, unit: TimeUnit): T {
        getStarted.countDown()
        if (!completed.await(timeout, unit)) throw TimeoutException()
        return completedValue()
    }

    fun complete(result: T) {
        value = result
        completed.countDown()
    }

    fun completeExceptionally(throwable: Throwable) {
        failure = throwable
        completed.countDown()
    }

    fun completeCancelled() {
        cancelled = true
        completed.countDown()
    }

    @Suppress("UNCHECKED_CAST")
    private fun completedValue(): T {
        if (cancelled) throw CancellationException("Task was cancelled.")
        failure?.let { throw ExecutionException(it) }
        return value as T
    }
}
