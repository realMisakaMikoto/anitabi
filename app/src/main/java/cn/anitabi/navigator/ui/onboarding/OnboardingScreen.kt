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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cn.anitabi.navigator.navigation.AndroidLocationProvider
import cn.anitabi.navigator.security.OrsKeyStore
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.Moss
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion

private const val TOTAL_STEPS = 3
private const val WELCOME_STEP = 0
private const val PERMISSION_STEP = 1
private const val KEY_STEP = 2
private const val ORS_ACCOUNT_URL = "https://account.heigit.org/"

@Composable
fun OnboardingRoute(
    keyStore: OrsKeyStore,
    onComplete: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uriHandler = LocalUriHandler.current
    val focusManager = LocalFocusManager.current
    var currentStep by rememberSaveable { mutableIntStateOf(WELCOME_STEP) }
    var hasLocationPermission by remember {
        mutableStateOf(AndroidLocationProvider.hasLocationPermission(context))
    }
    var hasNotificationPermission by remember {
        mutableStateOf(hasNotificationPermission(context))
    }
    var permissionAttempted by rememberSaveable { mutableStateOf(false) }
    var keyInput by rememberSaveable { mutableStateOf("") }
    var hasStoredKey by remember { mutableStateOf(keyStore.hasKey()) }
    var showKey by rememberSaveable { mutableStateOf(false) }
    var keyError by rememberSaveable { mutableStateOf<String?>(null) }

    fun refreshPermissions() {
        hasLocationPermission = AndroidLocationProvider.hasLocationPermission(context)
        hasNotificationPermission = hasNotificationPermission(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        permissionAttempted = true
        refreshPermissions()
        if (hasLocationPermission && hasNotificationPermission) currentStep = KEY_STEP
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
        keyError = null
    }

    val openOrsAccount: () -> Unit = {
        runCatching { uriHandler.openUri(ORS_ACCOUNT_URL) }
            .onFailure { keyError = "无法打开浏览器，请手动访问 account.heigit.org" }
    }
    val finishOnboarding: () -> Unit = {
        focusManager.clearFocus()
        refreshPermissions()
        when {
            !hasLocationPermission || !hasNotificationPermission -> {
                currentStep = PERMISSION_STEP
                permissionAttempted = true
            }
            keyInput.isBlank() && !hasStoredKey -> {
                keyError = "请先粘贴你自己的 ORS Key"
            }
            else -> runCatching {
                if (keyInput.isNotBlank()) {
                    keyStore.save(keyInput)
                    hasStoredKey = true
                }
                val readiness = OnboardingReadiness(
                    hasLocationPermission = hasLocationPermission,
                    hasNotificationPermission = hasNotificationPermission,
                    hasOrsKey = hasStoredKey,
                )
                check(readiness.canFinish)
                keyStore.markOnboardingComplete()
            }.onSuccess {
                keyInput = ""
                onComplete()
            }.onFailure {
                keyError = "Key 保存失败，请重试"
            }
        }
    }

    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                OnboardingProgress(currentStep = currentStep)
                Spacer(Modifier.height(28.dp))
                GuideTrail(currentStep = currentStep)
                Spacer(Modifier.height(28.dp))

                when (currentStep) {
                    WELCOME_STEP -> WelcomeStep(onContinue = { currentStep = PERMISSION_STEP })
                    PERMISSION_STEP -> PermissionStep(
                        hasLocationPermission = hasLocationPermission,
                        hasNotificationPermission = hasNotificationPermission,
                        permissionAttempted = permissionAttempted,
                        onRequestPermissions = {
                            refreshPermissions()
                            if (hasLocationPermission && hasNotificationPermission) {
                                currentStep = KEY_STEP
                            } else {
                                val missingPermissions = buildList {
                                    if (!hasLocationPermission) {
                                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                    if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                                permissionLauncher.launch(missingPermissions.toTypedArray())
                            }
                        },
                        onOpenSettings = { openAppSettings(context) },
                    )
                    else -> KeyStep(
                        keyInput = keyInput,
                        hasStoredKey = hasStoredKey,
                        showKey = showKey,
                        error = keyError,
                        onKeyChange = {
                            keyInput = it
                            keyError = null
                        },
                        onToggleKeyVisibility = { showKey = !showKey },
                        onOpenOrsAccount = openOrsAccount,
                        onFinish = finishOnboarding,
                    )
                }

                if (currentStep > WELCOME_STEP) {
                    TextButton(
                        onClick = {
                            currentStep -= 1
                            keyError = null
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
            Text(
                "第 ${currentStep + 1} 步，共 $TOTAL_STEPS 步",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
            )
        }
        LinearProgressIndicator(
            progress = { (currentStep + 1f) / TOTAL_STEPS },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
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
        TrailLine(currentStep >= KEY_STEP)
        GuideStop(Icons.Rounded.Key, "Key", currentStep >= KEY_STEP)
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
            modifier = Modifier
                .size(48.dp)
                .background(if (active) Vermilion else Sand, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (active) Color.White else MutedInk,
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) Ink else MutedInk,
            modifier = Modifier.padding(top = 6.dp),
        )
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
        "这个导览只出现一次。完成权限和个人 ORS Key 设置后，才能进入地图。",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedInk,
        modifier = Modifier.padding(top = 10.dp),
    )
    GuideCard(
        title = "权限、费用与隐私",
        body = "定位用于当前位置和导航，通知用于锁屏与后台导航；道路路线使用你自己的免费 ORS Key。Key 加密只存本机，应用没有广告、分析或位置日志。",
        modifier = Modifier.padding(top = 24.dp),
    )
    PrimaryButton(
        text = "开始设置",
        onClick = onContinue,
        modifier = Modifier.testTag("onboarding-start"),
    )
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
        "系统会依次显示权限弹窗。Anitabi 只会在功能需要时使用它们。",
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
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 14.dp),
        )
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(52.dp),
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
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = if (granted) Moss else Vermilion)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
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
private fun KeyStep(
    keyInput: String,
    hasStoredKey: Boolean,
    showKey: Boolean,
    error: String?,
    onKeyChange: (String) -> Unit,
    onToggleKeyVisibility: () -> Unit,
    onOpenOrsAccount: () -> Unit,
    onFinish: () -> Unit,
) {
    Text(
        "领取你的免费路线 Key",
        style = MaterialTheme.typography.headlineMedium,
        color = Ink,
        modifier = Modifier.testTag("onboarding-key-step"),
    )
    Text(
        "道路路线由 openrouteservice 提供。每位用户都要使用自己的免费 Standard Key。",
        style = MaterialTheme.typography.bodyLarge,
        color = MutedInk,
        modifier = Modifier.padding(top = 10.dp),
    )
    GuideCard(
        title = "获取方法",
        body = "1. 打开 HeiGIT 并注册、验证邮箱\n2. 登录后接受服务条款\n3. 在 Dashboard 复制 Standard API Key",
        modifier = Modifier.padding(top = 24.dp),
    )
    OutlinedButton(
        onClick = onOpenOrsAccount,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .height(52.dp)
            .testTag("onboarding-open-ors"),
        shape = RoundedCornerShape(10.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
        Text("打开 HeiGIT 申请页", modifier = Modifier.padding(start = 8.dp))
    }
    OutlinedTextField(
        value = keyInput,
        onValueChange = onKeyChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .testTag("onboarding-key-input"),
        label = { Text("ORS API Key（必填）") },
        placeholder = {
            Text(if (hasStoredKey) "本机已有 Key；留空即可继续" else "粘贴你的 ORS Key")
        },
        supportingText = {
            Text(if (hasStoredKey) "已检测到本机保存的 Key" else "保存前默认隐藏；不会发送到项目或聊天")
        },
        isError = error != null,
        visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleKeyVisibility) {
                Icon(
                    if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (showKey) "隐藏 Key" else "显示 Key",
                )
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onFinish() }),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
    )
    error?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .padding(top = 6.dp)
                .testTag("onboarding-key-error"),
        )
    }
    PrimaryButton(
        text = if (hasStoredKey && keyInput.isBlank()) "使用已保存的 Key 并进入" else "安全保存并进入",
        onClick = onFinish,
        modifier = Modifier.testTag("onboarding-key-submit"),
    )
}

@Composable
private fun GuideCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedInk,
                modifier = Modifier.padding(top = 6.dp),
            )
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
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp)
            .height(52.dp),
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
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${context.packageName}".toUri(),
        ),
    )
}
