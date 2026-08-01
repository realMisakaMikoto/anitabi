package cn.anitabi.navigator.ui.search

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.ui.map.NavigationMapView
import cn.anitabi.navigator.ui.map.pilgrimageMarkerOptions
import cn.anitabi.navigator.ui.map.withPositiveMapViewport
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

private const val SELECTED_HALO_RADIUS_METERS = 28.0

@Composable
fun PilgrimageMap(
    contentKey: String,
    points: List<PilgrimagePoint>,
    selectedPointIds: Set<String>,
    onPointToggle: (String) -> Unit,
    onVisibleBoundsChanged: (GeoBounds) -> Unit,
    onMapUnavailable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPointToggle by rememberUpdatedState(onPointToggle)
    val currentOnBoundsChanged by rememberUpdatedState(onVisibleBoundsChanged)
    val currentOnMapUnavailable by rememberUpdatedState(onMapUnavailable)
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var centeredContentKey by remember { mutableStateOf<String?>(null) }
    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }

    NavigationMapView(
        modifier = modifier,
        onUnavailable = onMapUnavailable,
        onViewportSizeChanged = { width, height ->
            viewportWidth = width
            viewportHeight = height
        },
        onMapReady = { readyMap ->
            try {
                readyMap.uiSettings.isMapToolbarEnabled = false
                readyMap.setOnMarkerClickListener { marker ->
                    (marker.tag as? String)?.let(currentOnPointToggle)
                    true
                }
                readyMap.setOnCameraIdleListener {
                    try {
                        val bounds = readyMap.projection.visibleRegion.latLngBounds
                        currentOnBoundsChanged(
                            GeoBounds(
                                north = bounds.northeast.latitude,
                                east = bounds.northeast.longitude,
                                south = bounds.southwest.latitude,
                                west = bounds.southwest.longitude,
                            ),
                        )
                    } catch (error: RuntimeException) {
                        Log.w("PilgrimageMap", "VISIBLE_BOUNDS failed (${error.javaClass.name})")
                    }
                }
                map = readyMap
            } catch (error: RuntimeException) {
                Log.w("PilgrimageMap", "READY_SETUP failed (${error.javaClass.name})")
                map = null
                currentOnMapUnavailable()
            }
        },
    )

    LaunchedEffect(points, selectedPointIds, map) {
        val readyMap = map ?: return@LaunchedEffect
        try {
            readyMap.clear()
            points.forEach { point ->
                if (point.id in selectedPointIds) {
                    readyMap.addCircle(
                        CircleOptions()
                            .center(LatLng(point.coordinate.latitude, point.coordinate.longitude))
                            .radius(SELECTED_HALO_RADIUS_METERS)
                            .fillColor(0x33C93E4F)
                            .strokeColor(0xCCC93E4F.toInt())
                            .strokeWidth(2f)
                            .clickable(false)
                            .zIndex(0.5f),
                    )
                }
                readyMap.addMarker(
                    pilgrimageMarkerOptions(point, point.id in selectedPointIds),
                )?.tag = point.id
            }
        } catch (error: RuntimeException) {
            Log.w("PilgrimageMap", "MARKERS failed (${error.javaClass.name})")
            map = null
            currentOnMapUnavailable()
        }
    }

    LaunchedEffect(contentKey, points, map, viewportWidth, viewportHeight) {
        val readyMap = map ?: return@LaunchedEffect
        try {
            if (points.isNotEmpty() && centeredContentKey != contentKey) {
                val cameraUpdate = withPositiveMapViewport(viewportWidth, viewportHeight) { width, height ->
                    if (points.size == 1) {
                        val point = points.single().coordinate
                        CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), 15f)
                    } else {
                        val builder = LatLngBounds.Builder()
                        points.forEach { point ->
                            builder.include(LatLng(point.coordinate.latitude, point.coordinate.longitude))
                        }
                        CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 88)
                    }
                } ?: return@LaunchedEffect
                readyMap.animateCamera(cameraUpdate)
                centeredContentKey = contentKey
            }
        } catch (error: RuntimeException) {
            Log.w("PilgrimageMap", "FIT_BOUNDS failed (${error.javaClass.name})")
        }
    }
}
