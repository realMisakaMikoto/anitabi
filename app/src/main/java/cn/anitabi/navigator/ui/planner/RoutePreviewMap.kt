package cn.anitabi.navigator.ui.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.ui.map.NavigationMapView
import cn.anitabi.navigator.ui.map.currentLocationMarkerOptions
import cn.anitabi.navigator.ui.map.routePointMarkerOptions
import cn.anitabi.navigator.ui.map.toGoogleLatLng
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions

@Composable
fun RoutePreviewMap(
    plan: TourPlan,
    modifier: Modifier = Modifier,
    currentLocation: GeoPoint? = null,
    followCurrentLocation: Boolean = false,
) {
    var map by remember { mutableStateOf<GoogleMap?>(null) }

    NavigationMapView(
        modifier = modifier,
        onMapReady = { readyMap ->
            map = readyMap
            readyMap.uiSettings.isMapToolbarEnabled = false
        },
    )

    LaunchedEffect(plan, currentLocation, map) {
        val readyMap = map ?: return@LaunchedEffect
        readyMap.clear()
        plan.legs.forEach { leg ->
            val geometry = leg.geometry.ifEmpty { listOf(leg.from, leg.to) }
                .fold(mutableListOf<GeoPoint>()) { coordinates, point ->
                    if (coordinates.lastOrNull() != point) coordinates += point
                    coordinates
                }
            if (geometry.size >= 2) {
                readyMap.addPolyline(
                    PolylineOptions()
                        .addAll(geometry.map(GeoPoint::toGoogleLatLng))
                        .color(Color(0xFFC94736).toArgb())
                        .width(10f),
                )
            }
        }
        plan.orderedPoints.forEach { point ->
            readyMap.addMarker(routePointMarkerOptions(point))
        }
        currentLocation?.let { location ->
            readyMap.addMarker(currentLocationMarkerOptions(location, "当前位置"))
        }
    }

    LaunchedEffect(currentLocation, followCurrentLocation, map) {
        val location = currentLocation ?: return@LaunchedEffect
        val readyMap = map ?: return@LaunchedEffect
        if (followCurrentLocation) {
            readyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location.toGoogleLatLng(), 16f))
        }
    }

    LaunchedEffect(plan.id, plan.legs, map) {
        val readyMap = map ?: return@LaunchedEffect
        val coordinates = plan.legs.flatMap { leg -> leg.geometry.ifEmpty { listOf(leg.from, leg.to) } }
            .ifEmpty { plan.orderedPoints.map { point -> point.coordinate } }
        when (coordinates.size) {
            0 -> Unit
            1 -> readyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coordinates.single().toGoogleLatLng(), 15f))
            else -> {
                val builder = LatLngBounds.Builder()
                coordinates.forEach { builder.include(it.toGoogleLatLng()) }
                readyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 76))
            }
        }
    }
}
