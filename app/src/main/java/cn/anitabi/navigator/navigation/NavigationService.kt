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
import android.os.Bundle
import android.os.Build
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
import cn.anitabi.navigator.core.navigation.afterRouteRefresh
import cn.anitabi.navigator.data.network.ApiException
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
    private var roadSyncJob: Job? = null
    private var progressSaveJob: Job? = null
    private var cleanupJob: Job? = null
    private var roadNavigationSession: GoogleRoadNavigationSession? = null
    private var lastRoadSyncLegIndex: Int? = null
    private var nativeRemainingDistanceMeters: Double? = null
    private var nativeRerouting = false
    private var lastSavedProgress: NavigationProgress? = null
    private var lastSpokenKey: String? = null
    private var lastTransitRefreshKey: String? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var stopping = false
    private var navigationGeneration = 0L

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
            ACTION_START -> startNavigation(intent.getStringExtra(EXTRA_TOUR_ID))
            else -> startNavigation(null)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onLocationChanged(location: Location) {
        if (stopping) return
        val current = GeoPoint(location.latitude, location.longitude)
        if (plan?.mode != TravelMode.TRANSIT) {
            NavigationRuntime.update { it.copy(currentLocation = current) }
            return
        }
        val update = engine?.onLocation(current, System.currentTimeMillis()) ?: return
        val generation = navigationGeneration
        processUpdate(update, generation)
        if (update.requestReroute && reroute?.isActive != true) {
            reroute = serviceScope.launch { rerouteFrom(current, update.progress, generation) }
        }
    }

    @Deprecated("Required by LocationListener on Android 8")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) = Unit

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
        roadSyncJob?.cancel()
        cleanupJob?.cancel()
        roadNavigationSession?.close()
        tts?.stop()
        tts?.shutdown()
        NavigationRuntime.update { it.copy(isRunning = false, isRerouting = false) }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startNavigation(tourId: String?) {
        val generation = ++navigationGeneration
        stopping = false
        startForeground(NOTIFICATION_ID, notification("正在恢复巡礼路线…"))
        val previousCleanup = cleanupJob
        val previousLoad = loadJob
        previousLoad?.cancel()
        runCatching { locationManager.removeUpdates(this) }
        ticker?.cancel()
        ticker = null
        val previousReroute = reroute
        previousReroute?.cancel()
        reroute = null
        roadSyncJob?.cancel()
        roadSyncJob = null
        roadNavigationSession?.close()
        roadNavigationSession = null
        engine = null
        plan = null
        lastSavedProgress = null
        lastRoadSyncLegIndex = null
        lastSpokenKey = null
        lastTransitRefreshKey = null
        nativeRemainingDistanceMeters = null
        nativeRerouting = false
        val pendingSave = progressSaveJob
        loadJob = serviceScope.launch {
            previousLoad?.join()
            previousReroute?.join()
            previousCleanup?.join()
            pendingSave?.join()
            if (generation != navigationGeneration) return@launch
            loadAndStart(tourId, generation)
        }
    }

    private suspend fun loadAndStart(tourId: String?, generation: Long) {
        var routeRefreshRequired = false
        try {
            if (generation != navigationGeneration) return
            NavigationRuntime.update { it.copy(errorMessage = null) }
            runCatching { locationManager.removeUpdates(this) }
            val saved = tourId?.let { container.tourRepository.get(it) }
                ?: container.tourRepository.getMostRecent()
                ?: error("没有可恢复的巡礼路线")
            if (generation != navigationGeneration) return
            if (saved.progress?.state == NavigationState.COMPLETED) error("这条巡礼路线已经完成")
            var loadedPlan = saved.plan
            plan = loadedPlan
            val destinations = loadedPlan.legs.mapNotNull { it.destinationPointId }.toSet()
            val startPointIds = loadedPlan.orderedPoints
                .filter { it.id !in destinations && it.coordinate == loadedPlan.initialStart }
                .map { it.id }
                .toSet() + listOfNotNull(saved.storedTour.startPointId)
            var initialProgress = (saved.progress ?: NavigationProgress(tourId = loadedPlan.id)).let {
                it.copy(completedPointIds = it.completedPointIds + startPointIds)
            }
            if (saved.routeNeedsRefresh) {
                routeRefreshRequired = true
                NavigationRuntime.set(
                    NavigationRuntimeState(
                        plan = loadedPlan,
                        progress = initialProgress,
                        instruction = ROUTE_REFRESH_REQUIRED_MESSAGE,
                        errorMessage = ROUTE_REFRESH_REQUIRED_MESSAGE,
                    ),
                )
                val currentLocation = container.locationProvider.currentLocation()
                if (generation != navigationGeneration) return
                loadedPlan = container.tourPlanner.replanRemaining(
                    plan = loadedPlan,
                    currentLocation = currentLocation,
                    completedPointIds = initialProgress.completedPointIds,
                    currentTime = OffsetDateTime.now().toString(),
                )
                if (generation != navigationGeneration) return
                initialProgress = initialProgress.afterRouteRefresh(loadedPlan.legs.isNotEmpty())
                container.tourRepository.save(loadedPlan, initialProgress)
                if (generation != navigationGeneration) return
                routeRefreshRequired = false
                NavigationRuntime.update { it.copy(errorMessage = null) }
            }
            if (initialProgress.state == NavigationState.COMPLETED) {
                error("这条巡礼路线已经完成")
            }
            val loadedEngine = NavigationEngine(loadedPlan, initialProgress)
            plan = loadedPlan
            engine = loadedEngine
            roadNavigationSession?.close()
            roadNavigationSession = if (loadedPlan.mode == TravelMode.TRANSIT) {
                null
            } else {
                GoogleRoadNavigationSession(
                    application = application,
                    backendApi = container.backendApi,
                    plan = loadedPlan,
                    initialLegIndex = initialProgress.legIndex.coerceAtLeast(0),
                    onArrival = { legIndex ->
                        serviceScope.launch {
                            if (generation == navigationGeneration) onNativeArrival(legIndex, generation)
                        }
                    },
                    onRemainingDistanceChanged = { meters ->
                        serviceScope.launch {
                            if (generation != navigationGeneration) return@launch
                            nativeRemainingDistanceMeters = meters
                            NavigationRuntime.update { it.copy(remainingDistanceMeters = meters) }
                        }
                    },
                    onReroutingChanged = { rerouting ->
                        serviceScope.launch {
                            if (generation != navigationGeneration) return@launch
                            nativeRerouting = rerouting
                            NavigationRuntime.update { it.copy(isRerouting = rerouting) }
                        }
                    },
                )
            }
            val firstUpdate = if (initialProgress.state == NavigationState.PLANNED) {
                loadedEngine.start()
            } else {
                loadedEngine.onTick(System.currentTimeMillis())
            }
            processUpdate(firstUpdate, generation)
            if (generation != navigationGeneration) return
            startLocationUpdates()
            startTicker(generation)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failAndStop(
                if (routeRefreshRequired) {
                    ROUTE_REFRESH_REQUIRED_MESSAGE
                } else {
                    navigationFailureMessage(exception)
                },
                saveAsUnresolved = routeRefreshRequired,
                expectedGeneration = generation,
            )
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

    private fun startTicker(generation: Long) {
        ticker?.cancel()
        ticker = serviceScope.launch {
            while (isActive && generation == navigationGeneration) {
                delay(1_000L)
                processUpdate(engine?.onTick(System.currentTimeMillis()), generation)
            }
        }
    }

    private fun processUpdate(update: NavigationUpdate?, expectedGeneration: Long = navigationGeneration) {
        if (stopping || expectedGeneration != navigationGeneration || update == null) return
        val currentPlan = plan ?: return
        val stateText = update.spokenText()
        val displayedText = if (
            currentPlan.mode != TravelMode.TRANSIT && update.progress.state == NavigationState.NAVIGATING
        ) {
            "Google 导航正在引导前往下一巡礼点"
        } else {
            stateText
        }
        NavigationRuntime.set(
            NavigationRuntimeState(
                plan = currentPlan,
                progress = update.progress,
                currentLocation = update.currentLocation ?: NavigationRuntime.state.value.currentLocation,
                instruction = displayedText,
                remainingDistanceMeters = if (currentPlan.mode == TravelMode.TRANSIT) {
                    update.remainingDistanceMeters
                } else {
                    nativeRemainingDistanceMeters ?: update.remainingDistanceMeters
                },
                isRunning = update.progress.state != NavigationState.COMPLETED,
                isRerouting = if (currentPlan.mode == TravelMode.TRANSIT) {
                    reroute?.isActive == true
                } else {
                    nativeRerouting
                },
                errorMessage = NavigationRuntime.state.value.errorMessage,
            ),
        )
        if (update.progress != lastSavedProgress) {
            lastSavedProgress = update.progress
            val previousSave = progressSaveJob
            progressSaveJob = serviceScope.launch {
                previousSave?.join()
                if (expectedGeneration != navigationGeneration) return@launch
                container.tourRepository.save(currentPlan, update.progress)
            }
        }
        updateNotification(displayedText)
        if (currentPlan.mode == TravelMode.TRANSIT) {
            speak(stateText, "${update.progress.state}:${update.progress.legIndex}:${update.progress.stepIndex}")
        }
        synchronizeRoadNavigation(update.progress, expectedGeneration)
        refreshTransitWhenNeeded(update, expectedGeneration)
        if (update.progress.state == NavigationState.COMPLETED) {
            finishCompletedNavigation(expectedGeneration)
        }
    }

    private fun synchronizeRoadNavigation(progress: NavigationProgress, expectedGeneration: Long) {
        val session = roadNavigationSession ?: return
        when (progress.state) {
            NavigationState.NAVIGATING -> {
                if (lastRoadSyncLegIndex == progress.legIndex) return
                lastRoadSyncLegIndex = progress.legIndex
                roadSyncJob?.cancel()
                roadSyncJob = serviceScope.launch {
                    try {
                        session.synchronize(progress.legIndex)
                    } catch (exception: CancellationException) {
                        throw exception
                    } catch (exception: Exception) {
                        lastRoadSyncLegIndex = null
                        failAndStop(
                            navigationFailureMessage(exception),
                            expectedGeneration = expectedGeneration,
                        )
                    }
                }
            }
            NavigationState.ARRIVING,
            NavigationState.DWELLING,
            NavigationState.NEXT_STOP,
            -> session.pauseGuidance()
            NavigationState.COMPLETED -> session.close()
            NavigationState.PLANNED -> Unit
        }
    }

    private fun onNativeArrival(legIndex: Int, expectedGeneration: Long) {
        if (expectedGeneration != navigationGeneration) return
        val activeEngine = engine ?: return
        if (
            activeEngine.progress.state == NavigationState.NAVIGATING &&
            activeEngine.progress.legIndex == legIndex
        ) {
            processUpdate(activeEngine.manualArrival(), expectedGeneration)
        }
    }

    private suspend fun rerouteFrom(
        location: GeoPoint,
        progress: NavigationProgress,
        expectedGeneration: Long,
    ) {
        if (expectedGeneration != navigationGeneration) return
        val oldPlan = plan ?: return
        NavigationRuntime.update { it.copy(isRerouting = true, errorMessage = null) }
        try {
            val updatedPlan = container.tourPlanner.replanRemaining(
                plan = oldPlan,
                currentLocation = location,
                completedPointIds = progress.completedPointIds,
                currentTime = OffsetDateTime.now().toString(),
            )
            if (expectedGeneration != navigationGeneration) return
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
            if (expectedGeneration != navigationGeneration) return
            processUpdate(engine?.onTick(System.currentTimeMillis()), expectedGeneration)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            if (expectedGeneration == navigationGeneration) {
                NavigationRuntime.update {
                    it.copy(isRerouting = false, errorMessage = "路线重算失败，已继续使用原路线")
                }
            }
        } finally {
            if (expectedGeneration == navigationGeneration) {
                NavigationRuntime.update { it.copy(isRerouting = false) }
            }
        }
    }

    private fun refreshTransitWhenNeeded(update: NavigationUpdate, expectedGeneration: Long) {
        if (expectedGeneration != navigationGeneration) return
        val currentPlan = plan ?: return
        if (currentPlan.mode != TravelMode.TRANSIT || reroute?.isActive == true) return
        val currentLeg = currentPlan.legs.getOrNull(update.progress.legIndex)
        if (!TransitRefreshPolicy.shouldRefresh(
                currentPlan,
                update.progress,
                update.targetPointId,
                nowEpochMillis = System.currentTimeMillis(),
            )
        ) return
        val cancelledLeg = currentLeg?.transit?.cancelled == true
        val key = "${update.progress.completedPointIds.size}:${update.progress.legIndex}:$cancelledLeg"
        if (key == lastTransitRefreshKey) return
        lastTransitRefreshKey = key
        val location = update.currentLocation
            ?: NavigationRuntime.state.value.currentLocation
            ?: currentLeg?.to
            ?: return
        reroute = serviceScope.launch { rerouteFrom(location, update.progress, expectedGeneration) }
    }

    private fun refreshTransitRoute() {
        val generation = navigationGeneration
        val currentPlan = plan ?: return
        val progress = engine?.progress ?: return
        if (currentPlan.mode != TravelMode.TRANSIT || reroute?.isActive == true) return
        val location = NavigationRuntime.state.value.currentLocation
            ?: currentPlan.legs.getOrNull(progress.legIndex)?.from
            ?: return
        reroute = serviceScope.launch { rerouteFrom(location, progress, generation) }
    }

    private fun stopNavigation() {
        val generation = ++navigationGeneration
        stopping = true
        val currentPlan = plan
        val progress = engine?.progress?.copy(
            state = NavigationState.PLANNED,
            offRouteSinceEpochMillis = null,
        )
        runCatching { locationManager.removeUpdates(this) }
        val previousCleanup = cleanupJob
        ticker?.cancel()
        ticker = null
        val previousLoad = loadJob
        previousLoad?.cancel()
        val previousReroute = reroute
        previousReroute?.cancel()
        reroute = null
        roadSyncJob?.cancel()
        roadSyncJob = null
        roadNavigationSession?.close()
        roadNavigationSession = null
        engine = null
        nativeRemainingDistanceMeters = null
        nativeRerouting = false
        NavigationRuntime.update {
            it.copy(
                progress = progress ?: it.progress,
                instruction = "导航已暂停，可从路线预览再次开始",
                isRunning = false,
                isRerouting = false,
            )
        }
        val pendingSave = progressSaveJob
        cleanupJob = serviceScope.launch {
            previousLoad?.join()
            previousReroute?.join()
            previousCleanup?.join()
            pendingSave?.join()
            completeNavigationCleanup(
                expectedGeneration = generation,
                currentGeneration = { navigationGeneration },
                persistRollback = {
                    if (currentPlan != null && progress != null) {
                        container.tourRepository.save(currentPlan, progress)
                    }
                },
                stopService = {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                },
            )
        }
    }

    private fun finishCompletedNavigation(expectedGeneration: Long) {
        if (expectedGeneration != navigationGeneration) return
        stopping = true
        runCatching { locationManager.removeUpdates(this) }
        ticker?.cancel()
        ticker = null
        roadSyncJob?.cancel()
        roadSyncJob = null
        roadNavigationSession?.close()
        roadNavigationSession = null
        val previousCleanup = cleanupJob
        val pendingSave = progressSaveJob
        cleanupJob = serviceScope.launch {
            previousCleanup?.join()
            pendingSave?.join()
            delay(2_000L)
            if (expectedGeneration != navigationGeneration) return@launch
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun failAndStop(
        message: String,
        saveAsUnresolved: Boolean = false,
        expectedGeneration: Long = navigationGeneration,
    ) {
        if (stopping || expectedGeneration != navigationGeneration) return
        stopping = true
        val currentPlan = plan
        val progress = currentPlan?.let { loadedPlan ->
            resumableProgressForTourAfterFailure(
                tourId = loadedPlan.id,
                engineProgress = engine?.progress,
                runtimeProgress = NavigationRuntime.state.value.progress,
            )
        }
        runCatching { locationManager.removeUpdates(this) }
        ticker?.cancel()
        ticker = null
        val pendingReroute = reroute
        pendingReroute?.cancel()
        reroute = null
        roadSyncJob?.cancel()
        roadSyncJob = null
        roadNavigationSession?.close()
        roadNavigationSession = null
        engine = null
        lastRoadSyncLegIndex = null
        nativeRemainingDistanceMeters = null
        nativeRerouting = false
        val previousCleanup = cleanupJob
        val pendingSave = progressSaveJob
        NavigationRuntime.update { previous ->
            navigationRuntimeAfterFailure(previous, currentPlan, progress, message)
        }
        cleanupJob = serviceScope.launch {
            previousCleanup?.join()
            pendingReroute?.join()
            pendingSave?.join()
            completeNavigationCleanup(
                expectedGeneration = expectedGeneration,
                currentGeneration = { navigationGeneration },
                persistRollback = {
                    if (currentPlan != null && progress != null) {
                        if (saveAsUnresolved) {
                            container.tourRepository.saveUnresolved(currentPlan, progress)
                        } else {
                            container.tourRepository.save(currentPlan, progress)
                        }
                    }
                },
                stopService = {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                },
            )
        }
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
            .setCategory(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    Notification.CATEGORY_NAVIGATION
                } else {
                    Notification.CATEGORY_SERVICE
                },
            )
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
        private const val ROUTE_REFRESH_REQUIRED_MESSAGE =
            "路线暂时无法刷新，请联网后重试；行程顺序和导航进度已保留"
        private const val CHANNEL_ID = "continuous_navigation"
        private const val NOTIFICATION_ID = 1001
    }
}

internal fun resumableProgressAfterFailure(progress: NavigationProgress?): NavigationProgress? =
    progress?.takeUnless { it.state == NavigationState.COMPLETED }?.copy(
        state = NavigationState.PLANNED,
        dwellingUntilEpochMillis = null,
        offRouteSinceEpochMillis = null,
    )

internal fun resumableProgressForTourAfterFailure(
    tourId: String,
    engineProgress: NavigationProgress?,
    runtimeProgress: NavigationProgress?,
): NavigationProgress? = resumableProgressAfterFailure(
    engineProgress?.takeIf { it.tourId == tourId }
        ?: runtimeProgress?.takeIf { it.tourId == tourId },
)

internal fun navigationRuntimeAfterFailure(
    previous: NavigationRuntimeState,
    currentPlan: TourPlan?,
    progress: NavigationProgress?,
    message: String,
): NavigationRuntimeState = previous.copy(
    plan = currentPlan ?: previous.plan,
    progress = if (currentPlan != null) progress else previous.progress,
    instruction = "导航未开始，请返回路线预览后重试",
    isRunning = false,
    isRerouting = false,
    errorMessage = message,
)

internal suspend fun completeNavigationCleanup(
    expectedGeneration: Long,
    currentGeneration: () -> Long,
    persistRollback: suspend () -> Unit,
    stopService: () -> Unit,
) {
    persistRollback()
    if (expectedGeneration == currentGeneration()) stopService()
}

internal fun navigationFailureMessage(throwable: Throwable): String = when (throwable) {
    is ApiException.QuotaExhausted -> "路线额度已用尽，暂时无法开始；不会继续产生费用"
    is ApiException.RateLimited -> "请求过于频繁，请稍后再试"
    is ApiException.Unauthenticated -> "匿名连接失败，请检查网络后重试"
    is ApiException.InvalidArgument -> "路线请求参数无效，请返回重新生成路线"
    is ApiException.NoRoute, is ApiException.NotFound -> "Google 未找到可用路线，请返回重新生成"
    is ApiException.UpstreamUnavailable -> "Google 路线服务暂时不可用，请稍后再试"
    is ApiException.BackendUnavailable, is ApiException.Server ->
        "路线服务暂时不可用；行程和导航进度仍保留在本机"
    is ApiException.Network -> "无法连接路线服务，请检查网络后重试"
    is ApiException.InvalidResponse -> "路线服务返回了无法识别的数据"
    is ApiException.InvalidCredentials, is ApiException.Forbidden, is ApiException.Http ->
        "路线请求失败，请稍后再试"
    is MissingLocationPermissionException -> "需要定位权限才能开始导航"
    is LocationUnavailableException -> "暂时无法取得当前位置，请检查系统定位开关"
    else -> throwable.message ?: "无法开始连续导航"
}
