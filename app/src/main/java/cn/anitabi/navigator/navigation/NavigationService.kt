package cn.anitabi.navigator.navigation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import cn.anitabi.navigator.AnitabiApplication
import cn.anitabi.navigator.MainActivity
import cn.anitabi.navigator.R
import cn.anitabi.navigator.core.model.GeoPoint
import cn.anitabi.navigator.core.model.NavigationProgress
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.core.navigation.NavigationEngine
import cn.anitabi.navigator.core.navigation.NavigationUpdate
import cn.anitabi.navigator.core.navigation.TransitRefreshPolicy
import java.time.OffsetDateTime
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NavigationService : Service(), LocationListener, TextToSpeech.OnInitListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val container by lazy { (application as AnitabiApplication).container }
    private val locationManager by lazy { getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    private var plan: TourPlan? = null
    private var engine: NavigationEngine? = null
    private var ticker: Job? = null
    private var loadJob: Job? = null
    private var reroute: Job? = null
    private var lastSavedProgress: NavigationProgress? = null
    private var lastSpokenKey: String? = null
    private var lastTransitRefreshKey: String? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopNavigation()
            ACTION_MANUAL_ARRIVAL -> processUpdate(engine?.manualArrival())
            ACTION_REFRESH_TRANSIT -> refreshTransitRoute()
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, notification("正在恢复巡礼路线…"))
                val tourId = intent.getStringExtra(EXTRA_TOUR_ID)
                loadJob?.cancel()
                loadJob = serviceScope.launch { loadAndStart(tourId) }
            }
            else -> {
                startForeground(NOTIFICATION_ID, notification("正在恢复巡礼路线…"))
                loadJob?.cancel()
                loadJob = serviceScope.launch { loadAndStart(null) }
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLocationChanged(location: Location) {
        val current = GeoPoint(location.latitude, location.longitude)
        val update = engine?.onLocation(current, System.currentTimeMillis()) ?: return
        processUpdate(update)
        if (update.requestReroute && reroute?.isActive != true) {
            reroute = serviceScope.launch { rerouteFrom(current, update.progress) }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.SIMPLIFIED_CHINESE)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        }
    }

    override fun onDestroy() {
        runCatching { locationManager.removeUpdates(this) }
        ticker?.cancel()
        loadJob?.cancel()
        reroute?.cancel()
        tts?.stop()
        tts?.shutdown()
        NavigationRuntime.update { it.copy(isRunning = false, isRerouting = false) }
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun loadAndStart(tourId: String?) {
        try {
            NavigationRuntime.update { it.copy(errorMessage = null) }
            runCatching { locationManager.removeUpdates(this) }
            val saved = tourId?.let { container.tourRepository.get(it) }
                ?: container.tourRepository.getMostRecent()
                ?: error("没有可恢复的巡礼路线")
            if (saved.progress?.state == NavigationState.COMPLETED) error("这条巡礼路线已经完成")
            val loadedPlan = saved.plan
            val destinations = loadedPlan.legs.mapNotNull { it.destinationPointId }.toSet()
            val startPointIds = loadedPlan.orderedPoints
                .filter { it.id !in destinations && it.coordinate == loadedPlan.initialStart }
                .map { it.id }
                .toSet()
            val initialProgress = (saved.progress ?: NavigationProgress(tourId = loadedPlan.id)).let {
                it.copy(completedPointIds = it.completedPointIds + startPointIds)
            }
            val loadedEngine = NavigationEngine(loadedPlan, initialProgress)
            plan = loadedPlan
            engine = loadedEngine
            val firstUpdate = if (initialProgress.state == NavigationState.PLANNED) {
                loadedEngine.start()
            } else {
                loadedEngine.onTick(System.currentTimeMillis())
            }
            processUpdate(firstUpdate)
            startLocationUpdates()
            startTicker()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failAndStop(exception.message ?: "无法开始连续导航")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (!AndroidLocationProvider.hasLocationPermission(this)) {
            throw MissingLocationPermissionException()
        }
        var requested = false
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            if (locationManager.isProviderEnabled(provider)) {
                locationManager.requestLocationUpdates(provider, 2_000L, 2f, this, Looper.getMainLooper())
                requested = true
            }
        }
        if (!requested) throw LocationUnavailableException("No location provider is enabled")
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = serviceScope.launch {
            while (isActive) {
                delay(1_000L)
                processUpdate(engine?.onTick(System.currentTimeMillis()))
            }
        }
    }

    private fun processUpdate(update: NavigationUpdate?) {
        if (update == null) return
        val currentPlan = plan ?: return
        val spokenText = update.spokenText()
        NavigationRuntime.set(
            NavigationRuntimeState(
                plan = currentPlan,
                progress = update.progress,
                currentLocation = update.currentLocation ?: NavigationRuntime.state.value.currentLocation,
                instruction = spokenText,
                remainingDistanceMeters = update.remainingDistanceMeters,
                isRunning = update.progress.state != NavigationState.COMPLETED,
                isRerouting = reroute?.isActive == true,
                errorMessage = NavigationRuntime.state.value.errorMessage,
            ),
        )
        if (update.progress != lastSavedProgress) {
            lastSavedProgress = update.progress
            serviceScope.launch { container.tourRepository.save(currentPlan, update.progress) }
        }
        updateNotification(spokenText)
        speak(spokenText, "${update.progress.state}:${update.progress.legIndex}:${update.progress.stepIndex}")
        refreshTransitWhenNeeded(update)
        if (update.progress.state == NavigationState.COMPLETED) finishCompletedNavigation()
    }

    private suspend fun rerouteFrom(location: GeoPoint, progress: NavigationProgress) {
        val oldPlan = plan ?: return
        NavigationRuntime.update { it.copy(isRerouting = true, errorMessage = null) }
        try {
            val updatedPlan = container.tourPlanner.replanRemaining(
                plan = oldPlan,
                currentLocation = location,
                completedPointIds = progress.completedPointIds,
                currentTime = OffsetDateTime.now().toString(),
            )
            val updatedProgress = progress.copy(
                legIndex = 0,
                stepIndex = 0,
                state = if (updatedPlan.legs.isEmpty()) NavigationState.COMPLETED else NavigationState.NAVIGATING,
                offRouteSinceEpochMillis = null,
            )
            plan = updatedPlan
            engine = NavigationEngine(updatedPlan, updatedProgress)
            lastSavedProgress = null
            container.tourRepository.save(updatedPlan, updatedProgress)
            processUpdate(engine?.onTick(System.currentTimeMillis()))
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            NavigationRuntime.update {
                it.copy(isRerouting = false, errorMessage = "路线重算失败，已继续使用原路线")
            }
        } finally {
            NavigationRuntime.update { it.copy(isRerouting = false) }
        }
    }

    private fun refreshTransitWhenNeeded(update: NavigationUpdate) {
        val currentPlan = plan ?: return
        if (currentPlan.mode != TravelMode.TRANSIT || reroute?.isActive == true) return
        val currentLeg = currentPlan.legs.getOrNull(update.progress.legIndex)
        if (!TransitRefreshPolicy.shouldRefresh(currentPlan, update.progress, update.targetPointId)) return
        val cancelledLeg = currentLeg?.transit?.cancelled == true
        val key = "${update.progress.completedPointIds.size}:${update.progress.legIndex}:$cancelledLeg"
        if (key == lastTransitRefreshKey) return
        lastTransitRefreshKey = key
        val location = update.currentLocation
            ?: NavigationRuntime.state.value.currentLocation
            ?: currentLeg?.to
            ?: return
        reroute = serviceScope.launch { rerouteFrom(location, update.progress) }
    }

    private fun refreshTransitRoute() {
        val currentPlan = plan ?: return
        val progress = engine?.progress ?: return
        if (currentPlan.mode != TravelMode.TRANSIT || reroute?.isActive == true) return
        val location = NavigationRuntime.state.value.currentLocation
            ?: currentPlan.legs.getOrNull(progress.legIndex)?.from
            ?: return
        reroute = serviceScope.launch { rerouteFrom(location, progress) }
    }

    private fun stopNavigation() {
        val currentPlan = plan
        val progress = engine?.progress?.copy(
            state = NavigationState.PLANNED,
            offRouteSinceEpochMillis = null,
        )
        runCatching { locationManager.removeUpdates(this) }
        ticker?.cancel()
        loadJob?.cancel()
        reroute?.cancel()
        NavigationRuntime.update {
            it.copy(
                progress = progress ?: it.progress,
                instruction = "导航已暂停，可从路线预览再次开始",
                isRunning = false,
                isRerouting = false,
            )
        }
        serviceScope.launch {
            try {
                if (currentPlan != null && progress != null) {
                    container.tourRepository.save(currentPlan, progress)
                }
            } finally {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun finishCompletedNavigation() {
        runCatching { locationManager.removeUpdates(this) }
        ticker?.cancel()
        serviceScope.launch {
            delay(2_000L)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun failAndStop(message: String) {
        NavigationRuntime.update { it.copy(isRunning = false, isRerouting = false, errorMessage = message) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun NavigationUpdate.spokenText(): String = when (progress.state) {
        NavigationState.PLANNED -> "准备开始巡礼"
        NavigationState.NAVIGATING -> instruction
        NavigationState.ARRIVING -> if (targetPointId == null && plan?.mode == TravelMode.TRANSIT) {
            "即将到达本段终点，请准备下车或换乘"
        } else {
            "已到达当前巡礼点"
        }
        NavigationState.DWELLING -> "已到达，开始停留"
        NavigationState.NEXT_STOP -> if (targetPointId == null && plan?.mode == TravelMode.TRANSIT) {
            "继续下一换乘段"
        } else {
            "准备前往下一站"
        }
        NavigationState.COMPLETED -> "巡礼路线已完成"
    }

    private fun speak(text: String, key: String) {
        if (!ttsReady || key == lastSpokenKey) return
        lastSpokenKey = key
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "navigation-$key")
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification(text))
    }

    private fun notification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, NavigationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_navigation_notification)
            .setContentTitle("巡礼手帖 · 连续导航")
            .setContentText(text)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .addAction(0, "结束", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "连续导航", NotificationManager.IMPORTANCE_LOW).apply {
                description = "在锁屏和后台继续提供巡礼导航"
            },
        )
    }

    companion object {
        const val ACTION_START = "cn.anitabi.navigator.navigation.START"
        const val ACTION_STOP = "cn.anitabi.navigator.navigation.STOP"
        const val ACTION_MANUAL_ARRIVAL = "cn.anitabi.navigator.navigation.MANUAL_ARRIVAL"
        const val ACTION_REFRESH_TRANSIT = "cn.anitabi.navigator.navigation.REFRESH_TRANSIT"
        const val EXTRA_TOUR_ID = "tour_id"
        private const val CHANNEL_ID = "continuous_navigation"
        private const val NOTIFICATION_ID = 1001
    }
}
