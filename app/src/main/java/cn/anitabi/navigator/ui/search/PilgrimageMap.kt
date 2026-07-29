package cn.anitabi.navigator.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.anitabi.navigator.core.model.PilgrimagePoint
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

@Composable
fun PilgrimageMap(
    contentKey: String,
    points: List<PilgrimagePoint>,
    selectedPointIds: Set<String>,
    onPointToggle: (String) -> Unit,
    onVisibleBoundsChanged: (GeoBounds) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember { MapView(context) }
    val currentOnPointToggle by rememberUpdatedState(onPointToggle)
    val currentOnBoundsChanged by rememberUpdatedState(onVisibleBoundsChanged)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var pointSource by remember { mutableStateOf<GeoJsonSource?>(null) }
    var centeredContentKey by remember { mutableStateOf<String?>(null) }

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
            val source = createPointSource()
            readyMap.setStyle(
                Style.Builder()
                    .fromUri(OPEN_FREE_MAP_STYLE)
                    .withSource(source)
                    .withLayer(createClusterLayer())
                    .withLayer(createClusterCountLayer())
                    .withLayer(createPointLayer()),
            ) {
                pointSource = source
            }
            readyMap.addOnMapClickListener { latLng ->
                val screenPoint = readyMap.projection.toScreenLocation(latLng)
                val pointFeature = readyMap.queryRenderedFeatures(screenPoint, POINT_LAYER_ID).firstOrNull()
                if (pointFeature != null) {
                    currentOnPointToggle(pointFeature.getStringProperty(POINT_ID_PROPERTY))
                    true
                } else {
                    val cluster = readyMap.queryRenderedFeatures(screenPoint, CLUSTER_LAYER_ID).firstOrNull()
                    if (cluster != null) {
                        val nextZoom = source.getClusterExpansionZoom(cluster).toDouble()
                        readyMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, nextZoom + 0.01))
                        true
                    } else {
                        false
                    }
                }
            }
            readyMap.addOnCameraIdleListener {
                val bounds = readyMap.projection.visibleRegion.latLngBounds
                currentOnBoundsChanged(
                    GeoBounds(
                        north = bounds.latitudeNorth,
                        east = bounds.longitudeEast,
                        south = bounds.latitudeSouth,
                        west = bounds.longitudeWest,
                    ),
                )
            }
        }
        onDispose { }
    }

    LaunchedEffect(points, selectedPointIds, pointSource) {
        pointSource?.setGeoJson(points.toFeatureCollection(selectedPointIds))
    }

    LaunchedEffect(contentKey, points, map) {
        val readyMap = map ?: return@LaunchedEffect
        if (points.isNotEmpty() && centeredContentKey != contentKey) {
            val builder = LatLngBounds.Builder()
            points.forEach { point ->
                builder.include(LatLng(point.coordinate.latitude, point.coordinate.longitude))
            }
            readyMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 88))
            centeredContentKey = contentKey
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun createPointSource(): GeoJsonSource = GeoJsonSource(
    POINT_SOURCE_ID,
    FeatureCollection.fromFeatures(emptyList<Feature>()),
    GeoJsonOptions()
        .withCluster(true)
        .withClusterMaxZoom(14)
        .withClusterRadius(46)
        .withClusterMinPoints(3),
)

private fun createPointLayer(): CircleLayer = CircleLayer(POINT_LAYER_ID, POINT_SOURCE_ID)
    .withProperties(
        PropertyFactory.circleColor(
            Expression.switchCase(
                Expression.toBool(Expression.get(SELECTED_PROPERTY)),
                Expression.literal(Color(0xFFC94736).toArgb()),
                Expression.literal(Color(0xFF20322C).toArgb()),
            ),
        ),
        PropertyFactory.circleRadius(
            Expression.switchCase(
                Expression.toBool(Expression.get(SELECTED_PROPERTY)),
                Expression.literal(10f),
                Expression.literal(7f),
            ),
        ),
        PropertyFactory.circleStrokeWidth(2f),
        PropertyFactory.circleStrokeColor(Color.White.toArgb()),
    )
    .withFilter(Expression.not(Expression.has("point_count")))

private fun createClusterLayer(): CircleLayer = CircleLayer(CLUSTER_LAYER_ID, POINT_SOURCE_ID)
    .withProperties(
        PropertyFactory.circleColor(Color(0xFFC94736).toArgb()),
        PropertyFactory.circleStrokeColor(Color.White.toArgb()),
        PropertyFactory.circleStrokeWidth(2f),
        PropertyFactory.circleRadius(
            Expression.interpolate(
                Expression.linear(),
                Expression.toNumber(Expression.get("point_count")),
                Expression.stop(3, 17f),
                Expression.stop(50, 25f),
            ),
        ),
    )
    .withFilter(Expression.has("point_count"))

private fun createClusterCountLayer(): SymbolLayer = SymbolLayer(CLUSTER_COUNT_LAYER_ID, POINT_SOURCE_ID)
    .withProperties(
        PropertyFactory.textField(Expression.get("point_count_abbreviated")),
        PropertyFactory.textSize(12f),
        PropertyFactory.textColor(Color.White.toArgb()),
        PropertyFactory.textAllowOverlap(true),
        PropertyFactory.textIgnorePlacement(true),
        PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
    )
    .withFilter(Expression.has("point_count"))

private fun List<PilgrimagePoint>.toFeatureCollection(selectedPointIds: Set<String>): FeatureCollection {
    val features = map { point ->
        Feature.fromGeometry(Point.fromLngLat(point.coordinate.longitude, point.coordinate.latitude)).apply {
            addStringProperty(POINT_ID_PROPERTY, point.id)
            addBooleanProperty(SELECTED_PROPERTY, point.id in selectedPointIds)
        }
    }
    return FeatureCollection.fromFeatures(features)
}

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val POINT_SOURCE_ID = "pilgrimage-points"
private const val POINT_LAYER_ID = "pilgrimage-point-circles"
private const val CLUSTER_LAYER_ID = "pilgrimage-clusters"
private const val CLUSTER_COUNT_LAYER_ID = "pilgrimage-cluster-count"
private const val POINT_ID_PROPERTY = "point_id"
private const val SELECTED_PROPERTY = "selected"
