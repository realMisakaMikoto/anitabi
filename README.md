# 注意：本产品为基于 https://github.com/anitabi 打造的开源产品。如需要访问圣地巡礼网站，请访问 anitabi.cn
# 巡礼手帖（Anitabi Navigator）

巡礼手帖是一款 Android 动漫圣地巡礼规划与连续导航应用。用户可以搜索 Bangumi 作品，从 Anitabi 选择任意数量的巡礼点，在手机本地生成访问顺序，再使用 Google 地图、路线与道路导航完成行程。

> 当前开发版本为 v0.2.1（versionCode 7）。公开稳定版仍是 [v0.2.0](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.0)。v0.2.1 已通过 65 个 JVM 测试、Android 8/API 26 与 Android 17/API 37 的完整模拟器矩阵，以及生产 VPS、HTTPS、Firebase 鉴权、Google Routes 和硬额度账本验证。正式签名 RC 用于后续覆盖安装验收；在真机覆盖安装、Google Navigation 语音/偏航/锁屏和长行程换批完成前，不发布稳定 v0.2.1。

## v0.2.1 功能

- Bangumi 搜索、Anitabi 巡礼点加载、多作品联合选择、地图/列表选点和可见区域批选。
- 行程总点数不设固定上限；最近邻与有限轮次 2-opt 生成全局顺序，最多 10 点的矩阵窗口可再用 Held–Karp 精确优化。
- Google 单次请求始终受限：矩阵窗口最多 10 个坐标/100 个元素，道路预览最多 12 个位置，Navigation SDK 每批最多 25 个目的地。
- 驾车、骑行和步行使用 Google Navigation SDK 的地图、定位、道路导航、偏航处理和语音。
- 公交通过 Google Routes API 按相邻两点逐段规划，展示线路、站点、换乘、时间和步行接驳；公交不启动 Google 原生导航。
- `StoredTourV2` 和 Room 只保存用户拥有的点位、顺序、设置、完成状态和导航状态；Google 矩阵、路线、折线、步骤、预计时间与公交详情仅驻留内存。
- 从公开 v0.2.0 覆盖升级时保留首次导览、作品选择、行程顺序、设置和进度，移除旧 ORS Key 与旧路线内容，并要求联网刷新路线。
- Firebase Anonymous Auth 只用于访问自建路线 API。Analytics 与 Crashlytics 分别默认关闭、独立选择加入，并可随时撤回。
- 无 GMS 设备仍可查看已经保存的点位、顺序与进度，但不提供地图或路线回退。

## 架构、费用与额度

| 用途 | 实现 | 边界 |
|---|---|---|
| 地图与道路导航 | Google Navigation SDK for Android | APK 只包含受包名、签名和 Navigation SDK 限制的 Android 客户端 Key |
| 矩阵、路线预览与公交 | Google Routes API，经自建 VPS | 服务账号只在 VPS；响应规范化后返回，不缓存 Google 路线内容 |
| 客户端鉴权 | Firebase Anonymous Auth | 不要求邮箱、姓名或密码；VPS 验证 Firebase ID Token |
| 可选遥测 | Firebase Analytics / Crashlytics | 两项默认关闭、分别同意；不记录坐标、动漫名、搜索词或路线正文 |
| 动漫元数据 | [Bangumi API](https://bangumi.github.io/api/) | 发送可识别 User-Agent |
| 巡礼点与图片 | [Anitabi API](https://github.com/anitabi/anitabi.cn-document/blob/main/api.md) | 仅访问官方 API/图片域名，低频用户触发；数据为 CC BY-NC-SA 4.0 |

Google 项目绑定了结算账户，但 VPS 以免费额度的 90% 为硬上限：矩阵每月 9,000 个计费元素、路线每月 9,000 次、导航每月 900 个目的地，并叠加每 UID 每日额度和突发限速。达到上限、账本异常、磁盘满或额度状态无法确认时会停止计费请求，不会自动清零或绕过限制。Google Cloud 预算告警不是硬停机开关，VPS SQLite 账本才是本项目的 fail-closed 熔断器。

生产 API 为 `https://api.anitabi.afunnypersonlol0.site`。断网、VPS 故障或额度耗尽时，应用保留行程和进度并明确提示路线暂时无法刷新。

## 隐私与安全

- 应用禁止系统备份和设备迁移，不建立用户邮箱账号，也不持久化 Google 路线响应。
- 规划所需的坐标、模式和出发时间经 HTTPS 发送到自建 VPS，再由 VPS 调用固定的 Google 上游。VPS 日志不包含 Token、原始 IP、坐标、动漫名、搜索词或正文。
- VPS 只用 HMAC 后的 IP 做宽松辅助限速；主要配额按 Firebase 匿名 UID 计数。
- Anitabi 数据与图片由 Android 直接低频访问官方域名，不经过 VPS。
- APK 不包含 Google 服务账号私钥、VPS 凭据、签名密码、ORS Key 或 Transitous 请求路径。
- Analytics 与 Crashlytics 的具体开关、清理和数据边界见 [隐私说明](PRIVACY.md)；安全报告方式见 [SECURITY.md](SECURITY.md)。

## 本地构建（Windows）

1. 安装 JDK 17、Android SDK Platform 37、Build-Tools 37.0.0 和 Platform-Tools。
2. 在项目根目录创建不提交的 `local.properties`：

   ```properties
   sdk.dir=C:\\Users\\your-name\\AppData\\Local\\Android\\Sdk
   ANITABI_NAVIGATION_API_KEY=your-android-restricted-navigation-key
   ```

3. 从你自己的 Firebase 项目下载 Android 配置到不提交的 `app/google-services.json`。应用包名必须为 `cn.anitabi.navigator`。不要使用 CI 的无效占位配置运行真实应用。
4. 将 Navigation SDK Key 限制为该包名、实际调试/正式 SHA-1 证书和 Navigation SDK API。服务账号 JSON 绝不能进入 Android 工程。
5. 运行：

   ```powershell
   .\gradlew.bat testDebugUnitTest lintDebug assembleDebug
   ```

调试 APK 位于 `app\build\outputs\apk\debug\app-debug.apk`。首次启动会依次说明用途、请求定位/通知权限并展示服务与隐私披露；不再要求用户填写 ORS Key。

## 固定签名与发布

正式 APK 必须沿用 v0.2.0 的 RSA-4096 固定签名。私钥与密码只允许位于工作区外或 GitHub Actions 加密 Secrets；Gradle 会拒绝缺少完整签名参数或 Navigation SDK Key 的 release 构建。

GitHub Actions 还需要：

- `ANITABI_KEYSTORE_BASE64`
- `ANITABI_STORE_PASSWORD`
- `ANITABI_KEY_ALIAS`
- `ANITABI_KEY_PASSWORD`
- `ANITABI_GOOGLE_SERVICES_JSON_BASE64`
- `ANITABI_NAVIGATION_API_KEY`

`v0.2.1-rc.N` 标签会生成正式签名、R8/resource-shrunk 的 GitHub Prerelease；`v0.2.1` 才生成稳定 Release。两者都会执行测试、Release Lint、源码/APK 密钥审计、签名验证和 SHA-256 生成。发布前逐项执行 [发布检查清单](docs/RELEASE_CHECKLIST.md)，证据见 [v0.2.1 RC 验收记录](docs/releases/v0.2.1-rc.1.md)。

## 已知限制

- 新路线必须联网；断网时只能查看已保存的用户数据和进度。
- 公交路线只能按相邻两点逐段计算，不提供 Google 原生公交导航；站台信息只显示 Google 上游实际提供的文字。
- Navigation SDK 要求设备具备可用的 Google Play 服务；无 GMS 时没有 MapLibre/ORS 回退。
- “无限点”只表示总行程不设产品上限，不表示绕过 Google 单次请求、每日或每月限制。
- 真实 8–12 点 GNSS 现场路线、长时间 Xiaomi/OEM 后台存活、真实错过班次，以及 v0.2.0 到 v0.2.1 的正式签名真机覆盖安装仍需现场验收。

## 许可证

项目自有代码采用 [GNU GPL v3 或更高版本，并附仅针对 Google Navigation/Firebase SDK 链接的窄范围例外](LICENSE)。该例外不改变第三方 SDK、服务或数据自身的条款，也不允许把项目自有代码改为闭源。完整第三方署名见 [NOTICE.md](NOTICE.md)。
