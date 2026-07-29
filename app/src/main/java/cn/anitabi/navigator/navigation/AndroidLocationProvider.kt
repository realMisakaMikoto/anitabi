package cn.anitabi.navigator.navigation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import cn.anitabi.navigator.core.model.GeoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

interface CurrentLocationProvider {
    suspend fun currentLocation(): GeoPoint
}

class AndroidLocationProvider(private val context: Context) : CurrentLocationProvider {
    override suspend fun currentLocation(): GeoPoint {
        if (!hasLocationPermission(context)) throw MissingLocationPermissionException()
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> throw LocationUnavailableException("No location provider is enabled")
        }
        return suspendCoroutine { continuation ->
            try {
                LocationManagerCompat.getCurrentLocation(
                    manager,
                    provider,
                    CancellationSignal(),
                    ContextCompat.getMainExecutor(context),
                ) { location ->
                    if (location == null) {
                        continuation.resumeWithException(LocationUnavailableException("Current location is unavailable"))
                    } else {
                        continuation.resume(GeoPoint(location.latitude, location.longitude))
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
    }
}

class MissingLocationPermissionException : Exception("Location permission is required")
class LocationUnavailableException(message: String) : Exception(message)
