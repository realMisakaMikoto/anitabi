package cn.anitabi.navigator.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.anitabi.navigator.core.model.NavigationState
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.navigation.NavigationViewModel
import cn.anitabi.navigator.ui.planner.RoutePreviewMap
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion

@Composable
fun NavigationRoute(viewModel: NavigationViewModel, onBack: (String?) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val plan = state.plan
    BackHandler { onBack(plan?.id) }
    if (plan == null) {
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(state.errorMessage ?: "没有正在进行的巡礼路线")
                OutlinedButton(onClick = { onBack(null) }, modifier = Modifier.padding(top = 12.dp)) {
                    Text("返回")
                }
            }
        }
        return
    }

    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Ink)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onBack(plan.id) }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("连续导航", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(plan.anime.nameCn ?: plan.anime.name, color = Sand, maxLines = 1)
                }
                Text(
                    text = state.progress?.state?.displayName().orEmpty(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 12.dp),
                )
            }

            RoutePreviewMap(
                plan = plan,
                currentLocation = state.currentLocation,
                followCurrentLocation = state.isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFFCF7))
                    .padding(18.dp),
            ) {
                val activeLeg = plan.legs.getOrNull(state.progress?.legIndex ?: 0)
                val targetName = activeLeg?.destinationPointId?.let { pointId ->
                    plan.selectedPoints.firstOrNull { it.id == pointId }?.name
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.MyLocation, contentDescription = null, tint = Vermilion)
                    Text(
                        state.instruction,
                        modifier = Modifier.padding(start = 10.dp),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    text = "剩余约 ${formatDistance(state.remainingDistanceMeters)} · " +
                        "第 ${(state.progress?.legIndex ?: 0) + 1}/${plan.legs.size.coerceAtLeast(1)} 段",
                    color = MutedInk,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = when {
                        state.progress?.state == NavigationState.COMPLETED -> "全部巡礼点已完成"
                        targetName != null -> "当前巡礼目标：$targetName"
                        plan.mode == TravelMode.TRANSIT -> "当前目标：完成本换乘段"
                        else -> "当前目标：返回起点"
                    },
                    color = MutedInk,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (state.isRerouting) {
                    Text("检测到持续偏航，正在重算剩余路线…", color = Vermilion, modifier = Modifier.padding(top = 8.dp))
                }
                state.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                Text(
                    "OpenFreeMap · OpenMapTiles · © OpenStreetMap contributors · " +
                        plan.legs.firstOrNull()?.source.orEmpty(),
                    color = MutedInk,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (plan.mode == TravelMode.TRANSIT) {
                    val transit = activeLeg?.transit
                    Text(
                        text = buildString {
                            append("换乘段 ${(state.progress?.legIndex ?: 0) + 1}/${plan.legs.size}")
                            transit?.line?.let { append(" · $it") }
                            transit?.direction?.let { append(" · 开往 $it") }
                        },
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    transit?.departurePlatform?.let {
                        Text("上车站台：$it", color = MutedInk, modifier = Modifier.padding(top = 4.dp))
                    }
                    transit?.arrivalPlatform?.let {
                        Text("下车站台：$it", color = MutedInk)
                    }
                    transit?.intermediateStops?.takeIf { it.isNotEmpty() }?.let {
                        Text("中途站：${it.joinToString(" → ")}", color = MutedInk, maxLines = 2)
                    }
                    if (transit?.cancelled == true) {
                        Text("该班次已取消，正在重算剩余行程", color = Vermilion, fontWeight = FontWeight.Bold)
                    } else if (transit?.realtime == true) {
                        Text("含实时信息", color = MutedInk)
                    }
                    OutlinedButton(
                        onClick = viewModel::refreshTransit,
                        enabled = !state.isRerouting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    ) {
                        Text("错过班次或严重延误：重算剩余行程")
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.stop()
                            onBack(plan.id)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text("结束导航")
                    }
                    Button(
                        onClick = viewModel::markArrived,
                        enabled = state.progress?.state == NavigationState.NAVIGATING,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Rounded.Flag, contentDescription = null)
                        Text("确认到达", modifier = Modifier.padding(start = 6.dp))
                    }
                }
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
