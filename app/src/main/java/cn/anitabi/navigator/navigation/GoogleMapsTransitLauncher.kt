package cn.anitabi.navigator.navigation

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import cn.anitabi.navigator.core.model.GeoPoint

class GoogleMapsTransitLauncher internal constructor(
    private val urlFactory: GoogleMapsTransitUrlFactory,
    private val starter: GoogleMapsTransitStarter,
) {
    constructor(context: Context) : this(
        urlFactory = AndroidGoogleMapsTransitUrlFactory,
        starter = AndroidGoogleMapsTransitStarter(context),
    )

    fun launch(origin: GeoPoint, destination: GeoPoint): Boolean {
        val url = urlFactory.build(origin, destination)
        return starter.start(url, GOOGLE_MAPS_PACKAGE) || starter.start(url, null)
    }

    private companion object {
        const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"
    }
}

internal fun interface GoogleMapsTransitUrlFactory {
    fun build(origin: GeoPoint, destination: GeoPoint): String
}

internal fun interface GoogleMapsTransitStarter {
    fun start(url: String, packageName: String?): Boolean
}

internal data class GoogleMapsTransitUrlSpec(
    val scheme: String,
    val authority: String,
    val path: String,
    val queryParameters: List<Pair<String, String>>,
)

internal fun googleMapsTransitUrlSpec(
    origin: GeoPoint,
    destination: GeoPoint,
): GoogleMapsTransitUrlSpec = GoogleMapsTransitUrlSpec(
    scheme = "https",
    authority = "www.google.com",
    path = "/maps/dir/",
    queryParameters = listOf(
        "api" to "1",
        "origin" to origin.toLatLngParameter(),
        "destination" to destination.toLatLngParameter(),
        "travelmode" to "transit",
    ),
)

private object AndroidGoogleMapsTransitUrlFactory : GoogleMapsTransitUrlFactory {
    override fun build(origin: GeoPoint, destination: GeoPoint): String {
        val spec = googleMapsTransitUrlSpec(origin, destination)
        return Uri.Builder()
            .scheme(spec.scheme)
            .authority(spec.authority)
            .path(spec.path)
            .apply {
                spec.queryParameters.forEach { (name, value) -> appendQueryParameter(name, value) }
            }
            .build()
            .toString()
    }
}

internal fun googleMapsTransitDirectionsUrl(origin: GeoPoint, destination: GeoPoint): String =
    AndroidGoogleMapsTransitUrlFactory.build(origin, destination)

private class AndroidGoogleMapsTransitStarter(
    private val context: Context,
) : GoogleMapsTransitStarter {
    override fun start(url: String, packageName: String?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            packageName?.let(::setPackage)
            if (context.findActivity() == null) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return startResolvableGoogleMapsIntent(
            hasHandler = intent.resolveActivity(context.packageManager) != null,
        ) {
            context.startActivity(intent)
        }
    }
}

internal fun startResolvableGoogleMapsIntent(
    hasHandler: Boolean,
    startActivity: () -> Unit,
): Boolean {
    if (!hasHandler) return false
    return try {
        startActivity()
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun GeoPoint.toLatLngParameter(): String = "$latitude,$longitude"

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
