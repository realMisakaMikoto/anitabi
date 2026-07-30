package cn.anitabi.navigator.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.navigation.NavigationView

@Composable
fun NavigationMapView(
    onMapReady: (GoogleMap) -> Unit,
    modifier: Modifier = Modifier,
    navigationUiEnabled: Boolean = false,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val navigationView = remember(navigationUiEnabled) {
        NavigationView(context).apply {
            onCreate(null)
            setNavigationUiEnabled(navigationUiEnabled)
            setHeaderEnabled(navigationUiEnabled)
            setEtaCardEnabled(navigationUiEnabled)
            setTripProgressBarEnabled(navigationUiEnabled)
        }
    }

    DisposableEffect(lifecycleOwner, navigationView) {
        var started = false
        var resumed = false

        fun start() {
            if (!started) {
                navigationView.onStart()
                started = true
            }
        }

        fun resume() {
            start()
            if (!resumed) {
                navigationView.onResume()
                resumed = true
            }
        }

        fun pause() {
            if (resumed) {
                navigationView.onPause()
                resumed = false
            }
        }

        fun stop() {
            pause()
            if (started) {
                navigationView.onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> start()
                Lifecycle.Event.ON_RESUME -> resume()
                Lifecycle.Event.ON_PAUSE -> pause()
                Lifecycle.Event.ON_STOP -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            resume()
        } else if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            start()
        }
        navigationView.getMapAsync { map -> currentOnMapReady.value(map) }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
            navigationView.onDestroy()
        }
    }

    AndroidView(factory = { navigationView }, modifier = modifier)
}
