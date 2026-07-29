package cn.anitabi.navigator.ui.planner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.TourPlan
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun RoutePreviewMap(
    plan: TourPlan,
    modifier: Modifier = Modifier,
    currentLocation: GeoPoint? = null,
    followCurrentLocation: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var routeSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var stopSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var currentLocationSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    DisposableEffect(lifecycleOwner, mapView) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { readyMap ->
            map = readyMap
            readyMap.uiSettings.isAttributionEnabled = false
            val newRouteSource = GeoJsonSource(ROUTE_SOURCE_ID)
            val newStopSource = GeoJsonSource(STOP_SOURCE_ID)
            val newCurrentLocationSource = GeoJsonSource(CURRENT_LOCATION_SOURCE_ID)
            readyMap.setStyle(
                Style.Builder()
                    .fromUri(OPEN_FREE_MAP_STYLE)
                    .withSource(newRouteSource)
                    .withSource(newStopSource)
                    .withSource(newCurrentLocationSource)
                    .withLayer(
                        LineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID).withProperties(
                            PropertyFactory.lineColor(Color(0xFFC94736).toArgb()),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineOpacity(0.88f),
                        ),
                    )
                    .withLayer(
                        CircleLayer(STOP_LAYER_ID, STOP_SOURCE_ID).withProperties(
                            PropertyFactory.circleColor(Color(0xFF20322C).toArgb()),
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            PropertyFactory.circleStrokeWidth(2f),
                        ),
                    )
                    .withLayer(
                        CircleLayer(CURRENT_LOCATION_LAYER_ID, CURRENT_LOCATION_SOURCE_ID).withProperties(
                            PropertyFactory.circleColor(Color(0xFF2878B5).toArgb()),
                            PropertyFactory.circleRadius(8f),
                            PropertyFactory.circleStrokeColor(Color.White.toArgb()),
                            PropertyFactory.circleStrokeWidth(3f),
                        ),
                    ),
            ) {
                routeSource = newRouteSource
                stopSource = newStopSource
                currentLocationSource = newCurrentLocationSource
            }
        }
        onDispose { }
    }

    LaunchedEffect(plan, routeSource, stopSource) {
        routeSource?.setGeoJson(plan.toRouteFeatures())
        stopSource?.setGeoJson(plan.toStopFeatures())
    }

    LaunchedEffect(currentLocation, currentLocationSource) {
        currentLocationSource?.setGeoJson(
            FeatureCollection.fromFeatures(
                currentLocation?.let { listOf(Feature.fromGeometry(it.toMapLibrePoint())) }.orEmpty(),
            ),
        )
    }

    LaunchedEffect(currentLocation, followCurrentLocation, map) {
        val location = currentLocation ?: return@LaunchedEffect
        val readyMap = map ?: return@LaunchedEffect
        if (followCurrentLocation) {
            readyMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16.0),
            )
        }
    }

    LaunchedEffect(plan.id, map) {
        val readyMap = map ?: return@LaunchedEffect
        val coordinates = plan.legs.flatMap { it.geometry.ifEmpty { listOf(it.from, it.to) } }
        if (coordinates.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            coordinates.forEach { builder.include(LatLng(it.latitude, it.longitude)) }
            readyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 76))
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun TourPlan.toRouteFeatures(): FeatureCollection {
    val features = legs.mapNotNull { leg ->
        val geometry = leg.geometry.ifEmpty { listOf(leg.from, leg.to) }.fold(mutableListOf<GeoPoint>()) { points, point ->
            if (points.lastOrNull() != point) points += point
            points
        }
        if (geometry.size < 2) null else Feature.fromGeometry(
            LineString.fromLngLats(geometry.map(GeoPoint::toMapLibrePoint)),
        )
    }
    return FeatureCollection.fromFeatures(features)
}

private fun TourPlan.toStopFeatures(): FeatureCollection = FeatureCollection.fromFeatures(
    orderedPoints.map { point -> Feature.fromGeometry(point.coordinate.toMapLibrePoint()) },
)

private fun GeoPoint.toMapLibrePoint(): Point = Point.fromLngLat(longitude, latitude)

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val ROUTE_SOURCE_ID = "preview-route-source"
private const val ROUTE_LAYER_ID = "preview-route-layer"
private const val STOP_SOURCE_ID = "preview-stop-source"
private const val STOP_LAYER_ID = "preview-stop-layer"
private const val CURRENT_LOCATION_SOURCE_ID = "preview-current-location-source"
private const val CURRENT_LOCATION_LAYER_ID = "preview-current-location-layer"
