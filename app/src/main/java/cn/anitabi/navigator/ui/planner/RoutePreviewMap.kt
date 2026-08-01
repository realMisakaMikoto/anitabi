package cn.anitabi.navigator.ui.planner

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.ui.map.NavigationMapView
import cn.anitabi.navigator.ui.map.currentLocationMarkerOptions
import cn.anitabi.navigator.ui.map.routePointMarkerOptions
import cn.anitabi.navigator.ui.map.toGoogleLatLng
import cn.anitabi.navigator.ui.map.withPositiveMapViewport
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.Vermilion
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.PolylineOptions

@Composable
fun RoutePreviewMap(
    plan: TourPlan,
    modifier: Modifier = Modifier,
    currentLocation: GeoPoint? = null,
    followCurrentLocation: Boolean = false,
) {
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }

    NavigationMapView(
        modifier = modifier,
        onViewportSizeChanged = { width, height ->
            viewportWidth = width
            viewportHeight = height
        },
        onUnavailable = {
            map = null
        },
        onMapReady = { readyMap ->
            readyMap.uiSettings.isMapToolbarEnabled = false
            map = readyMap
        },
    )

    LaunchedEffect(plan, currentLocation, map) {
        val readyMap = map ?: return@LaunchedEffect
        try {
            readyMap.clear()
            plan.legs.forEach { leg ->
                val geometry = leg.geometry.ifEmpty { listOf(leg.from, leg.to) }
                    .fold(mutableListOf<GeoPoint>()) { coordinates, point ->
                        if (coordinates.lastOrNull() != point) coordinates += point
                        coordinates
                    }
                if (geometry.size >= 2) {
                    val isWalkingConnector = plan.mode == TravelMode.TRANSIT && leg.mode == TravelMode.WALK
                    val options = PolylineOptions()
                        .addAll(geometry.map(GeoPoint::toGoogleLatLng))
                        .color((if (isWalkingConnector) Moss else Vermilion).toArgb())
                        .width(if (isWalkingConnector) 8f else 10f)
                    if (isWalkingConnector) options.pattern(listOf(Dot(), Gap(14f)))
                    readyMap.addPolyline(
                        options,
                    )
                }
            }
            plan.orderedPoints.forEach { point ->
                readyMap.addMarker(routePointMarkerOptions(point))
            }
            currentLocation?.let { location ->
                readyMap.addMarker(currentLocationMarkerOptions(location, "当前位置"))
            }
        } catch (error: RuntimeException) {
            Log.w("RoutePreviewMap", "DRAW_CONTENT failed (${error.javaClass.name})")
        }
    }

    LaunchedEffect(currentLocation, followCurrentLocation, map, viewportWidth, viewportHeight) {
        val location = currentLocation ?: return@LaunchedEffect
        val readyMap = map ?: return@LaunchedEffect
        if (followCurrentLocation) {
            try {
                val cameraUpdate = withPositiveMapViewport(viewportWidth, viewportHeight) { _, _ ->
                    CameraUpdateFactory.newLatLngZoom(location.toGoogleLatLng(), 16f)
                } ?: return@LaunchedEffect
                readyMap.animateCamera(cameraUpdate)
            } catch (error: RuntimeException) {
                Log.w("RoutePreviewMap", "FOLLOW_LOCATION failed (${error.javaClass.name})")
            }
        }
    }

    LaunchedEffect(plan.id, plan.legs, map, viewportWidth, viewportHeight) {
        val readyMap = map ?: return@LaunchedEffect
        val coordinates = plan.legs.flatMap { leg -> leg.geometry.ifEmpty { listOf(leg.from, leg.to) } }
            .ifEmpty { plan.orderedPoints.map { point -> point.coordinate } }
        try {
            val cameraUpdate = withPositiveMapViewport(viewportWidth, viewportHeight) { width, height ->
                when (coordinates.size) {
                    0 -> null
                    1 -> CameraUpdateFactory.newLatLngZoom(coordinates.single().toGoogleLatLng(), 15f)
                    else -> {
                        val builder = LatLngBounds.Builder()
                        coordinates.forEach { builder.include(it.toGoogleLatLng()) }
                        CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 76)
                    }
                }
            } ?: return@LaunchedEffect
            readyMap.animateCamera(cameraUpdate)
        } catch (error: RuntimeException) {
            Log.w("RoutePreviewMap", "FIT_BOUNDS failed (${error.javaClass.name})")
        }
    }
}
