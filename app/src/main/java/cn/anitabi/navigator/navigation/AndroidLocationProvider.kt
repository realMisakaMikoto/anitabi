package cn.anitabi.navigator.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import cn.anitabi.navigator.core.model.GeoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

interface CurrentLocationProvider {
    suspend fun currentLocation(): GeoPoint
}

class AndroidLocationProvider(private val context: Context) : CurrentLocationProvider {
    override suspend fun currentLocation(): GeoPoint = currentLocationFix().coordinate

    suspend fun currentLocationFix(
        maxAgeMillis: Long = Long.MAX_VALUE,
        maxAccuracyMeters: Double = Double.MAX_VALUE,
        requireFinePermission: Boolean = false,
    ): QualifiedLocationFix {
        if (requireFinePermission && !hasFineLocationPermission(context)) {
            throw MissingLocationPermissionException()
        }
        if (!hasLocationPermission(context)) throw MissingLocationPermissionException()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> throw LocationUnavailableException("No location provider is enabled")
        }
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (!continuation.isActive) return@getCurrentLocation
                    if (location == null) {
                        continuation.resumeWithException(LocationUnavailableException("Current location is unavailable"))
                    } else {
                        val elapsedRealtimeMillis = location.elapsedRealtimeNanos / 1_000_000L
                        val ageMillis = if (elapsedRealtimeMillis > 0L) {
                            (SystemClock.elapsedRealtime() - elapsedRealtimeMillis).coerceAtLeast(0L)
                        } else {
                            (System.currentTimeMillis() - location.time).coerceAtLeast(0L)
                        }
                        val accuracyMeters = if (location.hasAccuracy()) {
                            location.accuracy.toDouble()
                        } else {
                            Double.POSITIVE_INFINITY
                        }
                        if (!isQualifiedLocationFix(ageMillis, accuracyMeters, maxAgeMillis, maxAccuracyMeters)) {
                            continuation.resumeWithException(
                                LocationUnavailableException("A recent accurate location is unavailable"),
                            )
                        } else {
                            continuation.resume(
                                QualifiedLocationFix(
                                    coordinate = GeoPoint(location.latitude, location.longitude),
                                    accuracyMeters = accuracyMeters,
                                    capturedAtEpochMillis = location.time,
                                    elapsedRealtimeMillis = elapsedRealtimeMillis,
                                ),
                            )
                        }
                    }
                }
            } catch (exception: SecurityException) {
                continuation.resumeWithException(MissingLocationPermissionException())
            }
        }
    }

    companion object {
        fun hasLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        fun hasFineLocationPermission(context: Context): Boolean =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
}

data class QualifiedLocationFix(
    val coordinate: GeoPoint,
    val accuracyMeters: Double,
    val capturedAtEpochMillis: Long,
    val elapsedRealtimeMillis: Long,
)

internal fun isQualifiedLocationFix(
    ageMillis: Long,
    accuracyMeters: Double,
    maxAgeMillis: Long,
    maxAccuracyMeters: Double,
): Boolean = ageMillis in 0..maxAgeMillis &&
    accuracyMeters.isFinite() &&
    accuracyMeters in 0.0..maxAccuracyMeters

class MissingLocationPermissionException : Exception("Location permission is required")
class LocationUnavailableException(message: String) : Exception(message)
