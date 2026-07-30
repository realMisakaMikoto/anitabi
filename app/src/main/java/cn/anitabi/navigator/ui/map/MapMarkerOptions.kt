package cn.anitabi.navigator.ui.map

import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.PilgrimagePoint
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

internal fun pilgrimageMarkerOptions(
    point: PilgrimagePoint,
    selected: Boolean,
): MarkerOptions = MarkerOptions()
    .position(point.coordinate.toGoogleLatLng())
    .title(point.name)
    .alpha(if (selected) 1f else 0.62f)
    .zIndex(if (selected) 1f else 0f)

internal fun routePointMarkerOptions(point: PilgrimagePoint): MarkerOptions = MarkerOptions()
    .position(point.coordinate.toGoogleLatLng())
    .title(point.name)

internal fun currentLocationMarkerOptions(
    location: GeoPoint,
    title: String,
): MarkerOptions = MarkerOptions()
    .position(location.toGoogleLatLng())
    .title(title)
    .zIndex(2f)

internal fun GeoPoint.toGoogleLatLng(): LatLng = LatLng(latitude, longitude)
