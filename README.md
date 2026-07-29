# 巡礼手帖（Anitabi Navigator）

一个零预算、无自建后端、全程留在应用内的 Android 动漫圣地巡礼导航器。用户可以搜索 Bangumi 作品、从 Anitabi 选择巡礼点、在手机本地优化访问顺序，再用 MapLibre 连续导航全部地点。

> 当前状态：36 个 JVM 单元测试、Android SDK 37 编译、Lint、R8、APK 内容审计和固定签名发布均由 GitHub Actions 验证；debug APK 已在 Android 8（API 26）和 Android 17（API 37）模拟器通过全程断网、两次强制停止/恢复、前台服务通知、模拟 GPS 自动到达、息屏时自动进入下一站、两站完成和 Room 进度持久化，实际签名版也已在两个版本完成安装与冷启动复验。公交已按 Transitous 官方政策启用；纠正版 [v0.1.3 APK](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.1.3) 已公开发布。真机定位、锁屏、TTS 与 OEM 后台行为仍待实体设备验收。

## 功能

- Bangumi 动漫搜索与 Anitabi 巡礼点加载、地图/列表选点、当前地图范围批选。
- 驾车、骑行、步行最多 12 点：ORS Matrix + 手机本地 Held–Karp 排序 + 一次多停靠 Directions。
- 公交最多 8 点：按地理距离推荐顺序，并按出发/到达/停留时间逐段串联 Transitous 行程。
- 自由终点、指定终点、返回起点，以及路线预览和手动重排。
- Android 定位前台服务、应用内地图跟随、中文 TTS、自动到达/停留/下一站、偏航重算和进程恢复。
- Room 只缓存用户实际访问的数据与已生成路线；不包含广告、分析、账号、云同步或位置日志。

## 费用与外部服务

应用自身不使用服务器、域名、付费 SDK、Google Billing 或应用商店，必需现金成本为 0 元。公共服务均为 best-effort，没有 SLA：

| 用途 | 服务 | 使用条件 |
|---|---|---|
| 地图 | [MapLibre Native](https://maplibre.org/) + [OpenFreeMap](https://openfreemap.org/) | 无 Key；必须保留地图数据署名 |
| 道路路线 | [openrouteservice](https://openrouteservice.org/plans/) | 每位安装者申请自己的免费 Standard Key；当前 Directions 2,000/日、Matrix 500/日 |
| 公交 | [Transitous](https://transitous.org/api/) | 仅 FOSS/非营利 best-effort 使用；使用官方 API、可识别 User-Agent 和可见数据来源链接；对请求负载有疑虑时联系维护者 |
| 动漫 | [Bangumi API](https://bangumi.github.io/api/) | 无登录搜索，发送可识别 User-Agent |
| 巡礼点 | [Anitabi API](https://github.com/anitabi/anitabi.cn-document/blob/main/api.md) | 非商业并署名；程序仅访问官方数据与图片 API，不请求主域；数据为 CC BY-NC-SA 4.0 |

政策最后核对日期：2026-07-29。每次发布前请重新执行 [发布检查清单](docs/RELEASE_CHECKLIST.md)。

如果应用提示“当前公网 IP 被公共服务拒绝”，说明 Cloudflare 拒绝的是当前 Wi-Fi 或移动网络的公网出口。请停止重复请求并更换网络，必要时联系运营商更换公网 IP；重装应用或修改 User-Agent 不能解除 IP 封禁。

## 新手构建步骤（Windows）

1. 安装 Android Studio，并在 SDK Manager 安装 Android SDK Platform 37、Build-Tools 37.0.0 和 Platform-Tools。Android Studio 首次启动时会让你阅读并自行决定是否接受 Android SDK 许可。
2. 确保命令行使用 JDK 17。Android Studio 自带的 JBR 17 也可以。
3. 在项目根目录创建不提交的 `local.properties`，填入你实际的 SDK 路径：

   ```properties
   sdk.dir=C:\\Users\\your-name\\AppData\\Local\\Android\\Sdk
   ```

4. 运行测试和调试 APK：

   ```powershell
   .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
   ```

5. APK 位于 `app\build\outputs\apk\debug\app-debug.apk`。安装后，在规划道路路线时粘贴你自己的 ORS Key；它会经 Android Keystore 加密后保存在本机。

不要把 ORS Key 写入 `local.properties`、Gradle 文件、截图、日志或 GitHub Secrets。它是每位应用用户自己的 Key，不是项目共享 Key。

## Transitous 使用边界

Transitous 官方政策没有“必须取得明确批准后才能启用”的条件。项目只访问正式地址 `https://api.transitous.org/api/v6/plan`，发送包含应用名、版本和联系地址的 User-Agent，并在应用内显著链接其数据来源。

为控制路由请求负载，公交最多选择 8 个巡礼点；请求仅由用户生成路线、手动重算或到站/取消事件触发，按路段串行执行；已生成路线保存在本机供断网继续使用，不进行后台轮询、批量下载、并发抓取或自动重试。项目已按官网建议通过 Matrix 说明这一低频用途，相关记录见 [Transitous 沟通记录](docs/TRANSITOUS_CONTACT_RECORD.md)。如果用户规模或请求模式发生明显变化，发布者必须重新评估负载并再次联系维护者。

## 固定签名与 GitHub Releases

正式 APK 必须始终使用同一把签名私钥。私钥须放在工作区外；Gradle 会拒绝使用项目目录内的 keystore，也会拒绝生成未签名 release APK。

首次创建 keystore 的示例（请自行替换路径和别名，并安全备份）：

```powershell
keytool -genkeypair -v -keystore C:\keys\anitabi-release.jks -alias anitabi -keyalg RSA -keysize 4096 -validity 10000
```

本机打包前设置四个环境变量：

```powershell
$env:ANITABI_STORE_FILE = "C:\keys\anitabi-release.jks"
$env:ANITABI_STORE_PASSWORD = "your-store-password"
$env:ANITABI_KEY_ALIAS = "anitabi"
$env:ANITABI_KEY_PASSWORD = "your-key-password"
.\gradlew.bat testDebugUnitTest lintRelease assembleRelease
```

GitHub 仓库需配置以下 Actions Secrets：

- `ANITABI_KEYSTORE_BASE64`：keystore 文件的 Base64 内容。
- `ANITABI_STORE_PASSWORD`
- `ANITABI_KEY_ALIAS`
- `ANITABI_KEY_PASSWORD`

推送 `v*` tag 后，[发布工作流](.github/workflows/release.yml)会测试、Lint、R8、签名 APK、生成 SHA-256，再创建 GitHub Release。私钥在 runner 的临时目录恢复，不写入工作区或构建产物。

当前签名版与逐项证据见 [v0.1.3 发布验收记录](docs/releases/v0.1.3.md)。

## 隐私与安全

- ORS Key 的密文和 IV 存在应用私有 SharedPreferences，AES-GCM 密钥由 Android Keystore 管理；应用禁用备份和设备迁移。
- 路线规划或偏航重算时，必要坐标会发送给 ORS；规划或重算公交路线时会发送给 Transitous；地图瓦片从 OpenFreeMap 加载。应用自身不建立遥测或位置日志。
- 断网后可以沿已保存路线继续导航，但不能重新规划；公共地图只使用正常 HTTP 缓存，不批量下载瓦片。
- 仅允许 HTTPS，Manifest 禁止明文流量。

安全问题请参考 [SECURITY.md](SECURITY.md)。第三方署名与许可见 [NOTICE.md](NOTICE.md)。

## 已知限制

- Transitous 在日本只覆盖部分地区；无数据时应用会明确提示，不生成猜测路线。
- 第一版没有完整离线地图、断网重算、实时路况、车道级提示、精确票价或公交全局最优保证。
- 公交错过班次/严重延误的重算依赖网络与 Transitous 可用性。
- 公共服务策略和配额可能变化，发布者必须在每次发布前重新核对。

## 许可证

项目代码采用 [GNU GPL v3 或更高版本](LICENSE)。第三方代码、地图和数据不因此改为 GPL，仍分别遵守各自许可和服务条款。
