package cn.anitabi.navigator.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.ui.map.NavigationMapView
import cn.anitabi.navigator.ui.map.pilgrimageMarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

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

    NavigationMapView(
        modifier = modifier,
        onUnavailable = onMapUnavailable,
        onMapReady = { readyMap ->
            try {
                readyMap.uiSettings.isMapToolbarEnabled = false
                readyMap.setOnMarkerClickListener { marker ->
                    (marker.tag as? String)?.let(currentOnPointToggle)
                    true
                }
                readyMap.setOnCameraIdleListener {
                    val bounds = readyMap.projection.visibleRegion.latLngBounds
                    currentOnBoundsChanged(
                        GeoBounds(
                            north = bounds.northeast.latitude,
                            east = bounds.northeast.longitude,
                            south = bounds.southwest.latitude,
                            west = bounds.southwest.longitude,
                        ),
                    )
                }
                map = readyMap
            } catch (_: RuntimeException) {
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
                readyMap.addMarker(
                    pilgrimageMarkerOptions(point, point.id in selectedPointIds),
                )?.tag = point.id
            }
        } catch (_: RuntimeException) {
            map = null
            currentOnMapUnavailable()
        }
    }

    LaunchedEffect(contentKey, points, map) {
        val readyMap = map ?: return@LaunchedEffect
        try {
            if (points.isNotEmpty() && centeredContentKey != contentKey) {
                if (points.size == 1) {
                    val point = points.single().coordinate
                    readyMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(point.latitude, point.longitude), 15f),
                    )
                } else {
                    val builder = LatLngBounds.Builder()
                    points.forEach { point ->
                        builder.include(LatLng(point.coordinate.latitude, point.coordinate.longitude))
                    }
                    readyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 88))
                }
                centeredContentKey = contentKey
            }
        } catch (_: RuntimeException) {
            map = null
            currentOnMapUnavailable()
        }
    }
}
