package cn.anitabi.navigator.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.libraries.navigation.NavigationView

@Composable
fun NavigationMapView(
    onMapReady: (GoogleMap) -> Unit,
    modifier: Modifier = Modifier,
    navigationUiEnabled: Boolean = false,
    onUnavailable: () -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnMapReady = rememberUpdatedState(onMapReady)
    val currentOnUnavailable = rememberUpdatedState(onUnavailable)
    var attempt by remember(navigationUiEnabled) { mutableIntStateOf(0) }
    var runtimeFailure by remember(navigationUiEnabled, attempt) { mutableStateOf(false) }
    val creation = remember(navigationUiEnabled, attempt) {
        runCatching {
            initializeGoogleMapRuntime(
                initialize = { MapsInitializer.initialize(context) },
                create = {
                    NavigationView(context).apply {
                        onCreate(null)
                        setNavigationUiEnabled(navigationUiEnabled)
                        setHeaderEnabled(navigationUiEnabled)
                        setEtaCardEnabled(navigationUiEnabled)
                        setTripProgressBarEnabled(navigationUiEnabled)
                    }
                },
            )
        }
    }
    val navigationView = creation.getOrNull()
    val unavailable = navigationView == null || runtimeFailure

    LaunchedEffect(unavailable) {
        if (unavailable) currentOnUnavailable.value()
    }

    if (unavailable) {
        MapUnavailablePanel(
            modifier = modifier,
            onRetry = {
                runtimeFailure = false
                attempt += 1
            },
        )
        return
    }

    DisposableEffect(lifecycleOwner, navigationView) {
        var started = false
        var resumed = false

        fun failSafely(block: () -> Unit): Boolean = try {
            block()
            true
        } catch (_: RuntimeException) {
            runtimeFailure = true
            false
        }

        fun start() {
            if (!started) {
                started = failSafely(navigationView::onStart)
            }
        }

        fun resume() {
            start()
            if (started && !resumed) {
                resumed = failSafely(navigationView::onResume)
            }
        }

        fun pause() {
            if (resumed) {
                failSafely(navigationView::onPause)
                resumed = false
            }
        }

        fun stop() {
            pause()
            if (started) {
                failSafely(navigationView::onStop)
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
        failSafely {
            navigationView.getMapAsync { map -> currentOnMapReady.value(map) }
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
            runCatching(navigationView::onDestroy)
        }
    }

    AndroidView(factory = { navigationView }, modifier = modifier)
}

@Composable
private fun MapUnavailablePanel(modifier: Modifier, onRetry: () -> Unit) {
    Surface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Google 地图暂时无法加载", style = MaterialTheme.typography.titleMedium)
            Text(
                "选点、行程与导航进度已保留，请稍后重试。",
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(onClick = onRetry, modifier = Modifier.padding(top = 16.dp)) {
                Text("重试")
            }
        }
    }
}
