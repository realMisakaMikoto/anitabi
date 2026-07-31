package cn.anitabi.navigator.ui.planner

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.text.format.DateFormat
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cn.anitabi.navigator.navigation.AndroidLocationProvider
import cn.anitabi.navigator.navigation.requestGoogleNavigationTerms
import cn.anitabi.navigator.core.model.EndPolicy
import cn.anitabi.navigator.core.model.PilgrimagePoint
import cn.anitabi.navigator.core.model.RouteObjective
import cn.anitabi.navigator.core.model.TourPlan
import cn.anitabi.navigator.core.model.TourLeg
import cn.anitabi.navigator.core.model.TransitRoutingPreference
import cn.anitabi.navigator.core.model.TransitTimeMode
import cn.anitabi.navigator.core.model.TransitTravelMode
import cn.anitabi.navigator.core.model.TravelMode
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion
import kotlin.math.abs
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
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
    var navigationTermsRequestInFlight by remember { mutableStateOf(false) }
    val startAfterPermissions: (TourPlan) -> Unit = { pending ->
        if (pending.mode == TravelMode.TRANSIT) {
            onStartNavigation(pending)
        } else {
            val activity = context.findActivity()
            if (activity == null) {
                viewModel.navigationPermissionDenied("无法打开 Google 导航条款，请重新打开应用后再试")
            } else if (!navigationTermsRequestInFlight) {
                navigationTermsRequestInFlight = true
                requestGoogleNavigationTerms(
                    activity = activity,
                    onReady = {
                        navigationTermsRequestInFlight = false
                        onStartNavigation(pending)
                    },
                    onError = { message ->
                        navigationTermsRequestInFlight = false
                        viewModel.navigationPermissionDenied(message)
                    },
                )
            }
        }
    }
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
            startAfterPermissions(pending)
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
            onTransitScheduleChange = viewModel::setTransitSchedule,
            onTransitPreferenceChange = viewModel::setTransitRoutingPreference,
            onTransitTravelModeToggle = viewModel::toggleTransitTravelMode,
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
                    startAfterPermissions(plan)
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

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
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

@OptIn(ExperimentalMaterial3Api::class)
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
    onTransitScheduleChange: (TransitTimeMode, LocalDate, LocalTime) -> Unit,
    onTransitPreferenceChange: (TransitRoutingPreference) -> Unit,
    onTransitTravelModeToggle: (TransitTravelMode) -> Unit,
    onDwellChange: (String) -> Unit,
    onGenerate: () -> Unit,
) {
    val context = LocalContext.current
    val zoneId = remember(state.transitZoneId) {
        runCatching { ZoneId.of(state.transitZoneId) }.getOrDefault(ZoneOffset.UTC)
    }
    val now = LocalDateTime.now(zoneId)
    val today = now.toLocalDate()
    val selectableTransitDates = remember(today) {
        val firstDate = today.minusDays(7)
        val firstDateMillis = firstDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val lastDateMillis = today.plusDays(100).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis in firstDateMillis..lastDateMillis

            override fun isSelectableYear(year: Int): Boolean = year in firstDate.year..today.plusDays(100).year
        }
    }
    var showScheduleSheet by remember { mutableStateOf(false) }
    var showTransitOptionsSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingTimeMode by remember { mutableStateOf(TransitTimeMode.DEPART_AT) }
    var pendingDate by remember(state.transitDate) { mutableStateOf(state.transitDate) }
    var pendingTime by remember(state.transitTime) { mutableStateOf(state.transitTime) }

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
                    googleRouteBetaNotice(state.mode)?.let { notice ->
                        Text(
                            notice,
                            color = MutedInk,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp),
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
                        TransitSettingRow(
                            icon = Icons.Rounded.Schedule,
                            label = "公交时间",
                            value = transitScheduleLabel(
                                mode = state.transitTimeMode,
                                date = state.transitDate,
                                time = state.transitTime,
                                today = today,
                            ),
                            enabled = !state.isLoading,
                            onClick = { showScheduleSheet = true },
                        )
                        TransitSettingRow(
                            icon = Icons.Rounded.Tune,
                            label = "公交选项",
                            value = transitOptionsSummaryLabel(
                                state.transitRoutingPreference,
                                state.transitTravelModes,
                            ),
                            enabled = !state.isLoading,
                            onClick = { showTransitOptionsSheet = true },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        OutlinedTextField(
                            value = state.dwellMinutesInput,
                            onValueChange = onDwellChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            label = { Text("每个景点停留（分钟）") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                        Text(
                            "时间按手机时区 ${zoneId.id} 解释",
                            color = MutedInk,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                item {
                    state.errorMessage?.let {
                        PlannerErrorCard(message = it, modifier = Modifier.padding(bottom = 8.dp))
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
                            Text(
                                loadingRouteLabel(state),
                                modifier = Modifier.padding(start = 10.dp),
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

    if (showScheduleSheet) {
        ModalBottomSheet(onDismissRequest = { showScheduleSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("选择公交时间", style = MaterialTheme.typography.titleLarge)
                Text(
                    "查询会按每个巡礼点的到达时间和停留时间继续衔接。",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TransitSheetChoice(
                    title = "现在出发",
                    subtitle = "查询时使用当前时间",
                    selected = state.transitTimeMode == TransitTimeMode.NOW,
                    onClick = {
                        val current = LocalDateTime.now(zoneId).withSecond(0).withNano(0)
                        onTransitScheduleChange(TransitTimeMode.NOW, current.toLocalDate(), current.toLocalTime())
                        showScheduleSheet = false
                    },
                )
                TransitSheetChoice(
                    title = "选择出发时间",
                    subtitle = "从指定日期和时间开始行程",
                    selected = state.transitTimeMode == TransitTimeMode.DEPART_AT,
                    onClick = {
                        val current = LocalDateTime.now(zoneId).withSecond(0).withNano(0)
                        pendingTimeMode = TransitTimeMode.DEPART_AT
                        pendingDate = if (state.transitTimeMode == TransitTimeMode.NOW) current.toLocalDate() else state.transitDate
                        pendingTime = if (state.transitTimeMode == TransitTimeMode.NOW) current.toLocalTime() else state.transitTime
                        showScheduleSheet = false
                        showDatePicker = true
                    },
                )
                TransitSheetChoice(
                    title = "选择到达时间",
                    subtitle = "寻找在指定时间前到达的行程",
                    selected = state.transitTimeMode == TransitTimeMode.ARRIVE_BY,
                    onClick = {
                        val current = LocalDateTime.now(zoneId).withSecond(0).withNano(0)
                        pendingTimeMode = TransitTimeMode.ARRIVE_BY
                        pendingDate = if (state.transitTimeMode == TransitTimeMode.NOW) current.toLocalDate() else state.transitDate
                        pendingTime = if (state.transitTimeMode == TransitTimeMode.NOW) current.toLocalTime() else state.transitTime
                        showScheduleSheet = false
                        showDatePicker = true
                    },
                )
            }
        }
    }

    if (showTransitOptionsSheet) {
        val selectedTravelModes = selectedTransitTravelModes(state.transitTravelModes)
        ModalBottomSheet(onDismissRequest = { showTransitOptionsSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("公交选项", style = MaterialTheme.typography.titleLarge)
                Text(
                    "这些是 Google 路线偏好。系统会尽量遵循，必要时仍可能返回其他交通方式。",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "交通方式（可多选）",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    allTransitTravelModes.forEach { mode ->
                        val selected = mode in selectedTravelModes
                        FilterChip(
                            selected = selected,
                            onClick = { onTransitTravelModeToggle(mode) },
                            enabled = !state.isLoading && (!selected || selectedTravelModes.size > 1),
                            label = { Text(transitTravelModeLabel(mode)) },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                        )
                    }
                }
                Text(
                    "至少保留一种交通方式。四项全选等同于不限制。",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "路线偏好",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 6.dp),
                )
                TransitPreferenceSheetChoice(
                    preference = TransitRoutingPreference.RECOMMENDED,
                    subtitle = "综合时间、步行和换乘",
                    selected = state.transitRoutingPreference == TransitRoutingPreference.RECOMMENDED,
                    onSelect = onTransitPreferenceChange,
                )
                TransitPreferenceSheetChoice(
                    preference = TransitRoutingPreference.LESS_WALKING,
                    subtitle = "优先减少步行接驳",
                    selected = state.transitRoutingPreference == TransitRoutingPreference.LESS_WALKING,
                    onSelect = onTransitPreferenceChange,
                )
                TransitPreferenceSheetChoice(
                    preference = TransitRoutingPreference.FEWER_TRANSFERS,
                    subtitle = "优先减少换乘次数",
                    selected = state.transitRoutingPreference == TransitRoutingPreference.FEWER_TRANSFERS,
                    onSelect = onTransitPreferenceChange,
                )
                Button(
                    onClick = { showTransitOptionsSheet = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
                ) {
                    Text("完成")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = pendingDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = selectableTransitDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    enabled = datePickerState.selectedDateMillis != null,
                    onClick = {
                        pendingDate = Instant.ofEpochMilli(requireNotNull(datePickerState.selectedDateMillis))
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                        showDatePicker = false
                        showTimePicker = true
                    },
                ) {
                    Text("下一步")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = pendingTime.hour,
            initialMinute = pendingTime.minute,
            is24Hour = DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(if (pendingTimeMode == TransitTimeMode.ARRIVE_BY) "选择到达时间" else "选择出发时间") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        onTransitScheduleChange(pendingTimeMode, pendingDate, pendingTime)
                        showTimePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TransitSettingRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Sand),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Vermilion)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                Text(label, color = MutedInk, style = MaterialTheme.typography.labelMedium)
                Text(value, color = Ink, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Rounded.ArrowDropDown, contentDescription = "展开$label", tint = Moss)
        }
    }
}

@Composable
private fun TransitSheetChoice(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, if (selected) Vermilion else Sand),
        shape = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
            }
            if (selected) {
                Icon(Icons.Rounded.Check, contentDescription = "已选择", tint = Vermilion)
            }
        }
    }
}

@Composable
private fun TransitPreferenceSheetChoice(
    preference: TransitRoutingPreference,
    subtitle: String,
    selected: Boolean,
    onSelect: (TransitRoutingPreference) -> Unit,
) {
    TransitSheetChoice(
        title = transitPreferenceLabel(preference),
        subtitle = subtitle,
        selected = selected,
        onClick = { onSelect(preference) },
    )
}

internal fun transitScheduleLabel(
    mode: TransitTimeMode,
    date: LocalDate,
    time: LocalTime,
    today: LocalDate,
): String {
    if (mode == TransitTimeMode.NOW) return "现在出发"
    val dateLabel = when {
        date == today -> "今天"
        date.year == today.year -> "${date.monthValue}月${date.dayOfMonth}日"
        else -> "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    }
    val timeLabel = time.format(DateTimeFormatter.ofPattern("HH:mm"))
    val action = if (mode == TransitTimeMode.ARRIVE_BY) "前到达" else "出发"
    return "$dateLabel $timeLabel $action"
}

internal fun transitPreferenceLabel(preference: TransitRoutingPreference): String = when (preference) {
    TransitRoutingPreference.RECOMMENDED -> "最佳路线"
    TransitRoutingPreference.LESS_WALKING -> "少步行"
    TransitRoutingPreference.FEWER_TRANSFERS -> "少换乘"
}

internal fun transitTravelModeLabel(mode: TransitTravelMode): String = when (mode) {
    TransitTravelMode.BUS -> "公交"
    TransitTravelMode.SUBWAY -> "地铁"
    TransitTravelMode.TRAIN -> "火车"
    TransitTravelMode.LIGHT_RAIL -> "轻轨"
}

internal fun transitTravelModesLabel(storedModes: Set<TransitTravelMode>): String {
    if (storedModes.isEmpty()) return "全部方式"
    return allTransitTravelModes
        .filter(storedModes::contains)
        .joinToString("、", transform = ::transitTravelModeLabel)
}

internal fun transitOptionsSummaryLabel(
    preference: TransitRoutingPreference,
    storedModes: Set<TransitTravelMode>,
): String = "${transitPreferenceLabel(preference)} · ${transitTravelModesLabel(storedModes)}"

private fun loadingRouteLabel(state: PlannerUiState): String {
    if (state.mode != TravelMode.TRANSIT || state.totalTransitSegments <= 0) return "正在生成路线"
    val completed = state.plannedTransitSegments.coerceIn(0, state.totalTransitSegments)
    return if (completed < state.totalTransitSegments) {
        "正在查询第 ${completed + 1}/${state.totalTransitSegments} 段"
    } else {
        "正在整理完整路线"
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
    val transitSections = remember(plan.legs) { groupTransitJourneySections(plan.legs) }
    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PlannerTopBar(title = "路线预览", onBack = onBack)
            RoutePreviewMap(
                plan = plan,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
            )
            if (plan.mode == TravelMode.TRANSIT) {
                TransitJourneySummaryCard(plan)
            } else {
                RouteSummary(plan)
            }
            state.errorMessage?.let {
                PlannerErrorCard(
                    message = it,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                googleRouteBetaNotice(plan.mode)?.let { notice ->
                    item {
                        Text(notice, color = MutedInk, style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (plan.mode == TravelMode.TRANSIT) {
                    item {
                        Text(
                            "公交行程时间线",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    itemsIndexed(transitSections, key = { index, _ -> "transit-section-$index" }) { index, section ->
                        TransitJourneySectionCard(
                            section = section,
                            index = index,
                            sectionCount = transitSections.size,
                            plan = plan,
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text("巡礼点顺序", style = MaterialTheme.typography.titleMedium)
                        Text("长按拖动调整顺序", color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                    }
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
                item {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text(
                            "Google Maps",
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
                PlannerBottomAction(
                    onClick = onApplyOrder,
                    enabled = !state.isLoading,
                    label = "按此顺序重新生成",
                    isLoading = state.isLoading,
                )
            } else {
                PlannerBottomAction(
                    onClick = onStartNavigation,
                    enabled = !state.isLoading,
                    label = if (plan.mode == TravelMode.TRANSIT) "开始公交行程" else "开始连续导航",
                    isLoading = false,
                )
            }
        }
    }
}

@Composable
private fun PlannerErrorCard(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null)
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text("暂时无法生成路线", fontWeight = FontWeight.Bold)
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PlannerBottomAction(
    onClick: () -> Unit,
    enabled: Boolean,
    label: String,
    isLoading: Boolean,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Vermilion),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
            } else {
                Text(label)
            }
        }
    }
}

@Composable
private fun TransitJourneySummaryCard(plan: TourPlan) {
    val transitLegs = plan.legs.mapNotNull(TourLeg::transit)
    val firstTransit = transitLegs.firstOrNull()
    val lastTransit = transitLegs.lastOrNull()
    val departure = plan.departureTime ?: firstTransit?.departureTime
    val arrival = plan.arrivalTime ?: lastTransit?.arrivalTime
    val walkingDistance = plan.legs.filter { it.mode == TravelMode.WALK }.sumOf(TourLeg::distanceMeters)
    val lineLabels = transitLegs.mapNotNull { transit ->
        transit.line?.takeIf(String::isNotBlank) ?: transitVehicleLabel(transit.vehicleMode)
    }.distinct()
    val visibleLineLabels = lineLabels.take(6)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Sand),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.DirectionsBus, contentDescription = null, tint = Vermilion)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    Text("当前公交方案", color = MutedInk, style = MaterialTheme.typography.labelMedium)
                    Text(
                        if (departure != null || arrival != null) {
                            "${formatTransitTime(departure, firstTransit?.departureTimeZone)} → " +
                                formatTransitTime(arrival, lastTransit?.arrivalTimeZone)
                        } else {
                            "完整步行与换乘行程"
                        },
                        color = Ink,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        transitPreferenceLabel(plan.transitRoutingPreference),
                        color = Vermilion,
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        transitTravelModesLabel(plan.transitTravelModes),
                        color = MutedInk,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Text(
                "全程约 ${formatDuration(plan.estimatedDurationSeconds)}（含景点停留） · " +
                    "乘车 ${transitLegs.size} 段 · 步行 ${formatDistance(walkingDistance)}",
                color = MutedInk,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (lineLabels.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    visibleLineLabels.forEach { label ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Text(
                                label,
                                color = Ink,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                    if (lineLabels.size > visibleLineLabels.size) {
                        Text(
                            "+${lineLabels.size - visibleLineLabels.size} 条线路",
                            color = MutedInk,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

private data class TransitJourneySection(
    val legs: List<TourLeg>,
    val destinationPointId: String?,
)

private fun groupTransitJourneySections(legs: List<TourLeg>): List<TransitJourneySection> {
    val result = mutableListOf<TransitJourneySection>()
    val pending = mutableListOf<TourLeg>()
    legs.forEach { leg ->
        pending += leg
        if (leg.destinationPointId != null) {
            result += TransitJourneySection(pending.toList(), leg.destinationPointId)
            pending.clear()
        }
    }
    if (pending.isNotEmpty()) result += TransitJourneySection(pending.toList(), null)
    return result
}

@Composable
private fun TransitJourneySectionCard(
    section: TransitJourneySection,
    index: Int,
    sectionCount: Int,
    plan: TourPlan,
) {
    val destination = section.destinationPointId?.let { pointId ->
        plan.selectedPoints.firstOrNull { it.id == pointId }?.name
    } ?: if (index == sectionCount - 1 && plan.endPolicy == EndPolicy.RETURN_TO_START) {
        "返回起点"
    } else {
        "下一巡礼点"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Sand),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = Vermilion,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(30.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    Text("前往 $destination", color = Ink, fontWeight = FontWeight.Bold)
                    Text(
                        "第 ${index + 1}/$sectionCount 段 · " +
                            "${formatDuration(section.legs.sumOf(TourLeg::durationSeconds))} · " +
                            formatDistance(section.legs.sumOf(TourLeg::distanceMeters)),
                        color = MutedInk,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            section.legs.forEach { leg ->
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 10.dp),
                    color = Sand,
                )
                TransitTimelineLegRow(leg)
            }
        }
    }
}

@Composable
private fun TransitTimelineLegRow(leg: TourLeg) {
    val transit = leg.transit
    val walking = leg.mode == TravelMode.WALK
    Row(verticalAlignment = Alignment.Top) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50),
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    if (walking) Icons.AutoMirrored.Rounded.DirectionsWalk else Icons.Rounded.DirectionsBus,
                    contentDescription = null,
                    tint = if (walking) Moss else Vermilion,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
        ) {
            Text(
                if (walking) "步行接驳" else transit?.let { it.line ?: transitVehicleLabel(it.vehicleMode) }
                    ?: "公共交通",
                color = Ink,
                fontWeight = FontWeight.SemiBold,
            )
            if (walking) {
                Text(
                    "约 ${formatDuration(leg.durationSeconds)} · ${formatDistance(leg.distanceMeters)}",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
                leg.steps.firstOrNull()?.instruction?.takeIf(String::isNotBlank)?.let { instruction ->
                    Text(instruction, color = MutedInk, style = MaterialTheme.typography.bodyMedium)
                }
            } else if (transit != null) {
                val departure = formatTransitTime(transit.departureTime, transit.departureTimeZone)
                val arrival = formatTransitTime(transit.arrivalTime, transit.arrivalTimeZone)
                Text("$departure → $arrival · ${formatDuration(leg.durationSeconds)}", color = MutedInk)
                transit.direction?.takeIf(String::isNotBlank)?.let { Text("开往 $it", color = MutedInk) }
                if (transit.departureStop != null || transit.arrivalStop != null) {
                    Text(
                        listOfNotNull(transit.departureStop, transit.arrivalStop).joinToString(" → "),
                        color = MutedInk,
                    )
                }
                transit.stopCount?.let { Text("途经 $it 站", color = MutedInk) }
                transit.departurePlatform?.let { Text("上车站台：$it", color = MutedInk) }
                transit.arrivalPlatform?.let { Text("下车站台：$it", color = MutedInk) }
                transit.intermediateStops.takeIf { it.isNotEmpty() }?.let { stops ->
                    Text("中途站：${stops.joinToString(" → ")}", color = MutedInk, maxLines = 2)
                }
                if (transit.cancelled) {
                    Text("该班次已取消，需要重新查询", color = Vermilion, fontWeight = FontWeight.Bold)
                } else if (transit.realtime) {
                    Text("含实时信息", color = MutedInk)
                }
            } else {
                Text(
                    "约 ${formatDuration(leg.durationSeconds)} · ${formatDistance(leg.distanceMeters)}",
                    color = MutedInk,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun transitVehicleLabel(vehicleMode: String): String {
    val normalized = vehicleMode.uppercase()
    return when {
        "SUBWAY" in normalized || "METRO" in normalized -> "地铁"
        "TRAM" in normalized || "LIGHT_RAIL" in normalized -> "有轨电车"
        "RAIL" in normalized || "TRAIN" in normalized -> "铁路"
        "BUS" in normalized -> "公交"
        "FERRY" in normalized -> "轮渡"
        else -> "公共交通"
    }
}

internal fun googleRouteBetaNotice(mode: TravelMode): String? = when (mode) {
    TravelMode.WALK, TravelMode.BIKE ->
        "Google 地图的步行和骑行路线仍为测试版，请以现场道路和交通规则为准。"
    TravelMode.DRIVE, TravelMode.TRANSIT -> null
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
