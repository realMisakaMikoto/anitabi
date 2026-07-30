package cn.anitabi.navigator.ui.planner

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.anitabi.navigator.navigation.AndroidLocationProvider
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion
import kotlin.math.abs
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@Composable
fun PlannerRoute(
    viewModel: PlannerViewModel,
    onBack: () -> Unit,
    onStartNavigation: (TourPlan) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingNavigationPlan by remember { mutableStateOf<TourPlan?>(null) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.any { it }) viewModel.setUseCurrentLocation() else viewModel.locationPermissionDenied()
    }
    val navigationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val pending = pendingNavigationPlan
        pendingNavigationPlan = null
        val hasLocation = AndroidLocationProvider.hasLocationPermission(context)
        val hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val permissionError = navigationPermissionError(hasLocation, hasNotifications)
        if (pending != null && permissionError == null) {
            onStartNavigation(pending)
        } else if (permissionError != null) {
            viewModel.navigationPermissionDenied(permissionError)
        }
    }
    val plan = state.plan
    if (plan == null) {
        PlannerSettingsScreen(
            state = state,
            onBack = onBack,
            onModeChange = viewModel::setMode,
            onObjectiveChange = viewModel::setObjective,
            onEndPolicyChange = viewModel::setEndPolicy,
            onStartChange = viewModel::setStartPoint,
            onUseCurrentLocation = {
                if (AndroidLocationProvider.hasLocationPermission(context)) {
                    viewModel.setUseCurrentLocation()
                } else {
                    locationPermissionLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }
            },
            onFixedEndChange = viewModel::setFixedEndPoint,
            onDepartureDateChange = viewModel::setDepartureDate,
            onDepartureTimeChange = viewModel::setDepartureTime,
            onDwellChange = viewModel::setDwellMinutes,
            onGenerate = viewModel::generate,
        )
    } else {
        RoutePreviewScreen(
            state = state,
            plan = plan,
            onBack = viewModel::clearPlan,
            onMove = viewModel::moveDraft,
            onApplyOrder = viewModel::applyManualOrder,
            onStartNavigation = {
                val hasLocation = AndroidLocationProvider.hasLocationPermission(context)
                val hasNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (hasLocation && hasNotifications) {
                    onStartNavigation(plan)
                } else {
                    pendingNavigationPlan = plan
                    val permissions = buildList {
                        if (!hasLocation) {
                            add(Manifest.permission.ACCESS_FINE_LOCATION)
                            add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        }
                        if (!hasNotifications) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    navigationPermissionLauncher.launch(permissions.toTypedArray())
                }
            },
        )
    }
}

internal fun navigationPermissionError(
    hasLocation: Boolean,
    hasNotifications: Boolean,
): String? = when {
    !hasLocation && !hasNotifications -> "需要定位和通知权限才能开始导航"
    !hasLocation -> "需要定位权限才能开始导航"
    !hasNotifications -> "需要通知权限才能在锁屏和后台持续导航"
    else -> null
}

@Composable
private fun PlannerSettingsScreen(
    state: PlannerUiState,
    onBack: () -> Unit,
    onModeChange: (TravelMode) -> Unit,
    onObjectiveChange: (RouteObjective) -> Unit,
    onEndPolicyChange: (EndPolicy) -> Unit,
    onStartChange: (String) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onFixedEndChange: (String) -> Unit,
    onDepartureDateChange: (String) -> Unit,
    onDepartureTimeChange: (String) -> Unit,
    onDwellChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlannerTopBar(title = "编排一日路线", onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    SectionTitle("出行方式", "总行程点数不限，单次请求会自动安全分批")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ModeChip(TravelMode.DRIVE, state.mode, "驾车", onModeChange)
                        ModeChip(TravelMode.BIKE, state.mode, "骑行", onModeChange)
                        ModeChip(TravelMode.WALK, state.mode, "步行", onModeChange)
                        ModeChip(
                            TravelMode.TRANSIT,
                            state.mode,
                            "公交",
                            onModeChange,
                            enabled = true,
                        )
                    }
                }

                item {
                    SectionTitle("起点", "可从当前位置或某个已选巡礼点出发")
                    ChoiceChip("当前位置", state.useCurrentLocation, onUseCurrentLocation)
                    PointChoices(
                        points = state.selectedPoints,
                        selectedId = if (state.useCurrentLocation) null else state.startPointId,
                        onSelect = onStartChange,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                item {
                    SectionTitle("终点", "自由结束、指定终点或返回起点")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ChoiceChip("自由", state.endPolicy == EndPolicy.OPEN) { onEndPolicyChange(EndPolicy.OPEN) }
                        ChoiceChip("指定", state.endPolicy == EndPolicy.FIXED) { onEndPolicyChange(EndPolicy.FIXED) }
                        ChoiceChip("返回起点", state.endPolicy == EndPolicy.RETURN_TO_START) {
                            onEndPolicyChange(EndPolicy.RETURN_TO_START)
                        }
                    }
                    if (state.endPolicy == EndPolicy.FIXED) {
                        PointChoices(
                            points = state.selectedPoints.filterNot { it.id == state.startPointId },
                            selectedId = state.fixedEndPointId,
                            onSelect = onFixedEndChange,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }

                if (state.mode != TravelMode.TRANSIT) {
                    item {
                        SectionTitle("优化目标", "路线矩阵分批计算后由手机本地排序")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip("预计最快", state.objective == RouteObjective.FASTEST) {
                                onObjectiveChange(RouteObjective.FASTEST)
                            }
                            ChoiceChip("距离最短", state.objective == RouteObjective.SHORTEST) {
                                onObjectiveChange(RouteObjective.SHORTEST)
                            }
                        }
                    }
                } else {
                    item {
                        SectionTitle("出发与停留", "公交按每站到达时间串联后续班次")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = state.departureDate,
                                onValueChange = onDepartureDateChange,
                                modifier = Modifier.weight(1f),
                                label = { Text("日期") },
                                placeholder = { Text("2026-07-29") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = state.departureTime,
                                onValueChange = onDepartureTimeChange,
                                modifier = Modifier.weight(0.72f),
                                label = { Text("时间") },
                                placeholder = { Text("09:00") },
                                singleLine = true,
                            )
                        }
                        OutlinedTextField(
                            value = state.dwellMinutesInput,
                            onValueChange = onDwellChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            label = { Text("每个景点停留（分钟）") },
                            singleLine = true,
                        )
                    }
                }

                item {
                    state.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    Button(
                        onClick = onGenerate,
                        enabled = !state.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Rounded.Route, contentDescription = null)
                            Text("生成完整路线", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutePreviewScreen(
    state: PlannerUiState,
    plan: TourPlan,
    onBack: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onApplyOrder: () -> Unit,
    onStartNavigation: () -> Unit,
) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlannerTopBar(title = "路线预览", onBack = onBack)
            RoutePreviewMap(
                plan = plan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            )
            RouteSummary(plan)
            state.errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    Text("长按拖动调整顺序", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                }
                itemsIndexed(state.draftOrder, key = { _, point -> point.id }) { index, point ->
                    ReorderPointCard(
                        point = point,
                        index = index,
                        locked = point.id == state.startPointId ||
                            (state.endPolicy == EndPolicy.FIXED && index == state.draftOrder.lastIndex),
                        onMove = onMove,
                    )
                }
                if (plan.mode == TravelMode.TRANSIT) {
                    item {
                        Text(
                            "完整步行与换乘行程",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                    itemsIndexed(plan.legs, key = { index, _ -> "transit-leg-$index" }) { index, leg ->
                        TransitLegCard(leg = leg, index = index, legCount = plan.legs.size)
                    }
                }
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "OpenFreeMap · OpenMapTiles · © OpenStreetMap contributors",
                            color = MutedInk,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        plan.attribution.forEach { attribution ->
                            Text(attribution, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            if (state.orderChanged) {
                Button(
                    onClick = onApplyOrder,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("按此顺序重新生成")
                    }
                }
            } else {
                Button(
                    onClick = onStartNavigation,
                    enabled = !state.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
                ) {
                    Text("开始连续导航")
                }
            }
        }
    }
}

@Composable
private fun TransitLegCard(leg: TourLeg, index: Int, legCount: Int) {
    val transit = leg.transit
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        border = BorderStroke(1.dp, Sand),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DirectionsBus, contentDescription = null, tint = Moss)
                Text(
                    text = buildString {
                        append("第 ${index + 1}/$legCount 段 · ")
                        append(transit?.line ?: transit?.vehicleMode ?: "步行")
                        transit?.direction?.let { append(" · 开往 $it") }
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(
                "${formatTransitTime(transit?.departureTime, transit?.departureTimeZone)} → " +
                    "${formatTransitTime(transit?.arrivalTime, transit?.arrivalTimeZone)}" +
                    " · ${formatDistance(leg.distanceMeters)}",
                color = MutedInk,
                modifier = Modifier.padding(top = 6.dp),
            )
            transit?.departurePlatform?.let { Text("上车站台：$it", color = MutedInk) }
            transit?.arrivalPlatform?.let { Text("下车站台：$it", color = MutedInk) }
            transit?.intermediateStops?.takeIf { it.isNotEmpty() }?.let {
                Text("中途站：${it.joinToString(" → ")}", color = MutedInk, maxLines = 2)
            }
            if (transit?.cancelled == true) {
                Text("该班次已取消，开始导航后会自动重算", color = Vermilion, fontWeight = FontWeight.Bold)
            } else if (transit?.realtime == true) {
                Text("含实时信息", color = MutedInk)
            }
        }
    }
}

@Composable
private fun ReorderPointCard(
    point: PilgrimagePoint,
    index: Int,
    locked: Boolean,
    onMove: (Int, Int) -> Unit,
) {
    var dragDistance by remember { mutableFloatStateOf(0f) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        border = BorderStroke(1.dp, Sand),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(index, locked) {
                if (!locked) {
                    detectDragGesturesAfterLongPress(
                        onDragEnd = { dragDistance = 0f },
                        onDragCancel = { dragDistance = 0f },
                    ) { change, dragAmount ->
                        change.consume()
                        dragDistance += dragAmount.y
                        if (abs(dragDistance) >= 48f) {
                            onMove(index, index + if (dragDistance > 0) 1 else -1)
                            dragDistance = 0f
                        }
                    }
                }
            },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Vermilion, RoundedCornerShape(50)),
                contentAlignment = Alignment.Center,
            ) {
                Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(point.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "%.5f, %.5f".format(point.coordinate.latitude, point.coordinate.longitude),
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Icon(
                if (locked) Icons.Rounded.Lock else Icons.Rounded.DragHandle,
                contentDescription = null,
                tint = if (locked) Vermilion else Moss,
            )
        }
    }
}

@Composable
private fun RouteSummary(plan: TourPlan) {
    val totalDistance = plan.legs.sumOf { it.distanceMeters }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryValue("${plan.orderedPoints.size} 站", "巡礼点")
        SummaryValue(formatDuration(plan.estimatedDurationSeconds), "预计用时")
        SummaryValue(if (totalDistance > 0) "%.1f km".format(totalDistance / 1000) else "公交", "总距离")
    }
}

@Composable
private fun SummaryValue(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = Sand, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlannerTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
        }
        Text(title, color = Color.White, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(subtitle, color = MutedInk, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
}

@Composable
private fun ModeChip(
    mode: TravelMode,
    selected: TravelMode,
    label: String,
    onSelect: (TravelMode) -> Unit,
    enabled: Boolean = true,
) {
    val icon = when (mode) {
        TravelMode.DRIVE -> Icons.Rounded.DirectionsCar
        TravelMode.BIKE -> Icons.AutoMirrored.Rounded.DirectionsBike
        TravelMode.WALK -> Icons.AutoMirrored.Rounded.DirectionsWalk
        TravelMode.TRANSIT -> Icons.Rounded.DirectionsBus
    }
    FilterChip(
        selected = selected == mode,
        onClick = { onSelect(mode) },
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
    )
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun PointChoices(
    points: List<PilgrimagePoint>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        points.forEach { point ->
            ChoiceChip(point.name, point.id == selectedId) { onSelect(point.id) }
        }
    }
}

private fun formatDuration(seconds: Double): String {
    val totalMinutes = (seconds / 60).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun formatTransitTime(value: String?, timeZone: String?): String = value?.let {
    runCatching {
        val parsed = OffsetDateTime.parse(it)
        val localTime = timeZone?.let(ZoneId::of)?.let(parsed::atZoneSameInstant)?.toLocalTime()
            ?: parsed.toLocalTime()
        localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }.getOrDefault(it)
} ?: "时间未知"

private fun formatDistance(meters: Double): String =
    if (meters >= 1000.0) "%.1f km".format(meters / 1000.0) else "${meters.toInt()} m"
