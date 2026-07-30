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
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

@Composable
fun PilgrimageMap(
    contentKey: String,
    points: List<PilgrimagePoint>,
    selectedPointIds: Set<String>,
    onPointToggle: (String) -> Unit,
    onVisibleBoundsChanged: (GeoBounds) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnPointToggle by rememberUpdatedState(onPointToggle)
    val currentOnBoundsChanged by rememberUpdatedState(onVisibleBoundsChanged)
    var map by remember { mutableStateOf<GoogleMap?>(null) }
    var centeredContentKey by remember { mutableStateOf<String?>(null) }

    NavigationMapView(
        modifier = modifier,
        onMapReady = { readyMap ->
            map = readyMap
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
        },
    )

    LaunchedEffect(points, selectedPointIds, map) {
        val readyMap = map ?: return@LaunchedEffect
        readyMap.clear()
        points.forEach { point ->
            readyMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.coordinate.latitude, point.coordinate.longitude))
                    .title(point.name)
                    .icon(
                        BitmapDescriptorFactory.defaultMarker(
                            if (point.id in selectedPointIds) {
                                BitmapDescriptorFactory.HUE_RED
                            } else {
                                BitmapDescriptorFactory.HUE_GREEN
                            },
                        ),
                    ),
            )?.tag = point.id
        }
    }

    LaunchedEffect(contentKey, points, map) {
        val readyMap = map ?: return@LaunchedEffect
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
    }
}
