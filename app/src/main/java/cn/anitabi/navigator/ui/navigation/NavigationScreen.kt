package cn.anitabi.navigator.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.navigation.NavigationRuntimeState
import cn.anitabi.navigator.navigation.NavigationViewModel
import cn.anitabi.navigator.ui.map.NavigationMapView
import cn.anitabi.navigator.ui.planner.RoutePreviewMap
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Vermilion

private val WideNavigationBreakpoint = 840.dp

@Composable
fun NavigationRoute(viewModel: NavigationViewModel, onBack: (String?) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plan
    BackHandler { onBack(plan?.id) }

    if (plan == null) {
        MissingNavigationState(
            message = state.errorMessage ?: "没有正在进行的巡礼路线",
            onBack = { onBack(null) },
        )
        return
    }

    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            NavigationTopBar(
                plan = plan,
                navigationState = state.progress?.state,
                onBack = { onBack(plan.id) },
            )
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useSidePanel = maxWidth >= WideNavigationBreakpoint || maxWidth > maxHeight
                val hasTransitJourney = plan.mode == TravelMode.TRANSIT && plan.legs.isNotEmpty()
                val compactPanelHeight = minOf(maxHeight * 0.5f, 360.dp)

                if (useSidePanel) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationCanvas(
                            plan = plan,
                            state = state,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        NavigationDetailPanel(
                            plan = plan,
                            state = state,
                            onStop = {
                                viewModel.stop()
                                onBack(plan.id)
                            },
                            onArrived = viewModel::markArrived,
                            onRefreshTransit = viewModel::refreshTransit,
                            modifier = Modifier
                                .fillMaxHeight()
                                .widthIn(min = 360.dp, max = 440.dp),
                            transitDetailsScrollable = hasTransitJourney,
                            fillAvailableHeight = true,
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxSize()) {
                        NavigationCanvas(
                            plan = plan,
                            state = state,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                        NavigationDetailPanel(
                            plan = plan,
                            state = state,
                            onStop = {
                                viewModel.stop()
                                onBack(plan.id)
                            },
                            onArrived = viewModel::markArrived,
                            onRefreshTransit = viewModel::refreshTransit,
                            modifier = if (hasTransitJourney) {
                                Modifier
                                    .fillMaxWidth()
                                    .height(compactPanelHeight)
                            } else {
                                Modifier.fillMaxWidth()
                            },
                            transitDetailsScrollable = hasTransitJourney,
                            fillAvailableHeight = hasTransitJourney,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MissingNavigationState(message: String, onBack: () -> Unit) {
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
            OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp).heightIn(min = 50.dp)) {
                Text("返回")
            }
        }
    }
}

@Composable
private fun NavigationTopBar(
    plan: TourPlan,
    navigationState: NavigationState?,
    onBack: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = plan.anime.nameCn ?: plan.anime.name,
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "巡礼导航",
                    color = MutedInk,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            navigationState?.let { currentState ->
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.semantics {
                        stateDescription = "导航状态：${currentState.displayName()}"
                    },
                ) {
                    Text(
                        text = currentState.displayName(),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
        }
    }
}

@Composable
private fun NavigationCanvas(
    plan: TourPlan,
    state: NavigationRuntimeState,
    modifier: Modifier = Modifier,
) {
    when {
        plan.legs.isEmpty() && !state.isRunning -> {
            SavedTourRecoveryPanel(
                plan = plan,
                completedPointIds = state.progress?.completedPointIds.orEmpty(),
                modifier = modifier,
            )
        }

        plan.mode == TravelMode.TRANSIT -> {
            RoutePreviewMap(
                plan = plan,
                currentLocation = state.currentLocation,
                followCurrentLocation = state.isRunning,
                modifier = modifier,
            )
        }

        else -> {
            NavigationMapView(
                onMapReady = { map -> map.uiSettings.isMapToolbarEnabled = false },
                navigationUiEnabled = true,
                modifier = modifier,
            )
        }
    }
}

@Composable
internal fun NavigationDetailPanel(
    plan: TourPlan,
    state: NavigationRuntimeState,
    onStop: () -> Unit,
    onArrived: () -> Unit,
    onRefreshTransit: () -> Unit,
    modifier: Modifier = Modifier,
    transitDetailsScrollable: Boolean,
    fillAvailableHeight: Boolean,
) {
    val activeLeg = plan.legs.getOrNull(state.progress?.legIndex ?: 0)
    val targetName = activeLeg?.destinationPointId?.let { pointId ->
        plan.selectedPoints.firstOrNull { it.id == pointId }?.name
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 5.dp,
        modifier = modifier.testTag("navigation-control-panel"),
    ) {
        Column(
            modifier = if (fillAvailableHeight) Modifier.fillMaxSize() else Modifier.fillMaxWidth(),
        ) {
            if (fillAvailableHeight) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    NavigationSummary(
                        plan = plan,
                        state = state,
                        targetName = targetName,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    )
                    if (transitDetailsScrollable) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        TransitJourneyDetails(
                            activeLeg = activeLeg,
                            legIndex = state.progress?.legIndex ?: 0,
                            totalLegs = plan.legs.size,
                        )
                        OutlinedButton(
                            onClick = onRefreshTransit,
                            enabled = !state.isRerouting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text("重算剩余公交行程")
                        }
                    }
                }
            } else {
                NavigationSummary(
                    plan = plan,
                    state = state,
                    targetName = targetName,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                )
            }

            NavigationActions(
                state = state,
                onStop = onStop,
                onArrived = onArrived,
            )
        }
    }
}

@Composable
private fun NavigationSummary(
    plan: TourPlan,
    state: NavigationRuntimeState,
    targetName: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            Surface(
                color = Vermilion.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(
                    Icons.Rounded.MyLocation,
                    contentDescription = null,
                    tint = Vermilion,
                    modifier = Modifier.padding(8.dp).size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = state.instruction,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                Text(
                    text = "剩余约 ${formatDistance(state.remainingDistanceMeters)}  ·  " +
                        "第 ${(state.progress?.legIndex ?: 0) + 1}/${plan.legs.size.coerceAtLeast(1)} 段",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Text(
            text = when {
                state.progress?.state == NavigationState.COMPLETED -> "全部巡礼点已完成"
                targetName != null -> "当前目标：$targetName"
                plan.mode == TravelMode.TRANSIT -> "当前目标：完成本换乘段"
                else -> "当前目标：返回起点"
            },
            color = MutedInk,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (state.isRerouting) {
            Text(
                "检测到持续偏航，正在重算剩余路线…",
                color = Vermilion,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
        state.errorMessage?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
        Text(
            text = (if (plan.mode == TravelMode.TRANSIT) "Google Routes" else "Google Navigation") +
                plan.legs.firstOrNull()?.source?.let { "  ·  $it" }.orEmpty(),
            color = MutedInk,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun TransitJourneyDetails(
    activeLeg: TourLeg?,
    legIndex: Int,
    totalLegs: Int,
    modifier: Modifier = Modifier,
) {
    val transit = activeLeg?.transit
    val isWalkingConnector = activeLeg?.mode == TravelMode.WALK

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isWalkingConnector) {
                    Icons.AutoMirrored.Rounded.DirectionsWalk
                } else {
                    Icons.Rounded.DirectionsBus
                },
                contentDescription = null,
                tint = Vermilion,
            )
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    text = buildString {
                        append(if (isWalkingConnector) "步行接驳" else "公交行程")
                        append(" ${legIndex + 1}/$totalLegs")
                        transit?.line?.let { append("  ·  $it") }
                    },
                    color = Ink,
                    style = MaterialTheme.typography.titleMedium,
                )
                transit?.direction?.let { direction ->
                    Text("开往 $direction", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (transit != null) {
            TransitStop(
                label = "上车",
                name = transit.departureStop ?: "出发站",
                details = listOfNotNull(transit.departureTime, transit.departurePlatform?.let { "站台 $it" }),
                emphasized = true,
            )
            transit.stopCount?.let { stopCount ->
                Text("途经 $stopCount 站", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            }
            transit.intermediateStops.forEach { stop ->
                TransitStop(label = "途经", name = stop, details = emptyList(), emphasized = false)
            }
            TransitStop(
                label = "下车",
                name = transit.arrivalStop ?: "到达站",
                details = listOfNotNull(transit.arrivalTime, transit.arrivalPlatform?.let { "站台 $it" }),
                emphasized = true,
            )
            if (transit.cancelled) {
                Text(
                    "该班次已取消，正在重算剩余行程",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            } else if (transit.realtime) {
                Text("含实时班次信息", color = Moss, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Text(
                text = if (isWalkingConnector) "步行前往下一段行程" else "正在获取本段公交信息",
                color = MutedInk,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun TransitStop(
    label: String,
    name: String,
    details: List<String>,
    emphasized: Boolean,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(if (emphasized) 10.dp else 7.dp)
                .background(if (emphasized) Vermilion else MutedInk, CircleShape),
        )
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, color = MutedInk, style = MaterialTheme.typography.labelSmall)
            Text(
                text = name,
                color = Ink,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
            )
            if (details.isNotEmpty()) {
                Text(details.joinToString("  ·  "), color = MutedInk, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun NavigationActions(
    state: NavigationRuntimeState,
    onStop: () -> Unit,
    onArrived: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("结束导航")
            }
            Button(
                onClick = onArrived,
                enabled = state.isRunning && state.progress?.state == NavigationState.NAVIGATING,
                modifier = Modifier.weight(1f).heightIn(min = 50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.Flag, contentDescription = null)
                Text("确认到达", modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun SavedTourRecoveryPanel(
    plan: TourPlan,
    completedPointIds: Set<String>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(Paper),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        item {
            Text("已保存的巡礼顺序", style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(
                "开始导航后将按此顺序前往",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
            )
        }
        itemsIndexed(plan.orderedPoints, key = { _, point -> point.id }) { index, point ->
            val completed = point.id in completedPointIds
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = if (completed) "已完成" else "待前往" }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    color = if (completed) Moss.copy(alpha = 0.12f) else Vermilion.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(
                        1.dp,
                        if (completed) Moss.copy(alpha = 0.25f) else Vermilion.copy(alpha = 0.2f),
                    ),
                ) {
                    Text(
                        "${index + 1}",
                        color = if (completed) Moss else Vermilion,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
                Text(
                    point.name,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    color = Ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(if (completed) "已完成" else "待前往", color = if (completed) Moss else MutedInk)
            }
            if (index < plan.orderedPoints.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

private fun NavigationState.displayName(): String = when (this) {
    NavigationState.PLANNED -> "待出发"
    NavigationState.NAVIGATING -> "前往中"
    NavigationState.ARRIVING -> "已抵达"
    NavigationState.DWELLING -> "停留中"
    NavigationState.NEXT_STOP -> "下一站"
    NavigationState.COMPLETED -> "已完成"
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1000.0) "%.1f km".format(meters / 1000.0) else "${meters.toInt()} m"
