package cn.anitabi.navigator.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.anitabi.navigator.navigation.AndroidLocationProvider
import cn.anitabi.navigator.security.AppSettingsStore
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion

private const val TOTAL_STEPS = 3
private const val WELCOME_STEP = 0
private const val PERMISSION_STEP = 1
private const val SERVICE_STEP = 2

@Composable
fun OnboardingRoute(
    settingsStore: AppSettingsStore,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var currentStep by rememberSaveable { mutableIntStateOf(WELCOME_STEP) }
    var hasLocationPermission by remember {
        mutableStateOf(AndroidLocationProvider.hasLocationPermission(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var permissionAttempted by rememberSaveable { mutableStateOf(false) }
    var setupError by rememberSaveable { mutableStateOf<String?>(null) }

    fun refreshPermissions() {
        hasLocationPermission = AndroidLocationProvider.hasLocationPermission(context)
        hasNotificationPermission = hasNotificationPermission(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionAttempted = true
        refreshPermissions()
        if (hasLocationPermission && hasNotificationPermission) currentStep = SERVICE_STEP
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshPermissions()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    BackHandler(enabled = currentStep > WELCOME_STEP) {
        currentStep -= 1
        setupError = null
    }

    val finishOnboarding: () -> Unit = {
        refreshPermissions()
        val readiness = OnboardingReadiness(
            hasLocationPermission = hasLocationPermission,
            hasNotificationPermission = hasNotificationPermission,
        )
        if (!readiness.canFinish) {
            currentStep = PERMISSION_STEP
            permissionAttempted = true
        } else {
            runCatching { settingsStore.markOnboardingComplete() }
                .onSuccess { onComplete() }
                .onFailure { setupError = "设置无法保存，请释放设备空间后重试" }
        }
    }

    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                OnboardingProgress(currentStep)
                Spacer(Modifier.height(28.dp))
                GuideTrail(currentStep)
                Spacer(Modifier.height(28.dp))

                when (currentStep) {
                    WELCOME_STEP -> WelcomeStep { currentStep = PERMISSION_STEP }
                    PERMISSION_STEP -> PermissionStep(
                        hasLocationPermission = hasLocationPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        permissionAttempted = permissionAttempted,
                        onRequestPermissions = {
                            refreshPermissions()
                            if (hasLocationPermission && hasNotificationPermission) {
                                currentStep = SERVICE_STEP
                            } else {
                                val missingPermissions = buildList {
                                    if (!hasLocationPermission) {
                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                    if (!hasNotificationPermission &&
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                                    ) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                permissionLauncher.launch(missingPermissions.toTypedArray())
                            }
                        },
                        onOpenSettings = { openAppSettings(context) },
                    )
                    else -> ServiceStep(error = setupError, onFinish = finishOnboarding)
                }

                if (currentStep > WELCOME_STEP) {
                    TextButton(
                        onClick = {
                            currentStep -= 1
                            setupError = null
                        },
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Text("返回上一步", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingProgress(currentStep: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "初次使用设置",
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            Text("第 ${currentStep + 1} 步，共 $TOTAL_STEPS 步", color = MutedInk)
        }
        LinearProgressIndicator(
            progress = { (currentStep + 1f) / TOTAL_STEPS },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = Vermilion,
            trackColor = Sand,
        )
    }
}

@Composable
private fun GuideTrail(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GuideStop(Icons.Rounded.Map, "了解", currentStep >= WELCOME_STEP)
        TrailLine(currentStep >= PERMISSION_STEP)
        GuideStop(Icons.Rounded.LocationOn, "权限", currentStep >= PERMISSION_STEP)
        TrailLine(currentStep >= SERVICE_STEP)
        GuideStop(Icons.Rounded.CloudDone, "服务", currentStep >= SERVICE_STEP)
    }
}

@Composable
private fun GuideStop(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(48.dp).background(if (active) Vermilion else Sand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = if (active) Color.White else MutedInk)
        }
        Text(label, color = if (active) Ink else MutedInk, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun RowScope.TrailLine(active: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 8.dp)
            .height(2.dp)
            .background(if (active) Vermilion else Sand),
    )
}

@Composable
private fun WelcomeStep(onContinue: () -> Unit) {
    Text("出发前，先把旅途准备好", style = MaterialTheme.typography.headlineMedium, color = Ink)
    Text(
        "这个导览只出现一次。完成必要权限并了解路线服务后，即可进入地图。",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedInk,
        modifier = Modifier.padding(top = 10.dp),
    )
    GuideCard(
        title = "权限、费用与隐私",
        body = "定位用于当前位置与导航，通知用于锁屏和后台导航。路线通过项目自建服务请求 Google；达到免费额度上限后会停止请求，不会自动产生额外费用。",
        modifier = Modifier.padding(top = 24.dp),
    )
    PrimaryButton("开始设置", onContinue, Modifier.testTag("onboarding-start"))
}

@Composable
private fun PermissionStep(
    hasLocationPermission: Boolean,
    hasNotificationPermission: Boolean,
    permissionAttempted: Boolean,
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val permissionsReady = hasLocationPermission && hasNotificationPermission
    Text("一次授权，路上少打断", style = MaterialTheme.typography.headlineMedium, color = Ink)
    Text(
        "系统会依次显示权限弹窗。巡礼手帳只会在功能需要时使用它们。",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedInk,
        modifier = Modifier.padding(top = 10.dp),
    )
    PermissionStatus(
        icon = Icons.Rounded.LocationOn,
        title = "定位",
        description = "选择当前位置、导航和偏航重算",
        granted = hasLocationPermission,
        modifier = Modifier.padding(top = 24.dp),
    )
    PermissionStatus(
        icon = Icons.Rounded.NotificationsActive,
        title = "通知",
        description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "锁屏或切到后台时继续显示导航"
        } else {
            "此 Android 版本无需单独授权"
        },
        granted = hasNotificationPermission,
        modifier = Modifier.padding(top = 12.dp),
    )
    if (permissionAttempted && !permissionsReady) {
        Text(
            onboardingPermissionError(hasLocationPermission, hasNotificationPermission).orEmpty(),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 14.dp),
        )
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(52.dp),
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = null)
            Text("打开系统设置", modifier = Modifier.padding(start = 8.dp))
        }
    }
    PrimaryButton(
        text = if (permissionsReady) "权限已就绪，继续" else "授权定位与通知",
        onClick = onRequestPermissions,
        modifier = Modifier.testTag(
            if (permissionsReady) "onboarding-permission-continue" else "onboarding-permission-request",
        ),
    )
}

@Composable
private fun PermissionStatus(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = if (granted) Moss else Vermilion)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MutedInk)
            }
            Icon(
                if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = if (granted) "$title 已授权" else "$title 未授权",
                tint = if (granted) Moss else MutedInk,
            )
        }
    }
}

@Composable
private fun ServiceStep(error: String?, onFinish: () -> Unit) {
    Text(
        "路线服务已准备好",
        style = MaterialTheme.typography.headlineMedium,
        color = Ink,
        modifier = Modifier.testTag("onboarding-service-step"),
    )
    Text(
        "你不需要申请或填写 API Key。首次生成路线时，应用会自动创建 Firebase 匿名标识用于服务端配额。",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedInk,
        modifier = Modifier.padding(top = 10.dp),
    )
    GuideCard(
        title = "会发送什么",
        body = "规划路线时只发送坐标、出行方式和必要的出发时间；不会发送动漫名、搜索词或路线正文日志。新的路线需要联网。",
        modifier = Modifier.padding(top = 24.dp),
    )
    GuideCard(
        title = "匿名与可选遥测",
        body = "匿名标识不需要邮箱、姓名或密码。Analytics 与 Crashlytics 默认关闭，只有你以后明确同意才会启用，并可随时撤回。",
        modifier = Modifier.padding(top = 12.dp),
    )
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 14.dp).testTag("onboarding-service-error"),
        )
    }
    PrimaryButton(
        text = "确认并进入地图",
        onClick = onFinish,
        modifier = Modifier.testTag("onboarding-service-submit"),
    )
}

@Composable
private fun GuideCard(title: String, body: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(body, color = MutedInk, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().padding(top = 24.dp).height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Ink),
    ) {
        Text(text)
    }
}

private fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()),
    )
}
