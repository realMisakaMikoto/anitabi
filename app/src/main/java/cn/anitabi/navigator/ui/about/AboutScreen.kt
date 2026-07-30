package cn.anitabi.navigator.ui.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.anitabi.navigator.BuildConfig
import cn.anitabi.navigator.telemetry.TelemetryConsentController
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    telemetryConsentController: TelemetryConsentController,
) {
    val uriHandler = LocalUriHandler.current
    var telemetryConsent by remember(telemetryConsentController) {
        mutableStateOf(telemetryConsentController.currentConsent())
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
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
                }
                Column {
                    Text("关于巡礼手帖", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text("Google 路线 · 自建配额保护 · GPL-3.0-or-later", color = Sand)
                }
            }
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    AboutCard("隐私") {
                        Text("不含广告或云同步。路线与进度只保存在本机；路线响应不会持久化。")
                        Text(
                            "规划或偏航重算时，必要坐标、模式和出发时间会经自建服务发送给 Google。Firebase 匿名身份不需要邮箱、姓名或密码；Analytics 与 Crashlytics 默认关闭。",
                            color = MutedInk,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                item {
                    AboutCard("可选遥测") {
                        Text(
                            "两项默认关闭，分别选择加入，可随时撤回。不会记录坐标、动漫名、搜索词或路线正文。",
                            color = MutedInk,
                        )
                        TelemetryConsentRow(
                            title = "匿名使用分析",
                            description = "允许 Firebase Analytics 进行基础测量；应用自定义事件仅限版本、设备能力、模式、点数区间、延迟区间与错误类型。",
                            checked = telemetryConsent.analyticsEnabled,
                            onCheckedChange = { enabled ->
                                telemetryConsentController.setAnalyticsConsent(enabled)
                                telemetryConsent = telemetryConsent.copy(analyticsEnabled = enabled)
                            },
                        )
                        TelemetryConsentRow(
                            title = "崩溃报告",
                            description = "允许 Firebase Crashlytics 在崩溃后发送技术报告；关闭后会立即删除尚未发送的报告，并在下次启动完全停止采集。",
                            checked = telemetryConsent.crashlyticsEnabled,
                            onCheckedChange = { enabled ->
                                telemetryConsentController.setCrashlyticsConsent(enabled)
                                telemetryConsent = telemetryConsent.copy(crashlyticsEnabled = enabled)
                            },
                        )
                    }
                }
                item {
                    AboutCard("地图、路线与公共交通") {
                        SourceLink("Google Navigation SDK 与 Routes API") {
                            uriHandler.openUri("https://developers.google.com/maps/documentation/navigation/android-sdk")
                        }
                        SourceLink("Firebase") {
                            uriHandler.openUri("https://firebase.google.com/")
                        }
                        Text(
                            "道路导航使用 Navigation SDK；路线矩阵、道路预览与相邻两点公交使用 Routes API。无需用户填写 API Key，达到项目硬额度后停止请求。",
                            color = MutedInk,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        Text(
                            "公交仅按相邻两点逐段请求；没有路线时会明确提示，不会生成猜测路线。",
                            color = MutedInk,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                item {
                    AboutCard("动漫与巡礼数据") {
                        SourceLink("Bangumi API") { uriHandler.openUri("https://bangumi.github.io/api/") }
                        SourceLink("Anitabi API · CC BY-NC-SA 4.0") {
                            uriHandler.openUri("https://github.com/anitabi/anitabi.cn-document/blob/main/api.md")
                        }
                        Text("只缓存用户实际访问的作品；截图旁保留原始来源和链接。", color = MutedInk)
                    }
                }
                item {
                    AboutCard("开源与联系") {
                        Text(
                            "应用代码采用 GPL-3.0-or-later，并附仅用于 Google Navigation/Firebase SDK 的窄范围链接例外；项目自有代码仍保持开源。第三方服务和数据分别遵循其自身条款。",
                        )
                        SourceLink("源代码、GPL 与链接例外") {
                            uriHandler.openUri("https://github.com/realMisakaMikoto/anitabi/blob/main/LICENSE")
                        }
                        SourceLink("项目联系人：realMisakaMikoto") {
                            uriHandler.openUri("https://github.com/realMisakaMikoto")
                        }
                        Text("版本 ${BuildConfig.VERSION_NAME}", color = MutedInk)
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryConsentRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(description, color = MutedInk, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}

@Composable
private fun AboutCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFCF7)),
        border = BorderStroke(1.dp, Sand),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SourceLink(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Vermilion, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "打开网页", tint = Vermilion)
    }
}
