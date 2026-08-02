package cn.anitabi.navigator.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitHandoffRequestViewModelTest {
    @Test
    fun inFlightRequestCannotBeSentAgainAfterActivityRecreation() {
        val viewModel = TransitHandoffRequestViewModel()
        val request = request(action = "prepare_next")

        assertTrue(viewModel.beginRequest(request, explicitRetry = false))
        assertFalse(viewModel.beginRequest(request, explicitRetry = false))
        assertEquals(TransitHandoffRequestStatus.Loading(request), viewModel.status)
    }

    @Test
    fun resultReceivedWithoutResumedListenerIsCachedForExplicitUserAction() {
        val viewModel = TransitHandoffRequestViewModel()
        val request = request(action = "prepare_next")
        var deliveredCount = 0
        val listener: (TransitHandoffRequestResult) -> Unit = { deliveredCount += 1 }

        assertTrue(viewModel.beginRequest(request, explicitRetry = false))
        assertTrue(viewModel.completeRequest(request, resultCode = 1, response = response()))
        viewModel.attach(listener)

        assertEquals(0, deliveredCount)
        val cached = viewModel.status as TransitHandoffRequestStatus.Result
        assertEquals(request, cached.value.request)
        assertEquals(1, cached.value.resultCode)
    }

    @Test
    fun freshResultIsDeliveredOnceAndNeverReplayedOnReattach() {
        val viewModel = TransitHandoffRequestViewModel()
        val request = request(action = "prepare")
        var deliveredCount = 0
        val listener: (TransitHandoffRequestResult) -> Unit = { deliveredCount += 1 }

        viewModel.attach(listener)
        assertTrue(viewModel.beginRequest(request, explicitRetry = false))
        assertTrue(viewModel.completeRequest(request, resultCode = 1, response = response()))
        viewModel.detach(listener)
        viewModel.attach(listener)

        assertEquals(1, deliveredCount)
    }

    @Test
    fun completedCommandRequiresExplicitRetryBeforeItCanBeSentAgain() {
        val viewModel = TransitHandoffRequestViewModel()
        val request = request(action = "confirm_arrival")

        assertTrue(viewModel.beginRequest(request, explicitRetry = false))
        assertTrue(viewModel.completeRequest(request, resultCode = 0, response = response()))

        assertFalse(viewModel.beginRequest(request, explicitRetry = false))
        assertTrue(viewModel.beginRequest(request, explicitRetry = true))
    }

    @Test
    fun earlyConfirmationMayStartTheDistinctConfirmedCommand() {
        val viewModel = TransitHandoffRequestViewModel()
        val initial = request(action = "confirm_arrival", confirmEarly = false)
        val confirmed = request(action = "confirm_arrival", confirmEarly = true)

        assertTrue(viewModel.beginRequest(initial, explicitRetry = false))
        assertTrue(viewModel.completeRequest(initial, resultCode = 2, response = response()))

        assertTrue(viewModel.beginRequest(confirmed, explicitRetry = false))
        assertEquals(TransitHandoffRequestStatus.Loading(confirmed), viewModel.status)
    }

    private fun request(
        action: String,
        confirmEarly: Boolean = false,
    ) = TransitHandoffRequestKey(
        action = action,
        tourId = "tour-id",
        legIndex = 3,
        confirmEarly = confirmEarly,
    )

    private fun response() = TransitHandoffServiceResponse(
        message = null,
        expectedLegIndex = 3,
        originLatitude = 35.0,
        originLongitude = 139.0,
        destinationLatitude = 35.1,
        destinationLongitude = 139.1,
    )
}
