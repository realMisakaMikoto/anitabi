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
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cn.anitabi.navigator.BuildConfig
import cn.anitabi.navigator.ui.theme.Ink
import cn.anitabi.navigator.ui.theme.MutedInk
import cn.anitabi.navigator.ui.theme.Paper
import cn.anitabi.navigator.ui.theme.Sand
import cn.anitabi.navigator.ui.theme.Vermilion

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
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
                    Text("零预算 · 全应用内 · GPL-3.0-or-later", color = Sand)
                }
            }
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    AboutCard("隐私") {
                        Text("不含广告、分析、账号、云同步或位置日志。路线与进度只保存在本机。")
                        Text(
                            "规划或偏航重算时，必要坐标会发送给 ORS；规划或重算公交路线时发送给 Transitous。地图瓦片由 OpenFreeMap 提供。",
                            color = MutedInk,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                item {
                    AboutCard("地图与道路路线") {
                        SourceLink("OpenFreeMap · OpenMapTiles · © OpenStreetMap contributors") {
                            uriHandler.openUri("https://openfreemap.org/")
                        }
                        SourceLink("openrouteservice / HeiGIT") {
                            uriHandler.openUri("https://openrouteservice.org/")
                        }
                        Text(
                            "道路路线需要每位用户自己的免费 ORS Key；Key 经 Android Keystore 加密，仅保存在本机。",
                            color = MutedInk,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
                item {
                    AboutCard("公共交通") {
                        SourceLink("Transitous 数据来源与各运营方许可") {
                            uriHandler.openUri("https://transitous.org/sources/")
                        }
                        Text(
                            "仅在用户生成或重算公交路线时逐段请求，最多 8 个巡礼点；服务为 best-effort，覆盖范围不保证完整。",
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
                        Text("应用代码采用 GPL-3.0-or-later。第三方服务和数据分别遵循其自身条款。")
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
        Icon(Icons.Rounded.OpenInNew, contentDescription = "打开网页", tint = Vermilion)
    }
}
