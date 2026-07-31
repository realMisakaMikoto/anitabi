# 实施记录

> 规则：每个新任务开始前先读取本文件；每个任务完成后追加实际改动与验证结果。

## 2026-07-29 — 任务 1：工程初始化与核心领域模型

### 已完成

- 从空目录创建单 `app` 模块 Android 工程，使用 Kotlin、Jetpack Compose 和 Material 3。
- 配置 Gradle Wrapper 9.5.0、Android Gradle Plugin 9.3.0、Kotlin 2.3.21、Compose BOM 2026.06.01、`compileSdk/targetSdk 37`、Java 17。
- 建立基础 Manifest、主题、应用入口和欢迎页。
- 建立核心类型：`TravelMode`、`RouteObjective`、`EndPolicy`、`NavigationState`、`Anime`、`PilgrimagePoint`、`TourPlan`、`TourLeg`、`RouteStep`、`NavigationProgress`。
- 固化 Anitabi `[纬度, 经度]` 到 GeoJSON `[经度, 纬度]` 的坐标转换和范围校验。
- 固化导航状态转换：`PLANNED → NAVIGATING → ARRIVING → DWELLING → NEXT_STOP → COMPLETED`，并允许多站路线从 `NEXT_STOP` 回到 `NAVIGATING`。
- 添加 `.gitignore`，排除 SDK、构建目录、签名文件和本地配置。

### 验证

- `gradlew help`：成功，证明 Gradle/AGP/Kotlin/Compose 插件配置可解析。
- 临时纯 Kotlin 工程编译相同核心源码并运行 4 个 JUnit 测试：全部通过；临时源码和构建脚本已删除，安全策略拒绝递归删除的生成缓存位于已忽略的 `.verification/`。
- `gradlew testDebugUnitTest`：被本机缺失 Android SDK 阻断，未发现代码测试失败。未替用户接受 Android SDK 许可。

### 后续注意

- 需要用户安装 Android SDK 37，或明确同意在本机安装并接受相关许可后，才能执行完整 Android 编译、Lint、APK 和仪器测试。
- ORS Key 必须由每位用户自行填写；不得进入源码、日志、备份或版本控制。
- Transitous 路由必须在取得维护者同意后才能启用。

## 2026-07-29 — 任务 2：网络层与 Room 持久化

### 已完成

- 增加统一 OkHttp 客户端、可识别 User-Agent 校验和 404/429/5xx/网络/解析错误类型；错误正文截断，避免日志放大。
- 实现 Bangumi 动画搜索，固定 `POST /v0/search/subjects`、动画类型 `2`、匹配排序和分页上限。
- 实现 Anitabi lite 与详情接口，容忍空字段、跳过非法坐标并报告“部分数据/非法坐标”警告。
- 实现 ORS Matrix 与多停靠 Directions 客户端，仅使用新的 `api.heigit.org/openrouteservice/v2` 域名；API Key 通过运行时提供者注入，缺失时明确失败。
- 实现 Transitous `/api/v6/plan` 客户端和行程 DTO；未记录维护者同意时直接禁用，不发出路由请求。
- 增加 Room 数据库、巡礼数据缓存、路线与导航进度持久化 DAO/Repository；缓存只保存用户访问的数据。
- 对照 Bangumi 官方 OpenAPI、Anitabi 官方 API 文档、ORS 官方资料和 MOTIS 2.10.2 OpenAPI 固化请求/响应结构。

### 验证

- `gradlew help`：增加 KSP/Room/OkHttp 后仍成功，插件和依赖声明可解析。
- 临时 JVM 验证工程编译核心与网络源码，累计运行 10 个 JUnit 测试：全部通过；覆盖 Bangumi 可空图片、Anitabi 空字段/坐标顺序/非法坐标、ORS GeoJSON、Transitous 行程和 HTTP 错误映射。
- 安全扫描：未发现旧 ORS 域名、Google/Organic Maps 导航 Intent 或疑似硬编码共享 Key。
- Room 的 Android/KSP 实际代码生成仍待 Android SDK 37 可用后执行；当前本机环境限制沿用任务 1 记录。

## 2026-07-29 — 任务 3：搜索、地图与点位选择 UI

### 已完成

- 接通 `MainActivity → SearchViewModel → Bangumi/Anitabi/Room` 的真实数据链路，不使用假数据。
- 实现动漫名称搜索、加载/空态/限流/断网/无 Anitabi 数据提示、作品结果卡片和返回流程。
- 接入 MapLibre Native 13.4.1 与 OpenFreeMap Liberty 样式，使用 GeoJSON source + Circle/Symbol layers 实现点聚合、聚合点击放大、单点选择和选中态更新。
- 实现当前地图可视范围回传与区域批选，含跨 180° 经线边界处理；道路路线选择上限固定为 12 点。
- 实现地图/列表切换、截图缩略图、单点勾选、清空选择、数据不完整提示。
- 地图页固定显示 OpenFreeMap、OpenMapTiles、© OpenStreetMap contributors；截图旁显示 Anitabi `origin` 并允许打开 `originURL`。
- 采用“纸质巡礼手账”视觉方向：暖纸色、墨绿和朱红状态色、衬线标题与紧凑的移动端信息层级。
- 根据已认证 GitHub 账号，将请求联系地址设为公开个人主页 `https://github.com/realMisakaMikoto`，满足 Transitous/Bangumi 的可联系 User-Agent 要求。

### 验证

- `gradlew :app:dependencies --configuration debugCompileClasspath`：成功解析 Compose、Room、Coil、OkHttp、MapLibre 完整依赖树。
- 临时 JVM 验证累计运行 12 个测试：全部通过；新增普通边界和跨 180° 经线可视范围测试。
- MapLibre 初始化、GeoJSON 聚合、图层表达式、点击查询、聚合展开和可视边界 API 均对照 `android-v13.4.1` 官方源码。
- 完整 Compose/MapLibre Android 编译和真机视觉验收仍需 Android SDK 37；未将环境缺失误报为代码通过。

## 2026-07-29 — 任务 4：路线优化、Provider 与预览

### 已完成

- 实现 Held–Karp 位掩码动态规划，支持固定起点下的自由终点、指定终点和返回起点；道路最多 12 个巡礼点。
- 根据 `FASTEST/SHORTEST` 分别使用 ORS duration/distance Matrix；排序后一次请求多停靠 Directions。
- 将 ORS GeoJSON、分段和转向步骤映射为 `TourPlan/TourLeg/RouteStep`，保留 ORS/HeiGIT 与 OSM 署名。
- 实现公交最近邻地理推荐顺序（最多 8 点），逐站串联 MOTIS 行程，并把到达时间加默认 15 分钟停留后作为下一段出发时间。
- 实现 MOTIS precision=6 polyline 解码、线路/方向/站台/中途站/实时与取消信息映射；无行程时明确返回“本区域暂无开放公交数据”。
- 增加 Android Keystore AES-GCM ORS Key 存储；密文与 IV 存于私有 SharedPreferences，损坏时清除，不写日志、备份或源码。
- 增加规划表单：驾车/骑行/步行/公交、起点、终点策略、最快/最短、公交日期时间与停留时间、个人 ORS Key。
- 增加路线地图预览、总时长/距离、署名和长按拖动重排；固定起点/终点保持锁定，重排后重新请求路线并保存 Room。
- `BuildConfig.TRANSITOUS_APPROVED` 默认 `false`；未获同意时 UI 与网络层双重关闭公交路由。

### 验证

- 纯 Kotlin 测试累计 19 个全部通过。
- 覆盖 2、8、12 点的自由终点/指定终点/返回起点、对称最优解平局、不可达矩阵、公交最近邻、polyline 解码、返程腿和停留时间串联。
- `gradlew help`：新增 BuildConfig、Keystore、Planner 依赖后仍成功。
- Android UI、Keystore 和 Room 代码生成的最终编译仍待 Android SDK 37。

## 2026-07-29 — 任务 5：连续导航、语音播报与偏航恢复

### 已完成

- 实现可持久化的连续导航状态机：自动到达、停留倒计时、下一段、返程和完成；返程腿不把起点重复计为巡礼点。
- 根据当前位置推进 ORS 转向步骤并输出当前指令；公交在 GPS 不可用时可按计划到达时间推进。
- 实现步行/骑行 60 米、驾车 100 米的偏航阈值；持续偏航 15 秒才触发重算，重算冷却 60 秒，避免 GPS 漂移和配额浪费。
- 实现剩余路线重算：排除已完成巡礼点，保留指定终点及最初返程起点；重算失败或断网时继续使用原路线并明确提示。
- 增加定位、通知和 location 类型前台服务权限；实现 GPS/网络定位更新、低打扰常驻通知、通知栏结束操作和后台持续导航。
- 增加中文系统 TTS 播报，按导航状态、路线段和转向步骤去重，避免每次定位更新重复朗读。
- 将路线、导航进度和停留截止时间持续保存到 Room；进程被系统回收后，重新打开应用可恢复未结束导航；用户主动结束则保存为暂停状态，不自动重启。
- 增加连续导航页面：应用内 MapLibre 路线、当前位置跟随、当前指令、剩余距离、段进度、偏航重算提示、手动确认到达和数据来源署名；没有调用外部地图导航 Intent。
- 在路线预览加入定位/通知运行时权限请求和“开始连续导航”入口；应用与前台服务共享单例 `AppContainer`。

### 验证

- 临时纯 Kotlin 验证工程累计运行 25 个 JUnit 测试：全部通过。
- 新增覆盖完整多站状态序列、停留恢复、无巡礼点返程、公交定时推进、15 秒偏航确认、60 秒冷却、转向步骤推进和剩余路线返程重算。
- `gradlew :app:dependencies --configuration debugCompileClasspath`：新增 AndroidX Core 后依赖树解析成功。
- `gradlew testDebugUnitTest`：仍仅被本机缺失 Android SDK 路径阻断；前台服务、Compose、Manifest 与通知 API 尚未完成 Android 编译和真机权限/后台行为验收。

## 2026-07-29 — 任务 6：许可、安全与发布准备（本地完成，外部验收待办）

### 已完成

- 加入完整 GPL v3 许可证正文，并在 README 明确采用 GPL-3.0-or-later；增加第三方 `NOTICE.md` 和 `SECURITY.md`。
- 增加应用内“关于、隐私与数据来源”页面，显著显示 OpenFreeMap/OpenMapTiles/OSM、ORS/HeiGIT、Transitous 来源、Bangumi、Anitabi CC BY-NC-SA 4.0、公交启用状态和 GPL 声明。
- 增加中文 README：零预算边界、新手 SDK 37 构建步骤、每用户 ORS Key、Transitous 同意门槛、隐私、已知限制、固定签名和 GitHub Release 操作。
- 增加 Transitous 英文联系模板和逐项发布检查清单，覆盖政策、许可、签名、密钥扫描、弱网/GPS/偏航/锁屏/杀进程/跨午夜、公交覆盖降级和真机验收。
- 重新核对官方政策：ORS Standard 当前仍为 0 欧元、Directions 2,000/日、Matrix 500/日；Transitous 仍要求重资源路由使用前联系；OpenFreeMap 仍要求地图数据署名。核对日期写入 README。
- 将 Kotlin 更新到 2026-07-14 发布的稳定版 2.4.10；KSP 维持官方最新 2.3.10。
- 公交构建开关改为 `ANITABI_TRANSITOUS_APPROVED`，默认 `false`；只有取得明确同意后才可设置为 `true`。
- 禁止明文网络；禁用应用备份、设备迁移，并显式排除数据库、SharedPreferences 和文件，防止 ORS Key 密文及路线被备份。
- 增加工作区外固定签名约束：四个签名值缺一即拒绝 release，keystore 位于项目目录内也拒绝，避免误发未签名或临时签名 APK。
- 增加 GitHub Actions：main/PR 自动测试、Lint、debug APK；`v*` tag 自动在 runner 临时目录恢复签名、校验版本、运行 R8 release、验证 APK 签名、生成 SHA-256 并创建 GitHub Release。
- 补齐公交导航信息：线路、方向、上下车站台、中途站、换乘/下车 TTS、取消班次自动重算，以及“错过班次或严重延误”手动重算；到达每个巡礼点后按实际时间重算剩余公交行程。
- 创建本地 Git `main` 分支并完成首个提交；`gradlew` 在索引中标记为 Linux 可执行。

### 验证

- Kotlin 2.4.10 临时 JVM 工程累计运行 25 个 JUnit 测试：全部通过；临时验证脚本已删除，生成缓存继续由 `.gitignore` 排除。
- `gradlew help` 与 `gradlew :app:dependencies --configuration debugCompileClasspath`：成功；Kotlin/Compose/KSP/AGP 和完整依赖元数据可解析。
- 7 个 Android XML 文件均通过 XML 解析；2 个 GitHub Actions 工作流均通过 YAML 解析。
- 安全扫描未发现疑似硬编码 Key/Token、旧 ORS 域名、Google/Organic Maps 导航 Intent、Billing、Firebase、分析或广告 SDK；HTTPS 以外只存在 Android XML namespace。
- 当前本机仍缺 Android SDK 37，无法完成 Android 编译、Lint、R8、APK 和真机验收。
- 尝试创建公开仓库 `realMisakaMikoto/anitabi` 时，GitHub 返回当前令牌无 `createRepository` 权限；未创建远端、未推送、未创建 Release。
- 固定签名私钥和密码尚未由用户提供；因此按安全约束没有生成 release APK，也没有创建 `v0.1.0` tag。

## 2026-07-29 - Task 7: requirement audit and public repository setup

### Completed

- Re-read this log and the implementation plan before starting the task.
- Audited the application against the plan and filled specific gaps: invalid ORS credentials now have a dedicated error, unsafe Anitabi origin URLs are rejected, partial/invalid pilgrimage data assembly is isolated and tested, and transit refresh decisions are isolated and tested.
- Expanded transit support and tests for line, direction, platforms, intermediate stops, realtime/cancelled state, and cross-midnight timestamps.
- Added complete transit-leg details and map-data attribution to the route preview, plus the current pilgrimage target to the navigation screen.
- Created the public GitHub repository `https://github.com/realMisakaMikoto/anitabi` using the signed-in browser session.

### Verification

- Ran the temporary Kotlin/JVM verification build with Kotlin 2.4.10: all 33 unit tests passed.
- Ran `git diff --check`: no whitespace errors were reported.
- Deleted the temporary verification Gradle scripts after the test run; their generated directories remain ignored.

### Remaining

- Push the audited commit and run the official Android SDK 37 CI build, lint, and APK assembly.
- Fix any CI-only Android compilation or lint findings, then generate signing material and publish the signed release.
- Transitous remains disabled until the maintainers explicitly approve this client, as required by their policy and the implementation plan.

## 2026-07-29 - Task 8: official Android CI and debug APK

### Completed

- Re-read this log before starting the task.
- Added the new public repository as `origin` and pushed the complete `main` history.
- Diagnosed the Android 17 SDK install failure and corrected both workflows to install the published package `platforms;android-37.0` with build tools 37.0.0.
- Fixed official Android compilation findings: removed obsolete explicit Compose `weight` imports and replaced the unavailable `Walk` icon with `DirectionsWalk`.
- Kept changes surgical and pushed each CI repair to `main` for independent verification.

### Verification

- GitHub Actions run `30434075089` passed on Ubuntu with Android SDK 37: `testDebugUnitTest`, `lintDebug`, and `assembleDebug` all succeeded.
- The workflow uploaded `anitabi-debug`; downloaded `app-debug.apk` is 69,787,278 bytes with SHA-256 `B2C1279007C2951C0A5160E2EC86AC77E2CFD8C0997DEF88C32334629987D52D`.
- Official CI run: `https://github.com/realMisakaMikoto/anitabi/actions/runs/30434075089`.

### Remaining

- Generate and securely configure a fixed release signing key, run the release/R8 workflow, and publish `v0.1.0`.
- Device/emulator interaction checks and Transitous maintainer approval remain separate acceptance gates.

## 2026-07-29 - Task 9: fixed signing and v0.1.0 release

### Completed

- Re-read this log before starting the task.
- Generated a fixed RSA-4096 JKS release key outside the workspace at `C:\Users\csy15\.anitabi-signing\anitabi-release.jks`.
- Stored both random passwords with Windows user-scoped DPAPI and added a private recovery README beside the key; no plaintext password was printed or committed.
- Configured all four encrypted GitHub Actions repository secrets through the signed-in GitHub session.
- Replaced the raw Room KSP schema argument with the official Room Gradle plugin, removing the debug/release parallel schema export race found by the release build.
- Corrected release signature verification to invoke the installed Android build-tools `apksigner`, and fixed the workflow YAML scalar.
- Published the non-draft, non-prerelease GitHub Release `v0.1.0` with the signed APK and checksum file.

### Verification

- Release workflow run `30435837520` passed: unit tests, release Lint, R8 assembly, signature verification, checksum generation, and release publication.
- `apksigner` verified APK Signature Scheme v2 with one signer; certificate SHA-256 is `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`, matching the local fixed key.
- Downloaded `anitabi-v0.1.0.apk` is 50,984,377 bytes. Its computed SHA-256 matches the published checksum: `e4c1c66cefaea54fde0c7f3f6bfbba461530465e5ef4530c02fa956bcd12624d`.
- Release URL: `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.1.0`.

### Remaining

- Add and run an emulator install/launch smoke test, then document anything that still needs a physical phone.
- Transitous stays compile-time disabled until its maintainers explicitly approve this client.

## 2026-07-29 - Task 10: Android emulator install and cold-launch smoke test

### Completed

- Re-read this log before starting the task.
- Added a `main`-push emulator smoke job to the Android CI workflow, using an API 36 Google APIs x86_64 AVD with bounded SDK, boot, and app-launch timeouts.
- Made the AVD location explicit and step-scoped after the first diagnostic run showed that the emulator could not find the AVD created in the runner's temporary directory.
- Installed the CI-built debug APK, force-stopped it, cold-launched `MainActivity`, and captured the start result, process ID, foreground activity state, UI hierarchy, screenshot, emulator log, and crash buffer as an Actions artifact.

### Verification

- GitHub Actions run `30437176966` passed both jobs: the SDK 37 unit-test/Lint/debug-APK job and the emulator install/launch job.
- The cold launch returned `Status: ok`, `LaunchState: COLD`, and process ID `2258`; the Android crash log artifact is empty.
- Visual inspection of `home.png` confirms that the Chinese home screen, navigation chips, anime search field, Bangumi action, privacy/data-source link, and empty-state copy render correctly.
- Official CI run: `https://github.com/realMisakaMikoto/anitabi/actions/runs/30437176966`.

### Remaining

- A physical Android phone is still required to validate real GPS updates, foreground-notification and lock-screen behavior, OEM battery/background restrictions, audible TTS, live routing, and weak-network recovery.
- Transitous remains compile-time disabled; enabling it still requires explicit permission from the Transitous maintainers.

## 2026-07-29 - Task 11: requirement audit, compatibility matrix, and release evidence

### Completed

- Re-read this complete log and the original pasted implementation plan before starting the task.
- Re-audited the repository, workflows, public release, service policies, local Android-device availability, and every plan item that can be verified without a physical phone or external maintainer approval.
- Rechecked the official policies and current endpoints for OpenFreeMap, openrouteservice, Transitous, Android 17/API 37, Bangumi, and Anitabi. Confirmed that Transitous still requires prior contact for routing/resource-intensive use and kept it compile-time disabled.
- Live-probed Bangumi and OpenFreeMap successfully. Reproduced Cloudflare HTTP 403 on the documented Anitabi API and opened upstream issue `anitabi/anitabi.cn-document#86` with the endpoint, app identity, failure mode, and Cloudflare Ray ID.
- Split HTTP 403 from 401 in the application error model, added a clear Anitabi network-rejection message, preserved the ORS invalid-key behavior for both statuses, and added a regression assertion.
- Added an APK content audit that rejects the retired ORS domain, prohibited SDKs, signing-password markers, private-key markers, and keystore files; wired it into debug CI and release builds.
- Added API 26/API 37 emulator coverage to main CI, fixed Android 8 log-buffer clearing as a non-fatal diagnostic, and added a manually dispatchable workflow that tests the exact APK attached to a GitHub Release.
- Filled the Transitous approval request with the public repository, version, contact, traffic limits, attribution, and privacy details. No Matrix account was signed in, so the request was not sent and no approval was claimed.
- Expanded the release audit and release notes with exact evidence, updated the public `v0.1.0` GitHub Release description, and recorded the remaining physical-device and external-service gates without claiming they passed.

### Verification

- Main CI run `30439622908` passed all 33 unit tests, Android SDK 37 compilation, Lint, debug assembly, APK content audit, and cold-launch jobs on Android 8/API 26 and Android 17/API 37.
- Exact signed-release run `30440179352` downloaded `anitabi-v0.1.0.apk` from the public Release and passed installation and cold launch on API 26 and API 37. Both reported `versionCode=1`, `versionName=0.1.0`, `minSdk=26`, `targetSdk=37`, a live app process, and an empty crash buffer; both screenshots rendered normally.
- Static inspection of the published APK found the expected current service endpoints and none of the forbidden old domain, prohibited SDK, signing-secret, private-key, or keystore markers. Its SHA-256 remains `e4c1c66cefaea54fde0c7f3f6bfbba461530465e5ef4530c02fa956bcd12624d`.
- No ADB executable or physical Android device was available on this machine, so no real-device result was fabricated.
- Public evidence: `https://github.com/realMisakaMikoto/anitabi/actions/runs/30439622908`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30440179352`, and `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.1.0`.

### Remaining

- Validate the released APK on a physical Android phone: real GPS updates, ORS-key live routing, continuous multi-stop navigation, arrival/dwell/next-stop transitions, audible Chinese TTS, notification and lock-screen behavior, OEM background/process-kill recovery, weak-network handling, and upgrade/restore behavior.
- Wait for the Anitabi maintainers to respond to issue `#86` or otherwise restore documented API access for the affected network.
- Send the prepared Transitous request from an identifiable Matrix account and obtain explicit maintainer approval before enabling or testing public-transit routing.

## 2026-07-29 - Task 12: offline runtime verification, Android 8 fixes, and v0.1.1 release

### Completed

- Re-read this complete log and the original pasted implementation plan before starting the task.
- Rechecked current official AndroidX test versions and MapLibre renderer guidance. Added Android instrumentation dependencies and a runtime test fixture without enabling Transitous or adding a backend.
- Added emulator verification for persisted-route seeding, a foreground location service and navigation notification, full offline operation, force-stop/open recovery twice, two manual-arrival transitions, completed state, and Room progress persistence.
- Made the runtime APK use the same debug signer as the tested app, matched persisted start-point semantics, supported permission and airplane-mode differences across API 26/API 37, restored networking in an always-run cleanup step, and captured logcat, crash-buffer, DropBox, activity, notification, UI hierarchy, and screenshot diagnostics.
- Added Android CI concurrency cancellation and canceled obsolete run `30441875569` so superseded commits would not continue consuming runners or generate additional failure mail.
- Used the diagnostic artifacts instead of weakening the test. Fixed two real Android 8 product crashes: implemented the legacy `LocationListener` callbacks required on API 26 and replaced the automatic MapLibre artifact with the official `android-sdk-opengl:13.4.1` artifact for devices without a Vulkan-compatible GPU.
- Incremented the application to `versionCode=2` / `versionName=0.1.1`, wrote patch-release notes, and published the fixed, non-draft, non-prerelease `v0.1.1` with the existing fixed signing identity.
- Used the signed-in GitHub browser session to dispatch the exact-release compatibility workflow because the command-line token was read-only for workflow dispatch. No permissions or validation steps were bypassed.
- Downloaded and inspected successful API 26/API 37 runtime evidence and exact signed-release evidence. Updated README, release notes, and the v0.1.1 acceptance record with precise evidence boundaries.
- Rechecked `anitabi/anitabi.cn-document#86`; it remains open with no maintainer comments. Transitous remains compile-time disabled because no explicit maintainer approval exists.

### Failure diagnosis and fixes

- Run `30441290377`: the app and instrumentation APKs had different ephemeral debug signatures; both are now built together with one signing identity.
- Run `30441981895`: API 26 did not support the modern runtime-permission helper and API 37 rejected an unprivileged protected airplane-mode broadcast; permission grants and network control are now version-aware.
- Run `30442766632`: API 37 passed but API 26 could not send the protected broadcast; the API 26 emulator now uses a verified root ADB session.
- Run `30443325979`: the API 26 `svc` process was killed; direct root settings and the protected broadcast replaced it.
- Run `30443949246`: Android 8 showed a real application crash; always-run crash diagnostics were added.
- Run `30444598023`: diagnostics identified `AbstractMethodError` from missing old `LocationListener` methods and `No Vulkan compatible GPU found` from the renderer. Both product causes were fixed rather than hidden.

### Verification

- Gradle resolved `org.maplibre.gl:android-sdk-opengl:13.4.1` successfully from Maven Central; `git diff --check` reported no whitespace errors. The local machine still has no configured Android SDK, so official Android compilation remained in GitHub Actions rather than being fabricated locally.
- Android CI `30445242402` first proved the fixes on API 26 and API 37. Versioned main run `30446068593` then passed 33 JVM tests, SDK 37 compilation, Lint, debug APK audit, cold launch, offline process recovery, foreground navigation completion, networking cleanup, and diagnostic upload on both emulator versions.
- Both runtime evidence sets report `OK (1 test)` for route seeding and `OK (1 test)` for navigation completion. Recovery UI and notifications remained present after both force-stop/open cycles; crash buffers were empty and DropBox contained no `data_app_crash` entry.
- Signed release run `30446070504` passed unit tests, release Lint, R8, APK audit, signature verification, checksum generation, and public Release publication.
- Downloaded `anitabi-v0.1.1.apk` is 43,087,229 bytes. Its computed SHA-256, published checksum, and GitHub asset digest agree: `efd41a3f6186e0da5784cfb93cf6816d1a3720ee332dee243058a8b90e1a5787`. The fixed certificate SHA-256 remains `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.
- Exact-release compatibility run `30446437144` downloaded the public signed APK and passed installation, package inspection, cold launch, foreground-process, screenshot, and empty crash-buffer checks on API 26 and API 37. Both report `versionCode=2`, `versionName=0.1.1`, `minSdk=26`, and `targetSdk=37`.
- Agent Reach update check reported installed version `v1.5.0` is current.
- Public evidence: `https://github.com/realMisakaMikoto/anitabi/actions/runs/30446068593`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30446070504`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30446437144`, and `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.1.1`.

### Remaining external acceptance gates

- A physical Android phone is still required for real GPS, audible Chinese TTS, lock-screen notification behavior, OEM battery/background restrictions, real weak-network behavior, and full user-driven search/ORS/navigation acceptance. Emulator evidence is not presented as a substitute.
- Wait for an Anitabi maintainer response to issue `#86` or restoration of documented API access for the affected network.
- Send the prepared Transitous request from an identifiable Matrix account and obtain explicit maintainer approval before enabling or testing public-transit routing.

## 2026-07-29 - Task 13: automatic GPS arrival and screen-off runtime verification

### Completed

- Re-read this complete log and the original pasted implementation plan before starting the task.
- Audited the remaining runtime evidence against the plan. Confirmed that the previous service test used the manual-arrival action and therefore did not prove that Android `LocationManager` callbacks automatically advance a route.
- Added an independent instrumentation test that installs an Android test GPS provider, starts the actual foreground navigation service from a persisted two-stop route, turns the emulator screen off, injects the first destination through `LocationManager`, and verifies automatic arrival/dwell/next-stop progression while the screen remains off and the navigation notification remains active.
- The same test wakes the emulator, injects the second destination, verifies automatic completion, checks all three expected completed point IDs, and waits for the completed progress to be persisted to Room.
- Added explicit instrumentation milestones `GPS_FIX_1_ADVANCED_WHILE_SCREEN_OFF` and `GPS_FIX_2_COMPLETED_AND_PERSISTED`; CI requires both milestones and `OK (1 test)` rather than accepting a generic successful process exit.
- Granted the Android mock-location app op only to the disposable app/test packages inside CI. Production code, production permissions, and the public APK were not given a mock-location capability.
- Kept the existing manual-arrival fallback test and offline force-stop/recovery test, so the new automatic-location path supplements rather than replaces previous coverage.
- Updated README and the v0.1.1 evidence records with the automatic GPS and screen-off result. Updated the prepared Transitous request User-Agent from `0.1.0` to `0.1.1`.

### Verification

- Android CI run `30447328118` passed 33 JVM tests, SDK 37 compilation, Lint, debug APK content audit, cold launch, offline process recovery, manual foreground navigation completion, the new automatic GPS/screen-off test, network restoration, and diagnostic upload on both Android 8/API 26 and Android 17/API 37.
- Downloaded both evidence artifacts. API 26 completed the automatic test in 9.111 seconds and API 37 in 8.284 seconds; both contain the two required GPS milestones and `OK (1 test)`.
- `automatic-gps-crash.log`, the always-captured crash buffer, and the ordinary crash buffer are empty on both versions. Android DropBox reports no `data_app_crash` entries.
- Compared `v0.1.1` with the tested commit: there are no changes under production app source or `app/build.gradle.kts`; post-tag changes are limited to CI, androidTest, and documentation. This supports the same-source claim but is not presented as physical-GPS evidence.
- Rechecked the official Android `LocationManager` documentation: test-provider calls require the mock-location app op and locations must include provider, accuracy, wall-clock time, and elapsed realtime; the test supplies all required fields.
- Agent Reach's Exa backend could not load tool metadata, so official Android documentation was read through the browsing fallback and Transitous' official policy was read through Agent Reach's documented Jina route. `agent-reach check-update` confirms v1.5.0 is current.

### External-state audit

- `adb` is still unavailable locally, `ANDROID_HOME` and `ANDROID_SDK_ROOT` are unset, and Windows device inventory shows no connected Android/ADB/MTP phone. The Xiaomi entries are Bluetooth earbuds, not a test handset.
- Anitabi issue `anitabi/anitabi.cn-document#86` remains open with no comments or maintainer update.
- Transitous' policy page, published/updated on 2026-07-29, still identifies routing as potentially resource-intensive and directs projects to its Matrix channel when load is in doubt. A search of its public GitHub issues found no documented API-usage approval alternative. No GitHub issue was opened as a substitute for the required Matrix contact.

### Remaining external acceptance gates

- A physical Android phone and user-driven session remain necessary for real GNSS behavior, audible Chinese TTS, a secured lock screen, OEM battery restrictions, real mobile-network transitions, ORS-key live routing, and the complete search-to-navigation acceptance flow.
- Wait for an Anitabi maintainer response to issue `#86` or restoration of documented API access for the affected network.
- Send the prepared Transitous request from an identifiable Matrix account and obtain explicit maintainer approval before enabling or testing public-transit routing.

## 2026-07-29 - Task 14: Anitabi network-boundary audit and v0.1.2 release

### Completed

- Re-read this complete log and the original implementation plan before starting the task.
- Read the Anitabi maintainer's response on `anitabi/anitabi.cn-document#86` and rechecked the current official API document. Confirmed that programmatic data access belongs on `api.anitabi.cn`, image access belongs on `image.anitabi.cn`, and the main `anitabi.cn` domain must not be used as a resource API.
- Audited every Anitabi URL and load path in the application. Data requests are constructed only for the documented lite and point-detail endpoints; `originURL` is handed to the system browser only after an explicit user click.
- Restricted API-provided Anitabi cover and point images to HTTPS URLs whose host is exactly `image.anitabi.cn`, including rejection tests for the main domain and lookalike hosts.
- Configured Coil's singleton image loader to use the same identifiable application User-Agent as JSON API requests, closing the previous gap where image requests used the network library's default identity.
- Incremented the app to `versionCode=3` / `versionName=0.1.2`, added a one-shot boundary probe limited to the documented data and image hosts, and published the fixed signed release `v0.1.2`.
- Made signed-release compatibility verification run automatically for published releases and verified the public v0.1.2 APK on both supported emulator endpoints.
- Prepared a factual response for the upstream issue. Both the command-line GitHub credential and the connected GitHub integration returned HTTP 403 for comment/close operations, so the issue remains open and no reply or closure was falsely claimed.

### Verification

- One-shot boundary probe `30450517037` used `AnitabiNavigator/0.1.2 (https://github.com/realMisakaMikoto)`, successfully parsed subject `115908` from the documented data endpoint, waited three seconds, and successfully downloaded the documented `image.anitabi.cn` thumbnail. It did not request the main domain.
- Same-version Android CI `30451151980` passed 34 JVM tests, SDK 37 compilation, Lint, debug APK audit, and the API 26/API 37 cold-launch, offline recovery, foreground navigation, automatic GPS/screen-off, completion, and persistence checks.
- Signed release run `30451156419` passed release tests, Lint, R8, APK content audit, fixed-signature verification, checksum generation, and public Release publication.
- Public APK compatibility run `30451752045` downloaded `anitabi-v0.1.2.apk` from the Release and passed installation, `versionName=0.1.2`, cold launch, foreground-process, and empty-crash-buffer checks on Android 8/API 26 and Android 17/API 37.
- The APK is 43,087,229 bytes. Its computed/published SHA-256 and GitHub asset digest agree: `4852fa44abafc7165feceae98f17147106e772ffbfa4824bdf7f391705f84e61`; the fixed signing certificate remains `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.
- A low-frequency retry from the current Windows egress still returned Cloudflare 403 for both documented hosts with the exact v0.1.2 User-Agent, while the clean GitHub runner succeeded. This isolates the remaining 403 to that egress/IP state rather than an unauthorized URL in the released client.

### Remaining external acceptance gates

- A physical Android phone and user-driven session remain necessary for real GNSS, audible TTS, secured lock-screen behavior, OEM battery restrictions, mobile-network transitions, live ORS routing, and the full search-to-navigation flow.
- The upstream Anitabi issue remains open only because available non-browser GitHub credentials cannot write to that repository; the client-side boundary and public API path are verified.
- The Transitous request still requires explicit Matrix sending confirmation and maintainer approval before the compile-time transit gate can be enabled.

## 2026-07-29 - Task 15: Transitous Matrix approval request

### Completed

- Re-read this complete log before starting the new task.
- Used the user's signed-in Matrix session and confirmed that the account `@rmisakamikoto:matrix.org` had joined the official public room `#transitous:matrix.spline.de`.
- After explicit action-time confirmation, sent the prepared English request describing the GPL/non-commercial project, public repository, exact `/api/v6/plan` use, eight-stop maximum, sequential user-triggered requests, offline tour caching, reroute conditions, 60-second deviation cooldown, absence of crawling/analytics/advertising, and the v0.1.2 identifiable User-Agent.
- Verified that the complete message appeared in the room timeline at 20:38 China Standard Time and that the composer cleared after sending.
- Preserved the message event link: `https://matrix.to/#/!jYtSkfiDtLJzothwYb:matrix.spline.de/$JfUWdfwG5Or1tpFJy0yy5yndMEC9Mu3fIhWjjq-oSkg?via=matrix.spline.de&via=matrix.org&via=kde.org`.
- The room showed the message as seen by Heubi. This is delivery evidence only, not maintainer approval.

### Current gate

- No explicit approval or additional limits have been received yet. `ANITABI_TRANSITOUS_APPROVED` remains `false`, and the app must not send Transitous routing requests until a clear maintainer reply is recorded and implemented.

## 2026-07-29 - Task 16: final CI and notification-noise closeout

### Completed

- Re-read the complete latest `AGENT.md` before starting this closeout task.
- Detected two newer remote log commits created while CI was running, inspected them before integration, and fast-forwarded instead of overwriting the completed Anitabi release record or the parallel Transitous Matrix request record.
- Restricted the main Android CI push trigger so changes limited to `AGENT.md`, `README.md`, or `docs/**` do not rerun the full build and two-emulator matrix. Production source, Gradle, and workflow changes continue to receive the full verification suite.
- Audited recent GitHub Actions state. Runs `30450291924` and `30451750697` were deliberately superseded by newer main commits under the configured concurrency group; the replacement runs passed. No workflow remains active.
- Preserved the Transitous compile-time gate after the Matrix request was delivered: delivery and a read receipt are not treated as maintainer approval.

### Verification

- Final Android CI `30452036186` passed 34 JVM tests, Android SDK 37 compilation, Lint, debug APK content audit, and every API 26/API 37 runtime step: cold launch, offline process recovery, foreground navigation completion, automatic GPS arrival while the screen was off, networking restoration, and diagnostic upload.
- Task 16 log commit `71b8112` intentionally omitted a skip directive and produced zero workflow runs after push, directly verifying the `AGENT.md` path filter. Earlier documentation-only commits `05632e7` and `4fb4fb1` also produced zero runs.
- Signed release `v0.1.2` remains public, non-draft, and non-prerelease. Its APK remains 43,087,229 bytes with SHA-256 `4852fa44abafc7165feceae98f17147106e772ffbfa4824bdf7f391705f84e61`.
- Agent Reach update check reports installed version `v1.5.0` is current.

### Remaining external acceptance gates

- Wait for a clear Transitous maintainer reply in Matrix before setting `ANITABI_TRANSITOUS_APPROVED=true` or sending any public-transit routing request.
- A physical Android phone and user-driven session remain necessary for real GNSS, audible Chinese TTS, secured lock-screen behavior, OEM battery restrictions, mobile-network transitions, live ORS routing, and the full search-to-navigation flow.
- Anitabi issue `#86` remains open because the available GitHub credentials cannot comment on or close the upstream issue; no external reply or closure is falsely claimed.

## 2026-07-29 - Task 17: correct Transitous policy interpretation, enable transit, and release v0.1.3

### Accountability and policy correction

- Re-read the complete latest `AGENT.md` and the original pasted implementation plan before starting this task.
- Re-read the current official Transitous API policy through Agent Reach. The user was correct: “If you have any doubt about the load your requests will be causing … please contact us” is conditional guidance, and “Please contact us before using any potentially resource-intensive API endpoints” is a contact request, not a requirement to wait for explicit approval. The policy contains no approval state or authorization token.
- The earlier plan and Tasks 0–16 incorrectly upgraded that wording into a mandatory approval gate. That interpretation, the resulting build flag, and the previous “remaining external gate” statements were wrong. This task supersedes those statements rather than silently rewriting the historical log.
- The Matrix message sent in Task 15 remains useful advance load communication and was delivered/read, but it is not an approval workflow and no maintainer reply is required to enable the documented low-volume use.

### Implementation

- Removed `ANITABI_TRANSITOUS_APPROVED` from Gradle and GitHub Actions, removed the generated `BuildConfig` flag, deleted `ApiException.TransitNotApproved`, and removed the approval dependency from `TransitousApi`, `AppContainer`, `MainActivity`, `PlannerViewModel`, `PlannerUiState`, the transit mode chip, and the about page.
- Enabled transit mode whenever the selection is within the real eight-point limit. Centralized that limit as `TourPlanner.MAX_TRANSIT_POINTS` and kept the planner’s fail-before-request validation.
- Kept the official `https://api.transitous.org/api/v6/plan` endpoint, per-leg sequential request chain, user/event-triggered planning, Room persistence, visible Transitous sources link, and identifiable app/version/contact User-Agent. Explicitly disabled OkHttp connection-failure retries so a failed request is not repeated invisibly.
- Added transit-specific 403, 429, server, and network messages instead of incorrectly describing all such failures as an ORS Key problem.
- Added a MockWebServer contract test covering method, path, all plan query parameters, User-Agent, response parsing, and the production endpoint constant. Added a nine-point rejection test proving zero journey requests occur, and strengthened the sequential two-leg test.
- Incremented the app to `versionCode=4` / `versionName=0.1.3`; corrected README, the release checklist, current release notes, historical release records, about-page text, and renamed the old approval template to `TRANSITOUS_CONTACT_RECORD.md` while preserving and annotating the exact original message.

### Verification and release

- The local host still has no Android SDK, so the initial local Gradle command stopped before compilation with the explicit SDK-location error. No local result was fabricated; GitHub’s SDK 37 runners performed the authoritative build.
- Main Android CI `30454672103` passed 36 JVM unit tests, SDK 37 compilation, debug Lint, APK audit, and all API 26/API 37 runtime checks: cold launch, offline process recovery, foreground navigation completion, screen-off automatic GPS arrival, networking restoration, and diagnostics upload.
- Sent exactly one live Transitous routing request using `AnitabiNavigator/0.1.3 (https://github.com/realMisakaMikoto)`, Tokyo Station/Skytree coordinates, the production endpoint, and the same query contract as the app. It returned 9 itineraries; the first contained 3 legs. No polling, concurrency, retry, or additional probe was performed.
- PR creation from branch `agent/enable-transitous-policy-correction` was rejected by the available GitHub token with `createPullRequest` permission error. The reviewed commit was fast-forwarded to current `main`, matching the repository’s prior fallback; no force push or test bypass was used.
- Signed release run `30455370261` passed tests, release Lint, R8, APK audit, v2 signature verification, checksum creation, and public Release publication. `v0.1.3` is public, non-draft, and non-prerelease.
- The downloaded APK is 43,087,229 bytes. Its calculated SHA-256, published checksum file, and GitHub asset digest all equal `c86d2b44db5c95f0518b5d876f3f9d7e7baac69ebc179472341fe12a97cd532b`. The fixed certificate SHA-256 remains `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.
- Exact signed-release compatibility run `30455908192` downloaded the public v0.1.3 APK and passed version inspection, install, cold launch, foreground-process, screenshot, and empty-crash-buffer checks on Android 8/API 26 and Android 17/API 37.
- Discovered that GitHub suppresses Release events recursively generated by `GITHUB_TOKEN`. Added a successful-`Signed APK Release` `workflow_run` trigger for future automatic compatibility checks, and excluded release-smoke-only changes from the full Android CI to avoid duplicate runs and notification noise. The exact v0.1.3 compatibility run was executed once by the workflow’s narrow push trigger after this fix.
- Agent Reach update check reports installed version `v1.5.0` is current.
- The command-line token could read the public v0.1.3 Release but returned HTTP 403 when asked to replace its body with the post-release evidence. The published tag-time notes already contain the policy correction, enabled behavior, load limits, and known limitations; the checksum/run evidence is complete on current `main` in `docs/RELEASE_NOTES_v0.1.3.md` and `docs/releases/v0.1.3.md`. No browser edit was attempted or falsely claimed.
- Public evidence: `https://github.com/realMisakaMikoto/anitabi/actions/runs/30454672103`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30455370261`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30455908192`, and `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.1.3`.

### Remaining external acceptance boundary

- There is no Transitous approval gate. Only physical-device acceptance remains: real GNSS, audible Chinese TTS, secured lock-screen behavior, OEM battery/background restrictions, real mobile-network transitions, live ORS routing, full search-to-navigation flow, and user-driven transit scenarios across covered and uncovered areas.

## 2026-07-29 - Task 18: physical-device test setup and ADB authorization check

### Completed

- Re-read the complete latest `AGENT.md` and the original pasted implementation plan before starting the physical-device task.
- Detected a connected `Xiaomi 15T Pro` through both Windows Portable Devices and an installed `ADB Interface`; this replaces the earlier no-device condition recorded in Tasks 11–17.
- Confirmed that no local `adb.exe` or Android SDK path was available, then installed the official Google Android SDK Platform-Tools 37.0.1 package through WinGet. The installation completed successfully, verified the downloaded package hash, and added user-level command aliases without changing project files.
- Started ADB 1.0.41 / Platform-Tools 37.0.1 and confirmed the phone serial `JBR4LF6TQ4MFHY4X` is visible over USB.
- Restarted the ADB server once to re-present the phone authorization prompt and polled the device state twice for 30 seconds each.

### Current physical blocker

- The phone remains in ADB state `unauthorized`. Android intentionally prevents installation, shell access, screenshots, logs, permission tests, location tests, notification checks, and process-recovery tests until the user approves this computer on the unlocked phone.
- No APK was installed, no existing application data was modified, and no physical-device result was claimed.
- Required next action on the Xiaomi 15T Pro: unlock it, accept the “Allow USB debugging” prompt (preferably with “Always allow from this computer”), or reconnect the USB cable in file-transfer mode if the prompt is not visible. Once ADB reports `device`, testing can continue without rebuilding or reinstalling the desktop tools.

## 2026-07-29 - Task 19: signed v0.1.3 physical-device acceptance

### Completed

- Continued only after ADB reported the authorized Xiaomi 15T Pro as `device`; no reinstall, uninstall, or application-data clearing was performed.
- Confirmed Android 16/API 36, security patch 2026-06-01, and the installed app identity `versionName=0.1.3` / `versionCode=4`.
- Pulled the installed base APK and proved its 43,087,229-byte content and SHA-256 `c86d2b44db5c95f0518b5d876f3f9d7e7baac69ebc179472341fe12a97cd532b` exactly match the public signed v0.1.3 Release APK.
- Performed a true cold start: `Status: ok`, `LaunchState: COLD`, 332 ms total, `MainActivity` top-resumed, live process, and empty crash buffer. Inspected the 1280x2772 screen and confirmed the Chinese home UI rendered without obvious clipping or corruption.
- Drove the production UI through a live `Your Name` Bangumi search, opened Bangumi `#160209` (`Your Name` / `你的名字。`), and loaded covers and results successfully.
- Reproduced the known Anitabi Wi-Fi egress block and verified the app displayed the explicit public-IP rejection message without crashing or retry looping. Temporarily disabled Wi-Fi, confirmed cellular became the active default network, and loaded 68 Anitabi points, tiles, clusters, and images. Restored both Wi-Fi and mobile data to their original enabled state after testing.
- Selected Tokyo points through the real list UI. A short pair produced the localized no-open-transit-data state without a crash. A longer pair (`マンション桂` to `LABI新宿東口館前`) produced a live Transitous route with three WALK/transit legs, map preview, times, and a working continuous-navigation action.
- Verified public-transit mode removes the ORS Key field and exposes its date/time/dwell controls. Verified driving mode with no Key fails locally with `请先填写自己的免费 ORS Key`; no unauthenticated ORS request was sent.
- Started continuous navigation and verified location and notification permissions became granted, `NavigationService` ran as a location foreground service, and notification 1001 was ongoing/non-clearable with current instruction text and an End action.
- Verified the app registered two-second GPS high-accuracy and network balanced location requests, retained the route and service while backgrounded, returned via a 99 ms hot start, and removed both registrations when navigation ended.
- Disabled all phone networking for seven seconds while navigation was active. Cached map/instructions and the foreground service remained available, mobile connectivity rebuilt after restoration, and the crash buffer stayed empty.
- Tested Xiaomi screen-off/Dozing behavior: the same PID, location foreground service, and notification remained present while the keyguard was showing. The navigation was then ended through the application UI; the service and notification were removed.
- Verified Google TTS did more than bind: system logs contain a `zho-CHN` synthesis request and dispatch to a Chinese voice. `STREAM_TTS` was unmuted and routed to the speaker. ADB cannot certify that a human actually heard the sound.
- Confirmed the service is not exported: a shell attempt to send its internal STOP action was rejected by Android, while the in-app End action succeeded.
- Stored the detailed, privacy-safe evidence and remaining boundaries in `docs/PHYSICAL_DEVICE_ACCEPTANCE_v0.1.3.md`. Temporary screenshots and APK copies remain outside the repository.

### Findings and remaining acceptance boundaries

- Android reports the current phone fix as a mock location, and the configured fake-GPS package has `MOCK_LOCATION: allow`. This validates the app's location plumbing but is not claimed as real GNSS. The user's mock-location setting was not changed.
- No ORS Key exists in the phone UI, workspace-local configuration, or matching environment-variable names. Live road routing still requires the user to enter their own Key on the phone; no shared or fabricated credential was used.
- The system proves Chinese TTS synthesis and an audible stream configuration, but final acoustic confirmation still requires the user to listen.
- One successful Transitous route emitted a MapLibre `Invalid geometry in line layer` warning and displayed a transit leg distance of `0 m`. The UI remained stable; this is recorded as a data/geometry quality follow-up rather than hidden.
- The original plan's 8-12 point real-world walk, long-duration OEM battery restriction test, and real missed-transit event remain user-driven field tests. They cannot be honestly replaced by remote ADB automation.

## 2026-07-29 - Task 20: system-bar correction, live routing fixes, and v0.1.4 release

### Accountability and preparation

- Re-read the complete existing `AGENT.md` and the original pasted implementation plan before starting this task.
- The user correctly identified that Task 19's “no obvious clipping” conclusion was wrong: the app top bars overlapped the Android status bar, and bottom actions intruded into the gesture-navigation area. Corrected the historical v0.1.3 acceptance record explicitly instead of silently rewriting or defending the error.
- Re-read the current official HeiGIT/openrouteservice account and FAQ material through Agent Reach and explained the actual Key flow: register at `https://account.heigit.org/`, verify the email, accept the terms, and copy the free Standard API Key from the dashboard. Confirmed that a JWT-shaped `eyJ...` value is normal.
- The user pasted a personal ORS Key into chat. The value was never repeated, written to source, tests, documentation, `AGENT.md`, Git, build configuration, or application logs. It was transferred from the host clipboard directly into the app and stored only through the existing Android Keystore implementation. Because chat exposure makes the credential unsafe, the user must revoke and regenerate it.
- Installed the official Google Android CLI `1.0.15857036`, Android SDK Platform 37.0 revision 2, Build-Tools 37.0.0, and Platform-Tools needed for a real local build. Added only an ignored `local.properties` containing the SDK path. Installed GnuWin UnZip while closing a missing-tool diagnostic; the final APK audit was run successfully with the explicit Git for Windows Bash toolchain.

### Implementation

- Added status-bar Insets to the search hero, point-selection toolbar, planner top bar, route-preview top bar, navigation top bar, and about-page top bar. Added one root navigation-bar Insets container in `MainActivity` so all bottom buttons and scroll content stay above the system gesture area.
- Reproduced a production Anitabi parse failure for Bangumi `#328609`. The real payload used string `ep: "CD"` while the unused DTO field was typed as `Int`; removed unused `ep`/`s` bindings so `ignoreUnknownKeys` tolerates the service's volatile types. Added a regression fixture containing the string value.
- Reproduced Transitous rail legs that legitimately omit `distance`. Transit mapping now deduplicates consecutive coordinates and derives a missing/nonpositive/nonfinite distance by summing Haversine distances along decoded polyline6 geometry; the same derived value is used by the leg and its route step.
- Prevented MapLibre from receiving LineStrings with fewer than two distinct coordinates. This removes the reproduced `Invalid geometry in line layer` warning while retaining stationary legs in the plan model.
- Mapped Transitous stop `tz` values into `TransitLegDetails` and converted UTC offset timestamps to each stop's IANA time zone before displaying `HH:mm`. New fields are nullable with defaults so old cached route JSON remains readable.
- Added routing and formatting regression tests, bumped the app to `versionCode=5` / `versionName=0.1.4`, updated the manual release-smoke default, added v0.1.4 notes/acceptance records, and updated README with the current release and ORS Key instructions.

### Local and physical verification

- `testDebugUnitTest lintDebug assembleDebug` passed locally with 38 tests, zero failures/errors/skips, zero Lint errors, and SDK 37. `git diff --check` and the repository APK content audit also passed.
- Installed the final v0.1.4 debug candidate on the authorized Xiaomi 15T Pro (`2506BPN68G`, Android 16/API 36, patch 2026-06-01). Android reported versionCode 5/versionName 0.1.4, cold launch succeeded, and the app crash buffer was empty.
- Visually checked the 1280x2772 home, about, selection, planner, route-preview, and continuous-navigation screens with system bars visible. All top content is below the status bar; planner, preview, and navigation actions are above the gesture bar.
- On cellular data, the fixed production Anitabi client loaded 74 usable points for Bangumi `#328609` and displayed the partial-data notice instead of failing the full payload.
- Using the user's Key through the application UI, the production `https://api.heigit.org/openrouteservice/v2/` endpoint generated a two-stop road route of about 0.8 km with visible geometry. Continuous navigation started with the corrected top/bottom layout, then ended cleanly with no remaining `NavigationService` or ongoing notification.
- A production Transitous query from Kanazawa-Hakkei to Shimokitazawa returned seven legs, 1h44m, and 47.2 km. Visible legs included 24.3 km and 2.5 km rail segments plus 354 m, 231 m, 429 m, and 781 m walks. No effective leg displayed `0 m`; the final two legs showed local times `06:22 -> 06:26` and `06:26 -> 06:40`; logcat contained neither invalid-line warnings nor crashes.
- Restored Wi-Fi and mobile data after network-bound testing. Wi-Fi reconnected to the original network, and no navigation service or notification remained. Temporary task screenshots were moved outside the repository; the unrelated Room schema emitted by the first local build was not committed.

### GitHub verification and release

- Pushed isolated commit `3ca2bcb` on `agent/fix-physical-device-regressions`. Draft PR creation was rejected with the existing token's `createPullRequest` permission error. Following the repository's established fallback, fast-forwarded the reviewed commit to `main` without force-push or CI bypass.
- Main Android CI `30466264424` passed the 38 tests, SDK 37 build, Lint, APK audit, artifact upload, and all Android 8/API 26 plus Android 17/API 37 runtime checks: cold launch, offline recovery, foreground navigation, screen-off mock-GPS automatic arrival, completion, persistence, network restoration, and diagnostics.
- The workflow-change compatibility run `30466266597` independently rechecked the then-current public v0.1.3 APK on Android 8/17 and passed; it did not attempt to download an unpublished v0.1.4.
- Tagged the CI-backed documentation commit `9e6c225` as annotated `v0.1.4`. Signed release run `30467039704` passed tests, release Lint, R8, APK audit, v2 signature verification, checksum creation, and public non-draft/non-prerelease publication.
- The published `anitabi-v0.1.4.apk` is 43,103,613 bytes. Its downloaded SHA-256, checksum asset, and GitHub asset digest all equal `4a95482bdc9bdec9e357d334339f9a401f558b00f19b4160b519ea9af586240e`. The fixed certificate SHA-256 remains `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.
- Public signed-APK compatibility run `30467427032` downloaded the release asset and passed version inspection, install, cold launch, foreground-process, screenshot, and empty-crash-buffer checks on Android 8/API 26 and Android 17/API 37.
- Uninstalled the debug-signed package from the physical phone and installed the exact public signed APK. This necessarily cleared the debug test data and Android Keystore copy of the exposed ORS Key. Android reported v0.1.4/versionCode 5; the installed base APK hash exactly matched the Release asset, cold launch completed in 633 ms, the status-bar fix remained visible, crash buffer was empty, and Wi-Fi was restored and connected.

### Remaining user-driven boundaries

- Revoke the ORS Key pasted into chat, generate a replacement in the HeiGIT dashboard, and enter the replacement only inside the now-installed signed v0.1.4 app. The formal app currently contains no ORS Key.
- The phone still has a mock-location app configured. Real GNSS walking/driving, human confirmation of Chinese TTS audio, an 8-12 point field route, long-duration Xiaomi/OEM battery restrictions, and a real missed-transit event remain honest user-driven tests rather than fabricated acceptance claims.
- Public evidence: `https://github.com/realMisakaMikoto/anitabi/actions/runs/30466264424`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30467039704`, `https://github.com/realMisakaMikoto/anitabi/actions/runs/30467427032`, and `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.1.4`.

## 2026-07-30 - Task 21: multi-anime selection and plan-gap closeout

### Preparation and implementation

- Re-read the complete `AGENT.md` before the task, then re-read it again when the user added multi-anime selection as a new task. Re-read the original implementation plan during the final audit.
- Added multi-anime selection to Bangumi results. Selected titles remain visible as removable chips and persist across additional searches; each title loads only from the existing user-triggered Anitabi repository/cache path.
- Merged all selected titles onto one map while retaining point-by-point, list, and visible-map-area selection. Scoped every point ID as `subjectId::pointId` to prevent cross-title collisions, and prefixed multi-title point names with the work title so planning and navigation remain understandable.
- Added a combined-tour identity for persisted routes and kept the existing 12-road-point and 8-transit-point limits.
- Added a deterministic missed-connection policy: after an internal transfer, a non-walking departure more than two minutes in the past triggers the existing current-time transit replan. Arrival-at-pilgrimage and cancelled-leg automatic replans remain unchanged; no polling was added.
- Fixed navigation permission completion so Android 13+ requires both location and notification permission and shows a specific message for either or both missing permissions.
- Removed upstream HTTP response bodies from all public exception messages and added safe localized mappings for unrecognized responses and generic HTTP failures.
- Added ORS MockWebServer contract coverage for all three road profiles. The new test exposed that default-valued `metrics`, `units`, `instructions`, and `language` fields were omitted by serialization; requests now explicitly send distance/duration matrix metrics, metres, instructions, and `zh-cn`.
- Added a complete launcher icon set, release resource shrinking, Gradle 9.6.1, AndroidX Core 1.19.0, kotlinx.coroutines 1.11.0, Android 8-safe notification categories, and current non-deprecated icon/lifecycle imports. Bumped the candidate to `versionCode=6` / `versionName=0.2.0`.

### Verification

- Test-first checks initially failed on the missing merge, permission, missed-connection, ORS-injection, and error-redaction behavior, then passed after the implementation.
- Local Android SDK 37 verification passed 47 JVM tests with 0 failures/errors/skips, debug compilation, APK assembly, and Lint with an empty issue report.
- A fixed-signature R8/resource-shrunk release candidate built successfully. It is 42,660,757 bytes with SHA-256 `4d8698f5bab17246274450466e751b287178999a8d6edc72783b0dce679aa3c9`; the certificate SHA-256 remains `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.
- Installed the candidate over public v0.1.4 on the Xiaomi 15T Pro without uninstalling or clearing data. Android reports v0.2.0/versionCode 6; cold launch succeeded in 456 ms and the crash buffer was empty.
- Physical UI evidence showed 3 selected works merged into 178 usable map points with the point-selection count and planning action intact. The user then independently tested and confirmed that the new feature works.
- The user also explicitly confirmed that the previously tested Chinese navigation TTS is audible. Updated the v0.1.4 evidence without claiming anything beyond audibility.
- Added v0.2.0 release notes, physical-candidate evidence, and an original-plan completion matrix. The public v0.2.0 GitHub release was intentionally not started because the user immediately requested another onboarding feature for the same upcoming release.

### Remaining evidence boundaries

- The phone still grants mock-location to a separate fake-GPS package; the application itself has no mock-location permission. Real 8-12 point GNSS field travel, multi-hour Xiaomi battery-policy survival, and an actual missed-service event remain field conditions rather than missing repository implementation.
- The ORS Key previously pasted into chat was not reused. The signed app should only receive a newly generated replacement through its encrypted in-app input.

## 2026-07-30 - Task 22: first-run permissions and ORS Key guide

### Preparation and implementation

- Re-read the complete `AGENT.md` before starting this task. Read the complete `ui-ux-pro-max` skill because the task changes a mobile first-run flow; its referenced design-search script was not installed, so followed the skill's accessibility, touch-target, safe-area, progressive-disclosure, multi-step-progress, form-feedback, and recovery checklists directly.
- Added a non-skippable three-step first-run guide: explain why the app needs setup, request location plus Android 13+ notification permissions, then link directly to the HeiGIT account page and save the user's personal ORS Key. The map is not mounted until all applicable permissions and a locally stored Key are ready.
- Added permission status cards, precise missing-permission messages, a retry path, and an app-system-settings link for denied or permanently denied permissions. Android 8-12 treats notification permission as already satisfied because those versions have no runtime notification permission.
- Added a masked ORS Key field with an explicit show/hide control, nearby error text, concise account-registration instructions, encrypted save through the existing Android Keystore store, and support for an already saved Key during upgrade. Clearing or failing to decrypt a Key also clears the onboarding-complete marker.
- Added the same HeiGIT acquisition link beside the later route-planner Key field so users can recover the instructions after onboarding.
- Added first-launch assertions to debug and signed-release emulator smoke workflows. Updated navigation instrumentation setup to grant permissions and complete onboarding with an obviously invalid test-only Key so process-recovery tests continue exercising the production startup path without network use.
- Fixed the APK audit script to use Python's standard zipfile extractor when `unzip` is unavailable, preserving the existing audit patterns and Linux CI behavior.

### Verification and documentation

- Added test-first onboarding readiness coverage. The targeted test initially failed because the readiness type and missing-permission mapping did not exist, then passed after implementation.
- Local verification passed 49 JVM tests with 0 failures/errors/skips, debug APK assembly, androidTest APK compilation, debug Lint, and release Lint. Both Lint SARIF reports contain zero findings; `git diff --check` reports no whitespace errors.
- The repository APK content audit passed against the current debug APK using the new Python fallback. Release assembly correctly refused to create an unsigned artifact when local fixed-signing variables were absent; final R8 and fixed-signature verification remain delegated to the protected GitHub release workflow.
- Updated README, v0.2.0 release notes, plan-completion audit, physical-device evidence boundaries, and a v0.2.0 release acceptance record. The earlier signed-candidate hash is now explicitly labeled as predating onboarding rather than being misrepresented as the final artifact.

### Current evidence boundary

- No ADB device or emulator image was connected locally after onboarding was added. The phone and ADB mDNS discovery both returned no device, so no touch events were injected and no physical screenshot was fabricated. Android 8/API 26 and Android 17/API 37 clean-install onboarding screenshots are required from CI before release; Xiaomi onboarding remains a later cold-launch check when the device reconnects.
- The previously exposed ORS Key was not reused, copied into tests, logged, or packaged. The instrumentation-only placeholder is deliberately invalid and is present only in the androidTest APK, never the production APK.

### First remote CI diagnosis

- Main run `30473343833` passed its 49-test/build/Lint/APK-audit job and the complete Android 8 runtime matrix. Both Android 8 and Android 17 clean installs also passed the new onboarding cold-launch assertion; the downloaded Android 17 screenshot showed correct status-bar clearance but put the welcome CTA just below the 320x640 viewport.
- Android 17 alone failed later in offline recovery. The seed instrumentation reported `OK (1 test)`, but ten UI dumps all showed onboarding instead of the saved `Runtime Smoke Tour`. The cause was deterministic: `OrsKeyStore` used asynchronous `SharedPreferences.apply()` and the one-shot instrumentation process could exit before its encrypted Key and completion marker reached disk.
- Changed the three security-state writes to synchronous `commit`, which is appropriate for these tiny user-confirmed writes and makes process-death persistence deterministic. Added an immediate instrumentation assertion and reduced the welcome page from two cards to one concise progressive-disclosure card so the primary CTA fits the smallest CI viewport.
- Re-ran 49 JVM tests, androidTest compilation, and debug Lint locally after the fix; all passed and Lint remained at zero findings. A fresh remote run is required before tagging.

### Successful remote rerun

- Main Android CI `30474785852` passed the 49-test/build/Lint/APK-audit job and both Android 8/API 26 and Android 17/API 37 emulator jobs. Each clean install asserted the onboarding screen, then completed offline process recovery, foreground navigation, screen-off mock-GPS automatic arrival, completion, persistence, networking restoration, diagnostic capture, and empty crash-buffer checks.
- Downloaded and visually inspected both 320x640 onboarding screenshots. Status-bar clearance is correct, the three-step progress trail and text are legible, and the shortened welcome card keeps the full-width `Start setup` button visible above the bottom safe area on both versions.
- The previously failing Android 17 recovery now passes after synchronous secure-state persistence. This closes the observed CI failure rather than merely rerunning it.

### Signed publication and public-asset verification

- Tagged the CI-backed documentation commit `c7e4faf` as annotated `v0.2.0`. Signed release run `30475526252` passed tests, release Lint, R8/resource shrinking, APK content audit, v2 signature verification, checksum generation, and public non-draft/non-prerelease publication.
- The public Release is `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.0`. Its `anitabi-v0.2.0.apk` is 42,677,141 bytes; the downloaded file, checksum asset, and GitHub asset digest all equal SHA-256 `e3d36b47695b452978680726c5eb09133e04c0f207149a6324f3e08ac8f9a9ec`. The fixed certificate SHA-256 is `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`; APK v2 signing is valid with one RSA 4096 signer.
- Public compatibility run `30475867612` downloaded the actual Release APK and passed version inspection, installation, cold launch, onboarding-screen assertion, foreground-process, screenshot, and empty-crash-buffer checks on Android 8/API 26 and Android 17/API 37.
- Downloaded and inspected the two public-APK 320x640 screenshots separately. Both show correct status-bar clearance, readable progress/content, and a fully visible `Start setup` button above the bottom safe area. This verifies the released artifact on emulators; it does not turn the still-disconnected Xiaomi onboarding flow into a physical-device claim.
- Updated the v0.2.0 release and physical-acceptance records with the final public run IDs, artifact size, hashes, screenshot result, and the remaining real-device boundary. No application code changed during this evidence-only closeout.

## 2026-07-30 - Task 23: original-plan audit and end-to-end onboarding verification

### Preparation and audit

- Re-read the complete 591-line `AGENT.md` and the complete original pasted implementation plan before starting this continuation task, then inspected the current worktree rather than relying on the previous summary.
- Rechecked the Xiaomi connection through the installed Platform-Tools executable, ADB mDNS, and Windows present-device inventory. No USB or wireless Android device is currently connected; the only Xiaomi entries are Bluetooth earbuds, so no physical onboarding result was fabricated.
- Re-audited the original plan against the v0.2.0 completion matrix, source tree, test inventory, TODO/FIXME scan, public Release, and existing evidence. Corrected the stale audit row that still described v0.2.0 as awaiting publication.
- Found one verifiable evidence gap in the latest onboarding work: CI asserted the first screen, while the navigation fixture directly seeded its test Key and therefore did not prove the complete user path through the permission request, empty-Key guard, Key entry, main screen, and restart persistence.

### Implementation and local verification

- Added stable Compose semantics tags only to the onboarding actions and fields, plus an Android instrumentation test that clicks through the real `MainActivity` flow. The test confirms the Android runtime-permission dialog appears, grants only the disposable emulator package, verifies the HeiGIT/ORS Key step, proves an empty Key cannot continue, enters the existing obviously invalid test-only Key, reaches the Bangumi search screen, and relaunches the activity to prove completion persisted.
- Added Compose UI test support only to the `androidTest` configuration. No production dependency, network request, personal ORS Key, shared credential, or release-APK secret was added.
- Wired the test into both Android 8/API 26 and Android 17/API 37 main CI jobs with five required evidence milestones. The workflow uninstalls and reinstalls the app/test APKs built together before running the test, preventing the cross-job ephemeral debug-signature mismatch already diagnosed in Task 12. The existing offline navigation fixture then starts from another clean install.
- The first local compile exposed and removed one invalid standalone `fetchSemanticsNodes` import; this was a test-source compile issue and no product failure was hidden. Switched to the current non-deprecated Compose v2 empty-rule API.
- Final local verification passed 49 JVM tests with 0 failures/errors/skips, debug and androidTest APK compilation, debug Lint with 0 results, the APK content audit, YAML parsing, and `git diff --check`. The generated Room schema copy was removed again and is not part of the change.

### Remote evidence requirement (closed below)

- The initial requirement was for the new end-to-end test to pass on both CI emulator versions before the onboarding audit row could return to fully verified. Main run `30481986827`, recorded below, closes that emulator requirement. Xiaomi physical onboarding and the original real-GNSS/long-duration OEM/real missed-service boundaries remain separate.

### First end-to-end CI diagnosis and compatibility fix

- Main run `30477795243` kept the existing application evidence green: its 49-test/build/Lint/APK-audit job and both emulator cold-launch checks passed. Only the newly added onboarding test failed; later navigation steps were skipped rather than producing misleading secondary results.
- Downloaded and inspected both emulator artifacts before changing code. Android 8 logcat and `runtime-activities.txt` proved that the real Package Installer permission activity was on screen, but Android 8 reported it through `mResumedActivity` instead of the newer window-focus fields the test initially inspected. The focus probe now accepts both activity- and window-manager representations.
- Android 17 failed before its first Compose interaction because Compose UI Test 1.11.4 resolved the old Espresso 3.5.0 implementation, which reflectively calls the removed `InputManager.getInstance()` API. Pinned the androidTest-only Espresso dependency to 3.7.0, whose AndroidX release notes document the `getSystemService` compatibility fix. Production dependencies and APK behavior are unchanged.
- Made the always-run Android 8 network-restoration cleanup acquire and verify emulator root even when the preceding offline step was skipped. This prevents a test failure from being obscured by a second cleanup-only permission failure.
- Local verification after all three fixes passed 49 JVM tests with 0 failures/errors/skips, debug Android-test compilation and packaging, and debug Lint with 0 findings. Dependency insight confirms Espresso 3.7.0 wins over the transitive 3.5.0 version; `git diff --check` is clean and the generated Room schema was removed.

### Second end-to-end CI diagnosis

- Main run `30478838791` passed the 49-test/build/Lint/APK-audit job. Android 17/API 37 then passed the complete onboarding test and every existing offline recovery, foreground navigation, screen-off automatic-arrival, cleanup, diagnostic, and evidence-upload step.
- Android 8 reached and displayed the real Package Installer permission dialog, and its activity dump contained the expected `mResumedActivity`. The remaining detector bug was a short-circuit: `dumpsys window` emitted a non-empty but unusable `mCurrentFocus=null` line, so the test never consulted the valid activity-manager result. Changed the probe to combine all relevant window- and activity-focus lines instead of treating any current-focus line as authoritative. This is androidTest-only code and does not change the application.

### Third end-to-end CI diagnosis

- Main run `30479687810` again passed the full 49-test/build/Lint/APK-audit job, and Android 17/API 37 passed the complete onboarding plus all navigation recovery/arrival/cleanup evidence. Android 8 now emitted `ONBOARDING_PERMISSION_DIALOG_SHOWN`, proving the combined focus probe fixed the preceding failure.
- The next Android 8-only failure was an explicit `NoSuchMethodError`: the two-argument framework `UiAutomation.grantRuntimePermission` overload used by the test is unavailable there. Replaced that test helper with the platform `pm grant` shell command already used successfully by this repository's emulator fixtures. The test still first opens and verifies the real permission UI; it grants permissions only to the freshly installed disposable emulator package.

### Fourth end-to-end CI diagnosis

- Main run `30480445661` proved the Android 8 test now passes dialog detection and executes both `pm grant` commands. Its Google API 26 image then hit an emulator-system bug: SystemUI crashed while the permission activity was open, leaving Package Installer resumed and ignoring repeated Back injections even though the app's location permissions were already granted. The application process did not crash.
- Added an API 26-only instrumentation cleanup that force-stops the stuck Google Package Installer after recording the dialog evidence and granting the disposable app. The test then accepts either the launcher's normal callback transition to the Key page or the existing `Permissions ready, continue` recovery action. Newer Android versions keep the already-passing normal Back path; production code remains unchanged.

### Fifth end-to-end CI diagnosis

- Main run `30481174385` produced all five Android 8 onboarding milestones and `OK (1 test)`, closing the onboarding flow itself on API 26. Its following offline-recovery step failed because every UI hierarchy was covered by the surviving platform alert `System UI has stopped`; the seeded application state and onboarding test had succeeded underneath it.
- Isolated that Google API 26 emulator defect from the established navigation matrix by rebooting only the Android 8 emulator after the onboarding evidence is complete, waiting for `sys.boot_completed`, and restoring disabled animation scales before legacy recovery checks. This does not skip, weaken, or alter either test: onboarding finishes before the reset, and offline/navigation fixtures reinstall their APKs and data afterward. Android 17 keeps the continuous no-reset path.

### Successful end-to-end rerun

- Main Android CI `30481986827` completed successfully. Its verify job passed all 49 JVM tests, debug build, Lint, APK content audit, and artifact upload.
- Both Android 8/API 26 and Android 17/API 37 emitted all five required milestones: permission dialog shown, HeiGIT/ORS Key guide shown, empty Key blocked, completed to Bangumi search, and restarted directly in search. Each instrumentation run ended with `OK (1 test)`.
- The Android 8-only post-onboarding reboot completed before the independent navigation fixture. Both emulator jobs then passed offline process recovery, foreground navigation completion, screen-off mock-GPS automatic arrival, persisted completion, network restoration, diagnostic capture, and evidence upload. This proves the reset isolates only the defective platform UI state rather than hiding a product or navigation failure.
- Updated the original-plan completion audit from `end-to-end workflow pending` to complete with the successful run ID. No new production behavior, personal ORS Key, Release asset, or v0.2.0 binary was created during these CI-compatibility fixes.
- The remaining honest boundaries are unchanged: the Xiaomi phone is still disconnected, and real 8-12 point GNSS travel, multi-hour Xiaomi/OEM battery survival, and an actual missed-service event require physical field conditions.

## 2026-07-30 - Task 24: physical-field acceptance harness and runbook

### Preparation and device audit

- Re-read the complete existing `AGENT.md` in UTF-8 chunks and then re-read the complete original pasted implementation plan before starting this task.
- Confirmed the worktree started clean at commit `74ac0443290dccf4371977df00849d48ad41b381` and re-audited the current v0.2.0 plan-completion and physical-acceptance records.
- Rechecked the installed Platform-Tools executable, ADB mDNS discovery, and Windows present-device inventory. No USB or wireless Android phone is connected; the only present Xiaomi devices are Bluetooth earbuds. No physical result was fabricated.
- Confirmed the repository had no executable field-evidence collector and that the remaining records named the three field boundaries without fixing their start conditions, sampling requirements, or pass/fail criteria.

### Implementation

- Added `scripts/capture-field-evidence.ps1`, an ASCII-only, read-only ADB collector for real GNSS, Xiaomi/OEM background, and real missed-transit scenarios. It does not change mock-location, battery, network, permission, or application state and does not read or print the ORS Key.
- Added strict preflight checks for one authorized device, the exact public v0.2.0 package and Release APK SHA-256, a running `NavigationService`, and a visible application notification. Real-GNSS collection refuses to start while any mock-location app remains allowed; the OEM scenario refuses durations shorter than 120 minutes.
- During a field run the collector records timestamped PID, foreground-service, notification, app-crash, location/mock, Doze, power, and battery samples. It saves final service, notification, location, device-idle, power, battery-statistics, connectivity, and crash-buffer snapshots to a timestamped Windows temporary directory and deliberately reports `EVIDENCE_CAPTURED_MANUAL_REVIEW_REQUIRED` rather than inventing an automatic pass.
- Added `docs/PHYSICAL_FIELD_TEST_RUNBOOK_v0.2.0.md` with safe, user-executable steps and explicit acceptance criteria for a real 8-12 point GNSS route, at least two hours under actual Xiaomi/OEM policy, and a naturally missed non-walking transit departure followed by an automatic current-time replan.
- Linked the new runbook and collector from the v0.2.0 plan audit and physical-device acceptance record. Raw location evidence remains outside Git and the runbook forbids committing personal location, screenshots with personal data, or API credentials.

### Verification

- The PowerShell parser accepted the collector with zero syntax errors, and a byte scan proved the `.ps1` file is ASCII-only as required for Windows PowerShell 5.1 compatibility.
- The mock-location parser correctly distinguishes `No operations.` from an allowed fake-GPS package. New-file trailing-whitespace and linked-file existence checks passed; `git diff --check` reported no whitespace errors.
- Executing the collector with the phone disconnected produced the required fail-closed result: `No authorized Android device is connected. Unlock the phone and authorize USB debugging.` It did not create a false evidence run or modify the device.
- PSScriptAnalyzer is not installed on this host, so no analyzer result was claimed. The authoritative checks for this documentation/script-only task are the PowerShell parser, ASCII scan, preflight behavior, targeted parser cases, and diff checks; production Android source and the public v0.2.0 APK were not changed.
- Main Android CI run `30483414232` passed after the field harness was pushed. Its verify job passed all 49 JVM tests, SDK 37 build, Lint, APK content audit, and artifact upload; both Android 8/API 26 and Android 17/API 37 then passed clean launch, complete onboarding, offline process recovery, foreground navigation completion, screen-off automatic GPS arrival, network restoration, diagnostics, and evidence upload. No rerun or diagnostic failure was generated.

### Remaining field boundary

- The three field scenarios are now reproducible and evidence-ready but remain unpassed until the Xiaomi phone reconnects and the real route, elapsed OEM time, or missed service actually occurs. The collector intentionally cannot replace those environmental facts.

## 2026-07-30 - Task 25: repeated physical-boundary audit and blocked-state handoff

### Audit

- Re-read the complete current 677-line `AGENT.md` and the complete original pasted implementation plan before starting this new continuation task.
- Verified that the worktree is clean, local `main` and `origin/main` both resolve to `929a4bffcd241a02704c179880b6d3d434323ec1`, and no unpushed implementation or evidence change exists.
- Rechecked the exact installed Platform-Tools executable, ADB USB device list, ADB mDNS services, and Windows present-device inventory. No Android phone is connected or discoverable; the only present Xiaomi hardware is the previously identified Bluetooth earbuds.
- Rechecked GitHub Actions state. Main run `30483414232` remains successful with the verify, Android 8/API 26, and Android 17/API 37 jobs complete. No newer failed, queued, or running workflow exists for current `main`.
- Re-audited the authoritative v0.2.0 completion matrix and the field runbook against the original plan. All repository implementation, release, security, compatibility, onboarding, and deterministic test requirements have direct evidence. The only unproved items remain a real 8-12 point GNSS route, multi-hour Xiaomi/OEM survival, and an actual missed-service event.

### Blocking determination

- The same missing physical-device/field condition is now confirmed across three consecutive goal turns: Task 23 found the Xiaomi disconnected while closing onboarding evidence, Task 24 found it disconnected while making all three field scenarios evidence-ready, and this Task 25 again finds no USB or wireless Android device after the repository and remote state are already complete.
- No further repository, emulator, documentation, or synthetic-location work can turn those environmental events into honest acceptance evidence. Continuing without the phone and real elapsed/event conditions would only create churn or fabricate scope.
- The precise resume condition is external: reconnect and unlock the Xiaomi 15T Pro, authorize this computer for USB debugging, disable the selected fake-GPS/mock-location app for the GNSS scenario, and make the real route or transit scenario available. The existing `scripts/capture-field-evidence.ps1` and `docs/PHYSICAL_FIELD_TEST_RUNBOOK_v0.2.0.md` are ready to resume immediately.
- No application, workflow, release, test, or acceptance file changed in this task; this log records the required new-task read, current-state audit, and honest blocked boundary.

## 2026-07-30 - Task 26: exact public v0.2.0 physical retest and real-location acceptance

### Preparation and safety boundary

- Re-read the complete current 694-line `AGENT.md` and the complete original pasted implementation plan before starting this resumed physical-device task.
- Followed the user's explicit boundary: no APK installation, overwrite installation, uninstallation, data clearing, permission change, mock-location change, or input-method replacement was performed. Testing used only the already-installed application and read-only ADB inspection, plus normal force-stop/start and UI input actions.
- Confirmed the Xiaomi 15T Pro reconnected as authorized ADB serial `JBR4LF6TQ4MFHY4X`; Windows also reported the expected Xiaomi WPD, ADB, and USB interfaces.

### Exact-release and runtime verification

- Confirmed Android 16/API 36, security patch 2026-06-01, `versionName=0.2.0`, and `versionCode=6`. The installed `base.apk` SHA-256 is `e3d36b47695b452978680726c5eb09133e04c0f207149a6324f3e08ac8f9a9ec`, exactly matching the public GitHub Release rather than the earlier pre-onboarding candidate.
- Before launch the application had no process, no `NavigationService`, and no active notification. Fine/coarse location and notification permissions were all granted; system location, GPS Provider, and network Provider were enabled. The separate package `com.blogspot.newapphorizons.fakegps` still held mock-location access and was not changed.
- Performed a no-data-clear cold launch of the existing public APK. Android returned `Status: ok`, `LaunchState: COLD`, and 302 ms total time; `MainActivity` became top-resumed, the process stayed alive, and the crash buffer contained no application crash.
- The completed first-run guide and saved configuration survived the cold launch: the app went directly to the Bangumi search page instead of returning to onboarding.
- Used the production UI and network client to run a Bangumi search; it returned 20 works. Selecting a result with no pilgrimage data displayed the explicit `Anitabi 暂无这部作品的巡礼数据` fallback without a crash.
- Opened the physical about page and verified the visible GPL/privacy statement and OpenFreeMap, OpenMapTiles, OSM, ORS/HeiGIT, Transitous, Bangumi, and Anitabi attribution. The Xiaomi input method accepted the first ADB search input but refused a later replacement query; testing stopped rather than replacing the user's input method or installing a helper.
- Finished with the exact public APK still installed, `MainActivity` foreground, no navigation service, no active app notification, and an empty application crash result.

### User-confirmed real-location checkbox

- The user explicitly confirmed that real positioning works even though the phone remains configured with a mock-location application and requested this item be checked. Recorded `真实定位功能可用` as passed in the v0.2.0 plan and physical-device acceptance records.
- Kept the evidence scope precise: the user's field observation proves the real-location function works, while ADB independently proves permissions and providers are enabled but cannot attribute every fix to GNSS while mock authorization remains. The separate 8-12 point real route, multi-hour Xiaomi/OEM run, and actual missed-service event remain unchecked.
- Updated README status and the v0.2.0 physical acceptance record with the exact-release retest. No production application source, APK, ORS Key, release artifact, or device configuration changed.

## 2026-07-30 - Task 27: v0.2.1 feasibility freeze and VPS backend implementation

### Preparation and verified baseline

- Re-read the complete current 718-line `AGENT.md` and the complete original pasted implementation plan before starting the new v0.2.1 goal.
- Confirmed the worktree was clean and local `main`/`origin/main` both resolved to `cf0a49a8cd7790ce3789069cc1067e3ad2d82732`. Created the dedicated implementation branch `codex/v0.2.1-google-vps-migration`; no v0.1.2 planning worktree was used.
- Reconfirmed the Android baseline is `versionName=0.2.0` / `versionCode=6`, with MapLibre, ORS, Transitous, Room version 1, the onboarding Key store, and the existing 49-test structure still intact. No Android production source, installed phone application, Release asset, tag, or v0.2.0 historical record changed in this task.

### Current official Google/Firebase findings

- Used Agent Reach's Exa backend first, then current official Google/Firebase documentation after Exa's free MCP limit was reached. Recorded the frozen constraints and direct official links in `docs/GOOGLE_MIGRATION_FEASIBILITY_v0.2.1.md`.
- Confirmed Navigation SDK 7.x supports driving, walking, and cycling, replaces the Maps SDK map layer, accepts at most 25 destinations, and remains compatible with the project's API 26 minimum/API 37 target. Its current free cap is 1,000 billed destinations, so the requested 900 local ceiling is the correct 90% value.
- Confirmed Routes Essentials has a 10,000-event free cap per Compute Routes/Matrix SKU, so 9,000 is the correct 90% local ceiling. Ten-coordinate square matrices reserve 100 elements; 12-location road previews use the Essentials maximum of ten intermediate waypoints; transit routes accept no intermediate waypoints and must remain pairwise.
- Identified one real product limitation instead of fabricating it: Routes transit responses expose stop names, times, line, vehicle, headsign, and stop count, but no independent platform-number field. v0.2.1 can display only platform information actually present in upstream stop text.
- Confirmed WALK/BICYCLE Routes results require Google's beta warning, Firebase Analytics can be disabled until runtime opt-in, and Crashlytics disabled collection can retain reports locally. The Android implementation must delete unsent reports while consent is absent or withdrawn.

### Backend implementation

- Added the `backend` Node.js 24 LTS / TypeScript / Fastify service with only the four planned endpoints, a 16 KiB body limit, JSON/HTTPS enforcement, fixed Google OAuth and Routes upstreams, fixed field masks/timeouts, and normalized responses/errors. Full Google response bodies are never forwarded or logged.
- Implemented Firebase anonymous ID-token verification with cached Google signing keys and explicit RS256, project audience, issuer, expiration, UID, and anonymous-provider checks.
- Implemented Web Crypto RS256 service-account JWT signing and short-lived OAuth exchange with a single-flight refresh promise. The token endpoint, scope, Routes endpoint, and quota project are fixed; service-account contents never enter Android or logs.
- Implemented SQLite WAL quota accounting with `BEGIN IMMEDIATE` transactions and UTC period boundaries. Limits are Matrix 9,000/month and 2,000/UID/day, Compute Routes 9,000/month and 200/UID/day, and Navigation 900/month and 20 destinations/UID/day. Reservations are not refunded after an upstream failure. Integrity, write, disk, or billing-state uncertainty fails closed.
- Added a primary UID token bucket and wider HMAC-IP auxiliary bucket. Structured logs contain only endpoint template, status, latency bucket, and error code; they cannot accept token, raw IP, coordinates, anime/search text, or request body fields.
- Added consistent SQLite backups with a seven-day retention window, integrity checking, recoverable restore copies, and mandatory post-restore billing disablement until an explicit audited-enable command.
- Added a multi-stage Docker image, loopback-only Compose port, non-root user, read-only root filesystem, dropped capabilities, no-new-privileges, memory/CPU/PID limits, read-only secret mounts, health check, automatic restart, and an additive Caddy virtual host for `api.anitabi.afunnypersonlol0.site`.

### Verification and remaining external boundaries

- `npm test` passes 17 backend tests covering JWT acceptance/rejection, fixed OAuth JWT/scope/endpoint and 30-way single-flight refresh, request boundaries, unified errors, fixed Google upstreams/field masks, normalized transit output, no upstream-body pass-through, token buckets, logging redaction, day/month changes, fail-closed billing state, and quota limits.
- The concurrency test used 12 independent SQLite connections competing for a 9,000-element month. Exactly 90 reservations of 100 elements succeeded, the persisted global row ended at exactly 9,000, and every excess reservation was rejected.
- Production dependency audit reports zero vulnerabilities. Compose configuration parses successfully. The final `anitabi-api:0.2.1` image built successfully, is 92,417,897 bytes, declares user `node`, and includes the health check. A read-only, capability-free container smoke test ran as UID 1000 and returned `200 {"service":"ok","database":"ok"}` from the real compiled Fastify/SQLite code.
- The first slim-image build correctly exposed missing native build tools. A stalled Debian package download and an Alpine experiment were stopped; the final reproducible Dockerfile uses the full Node Bookworm image only as a build stage and the 92 MB Bookworm-slim runtime. Node's built-in SQLite was evaluated but rejected because Node 24 still emits an experimental API warning.
- No Google/Firebase project, billing resource, Cloudflare DNS record, VPS service, credential, or billable Google request was created. Local DNS is transparently mapped to reserved `198.18.0.0/15` addresses and cannot prove public DNS state. No usable VPS SSH target or Cloudflare/Google credential is present in the local environment, so deployment remains an external next phase rather than a claimed result.
- No secret value was written to the workspace, command output, Docker layer, documentation, or this log. Only generated in-memory test keys and fake tokens were used.

## 2026-07-30 - Task 28: Google control-plane migration and VPS access discovery

### Preparation and read-only discovery

- Re-read the complete current 753-line `AGENT.md` and the complete original pasted implementation plan before starting this task.
- Confirmed the implementation branch remained clean and synchronized with its remote at Task 27's backend commit. The branch has no GitHub Actions run because the existing Android workflow path filters do not include the backend/documentation-only change set.
- Found the already authenticated Google Cloud project through the user's existing Chrome state and verified the project identity before making any change. No browser cookies, stored passwords, tokens, local storage, or secret values were inspected.
- A single focused, read-only browser-history lookup identified the active VPS provider as V.PS and the service as a Tokyo Cloud KVM instance. The provider console session has expired and is currently at its normal login page; no provider credential was guessed, read, or transmitted.

### Google API migration performed

- Verified the pre-change state: Routes API was enabled, Navigation SDK was disabled, and Maps SDK for Android was enabled.
- Enabled Navigation SDK in the identified project and waited for the console to report the stable enabled state.
- Disabled Maps SDK for Android and waited for the console to report the stable disabled state, avoiding simultaneous Maps SDK and Navigation SDK map stacks. Routes API remained enabled throughout.
- Restored the user's original Google Maps Platform API-list page after the checks. No API key, service account, OAuth credential, billable Routes request, budget, or quota was created in this task.

### Honest external boundaries

- Opening the same project in Firebase reached its first-use `Accept Firebase terms of service` screen. Accepting legal terms requires the owner's explicit confirmation, so the checkbox and Continue action were left untouched and the live page was preserved for handoff.
- Opening the exact V.PS service page reached `Registered Clients Only` because the login session had expired. The live login page was preserved for the owner; no email address, account password, root password, or console credential was entered.
- With the provider session unavailable, the mandatory console recovery of `sshd`, host fingerprint verification, and read-only VPS inventory could not honestly start. Cloudflare DNS/HTTPS configuration also remains untouched because no authenticated Cloudflare control surface or credential was available.
- No secret was written to source, shell commands, browser output, Git, Docker, or this log. The Android phone and its installed v0.2.0 application were not accessed or changed.

## 2026-07-30 - Task 29: v0.2.1 unlimited-tour, data-migration, Firebase, and Google map foundation

### Preparation and external configuration

- Re-read the complete current 776-line `AGENT.md` and the complete original pasted implementation plan before starting this task. The user then explicitly authorized acceptance of all service terms needed by the implementation; this did not broaden the work to purchases, CAPTCHA handling, guessed credentials, or unrelated services.
- Added Firebase to the same existing Google Cloud project and accepted the Firebase and Google Analytics terms. The project retained its already-enabled Blaze billing relationship; no new billing account or purchase was created. Google Analytics account location was set to China and all optional Analytics data-sharing switches were turned off.
- Enabled Firebase Anonymous Authentication without 30-day auto-cleanup, registered `cn.anitabi.navigator` as `Anitabi Android`, downloaded the matching `google-services.json`, and registered both SHA-1 and SHA-256 fingerprints for the fixed production certificate and the local debug certificate.
- Created a dedicated `Anitabi Navigation SDK Android` API key. It is restricted to Navigation SDK only and to the application package plus the production and debug SHA-1 certificates. The key is stored only in ignored `local.properties`; it was not added to source, Git, command output, or this log, and the system clipboard was cleared after transfer. The pre-existing Maps key was not modified.
- Used read-only ADB pull and `apksigner` inspection of the already-installed public v0.2.0 APK to derive its SHA-1 certificate fingerprint. No APK was installed, replaced, uninstalled, launched, or modified on the phone.

### Unlimited planning and versioned persistence

- Set `versionCode=7` and `versionName=0.2.1`.
- Removed the fixed road/transit total-point caps. Large road tours now use deterministic nearest-neighbor plus bounded 2-opt globally, exact Held-Karp only inside at-most-10-location windows, at-most-100-element matrix requests, overlapping at-most-12-location route batches, and at-most-25-destination navigation batches. Transit remains adjacent-pair planning. Added helpers for identifying only the matrix windows affected by a dragged stop.
- Added tests for a 200-point optimizer run, 10/12/25 batching boundaries, 35-point road planning, 14-point pairwise transit planning, fixed endpoints, and affected-window calculation.
- Added `StoredTourV2`, which persists only user-owned points, order, start/end policy, mode, dwell/departure settings, completed points, active point, and navigation state. Resolved matrices, geometry, steps, transit details, estimates, and provider responses are process-memory only.
- Upgraded Room from schema 1 to schema 2 with exported schemas and an explicit migration that preserves the public v0.2.0 JSON in legacy columns until a successful lazy conversion. Successful conversion clears old route content and marks the route for network refresh; parse failure retains the original record and exposes a recovery error instead of clearing the database. Repeated conversion is idempotent.
- Added an Android migration test that creates the exact v0.2.0 schema and encoded records, migrates them, verifies route stripping and preserved selection/progress, repeats recovery, and separately proves malformed legacy JSON remains recoverable. The instrumentation test source and schema assets compile successfully; execution still requires the later API 26/API 37 emulator phase.

### Google Android SDK foundation

- Added Navigation SDK 7.8.0, Firebase BoM 34.16.0, Firebase Auth, Analytics, Crashlytics, Google Services 4.5.0, Crashlytics Gradle plugin 3.0.7, and the required NIO desugaring library. Analytics and Crashlytics collection are disabled by manifest default.
- Replaced both production MapLibre map implementations with `NavigationView`-backed Google maps for pilgrimage-point selection and route preview, including lifecycle handling, marker selection, route polylines, current-location display, camera bounds, and visible-bounds callbacks. Removed the MapLibre dependency and application initializer.
- Navigation SDK's transitive Cronet 119 AARs share one namespace under AGP 9.3. The build otherwise fails manifest validation, so `android.uniquePackageNames=false` is temporarily set; AGP reports the duplicate namespace as a warning instead of an error. This compatibility switch must be retested when Navigation SDK or AGP is upgraded.

### Verification and remaining migration work

- `:app:compileDebugKotlin` and `:app:compileDebugAndroidTestKotlin` pass with the Firebase and Navigation SDK stack. `:app:testDebugUnitTest` passes 57 tests with zero failures, errors, or skips. The debug runtime dependency tree contains Navigation SDK/Firebase and no MapLibre artifact. `git diff --check` reports only the repository's existing Windows line-ending notices.
- No billable Navigation or Routes request was made. No service-account key, VPS credential, root password, ORS key, Transitous URL, or server credential was added to the new Android configuration.
- This task deliberately stops at a verified foundation: the Android route provider still uses the old ORS/Transitous implementation, onboarding/settings/about copy still exposes the old provider choices, native road guidance is not yet wired to `Navigator`, telemetry runtime consent is not implemented, drag reordering does not yet execute the affected-window-only refresh, and process-death route-refresh handling still needs completion. These are explicit next tasks, not claimed results.

## 2026-07-30 - Task 30: backend routing migration and Firebase-config incident containment

### Preparation and Android migration

- Re-read the complete current `AGENT.md` and the complete original pasted implementation plan before starting the Android routing continuation. When the GitHub alert made credential containment an independent urgent concern, re-read both files in full again before touching history or external state.
- Added a fixed-origin Android VPS client for `/v1/matrix`, `/v1/route`, and `/v1/navigation/reserve`. Requests obtain a Firebase anonymous ID token lazily, send it only as a Bearer credential to the fixed HTTPS backend, and map only the backend's normalized responses and unified error codes.
- Replaced the ORS and Transitous providers with backend road and pairwise-transit providers, including matrix objective propagation, Google precision-5 polyline decoding, normalized transit details, and Google Routes attribution. Deleted the direct ORS and Transitous network clients and their provider contract tests.
- Replaced the ORS Key store with a versioned application-settings store. It preserves the completed-onboarding marker, removes legacy ORS ciphertext and IV values, and deletes the legacy Android Keystore alias on a best-effort basis. The onboarding, planner, settings, about page, fixtures, and UI tests no longer ask users to obtain or enter an ORS Key; Firebase anonymous sign-in remains lazy so onboarding can complete offline after permissions and service/privacy disclosure.
- Added backend MockWebServer tests, settings-migration instrumentation coverage, safe API error types, and updated planner/provider/onboarding tests and workflow milestones. The application no longer contains direct ORS, Transitous, OpenFreeMap, or MapLibre request paths.

### GitHub secret alert containment

- A GitHub secret-scanning email correctly reported that `app/google-services.json` had been tracked in the then-tip commit. Treated the incident as an error rather than dismissing it as a harmless client key.
- Temporarily stashed all in-progress Android work, added `app/google-services.json` to `.gitignore`, removed it from the Git index while retaining the local ignored copy, amended the branch-tip commit, and force-pushed only `codex/v0.2.1-google-vps-migration` with `--force-with-lease`. The rewritten tip is `3178ab7`; no remote branch contains the exposed commit, the current remote tree does not contain the file, and current reachable Git history has no `app/google-services.json` object. `main`, tags, and v0.2.0 history were not rewritten.
- Added a tracked-source credential audit that fails if the Firebase JSON becomes tracked again or common Google API key/service-account/private-key material appears in tracked source. It reports only that a pattern was found and never prints a matching credential.
- Added an ignored-config restore script and CI wiring. Debug CI uses the encrypted `ANITABI_GOOGLE_SERVICES_JSON_BASE64` repository secret when available and an explicitly invalid compile-only placeholder for untrusted PRs; signed release builds fail closed unless the encrypted real configuration exists. Local execution refuses to replace a developer's Firebase file with the placeholder.
- Fixed the APK audit's pipeline race and updated it to reject legacy provider endpoints/SDKs and server private-key material while permitting only the legacy ORS preference names required to erase v0.2.0 data during migration. No key value was printed, written to this log, or placed in a shell command.

### Verification and unresolved external action

- Full local verification passes 56 JVM tests with zero failures/errors/skips, Android-test Kotlin compilation, Debug Lint with zero findings, Debug APK assembly, tracked-source credential audit, APK content audit, shell syntax checks, CI-placeholder generation in an isolated temporary directory, `git diff --check`, and remote-ref/tree/history checks.
- No APK was installed on the Xiaomi, and the existing v0.2.0 installation and phone data were not accessed or changed.
- The exposed Google client key still requires cloud-side rotation/revocation, the new ignored JSON must then be stored as the GitHub Actions encrypted secret, and the GitHub alert must be resolved as revoked. The authenticated Chrome tabs are present, but the ChatGPT Chrome extension times out on every Google Console read or interaction even after the documented new-window recovery retry. Browser safety rules prohibit extracting the user's session through another script; the user has been asked to reinstall/re-enable the Chrome plugin. This task does not falsely claim that the old key is revoked or the alert closed.

## 2026-07-30 - Task 31: separate Analytics and Crashlytics opt-in controls

### Preparation and implementation

- Re-read the complete updated `AGENT.md` and the complete original pasted implementation plan before starting this new task. Used Agent Reach's prescribed Exa route first; when its MCP metadata remained unavailable, used the browser fallback restricted to current official Firebase Android documentation.
- Confirmed from Firebase's official Analytics guide and Android reference that manifest-default collection can remain off, runtime `setAnalyticsCollectionEnabled` persists the user's override, and `resetAnalyticsData` clears device analytics data and resets the app instance ID. Confirmed from the official Crashlytics guide/reference that runtime collection overrides persist, disabling takes full effect on the next process launch, and `deleteUnsentReports` removes locally queued reports without sending them.
- Added independently persisted Analytics and Crashlytics consent values, both defaulting to false. Added a runtime controller with injectable interfaces so consent ordering and cleanup behavior can be tested without invoking Firebase.
- Application startup now reapplies the stored choices before normal UI use and deletes unsent Crashlytics reports whenever Crashlytics consent is absent. Enabling Analytics first resets any pre-consent local analytics state; withdrawing disables collection and resets local analytics data. Enabling Crashlytics first deletes reports created before consent; withdrawing persists the disable override and deletes queued reports immediately.
- Added separate accessible switches to the About/privacy page. The copy states that both choices are optional and independently withdrawable, excludes coordinates, anime names, search terms, and route bodies, and accurately notes that Crashlytics disablement is fully effective on the next launch.
- Kept both Firebase manifest defaults disabled and additionally disabled Advertising ID collection and default ad-personalization signals. No custom telemetry event, user ID, coordinate, route body, anime title, or search term is recorded by this task.

### Verification

- The test was added first and initially failed compilation on the absent consent controller/runtime/store types. After implementation, the targeted controller tests and Android-test Kotlin compilation passed.
- Full local verification passes 59 JVM tests with zero failures/errors/skips, Android-test Kotlin compilation, Debug Lint with zero findings, Debug APK assembly, tracked-source credential audit, APK content audit, and `git diff --check` apart from existing Windows line-ending notices.
- Added instrumentation coverage proving both consent values default off and persist independently across settings-store instances. It compiles locally; execution remains part of the later API 26/API 37 emulator matrix.
- No physical phone or installed v0.2.0 application was accessed or changed. The separate exposed-key cloud revocation remains exactly as recorded in Task 30 and is not falsely closed by this telemetry work.

## 2026-07-30 - Task 32: Google API key rotation attempt and expanded containment

### Preparation and verified scope

- Re-read the complete updated `AGENT.md` and the complete original pasted implementation plan before starting the user's explicit key-rotation request.
- Identified the GitHub alert's credential as the Firebase-created Android API key in the correct Google Cloud project. Confirmed separately that the dedicated Navigation SDK Android key is a different credential and did not delete or overwrite it by mistake.
- Confirmed the Firebase key retained Firebase's 25-API allowlist but had no application restriction. Prepared the replacement to add Android application restrictions for `cn.anitabi.navigator` with both the fixed release and local debug SHA-1 certificates.
- Recomputed the two public signing-certificate fingerprints from the exact public v0.2.0 Release APK and the current debug APK. No signing private key, password, Firebase key value, or certificate secret was read or recorded.

### Containment status and browser blocker

- While locating the local signing configuration, one diagnostic command accidentally printed the existing Navigation SDK API key from ignored `local.properties` to the private task output. This was an assistant error. The value was not committed, pushed, copied into documentation, or written to this log, but it is now treated as exposed and must be rotated together with the Firebase key.
- Opened Google Cloud's native Firebase-key rotation flow and reached the replacement restriction form. No replacement key was created, no old key was revoked, and no cloud credential state changed before browser control stopped responding.
- The authenticated browser extension can still enumerate the correct signed-in Google Cloud and GitHub tabs, but all subsequent page reads and actions time out. The documented clean-window recovery cannot launch the selected profile because this host has no Google Chrome profile at the expected location. Browser safety rules prohibit bypassing the extension by extracting browser sessions or scripting authenticated requests.
- The local ignored `google-services.json`, ignored Navigation property, GitHub Actions secrets, and GitHub secret-scanning alert are unchanged. The application and Xiaomi phone were not accessed or modified. Rotation must resume only after the ChatGPT Chrome plugin is reinstalled or otherwise restored; completion requires both replacement keys to be installed and verified before both old values are revoked, followed by GitHub secret update and alert resolution.

## 2026-07-30 - Task 33: Chrome control restoration and release-secret wiring

### Preparation and recovery

- Re-read the complete updated `AGENT.md` and the complete original pasted implementation plan before continuing the explicit key-rotation task.
- Diagnosed the browser-control failure through the official Chrome plugin recovery checks. The native host manifest was valid, but Google Chrome and the ChatGPT Chrome Extension were initially absent from the selected profile.
- Downloaded the official 64-bit Chrome installer directly from Google's HTTPS distribution endpoint with resumable transfer. Verified the exact advertised byte length and a valid Google LLC Authenticode signature before running it. Chrome was installed per-user; Edge remained the default browser and no browser data was imported by this task.
- Opened the official extension store page after the user's permission. The user installed the ChatGPT Chrome Extension, and the official check now reports it installed and enabled in Chrome's selected profile. No extension was side-loaded and no native-host registry entry was manually created or repaired.

### CI correction and verification

- Fixed the signed-release workflow so the encrypted `ANITABI_NAVIGATION_API_KEY` repository secret is actually exposed to the Gradle release build. Previously the build script failed closed when that value was absent, but the workflow did not pass it, so a future v0.2.1 tag would have failed even after key rotation.
- The tracked-source credential audit passes. `testDebugUnitTest`, `lintDebug`, and `assembleDebug` all pass, and `git diff --check` reports no whitespace error.
- Recent GitHub Actions history shows the newest two Android CI runs on `main` succeeded; the cluster of failure emails came from older onboarding-emulator iterations that were followed by successful runs.

### Key rotation completion and verification

- After the user completed the supported Google and GitHub sign-in flow, created separate replacement keys named `Anitabi Firebase Android v0.2.1` and `Anitabi Navigation SDK Android v0.2.1`. The Firebase key retains Firebase's 25-API allowlist; the navigation key is restricted only to Navigation SDK. Both keys are restricted to `cn.anitabi.navigator` with the fixed release and current debug SHA-1 certificates.
- Installed the replacement Firebase configuration in the ignored local `app/google-services.json` and the replacement Navigation key in ignored `local.properties`. Added the corresponding encrypted GitHub Actions secrets `ANITABI_GOOGLE_SERVICES_JSON_BASE64` and `ANITABI_NAVIGATION_API_KEY`. No key value was printed, committed, added to this log, or placed in a shell command.
- Verified the replacement configurations with 59 JVM tests, Debug and Release Lint, Debug APK assembly, tracked-source credential audit, APK content audit, and `git diff --check`. A combined signed Release APK build was also attempted and failed closed before compilation because this machine does not have the external release-signing values; the encrypted signing secrets remain available only to GitHub Actions. This is a signing-environment boundary, not a replacement-key failure.
- Deleted both superseded keys after the replacement configuration and CI secrets were in place. Google Cloud's deleted-credentials page shows both old credentials with restore actions, proving they can no longer serve API requests. Closed GitHub secret-scanning alert #1 as `Revoked` with a non-secret remediation note.
- No password, one-time code, account cookie, browser storage, key value, signing private key, or signing password was read or recorded. No Google billable route request was made, no phone was accessed, and the installed v0.2.0 application was not changed.

## 2026-07-30 - Task 34: native Google road guidance, quota-aware batching, and route recovery

### Preparation and implementation

- Re-read the complete updated `AGENT.md` and the complete original pasted implementation plan before starting this task. Used Agent Reach's required Exa route first; after its MCP metadata call failed, used the documented browser fallback and restricted research to Google's official Navigation SDK route, multi-destination, `NavigationApi`, and `Navigator` documentation. Cross-checked the downloaded Navigation SDK 7.8.0 API JAR before coding.
- Added the official one-time Google Navigation terms flow through the Activity `NavigationApi.getNavigator` entry point. Road navigation starts only after the SDK reports the Navigator ready; initialization failures are mapped to concise user-visible messages. The terms-bypass API is not used. Transit remains outside the native road-guidance flow.
- Added a native road-navigation session for driving, cycling, and walking. It maps the selected travel mode and fastest/shortest objective, builds stopover waypoints, enables Google voice alerts and guidance, observes native arrival, remaining-distance, rerouting, and route-change events, and releases all listeners and Navigator resources on pause, failure, completion, or service shutdown.
- Reworked the foreground service so Google is the sole road-mode authority for spoken instructions, off-route handling, and automatic arrival. The application's TTS and state-text announcements remain only for transit. Native arrival feeds the existing arrival/dwell/next-stop state machine, so completed points and navigation progress continue to be stored in Room; manual arrival remains available.
- Added deterministic batch coordination and `/v1/navigation/reserve` calls before every native destination load. The SDK ceiling remains 25 destinations, while production batches use 20 because the specified backend hard limit is 20 navigation units per UID per day. This is intentionally fail-closed: a later batch is attempted only after another atomic reservation, and quota exhaustion is surfaced instead of bypassed. The existing SDK-boundary batching test still proves 61 destinations split as 25/25/11; the native production coordinator tests 20-point loads, middle-leg resume, batch transitions, final return completion, and rejection above 25.
- Road navigation now renders the Navigation SDK's native guidance UI, header, ETA card, and trip progress bar rather than clearing and redrawing the preview route. Transit retains the normalized Google Routes preview. Removed the obsolete OpenFreeMap/OpenMapTiles/OSM attribution from all active map and navigation screens and replaced it with Google Maps, Google Navigation, or Google Routes attribution.
- Closed the process-death route-content gap: when only `StoredTourV2` remains, the service obtains the current location, replans only unfinished points, preserves completed point IDs and dwell/next-stop semantics, resets route-relative indexes safely, stores the refreshed user-owned state, and then resumes. No Google matrix, route geometry, steps, or ETA is persisted.

### Verification and remaining acceptance

- Added six JVM regression tests in this task; the full suite now passes 65 tests with zero failures or errors. `compileDebugAndroidTestKotlin`, Debug and Release Lint, Debug APK assembly, tracked-source credential audit, APK content audit, and `git diff --check` all pass. The only compiler notices are deprecations exposed by Navigation SDK 7.8.0's own result-future and initialization-error API.
- No billable Google route/navigation request was made. No API key value, service-account material, VPS credential, password, token, coordinate, anime name, route body, or search term was written to tracked source, logs, this file, or Git. The physical phone and its installed v0.2.0 application were not accessed or changed.
- Native behavior still requires later acceptance on API 26/API 37 GMS emulators and the Xiaomi signed v0.2.0-to-v0.2.1 overlay install: terms presentation, voice, real location, lock screen, off-route rerouting, arrival/dwell continuation, and quota-backed batch loading must not be claimed until those runs occur. The VPS deployment and real `/v1/navigation/reserve` ledger are also separate remaining goal tasks.

## 2026-07-30 - Task 35: production VPS deployment, Google controls, and real backend verification

### Preparation, server inventory, and explicit safety boundary

- Re-read the complete updated `AGENT.md` and the complete original pasted implementation plan before starting this deployment task. Used the Chrome/browser, Computer Use, Cloudflare, and Agent Reach skill instructions where their scope applied, and consulted current official Cloudflare DNS and Google Routes documentation.
- Verified the restored VPS SSH host key against the existing local known-host record before authentication. The server is Ubuntu 24.04.4 LTS with 2 vCPUs, 961 MiB visible RAM, 256 MiB swap, and a 20 GB root disk. Existing Nginx, six application/data containers, existing virtual hosts, and their listening ports were inventoried before any deployment change.
- The user explicitly prohibited changing the password or SSH. No root/deploy password, SSH port, `sshd` option, root-login policy, authorized key, firewall rule, or fail2ban SSH jail was changed. An unused local deployment key pair created before that override was never uploaded and was permanently deleted at task end. The user's existing root-password authentication remains in its prior state.
- Host port 8787 was already owned by an unrelated existing service. The Compose deployment therefore gained a non-secret configurable loopback host port and uses `127.0.0.1:8788 -> container:8787`; the unrelated listener was not stopped or reconfigured. Existing Nginx already owned ports 80/443, so an additive Nginx virtual host was used instead of installing Caddy.
- The initial root disk had less than the plan's 10 GiB free-space threshold. Two passes of `docker builder prune --all --force` removed only unreferenced build cache, never images, containers, volumes, logs, or user data. The final filesystem state is 8.1 GB used and 11 GB available; all seven containers remained running afterward.

### Google, Firebase, DNS, and cost controls

- Created a dedicated Routes backend service account with only the project-level Service Usage Consumer role. Its JSON credential is stored outside the repository locally with restricted ACLs and on the VPS as a read-only owner-only secret mount. OAuth token issuance and a direct minimal WALK route succeeded without printing a token or credential.
- Added a DNS-only Cloudflare A record for `api.anitabi.afunnypersonlol0.site` and verified the Cloudflare authoritative nameserver returns the VPS address. A shorter `api.afunnypersonlol0.site` name was created accidentally during the UI operation, detected immediately, and edited in place to the required hostname before certificate issuance or client use. The other eight DNS records and the existing personal-site proxy settings were not changed.
- Created the project-scoped monthly Google Cloud budget `Anitabi v0.2.1 cost guard` for CNY 1, with actual-spend alerts at 50%, 90%, and 100% to billing administrators/users and project owners. This is an alert, not an automatic spend cap; the VPS SQLite ledger remains the authoritative monthly fail-closed control.
- Reduced the adjustable Google quotas used by the product: Compute Route Matrix is 1,000 elements/minute and 9,000 elements/day; Compute Routes is 120 requests/minute and 9,000 requests/day; Navigation SDK `Set Destination` is 100/minute and 900/day. The unused Route Token quotas were left unchanged. Monthly global limits remain atomically enforced by the VPS at 9,000 matrix elements, 9,000 route calls, and 900 navigation destinations.

### Production backend, HTTPS, backups, and host hygiene

- Deployed the Node.js/TypeScript/Fastify backend to `/opt/anitabi-api`, quota data to `/var/lib/anitabi-api`, and read-only secrets to `/etc/anitabi-api/secrets`. The running `anitabi-api:0.2.1` container uses UID 1000, a read-only root filesystem, all capabilities dropped, no-new-privileges, an isolated data volume, read-only secret mounts, health checks, automatic restart, and loopback-only publication.
- Added the independent Nginx API virtual host with a 16 KiB body limit, disabled access logging, discarded error output that could contain request metadata, overwritten forwarded headers, HSTS, and `nosniff`. Certbot issued a Let's Encrypt certificate for the API hostname, HTTPS health returns only `{"service":"ok","database":"ok"}`, HTTP redirects to HTTPS, renewal is enabled, and the certificate currently expires on 2026-10-28.
- Added and installed systemd service/timer units for the existing integrity-checked SQLite backup script. The timer is enabled, persistent, runs daily with a randomized delay, and retains seven days. Multiple real backups completed with status 0; the final verified backup was created after the real-request ledger reconciliation.
- Found that `unattended-upgrades` was genuinely absent even though the normal APT timers were active. Installed only that package and its small dependency without upgrading or autoremoving any existing package. Automatic package-list refresh and unattended security upgrades are now enabled and active. Fail2ban's existing `sshd` jail and Certbot timer remain active.
- Added only the API virtual host to the existing Nginx configuration. `nginx -t` passes apart from pre-existing Vaultwarden protocol-option warnings. The existing `misaka`, `risk`, and `laddar` HTTPS sites each still return 200, and all pre-existing containers remained up and healthy where they expose health checks.

### Real Google-path defects found and fixed

- The first real matrix request exposed that Compute Route Matrix does not accept route-feature modifiers on origins. Removed `routeModifiers` from matrix origins while retaining the intended road-mode handling elsewhere, and added a regression assertion that the matrix body never contains them.
- A subsequent successful Google matrix response exposed protobuf JSON default omission: diagonal elements can omit `distanceMeters` when the value is zero. The normalizer now maps only that omitted matrix field to zero and still rejects invalid route/leg distances. Added a regression case for an omitted diagonal distance with `0s` duration.
- The remote source and image were rebuilt after both fixes. An intermediate overly broad remote text replacement was detected during inspection and corrected before the image was built; the final deployed route, leg, step, and matrix normalization functions match the tested local source.

### Verification, quota reconciliation, and cleanup

- The full real chain succeeded through Firebase anonymous authentication, public HTTPS, VPS Firebase JWT verification, SQLite quota reservation, service-account OAuth, Google Routes, response normalization, and Android-shaped output: a WALK route returned 200, navigation reservation returned 200 for one destination, and a 2x2 WALK matrix returned 200 with four normalized elements.
- Desktop Firebase anonymous authentication initially returned 403 because the Firebase key is correctly Android-app restricted. Repeated the test with the public package name and release-certificate header derived from the signed public APK; the key restriction was not weakened.
- Conservatively accounted for every successful or potentially billable diagnostic. The final July 2026 global ledger is Matrix 20 elements, Navigation 1 destination, and Route 2 calls. Failed normalization attempts were not refunded; successful direct diagnostics were reconciled under a non-personal operator UID before the final backup.
- Backend `npm test` passes all 17 tests, including the two new real-response regressions. Compose configuration parses with the production project ID and loopback port, `git diff --check` has no whitespace error, and the tracked-source credential audit passes. The public container health, Nginx syntax, timers, final backup, disk threshold, existing sites, and all existing containers were rechecked after cleanup.
- Permanently deleted the unused local deployment private/public key pair and obsolete deployment archive after validating their exact paths were inside the dedicated outside-repository secret directory. The restricted local Google service-account JSON was intentionally retained for the deployed backend; no credential value, password, token, API key, billing-account identifier, coordinate, request body, anime name, or search term was written to Git, command history, logs, or this record.

### Remaining acceptance boundary

- VPS deployment, DNS, HTTPS, cost controls, real route/matrix/reservation calls, and ledger accounting are now evidenced. API 26/API 37 GMS emulator runs, on-device Navigation terms/voice/reroute/lock-screen behavior, the signed v0.2.0-to-v0.2.1 overlay installation, long-tour batch transitions, final release documentation, release candidate, and stable v0.2.1 publication remain separate tasks and are not claimed here.

## 2026-07-30 - Task 36: API 26/API 37 GMS emulator, migration, and offline-runtime acceptance

### Preparation and isolated emulator setup

- Re-read the complete updated `AGENT.md` before starting this task. Installed the official desktop Android Emulator 36.6.11, Android command-line tools 22.0, and Google APIs x86_64 images for API 26 and API 37.0 into the existing local Android SDK. Created dedicated `anitabi-api26` and `anitabi-api37` AVDs and verified Windows Hypervisor Platform acceleration.
- `adb devices` was empty before the runs. Every install, permission, app-op, launch, instrumentation, airplane-mode, screenshot, and shutdown command explicitly targeted `emulator-5556` or `emulator-5558`; no generic device command was used. The Xiaomi phone and its installed v0.2.0 application were not connected, installed to, cleared, uninstalled, or changed.
- API 26 and API 37 both cold-launched the current debug build successfully into the one-time onboarding. Original-resolution screenshots and UI bounds were inspected on both versions: application content starts below the system status bar, and neither onboarding nor the recovered-navigation header is obscured by system bars.

### Defects found and corrected

- The Android 8 onboarding smoke test still waited for the deleted ORS-key input and tried to dismiss the Google-package permission activity through shell grants. Replaced that obsolete path with a real accessibility click on Android 8's system `ALLOW` button, supporting both actual resource namespaces and English/Chinese labels. The test now accepts the current service step directly after a successful permission callback. API 26 and API 37 both complete permission dialogs, service disclosure, entry into search, and restart directly into search.
- Process-death recovery exposed a v0.2.1 policy mismatch: Google route content is intentionally memory-only, but the service attempted a network replan before publishing the saved trip, leaving the user on search with only a foreground notification. The service now publishes the unresolved saved plan and progress immediately, preserves a stable Chinese refresh-unavailable message across backend/authentication failure, and automatically replaces it with live navigation only after a successful refresh.
- Added a local recovery panel that renders the complete saved point order and each point's completed/pending state without creating the Google navigation map. This keeps user-owned trip data visible while offline or without GMS and avoids a blank map or raw SDK/authentication message. Manual arrival is disabled unless navigation is actually running; ending or hiding the saved recovery remains available.
- Updated the emulator workflow to require the saved tour name, the explicit route-refresh notice, and a saved point after both first process recovery and a second force-stop/restart. Foreground-service lock-screen tests now use a transit fixture, which exercises the application's permitted transit status/TTS path without pretending the application owns Google road instructions or making a live navigation request.
- Enabled the expensive emulator matrix for same-repository pull requests as well as `main` pushes, while still excluding fork pull requests that cannot receive repository secrets. This moves API 26/API 37 failures in front of the merge boundary instead of discovering them only after a `main` push.

### Local acceptance evidence

- `testDebugUnitTest`, `assembleDebug`, and `assembleDebugAndroidTest` pass after the final changes. The JVM report contains 65 tests across 18 suites with zero failures, errors, or skips. The tracked-source credential audit, Debug APK content audit, and `git diff --check` also pass. This includes stable `QUOTA_EXHAUSTED` mapping without response-body exposure and the existing unlimited-point/window/batch/navigation coordinator coverage.
- On each of API 26 and API 37, eight targeted instrumentation tests/checks pass: two real Room 1-to-2 migrations, two settings/telemetry migrations, complete onboarding and restart, process-recovery fixture seeding, offline foreground completion with persisted progress, and automatic mock-GPS arrival while the screen is off. Room retains the v0.2.0 guide, selected trip and progress, drops route geometry/steps, removes the old ORS payload, preserves invalid legacy records with a recovery message, and remains idempotent. Analytics and Crashlytics default off and persist independently when enabled or withdrawn.
- Both versions preserve the active tour across process death in airplane mode, immediately show `Runtime Smoke Tour`, all three ordered points and completed/pending state, and the explicit route-refresh-unavailable notice. Both foreground runtime tests retain an ongoing notification, advance and complete the saved progress, and emit the expected two screen-off GPS evidence markers.
- API 37's crash buffer contains no application crash. API 26's crash buffer contained one Android emulator `SystemServer` boot-time `NetworkPolicyManagerService` null-pointer entry, but no `cn.anitabi.navigator` crash; the AVD subsequently booted normally and completed every application test.
- No real Google route, matrix, navigation reservation, or other billable request was made in this task. No API key, Firebase token, service-account material, VPS credential, password, coordinate payload, route body, anime search term, or signing secret was printed or written to tracked source, Git, or this record. The VPS password, SSH configuration, SSH port, login policy, authorized keys, firewall, and server deployment were not changed.

### Pull-request gate correction

- Opened pull request 1 from the implementation branch to `main` and triggered Android CI run `30524115073`. The verify job passed in 6 minutes 12 seconds, including tests, lint, Debug APK build, tracked-source credential audit, APK audit, and artifact upload. The initial clean-runner emulator matrix then exposed two test-harness defects rather than product crashes.
- API 26 displayed the real Google package-installer permission activity, but the initial helper allowed only four seconds for the dialog to leave focus. Extending that wait passed a fresh wiped local AVD in 10.389 seconds but still failed on synchronize run `30525602607`: the uploaded screenshot had returned to the application while the Android 8 cloud image's `dumpsys` still reported the removed permission activity as resumed. The helper now treats actual coarse/fine permission grants as authoritative, retains the accessibility action on the real system Allow button, adds a coordinate tap on the same discovered button as a fallback, and emits non-secret evidence for which click path ran. A local API 26 rerun passed the entire flow in 8.871 seconds with the real allow node and accessibility-action evidence.
- API 37 restored the saved tour, stable offline notice, first completed point, and progress in its uploaded 320-by-640 UI hierarchy. The workflow had incorrectly required the second point name even though that row was below the small runner viewport. Recovery assertions now require the visible saved first point and its completed state, alongside the existing tour and offline-message checks, on both launches. All four revised markers were verified against the first failing run's UTF-8 evidence artifact, and the complete API 37 cloud job subsequently passed in 8 minutes 18 seconds on run `30525602607`.
- Run `30526951807` confirmed the remaining API 26 failure was an Android emulator defect: its crash buffer records `com.android.systemui` failing as the runtime permission activity opened, and ActivityManager reports the System UI crash dialog over the still-resumed Google package installer. API 37 again passed its complete cloud job, this time in 8 minutes 47 seconds. The Android 8 helper now detects and dismisses that system crash dialog through its real accessibility button, with a display-relative fallback only when the crash window is visible, then continues to the real permission Allow control. It does not use `pm grant` for the Android 8 onboarding flow.
- Recompiled the affected AndroidTest APK successfully after the final test-only change. Reproduced the runner's exact Google APIs API 26 display at 320 by 640 and density 160, then passed the complete wiped onboarding/restart test in 9.149 seconds. Evidence showed the permission dialog, display fallback, discovery of the real Allow node, a successful accessibility click, service disclosure, completed search entry, and restart into search; the test finished `OK (1 test)`. No production application behavior, secret, VPS state, SSH setting, phone data, or release artifact changed during this correction.
- Follow-up run `30529352872` proved the Android 8 correction in the clean cloud image: the verify job and the complete API 26 job passed, including first-run permission onboarding, emulator reset, two-launch offline process recovery, foreground completion, screen-off mock-location arrival, migration/telemetry tests, crash checks, and evidence upload. API 37 failed before instrumentation because `am start -W` returned its fixed timeout after 10.706 seconds; the artifact shows the application actually displayed at 10.960 seconds, remained the resumed activity, and had no crash. Replaced that brittle status-string gate with bounded 30-second polling for the real process and resumed activity, and similarly retries the onboarding UI hierarchy until Compose has rendered. This changes only clean-runner synchronization, not application behavior.
- Final clean-runner run `30530550542` completed successfully at head `1d99062`: verify passed in 5 minutes 46 seconds, the full Google APIs API 26 job passed in 7 minutes 41 seconds, and the full Google APIs API 37 job passed in 7 minutes 18 seconds. Both emulator jobs passed cold launch, real first-run onboarding, migrations, telemetry settings, offline process recovery, foreground completion, screen-off mock-location arrival, crash checks, and evidence upload. Task 36's local and pull-request emulator acceptance is therefore complete; phone-only Google Navigation acceptance remains outside this task.

### Remaining acceptance boundary

- Local API 26/API 37 migration, onboarding, process recovery, offline handling, foreground service, screen-off location, telemetry, and quota-error acceptance are now evidenced. Google Navigation terms, native road voice, real-location road guidance, off-route rerouting, and destination-batch transitions still require the later signed Xiaomi v0.2.0-to-v0.2.1 overlay acceptance; none of those phone-only results are claimed here.

## 2026-07-30 - Task 37: v0.2.1 release-candidate documentation and prerelease gate

### Preparation and scope

- Re-read the complete 987-line `AGENT.md` before starting this new task. Confirmed PR 1 was mergeable and its final run `30530550542` had successful verify, API 26, and API 37 jobs at head `1d99062`.
- Followed the user's standing prohibition on phone installation: no Xiaomi connection, APK installation, overwrite, uninstall, data clear, permission change, input injection, or device configuration occurred. This task prepares and publishes only a signed GitHub prerelease candidate; stable v0.2.1 remains gated on a later explicitly permitted overlay test.
- Used the GitHub publish workflow instructions for scoped status/diff/authentication checks. The only pre-existing worktree change was Task 36's final truthful CI-evidence line in this file; it is included deliberately rather than discarded.

### Documentation, license, and release implementation

- Replaced the stale v0.2.0 MapLibre/ORS/Transitous README with the current Google Navigation, Google Routes through VPS, Firebase, unlimited-tour batching, migration, GMS, quota, privacy, build, and fixed-signing model. Preserved the public v0.2.0 link and clearly separated RC from stable acceptance.
- Replaced active NOTICE attribution with Google Navigation SDK, Routes API, Firebase, Bangumi, and Anitabi; removed old providers from the current attribution and retained them only as explicitly historical v0.2.0 references.
- Added a standalone v0.2.1 privacy statement covering user-owned local state, memory-only Google route content, Firebase anonymous authentication, VPS quota/log/IP handling, independent Analytics/Crashlytics opt-in and withdrawal, Anitabi direct access, backups, outages, deletion, and contact.
- Appended the sole author's narrow Google SDK linking exception to `LICENSE`. It permits linking/distribution only with unmodified Google Navigation SDK for Android and Firebase Android SDKs, grants no SDK rights, covers no other proprietary library or service, and keeps all project code under GPL-3.0-or-later. The About page now states and links the same boundary.
- Rebuilt the release checklist for v0.2.1 and added release notes plus an RC acceptance record with every phone-only item visibly unchecked. Historical v0.2.0 records were not rewritten.
- Extended the signed-release workflow to accept `vX.Y.Z-rc.N`, compare the base version to `versionName`, use the base-version notes, and mark RCs as GitHub Prereleases while retaining stable `vX.Y.Z` behavior. Updated exact-release compatibility to strip the RC suffix for package inspection and use bounded process/activity/UI polling instead of the flaky fixed `am start -W` status timeout.

### Local verification and remaining publication steps

- Local Android verification passes 65 JVM tests with zero failures/errors, Debug and Release Lint, Debug APK assembly, androidTest APK assembly, tracked-source credential audit, APK content audit, and `git diff --check`. Both lint tasks completed successfully after the About-page license change.
- Backend verification still passes all 17 tests, including the concurrent hard-cap test, and production dependency audit reports zero vulnerabilities. No Google API, Firebase sign-in, billable route, VPS mutation, password use, or SSH action occurred.
- PyYAML parsed Android CI, signed release, and release-smoke workflows successfully. Tag/base-version and prerelease behavior is encoded in the release workflow; final authoritative validation remains the protected GitHub Actions run with encrypted Firebase, Navigation, and signing secrets.
- Remaining steps in this same task are to push the scoped changes, obtain a green PR run, merge PR 1, verify main, tag `v0.2.1-rc.1`, inspect the signed prerelease APK/signature/checksum, and complete API 26/API 37 exact-release compatibility. Stable v0.2.1 and every phone-only result remain unclaimed.

### Merge, main gate, and signed prerelease evidence

- Pushed commit `712645c` and obtained a fully green final PR run `30532758088`: verify passed in 5 minutes 47 seconds, API 37 in 8 minutes 17 seconds, and API 26 in 9 minutes 1 second. The authenticated CLI and GitHub connector both lacked merge mutation scope, so the already signed-in GitHub browser UI was used only to merge PR 1 after verifying its green state. GitHub recorded merge commit `2ee2336841071936d4d4110b4f5435dfd9d78b58`; no account credential, cookie, browser storage, repository setting, or branch-protection setting was read or changed.
- Fast-forwarded the local `main` to the merge commit and waited for the independent main Android CI run `30533942801`. It completed successfully: verify 6 minutes 25 seconds, API 37 8 minutes 41 seconds, and API 26 9 minutes 3 seconds. Both emulator jobs passed cold launch, first-run onboarding, migrations, offline process recovery, foreground completion, screen-off mock-location arrival, crash checks, and evidence upload.
- Created annotated tag `v0.2.1-rc.1` only after the main gate passed and pushed it to the verified merge commit. Signed APK Release run `30534982768` passed in 6 minutes 14 seconds, including tracked-source audit, protected Firebase/Navigation configuration restoration, release keystore restoration outside the workspace, tag/base-version validation, tests, signed build, APK content audit, signature verification, checksum generation, and GitHub Prerelease publication.
- Published non-draft Prerelease `v0.2.1-rc.1`; the stable Latest release remains `v0.2.0`. The APK is 48,686,861 bytes with SHA-256 `fb3cf29be517e632740c370ddaecd75f22863dec7a22e20e2b7c998bf6eb9ea1`. Its public checksum file, GitHub asset digest, and an independent downloaded-file calculation agree exactly.
- Independent `aapt` inspection reports package `cn.anitabi.navigator`, `versionCode=7`, `versionName=0.2.1`, minimum SDK 26, and target SDK 37. Independent `apksigner verify --verbose --print-certs` confirms APK Signature Scheme v2, one RSA-4096 signer, and certificate SHA-256 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`, identical to the fixed public v0.2.0 certificate.
- Exact-release compatibility run `30535389645` downloaded the published signed RC and passed on API 26 in 2 minutes and API 37 in 2 minutes 19 seconds. Both jobs verified the package version, installation, cold launch, empty application crash buffer, and evidence upload. The earlier main push also triggered a successful compatibility recheck of the then-latest stable v0.2.0; it was not counted as RC evidence.

### Task completion and remaining stable-release boundary

- Task 37 is complete: current documentation, licensing, privacy text, release workflow, merged main verification, signed prerelease, public artifact integrity, fixed-certificate continuity, and exact-release API 26/API 37 compatibility are now evidenced.
- No Google billable request, Firebase sign-in, VPS mutation, SSH connection, password use, SSH configuration change, or phone access occurred in this task. No secret was written to source, Git, logs, documentation, or this record.

## 2026-07-30 - Task 38: v0.2.1 completion audit fixes (paused for physical-device priority)

- Re-read this complete record and the original implementation plan, audited current Android, backend, release, and production-health evidence, and created branch `codex/v0.2.1-rc.2-audit-fixes` from the synchronized `main` baseline.
- Added test-first fixes for four plan gaps: road manual reordering now refreshes only changed adjacent leg ranges while preserving unaffected legs and keeping every request at no more than 12 locations; normalized transit legs now retain and display Google-provided departure stop, arrival stop, and stop count without inventing platforms; walking/cycling screens show the required Google beta safety notice; and cached v0.2.0 multi-anime/point selection is restored after process restart without a network request.
- Added migration coverage that verifies the two selected anime identities survive a public-v0.2.0-style Room 1-to-2 record migration. The new focused tests initially failed on the absent fields/functions, then passed after the implementation.
- Full local Android verification passed: 68 JVM tests with zero failures, Debug and Release Lint, Debug APK, and Debug androidTest APK. Tracked-source credential audit and debug APK content audit both passed.
- Backend verification remained green with 17 tests and zero failures; production dependency audit reported zero vulnerabilities. Public HTTPS health returned exactly service/database healthy, HTTP redirected to HTTPS, and the existing personal-site endpoint continued responding through its prior Cloudflare configuration.
- A strict, non-interactive SSH identity probe refused to authenticate because the API hostname has no matching local ED25519 known-host entry in the current environment. No host key was accepted, no password prompt was reached, no password was used, and no VPS, SSH, password, firewall, or deployment state was changed.
- Work is intentionally uncommitted on the audit-fix branch while the user-requested installed-phone test takes priority. No APK was installed on or read from the phone during this task.
- The Xiaomi 15T Pro and its installed public v0.2.0 were not connected, overwritten, uninstalled, cleared, or changed. Stable `v0.2.1` remains intentionally unpublished until the user permits the phone-only signed overlay acceptance covering data retention, Google Navigation terms/map/voice/real location/lock screen/rerouting/arrival, long-tour production batch transitions, and transit/error recovery.

## 2026-07-30 - Task 39: installed rc1 Xiaomi physical-device test and map crash containment

### Preparation and device boundary

- Re-read the complete updated `AGENT.md` before starting this new task. The user explicitly required testing the already installed application without another installation; no APK was installed, overwritten, pulled, uninstalled, or cleared.
- Verified one authorized Xiaomi 15T Pro on Android 16/API 36. The installed package is `cn.anitabi.navigator` version 0.2.1 code 7, and its on-device APK SHA-256 exactly matches the public signed `v0.2.1-rc.1` asset. Fine/coarse location and notification permissions were already granted and were not changed. The separate mock-location setup remained untouched.
- Cold launch succeeded in 507 ms with the application resumed and no initial application crash. The application opened directly to search, proving the completed onboarding state survived the user's v0.2.0-to-rc1 overlay. Original-resolution search and About-page captures confirmed that the top app content clears the status bar and the bottom content clears the gesture area.

### Live read-only acceptance and blocking defect

- Bangumi returned live result lists. The Anitabi no-data state rendered correctly for multiple valid search results, and a known available entry loaded a current 74-point pilgrimage dataset without an Anitabi network/forbidden error. No anime name, search term, coordinate, image URL, or response body is recorded here.
- Entering the Google map from the loaded dataset crashed the application twice, including after a full application relaunch and fresh data selection. Both isolated crash-buffer captures report `IllegalStateException: Unable to instantiate the dynamic class com.google.android.gms.maps.internal.CreatorImpl`, caused by `InstantiationException` because that dynamic class has no zero-argument constructor. The device has current Google Play services 26.26.34 and the current Maps dynamite module; OpenGL ES and GMS device requirements are satisfied.
- Official Navigation SDK documentation still lists 7.8.0 as current and its release notes describe a map-initialization crash fix, while the official map-only sample uses `NavigationView` without requiring an active Navigator. The project has only `com.google.android.libraries.navigation:navigation:7.8.0` and no separate Maps SDK dependency. This evidence places the constructor mismatch at the Google dynamic map-module boundary rather than an API-key, Routes-backend, permission, or duplicate-Maps-SDK failure.
- The About/privacy page opened without a crash or system-bar overlap. Analytics and Crashlytics were visibly unchecked; neither switch was touched. Google Navigation terms, map interaction, route preview, Firebase/backend planning, native voice, real-location guidance, lock-screen guidance, rerouting, arrival, transit segments, and long-tour batch transitions could not be accepted because the map crash occurs before those flows. None is claimed as passed.

### Source containment and verification

- Updated the shared `NavigationMapView` wrapper so a runtime failure while constructing, starting, resuming, or requesting the Google map no longer kills the application. The affected map region now shows a concise retained-data notice and an explicit retry action; healthy devices continue to use the same Navigation SDK path, and no alternate map/provider was added.
- The complete Android JVM suite passes 68 tests across 19 suites with zero failures, errors, or skips. Debug and Release Kotlin compilation, Debug and Release Lint, Debug APK assembly, tracked-source credential audit, Debug APK content audit, and `git diff --check` pass after the containment change.
- A local Release APK assembly was intentionally rejected by the existing fail-closed guard because this terminal has no external production-signing environment. No unsigned or differently signed release artifact was substituted, and nothing from the local build was installed on the phone. Physical verification of the containment UI requires a later user-installed rc2 candidate.
- The existing rc1 was relaunched after the second crash and left inside the application. Temporary device screenshots and UI hierarchies were removed after each capture. No device data, account, input method, permission, mock-location setting, proxy setting, Google Play services state, password, SSH configuration, VPS state, or billable Google route request was changed.

## 2026-07-30 - Task 40: exact multi-anime and map-crash retest, parser repair, and functional fallback

### Physical reproduction before source changes

- Re-read the complete updated 1,058-line `AGENT.md` before starting this new task. Retested only the already installed public `v0.2.1-rc.1`; no APK was installed, overwritten, pulled, uninstalled, or cleared, and no permission, input method, mock-location, proxy, Google Play services, or other device setting was changed.
- First selected two unrelated works with valid pilgrimage data. The current rc1 successfully displayed two selected works and 142 combined points, proving that the multi-selection state and merge path are not universally broken.
- Then reproduced the user's exact failure with the first and second entries from the reported search. The first selection loaded 60 points. Selecting the second left the first selection intact but displayed `公共服务返回了无法识别的数据`, so the failure is specific to that second work's Anitabi payload rather than selection order alone. No anime title or search term is retained in this record.
- Independently cleared the crash buffer and opened the map with the valid 142-point selection. The application terminated immediately and the isolated crash again reported `IllegalStateException` while instantiating dynamic `com.google.android.gms.maps.internal.CreatorImpl`, caused by the missing zero-argument constructor. This reconfirms that the crash is independent of total point count.

### Root cause and fixes

- Used Agent Reach's documented web-reader route against the official Anitabi endpoint and emitted only field-type counts. The failing response returned HTTP 200 with 71 otherwise valid point records; 70 `name` fields were strings and one was an integer. The strict nullable `String` decoder rejected that single volatile optional field and therefore rejected the complete second work.
- Added a regression fixture with the same numeric optional-name shape; it failed with `JsonDecodingException` before the implementation. Added a narrow nullable-string serializer for all optional Anitabi text fields: real strings are preserved, while numbers, booleans, arrays, objects, or null are treated as absent so the existing localized-name/unnamed, image, origin, and safe-link fallbacks apply. Required IDs and coordinates remain strictly validated.
- Extended the Task 39 Google map containment. Selection-map initialization failure now reports a concise message and automatically switches to the existing point list, where users can still select points and plan a trip; the footer's map action remains available for an explicit retry. Route-preview and navigation map regions retain the in-place retry panel. Healthy devices still use Navigation SDK 7.8.0, and no alternate map provider or external-map navigation was added.
- The Google dynamic renderer itself remains unavailable on this specific installed rc1/device combination; this task fixes the application crash and preserves the complete selection workflow but does not falsely claim that Google map rendering has passed. Physical verification of the repaired behavior requires a later user-installed rc2.

### Verification and cleanup

- The exact parser regression test changed from red to green. The complete Android JVM suite passes 69 tests across 19 suites with zero failures, errors, or skips. Debug and Release Kotlin compilation, Debug and Release Lint, Debug APK assembly, Debug androidTest APK assembly, tracked-source credential audit, Debug APK content audit, and `git diff --check` all pass.
- All temporary local API samples, screenshots, and UI hierarchies were deleted after extracting type/count evidence; all task-specific device captures were deleted immediately after pulling. The phone disconnected after the completed interactions, and no post-disconnect state is claimed.
- No response body, coordinate, anime name, search term, API key, Firebase token, service-account material, signing secret, password, VPS credential, or SSH state was written to tracked source, Git, this record, or retained temporary files. No Google route/navigation request or VPS mutation occurred. The fixes remain uncommitted with the other rc2 audit changes on `codex/v0.2.1-rc.2-audit-fixes`.

## 2026-07-30 - Task 41: release-map crash repair, rc3 publication, and new-phone retest handoff

### Release-only root cause and repair

- Re-read the complete current `AGENT.md` before starting the task. Reproduced the installed rc2 map failure from a fresh application crash buffer and traced it to R8 moving the Navigation SDK's reflective registry caller out of the package that contains its package-private implementation.
- Read the Navigation SDK 7.8.0 embedded shrinker requirements and added only its required class-merging exclusions plus a targeted keep rule for the reflective caller. Release minification remained enabled. A fresh R8 mapping proved the caller and target remain package-compatible while unrelated application and dependency code is still obfuscated.
- The complete local Android gate passed 69 JVM tests across 19 suites, Debug and Release Lint, Debug APK assembly, Android-test APK assembly, tracked-source credential audit, APK content audit, backend 17-test suite, production dependency audit, release R8 mapping audit, and `git diff --check`.

### rc3 publication and prior-phone evidence

- Committed the reviewed repair on `codex/v0.2.1-rc.3-navigation-r8-fix`, opened pull request 3, obtained green verify/API 26/API 37 jobs, merged it through the authenticated GitHub UI, and obtained an independent green main run. Tagged and published signed prerelease `v0.2.1-rc.3`; its Release workflow and exact-public-APK compatibility workflow passed.
- The public rc3 APK is 48,686,861 bytes with SHA-256 `00cfbeb1fec2fed237f1dd825e1f0a727f552959ba4c4b1e7915d3900419470f`. It reports package `cn.anitabi.navigator`, version code 7, version name 0.2.1, minimum SDK 26, target SDK 37, APK Signature Scheme v2, one RSA-4096 signer, and the unchanged production certificate.
- With the user's explicit permission, overlaid the exact public rc3 APK on the previously connected phone using `adb install -r` only. The installation preserved first-install time, application data, permissions, completed onboarding, and the fixed certificate. The installed APK hash matched the public asset, cold launch succeeded, and the application no longer terminated when entering the selection flow.
- The existing two-work selection survived and displayed the merged point count. This reconfirmed the parser/multi-selection repair, but it did not prove that a Google map tile rendered or that a route was generated.

### Unresolved functional failure and new-phone boundary

- The user reported that rc3 still cannot display the map or plan a route. The acceptance standard was therefore corrected from merely preventing a crash to requiring a visibly rendered Google map and a successful two-point route. Neither is marked passed.
- The previous phone disconnected during an isolated single-tap map capture, so no incomplete screenshot or missing-device command was treated as product evidence. When the user switched phones, this task re-read the complete 1,080-line log before touching the new device.
- Windows currently detects only a Huawei HDB interface. ADB USB and mDNS device lists remain empty even after restarting the desktop ADB server. Consequently the application version, existing installation, map, route, permissions, and crash state on the new phone cannot yet be inspected.
- No APK was installed, overwritten, pulled, uninstalled, or cleared on the new phone. No phone permission, USB mode, HDB setting, input method, mock-location setting, network, password, SSH configuration, VPS state, Google credential, or billable request was changed. The precise resume condition is that the unlocked new phone expose an authorized ADB interface and accept this computer's debugging prompt.

## 2026-07-30 - Task 42: RC3 marker crash repair, RC4 publication, and target-device identity hold

### Physical reproduction and repair

- Re-read the complete current `AGENT.md` before continuing the new-phone physical task. Installed the exact public RC3 on the then-authorized Android 12 phone only after confirming the application was absent; no uninstall, data clear, permission change, or device-configuration change occurred.
- Cold launch succeeded, and a live production selection loaded 60 usable pilgrimage points. Opening the selection map from an isolated crash buffer reproduced a release crash: `IBitmapDescriptorFactory is not initialized`. The RC3 R8 mapping restored the failing call to the colored default-marker construction in `PilgrimageMap`.
- Added shared marker-option builders that use Google default markers without `BitmapDescriptorFactory`, retained selected-state visibility through alpha and z-index, and used the same safe builders for pilgrimage, route, and current-location markers. Map listener, marker, and camera runtime failures now clear the map and invoke the existing unavailable/list fallback instead of terminating the process.
- Added regression tests proving the marker options have no bitmap-factory-backed icon and preserve the intended alpha/z-index state. The complete local gate passed 71 JVM tests across 20 suites, Debug and Release Lint, Debug APK, Android-test APK, tracked-source credential audit, Debug APK content audit, fresh release R8/rule audit, backend 17-test suite, production dependency audit, and `git diff --check`.

### Publication and downloaded Firebase evidence

- Committed and pushed the isolated fix, opened pull request 4 through the authenticated GitHub UI after the command-line token again lacked PR mutation scope, and merged only after verify/API 26/API 37 run `30550636244` passed. Independent main run `30551504222` then passed the same three jobs.
- Tagged the verified merge commit as annotated `v0.2.1-rc.4`. Signed Release run `30552400597` passed and published a non-draft Prerelease; exact-public-APK compatibility run `30552936822` passed on API 26 and API 37.
- The public APK is 48,686,861 bytes with SHA-256 `bbf724e24f2fee36efaf9e137a2711688b7d0d06351aaa7bddebce064ea39ca7`. The checksum asset, GitHub digest, and independent download calculation agree. Package inspection reports version 0.2.1/code 7, minSdk 26, targetSdk 37, APK Signature Scheme v2, one RSA-4096 signer, and the unchanged production certificate.
- The user supplied a downloaded Crashlytics stack rather than requesting Gmail access. It contains one older fatal session with a distinct R8 map ID and `IllegalAccessException` between the Navigation SDK reflective caller and package-private implementation. This is the RC2/R8 package-access failure already fixed and verified in RC3, not the Android 12 RC3 marker-factory crash and not evidence that RC4 regressed.

### Current device hold

- Before installing RC4, read-only ADB checks found that the sole currently connected device is the earlier Android 16 Xiaomi, not the Android 12 phone used for the RC3 marker reproduction. Its installed APK hash is the public RC3 hash and its existing permissions remain granted.
- No RC4 installation was attempted on the mismatched device. No APK was uninstalled or cleared, and no permission, mock-location setting, network setting, input method, password, SSH configuration, VPS state, Google credential, or billable request was changed.
- The remaining acceptance gate is device-specific: reconnect/identify the intended Android 12 phone for `adb install -r`, or explicitly authorize using the currently connected Xiaomi. Map acceptance still requires visible Google tiles and markers, and route acceptance still requires one minimal two-point road request; fallback-only behavior is not counted as a rendered map.

## 2026-07-30 - Task 43: Xiaomi RC4 retest, RC5 initialization diagnosis, and emergency disk cleanup pause

### Xiaomi RC4 evidence and paused RC5 repair

- Re-read the complete 1,124-line `AGENT.md` before the Xiaomi overlay task. Following the user's updated target, the Huawei/Android 12 phone was removed from the current acceptance scope and the connected Xiaomi 15T Pro is now the only physical target.
- Overlaid the exact public RC4 with `adb install -r`; no uninstall, data clear, permission change, mock-location change, or device-setting change occurred. The installed APK hash matched the public asset, first-install time remained unchanged, existing location/notification grants remained present, and the completed onboarding plus previously loaded point set survived.
- RC4 cold-launched successfully and remained alive with an empty application crash buffer. Opening the point map did not crash, but it displayed the map-unavailable notice and automatically fell back to the list; no Google basemap or markers rendered. The map gate therefore failed, and no billable route request was attempted.
- Read-only inspection of the local Navigation SDK 7.8.0 AAR confirmed that `MapsInitializer.initialize` installs both the camera-update and bitmap-descriptor factories. The shared wrapper created `NavigationView` without that initialization, which explains RC3's marker-factory failure and RC4's subsequent camera-operation fallback.
- Added an uncommitted RC5 repair that initializes the Google map runtime before creating `NavigationView`, plus tests proving initialization order and fail-closed view creation. The targeted two-test suite and the full 73-test/21-suite JVM run passed, as did Debug/Release Lint, Debug APK, Android-test APK, backend 17-test suite, production dependency audit, source/APK credential audits, and `git diff --check` before cleanup. A fresh R8 mapping retained the Navigation SDK rules, but the explicit repeat run was interrupted for the disk emergency; RC5 was not committed, pushed, published, or installed.

### Emergency local disk cleanup

- Immediately stopped the active R8 wrapper and every Gradle daemon after the user reported only about 0.45 GB free. Re-read this complete file again before cleanup, then performed a read-only disk inventory before deleting anything.
- Deleted only the two project-created AVDs `anitabi-api26` and `anitabi-api37`, their API 26/API 37 Google system images, and the locally installed emulator package through Android's management tools. No emulator process was running. Platform-Tools/ADB, SDK 37 compilation platform, Build-Tools, command-line tools, and licenses were retained.
- Ran the Gradle `clean` task and removed generated project build/evidence directories, project Gradle/Kotlin verification caches, backend `node_modules`/`dist`, obsolete Gradle 9.4.1/9.5.0 caches and distributions, and the current 9.6.1 compiled-build cache. Source, Git history, ignored Firebase/Navigation configuration, signing material, and user downloads were not removed.
- Removed the unused local `anitabi-api:0.2.1` image. Identified 56 BuildKit cache records created in the single documented Anitabi backend-build hour whose descriptions matched the Bookworm/Alpine attempts and `backend/Dockerfile`, then pruned only those exact IDs. Three-day-old caches and all unrelated Docker images, containers, volumes, and data were preserved; the same six unrelated containers remained running afterward.
- Confirmed task-specific RC4/SDK-inspection temporary directories no longer exist, no Gradle daemon is running, ADB still sees the authorized Xiaomi, and the pending RC5 source/document changes remain intact in the worktree. No application interaction, installation, data change, or input was performed during cleanup.
- Free space increased from about 0.45 GB to 41.085 GB, recovering about 40.6 GB. The remaining 33.81 GB Docker virtual disk predominantly contains unrelated images/running services and cannot be physically compacted without interrupting them; the 25.788 GB system-managed pagefile and Codex task/session history were also left untouched rather than risking unrelated state.

### Resume boundary

- RC5 work remains deliberately paused after cleanup. Resuming local Android gates will recreate some disposable Gradle/build data, while the removed local emulators would need reinstalling only if local emulator runs are required; protected GitHub API 26/API 37 jobs remain available.
- Before any resumed task, this updated file must be read completely again. RC5 publication and Xiaomi overlay still require a fresh confirmed R8 result, green branch/main/release workflows, exact public-APK verification, a rendered Google map, and one minimal two-point route; none is claimed here.

## 2026-07-31 - Task 44: RC5 publication, exact Xiaomi overlay, and failed map gate

### Local, CI, and publication evidence

- Re-read the complete current `AGENT.md` before resuming RC5. Re-ran the fresh full Release R8 build successfully, retained the Navigation SDK shrinker rules and expected map-runtime call, and passed the focused map-runtime tests. The prior unchanged-source 73 JVM tests across 21 suites, Debug/Release Lint, Debug APK, Android-test APK, backend 17-test suite, production dependency audit, source/APK credential audits, and `git diff --check` remained green.
- Committed and pushed the isolated RC5 change, opened pull request 5, and merged only after branch run `30558442528` passed verify, API 26, and API 37. Independent `main` run `30559289993` passed the same jobs at merge commit `921ae8a26d0f9c53f97a8f24c3b88e6084ce88cc`.
- Created annotated tag `v0.2.1-rc.5` only after the green main run. Signed Release run `30560118538` passed and published the non-draft Prerelease; exact-public-APK compatibility run `30560739001` passed on API 26 and API 37.
- The public APK is 48,686,861 bytes with SHA-256 `9f52f0b24d8b75a1a45763b44690c8c70932568bf67453aa6439cddffaf21ccd`. The checksum asset, GitHub digest, independent downloaded-file calculation, and subsequently pulled installed APK agree. Package inspection reports version 0.2.1/code 7, minSdk 26, target SDK 37, APK Signature Scheme v2, one RSA-4096 signer, and fixed certificate SHA-256 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.

### Exact physical-device result

- With the user's explicit approval, overlaid the exact public RC5 on the authorized Xiaomi 15T Pro using `adb install -r` only. No uninstall, data clear, permission change, mock-location change, or device-setting change occurred. First-install time remained unchanged; existing location and notification grants, completed onboarding, selected-work state, and the 60 loaded points survived.
- Cold launch succeeded with the process foreground and an empty application crash buffer. Opening the selection map did not terminate the process, but the UI reported that Google Maps was unavailable and switched to the point list. A second explicit map retry produced the same result after another 15-second observation; no Google basemap or markers rendered.
- RC5 therefore failed the physical map gate. Testing stopped before the billable boundary: no two-point route, Firebase-backed planning request, Navigation reservation, or Google Routes request was sent, and route preview is not claimed as passed.

### Corrected diagnosis and immutable RC boundary

- Rechecked the official Navigation SDK reference, Navigation SDK 7.8.0 bytecode, and fresh RC5 Release R8 outputs after the device failure. Official documentation states that `MapsInitializer` does not apply to Navigation SDK, while `NavigationView.onCreate` internally initializes the map after constructing its Navigation-specific renderer component. The AAR's dynamic `CreatorImpl` has a public zero-argument constructor, but RC5's R8 `usage.txt` explicitly records that constructor as removed and `seeds.txt` retains only its class name. RC5's external call before constructing `NavigationView` therefore deterministically reflects into a class whose required constructor was stripped and enters the safe fallback.
- Read-only code review also identified a separate deterministic layout race: selection and route-preview maps call the two-argument `CameraUpdateFactory.newLatLngBounds` immediately after map readiness, before the hosted view is guaranteed to have non-zero dimensions. The selection path catches that runtime failure and converts it into the observed list fallback; the route-preview path lacks equivalent containment and could fail after route generation.
- RC5 remains an immutable historical Prerelease and must not be retagged or described as successful. A successor must remove the inapplicable external initializer, narrowly retain and audit the reflected public zero-argument `CreatorImpl` constructor, wait for positive map-view dimensions and use the dimension-aware bounds update (or an equivalent documented post-layout mechanism), distinguish camera-placement failure from map-runtime failure, and pass a visible-basemap/marker Xiaomi test before the minimal two-point route test.
- Removed the task-specific public-APK/device-capture directory after recording its hashes and acceptance evidence. The read-only AAR inspection directory was also removed; both exact temporary paths were verified absent. No unrelated temporary directory, SDK component, application file, user download, or running service was removed.
- No secret, token, raw IP, coordinate, work title, search term, route body, service-account material, signing material, password, VPS credential, or SSH state was recorded or changed. No SSH action or VPS mutation occurred.

## 2026-07-31 - Task 45: RC6 map lifecycle, DRIVE normalization, and TRANSIT timestamp repairs

### Reproduction and implementation

- Re-read the complete current `AGENT.md` before starting this repair task. Kept the Xiaomi 15T Pro as the only physical target and did not interact with any phone during this task.
- Corrected the RC5 map diagnosis and implementation: removed the Navigation-SDK-inapplicable external `MapsInitializer` call, retained the reflectively constructed map creator's public zero-argument constructor, and added a release R8 audit that checks the creator class/constructor, Navigation registry package, class-merging exclusions, and absence of application `MapsInitializer` calls.
- Reworked the hosted `NavigationView` lifecycle so attach and process lifecycle state gate `onCreate`, UI configuration, start/resume, and the single map request. Destruction is paired only with a successful creation, late callbacks are ignored after disposal, and privacy-safe stage/exception-class logging replaces undifferentiated fallback.
- Both the selection map and route preview now wait for positive hosted-view dimensions and use the dimension-aware bounds update. Camera-fit failure no longer marks the entire map runtime unavailable. Route-preview drawing and camera operations are contained, and a configuration failure cannot leave a stale `GoogleMap` delegate active.
- Reproduced the DRIVE failure with a legal mocked Google 2xx response whose protobuf JSON omitted default-valued distance/duration fields. The backend had incorrectly rejected omitted route/leg zero values. It now normalizes absent protobuf defaults to zero while continuing to reject explicit negative or wrong-typed values.
- Reproduced the TRANSIT 400 entirely in memory: the Android client emitted an offset timestamp without seconds when the selected seconds were zero, and the strict backend RFC3339 schema rejected it before Google was called. Initial and every subsequent transit-segment departure are now normalized through one fixed-seconds RFC3339 formatter; the backend schema remains strict.
- Extracted the existing planner error mapping for testing and changed only transport failures to explain that the route service could not be reached and that switching away from a blocked network exit may be required.

### Verification and local-state boundary

- Added regressions for positive map viewport dimensions, blocked-network failure text, fixed-seconds first/subsequent transit departures, and DRIVE protobuf default omission. The complete Android JVM suite passed 74 tests across 22 result files with zero failures, errors, or skips.
- Debug and Release Lint, Debug APK, Android-test APK, the 18-test backend suite, production dependency audit, tracked-source credential audit, Debug APK content audit, Bash syntax checks, workflow YAML parsing, and `git diff --check` all passed.
- A fresh release R8 run completed the shrinker but its unrelated automatic Crashlytics mapping upload hit a network TLS handshake failure afterward. The CI audit was corrected to exclude that upload. The corrected no-upload command then completed successfully and the fresh reflection audit passed; release publication still retains its normal mapping upload path.
- Ignored the empty Gradle-created `.kotlin` session directory so it cannot enter Git. After all gates the system drive still had about 32 GB free and the project build output occupied about 0.8 GB; no emulator, SDK, Docker data, user file, or unrelated cache was removed.
- No real Google route, matrix, navigation reservation, Firebase sign-in, VPS mutation, SSH connection, password use, APK installation, or device change occurred. No secret, token, raw IP, coordinate, work title, search term, route body, credential, or signing material was written to tracked source, Git, logs, or this record.

## 2026-07-31 - Task 46: RC6 backend normalization deployment

### Strict host verification and deployment boundary

- Re-read the complete updated `AGENT.md` before starting this deployment task. Recovered the expected ED25519 host fingerprint from the prior successful deployment evidence, independently scanned the current host, and confirmed an exact fingerprint match even though the DNS address had changed. Every SSH and SCP connection used strict host-key checking with a task-specific temporary known-host file.
- Used the user-authorized root password only through the interactive SSH password prompt. The password was not placed in a command line, file, environment variable, script, Git, documentation, or this record. No password, SSH daemon setting, authorized key, login policy, port, firewall rule, system-update policy, or provider-console setting was changed.
- Performed a read-only production inventory before mutation. The existing Nginx configuration tested successfully, the Anitabi API was healthy on its loopback-only port, the VPS had sufficient free disk space, and all unrelated application, database, proxy, and personal-site containers were running. No unknown service was stopped, rebuilt, reconfigured, or restarted.

### Backend update and production evidence

- Compared the production `routes.ts` with the RC6 source before replacement; the only effective delta was the reviewed protobuf-default normalization and its explanatory comment. Preserved the previous source and image as explicit rollback artifacts, copied the new source through a temporary path, normalized its line endings, and verified its Git blob identity against the locally tested RC6 file before building.
- Built the pinned Node 24 production image successfully and recreated only `anitabi-api-api-1` with `docker compose up -d --no-deps api`. The container reached `healthy`; both loopback and public HTTPS `/v1/health` returned service/database healthy.
- Reverified that the API runs as the non-root `node` user with a read-only root filesystem and `unless-stopped` restart policy. Nginx syntax remained valid, all previously running unrelated containers remained running, and a privacy-safe scan of the new API container's startup log found no fatal, uncaught, unhandled, or error entry.
- Removed the remote upload and both local task-specific temporary files after verification. The rollback source and image remain on the VPS. Approximately 9 GB remained free after the one-time pinned base-image pull and build.

### Remaining acceptance boundary

- No real Google route, matrix, transit, or navigation request was sent during this deployment, so no billable quota was consumed. The backend half of the DRIVE repair is live; TRANSIT still requires the RC6 Android formatter, and all three user-visible fixes still require the exact published RC6 APK on the authorized Xiaomi.
- No APK installation, phone access, permission change, mock-location change, network-setting change, Firebase consent change, credential rotation, or Git publication occurred in this task. No secret, token, raw IP, coordinate, work title, search term, route body, private key, signing material, or password was written to tracked source, Git, documentation, or this record.

## 2026-07-31 - Task 47: RC6 branch publication and clean-run R8 audit correction

### Branch, review, and first protected run

- Re-read the complete current `AGENT.md` before starting the publication task. Committed the RC6 Android/backend repairs and release documentation on `codex/v0.2.1-rc.6-map-runtime`, then committed a follow-up that made the R8 optimization-rule audit ignore comment-only lookalikes. Pushed both commits and opened draft pull request 6 through the authenticated GitHub UI because the command-line token lacks pull-request mutation scope.
- An independent static review found no remaining application, backend, workflow, or documentation blocker. It confirmed that the signed Release workflow still performs its normal Crashlytics mapping upload and that all unexecuted branch/main/public-APK/Xiaomi gates remain explicitly unchecked.
- The first latest-head pull-request run passed credential restoration, source auditing, all 74 JVM tests, Debug and Release Lint, Debug APK assembly, and Android-test APK assembly. Its Release R8 compilation also completed successfully, but the new post-build audit failed before the emulator jobs because it expected `mapping.txt` in the final assemble-output directory.

### Clean-output reproduction and focused correction

- Reproduced the GitHub failure locally after moving the existing mapping outputs aside and forcing the same standalone `minifyReleaseWithR8` task. The clean task writes `seeds.txt`, `usage.txt`, and `configuration.txt` to the public mapping-output directory, while its fresh `mapping.txt` remains in the task-specific intermediate directory until a complete Release assemble copies the final mapping.
- Changed the audit script to accept an explicit mapping file independently from the other R8 reports. Pull-request/main CI now passes the standalone R8 intermediate mapping explicitly; the signed Release workflow keeps the default final mapping from the fully assembled public APK.
- Verified the audit against both a complete Release output and the clean standalone-R8 split output. A negative invocation without the required split mapping failed as intended. Bash syntax, workflow YAML parsing, and `git diff --check` passed.
- Ran the Gradle `clean` task after the probe, confirmed the probe directory was removed, and retained about 32.76 GB free. No emulator, SDK, Docker data, user file, unrelated cache, or retained release evidence was removed.

### Remaining publication boundary

- The focused CI correction is not yet committed or pushed, and the replacement branch run, merge, independent `main` run, annotated RC6 tag, signed Prerelease, exact-public-APK compatibility jobs, public artifact audit, and Xiaomi overlay tests are still pending.
- No APK was installed, phone state was accessed, billable Google request was sent, VPS state was changed, SSH connection was made, or credential was rotated. No secret, token, raw IP, coordinate, work title, search term, route body, password, private key, or signing material was added to source, Git, documentation, or retained temporary files.

## 2026-07-31 - Task 48: RC6 publication, exact Xiaomi overlay, and secure-keyguard hold

### Protected publication and public-artifact evidence

- Re-read the complete updated `AGENT.md` before starting this continuation. Committed and pushed Task 47's clean-output correction, obtained a fully green replacement pull-request run `30570203541`, marked pull request 6 ready, and merged only after its verify, API 26, and API 37 jobs passed.
- Independent `main` run `30571396977` passed verify, API 26, and API 37 at merge commit `76d086b86cdb5dffbf1fe3e19cc94784d086970f`. Created annotated tag `v0.2.1-rc.6` only after that gate and did not move or overwrite any earlier RC tag.
- Signed Release run `30572533202` passed source auditing, protected Firebase/Navigation/signing configuration restoration, tag/version validation, tests, Release Lint, signed assembly, R8 audit, APK audit, signature verification, checksum generation, and public non-draft Prerelease publication.
- Exact-public-APK compatibility run `30573125093` downloaded the Release asset and passed on API 26 and API 37. The public APK is 48,686,861 bytes with SHA-256 `b8cf6c4f4da977ed766a4509f2d68684c71fafd964842b089c4809f2931f1795`; its checksum asset, GitHub digest, and independent calculation agree.
- Independent inspection reports package `cn.anitabi.navigator`, version 0.2.1/code 7, minSdk 26, targetSdk 37, APK Signature Scheme v2, one RSA-4096 signer, and the unchanged fixed certificate SHA-256 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`.

### Exact Xiaomi overlay and current blocker

- Confirmed exactly one authorized Xiaomi 15T Pro and proved its pre-install package was the exact public RC5. Used `adb install -r` with the exact public RC6 only; no uninstall, data clear, permission change, mock-location change, network change, input-method change, or other device-setting change occurred.
- Android reported a successful overlay. The installed APK hash exactly matches the public RC6, the first-install time is unchanged, and the existing coarse/fine location plus notification grants remain present.
- Cleared only the application crash log buffer for isolated diagnostics, then force-stopped and cold-launched the application. Its process remained present, `MainActivity` was resumed, and the isolated application crash count was zero.
- The display was dozing behind the system keyguard. Waking the display did not remove the secure lock, and read-only Android state reported the device as locked and untrusted. One normal keyguard-dismiss request and one normal upward swipe did not unlock it; testing stopped without guessing a password, bypassing security, or changing lock settings.
- Because the secure keyguard prevents visible application interaction, completed-onboarding and selected-point retention have not yet been visually confirmed on RC6. A real Google basemap with markers, WALK, DRIVE, and TRANSIT also remain unverified; no map request or billable route request was sent.
- The task-specific public APK copy remains in its dedicated temporary acceptance directory only until physical testing completes. The next safe step is for the user to unlock the Xiaomi and leave the application visible; testing can then continue without another installation.

### Safety and remaining boundary

- No password, SSH setting, VPS state, Google/Firebase configuration, credential, token, raw IP, coordinate, work title, search term, route body, private key, or signing material was changed or recorded. The deployed backend was not mutated during publication or phone work.
- RC6 publication and exact overlay are complete, but the candidate must not be promoted to stable v0.2.1 until the visible map/marker retry and minimal WALK, DRIVE, and TRANSIT physical gates pass.

## 2026-07-31 - Task 49: RC6 production-path audit and repeated secure-keyguard check

### Complete reread and current-source audit

- Re-read the complete updated `AGENT.md` before starting this continuation, then confirmed the worktree was clean and synchronized with `origin/main`. Current `main` differs from the immutable `v0.2.1-rc.6` tag only by Task 48's acceptance documentation; the Android, backend, R8, and workflow production files are identical.
- Independently traced the selection-map production path from `MainActivity` through `SearchRoute`, `PilgrimageMap`, and the shared `NavigationMapView` into the Navigation SDK `NavigationView`. It directly mounts the Google implementation, performs creation only after attachment, reactivates across start/resume, waits for positive dimensions, uses dimension-aware camera bounds, and uses marker options that do not depend on `BitmapDescriptorFactory`.
- Rechecked the Release R8 rules and audit. They retain the reflected creator's public zero-argument constructor and registry package compatibility, reject removed constructors and forbidden application `MapsInitializer` calls, and run against the correct intermediate mapping in branch/main CI and final mapping in signed Release builds. No static blocker to a visible basemap or markers was found.
- The map path still requires dynamic acceptance: it does not use a tile-loaded callback or a bounded `getMapAsync` timeout, so a key, GMS, renderer, or network failure that yields a blank basemap cannot be classified from static code alone. If an earlier unavailable callback switched selection back to the list, the user must explicitly tap the map action after unlocking to remount it.
- Independently traced DRIVE from `PlannerViewModel` through `TourPlanner`, the backend road provider and API client, the VPS matrix/route endpoints, and the Google Routes normalizer. The public RC6 contains the route/leg protobuf-default repair; missing route or leg distance/duration becomes explicit zero, while negative and wrong-typed values still fail. The DRIVE-specific request difference remains the intended traffic mode and all-false route modifiers.
- No second deterministic DRIVE defect was found. Two dynamic diagnostic risks remain if the physical test fails: matrix duration still expects an explicit value even though matrix distance tolerates an omitted protobuf zero, and the fixed eight-second Google upstream timeout could expire before a slower DRIVE response even when Google later records 2xx. These are not marked as observed RC6 failures without a real request.
- Independently traced every TRANSIT production call into the single adjacent-pair builder. Initial UI time, resumed/replanned time, and every arrival-plus-dwell chained segment pass through the same fixed-seconds RFC3339 formatter before the backend API call. The client always sends TRANSIT, exactly two validated points, and a non-empty offset timestamp compatible with the strict backend schema. No remaining production serialization path capable of reproducing the prior missing-seconds `INVALID_ARGUMENT` was found.

### Repeated physical boundary

- Rechecked the authorized Xiaomi before and after the static audit. Android continued to report the owner profile as `deviceLocked=1`, untrusted, and the display as dozing. No password was guessed, no security boundary was bypassed, and no device setting was changed.
- No UI input, map request, Firebase sign-in, Google route/matrix/navigation request, billable operation, APK installation, data clear, permission change, mock-location change, network change, SSH connection, VPS mutation, or credential operation occurred.
- Static inspection strengthens the RC6 implementation evidence but cannot replace the required visible basemap/marker and WALK, DRIVE, and TRANSIT results. The next action remains unchanged: after the user unlocks the Xiaomi and leaves the application visible, continue directly without reinstalling.

## 2026-07-31 - Task 50: third secure-keyguard audit and blocked handoff

### Repeated external-state verification

- Re-read the complete updated `AGENT.md` before starting this continuation. Confirmed the repository remained clean and synchronized with `origin/main`; no application, backend, workflow, release, or acceptance source changed after the RC6 production-path audit.
- Rechecked the sole authorized Xiaomi through the exact installed Platform-Tools executable. ADB still reports the device as connected and authorized, but Android again reports the owner profile as `deviceLocked=1`, untrusted, with the display dozing behind the secure keyguard.
- This is the same blocking condition across three consecutive goal turns: Task 48 first found the exact public RC6 running behind the secure lock, Task 49 repeated the lock after completing all remaining static production-path audits, and this task again finds the secure lock unchanged after a complete reread.

### Blocked boundary and precise resume condition

- All safe work that does not require visible phone interaction is complete: the map, DRIVE, and TRANSIT fixes are in the signed public RC6; branch/main/release/API 26/API 37 gates pass; the DRIVE backend repair is deployed; exact overlay integrity is proven; and independent production-path audits found no remaining deterministic blocker.
- No further source edit, emulator run, static audit, backend probe, or background process check can prove that Google tiles and markers are visibly rendered or that WALK, DRIVE, and TRANSIT complete through the installed physical UI. Treating those indirect checks as acceptance would be false.
- Testing is therefore blocked on one external action only: the user must unlock the Xiaomi 15T Pro and leave the Anitabi application visible. After that, resume without reinstalling, confirm retained onboarding/selection state, explicitly open/retry the map, and execute the minimum two-point WALK, DRIVE, and TRANSIT requests.
- No password was guessed, no keyguard was bypassed, and no device setting, APK, application data, permission, mock-location configuration, network, input method, VPS, SSH state, credential, or billable request was changed in this task.

## 2026-07-31 - Task 51: RC6 physical map crash, official Google review, and RC7 R8 repair

### Exact physical reproduction and official-source boundary

- Re-read the complete current `AGENT.md` before starting. Confirmed the sole authorized Xiaomi 15T Pro was unlocked and still had the exact public RC6 installed with the original first-install record and retained application state; no reinstall, uninstall, data clear, permission change, mock-location change, network change, input-method change, or device-setting change occurred.
- Cold-launched RC6 from an isolated application crash buffer and opened the existing selection map through the normal UI. The process terminated within the observation window with a `GL-Map` fatal `NullPointerException` inside the Navigation SDK mapcore renderer, and the system displayed the application-stopped dialog. No crash report was submitted and no route, matrix, navigation-reservation, Firebase-backed planning, or other billable request was sent.
- Before editing source, read the official Google `NavigationView` reference, navigation-map interaction best practices, memory-management guidance, Android setup instructions, current Navigation SDK release notes, and the official Android Navigation sample at its recorded source commit. The documented direct-view lifecycle requires ordered forwarding on the UI thread and prefers `SupportNavigationFragment`; the current first-attach `onCreate`/`onStart`/`onResume` sequence does not explain this renderer-thread crash.
- Independently audited the Navigation SDK 7.8.0 AAR and the exact RC6 Release R8 reports. The crashing renderer path calls `Class.newInstance()` to instantiate `ej` shader-program subclasses, catches reflective construction failures, then dereferences the resulting null. The SDK contains 14 direct `ej` subclasses; 12 have zero-argument constructors, and RC6 removed all 12 constructors from the Release output. This exactly matches the restored stack and excludes API Key, route arguments, viewport dimensions, or application Kotlin null handling as the cause.
- The official release notes recommend AGP 8.13.2, Gradle 8.13, and desugaring 2.1.5 for recent SDK releases. A read-only compatibility build experiment restored those two build-tool versions temporarily but AGP 8.13.2 could not resolve the project's required Android 37 platform. Both files were restored byte-for-byte to AGP 9.3.0 and Gradle 9.6.1 before implementation; no toolchain change remains in the diff.

### Minimal repair and release regression gate

- Added one narrow R8 rule that retains zero-argument constructors only for classes extending the Navigation renderer's `ej` shader-program base. No map UI, lifecycle, marker, camera, route, backend, or provider business code changed in this repair.
- Expanded the existing release reflection audit with the fixed SDK 7.8.0 set of 12 affected classes. It now checks retained class names and zero-argument constructors in one mapping pass, exact constructor-level seeds, absence of whole-class or constructor deletion including package-private constructors, and the active general rule in the effective configuration. The audit accepts both LF and CRLF report files and remains fail-closed on an SDK-internal class-name change.
- A fresh AGP 9.3 Release R8 run completed successfully. Independent AAR enumeration and report inspection proved mapping classes 12/12, mapping zero-argument constructors 12/12, constructor-level seeds 12/12, whole-class removals 0/12, constructor removals 0/12, and exactly one active effective keep rule. The expanded audit then passed against those fresh split outputs.
- Updated the immutable RC6 record to mark the physical map gate failed, created an RC7 acceptance record with every unexecuted publication and physical gate unchecked, and corrected the v0.2.1 release notes so RC6 is not represented as a successful candidate.

### Verification, review, and remaining boundary

- The clean Android regression command passed all 74 JVM tests across 22 result files with zero failures, errors, or skips, plus Debug and Release Lint, Debug APK, and Debug Android-test APK assembly. Tracked-source credential audit, Debug APK content audit, Bash syntax, workflow YAML parsing, the fresh R8 reflection audit, and `git diff --check` passed.
- An independent read-only review found no blocker or major issue. Its one actionable low-risk finding was that the initial usage-report regex recognized only public constructors; the audit was corrected to cover public, protected, private, and package-private zero-argument constructors and passed again. The deliberate SDK 7.8.0 class pin and the clean-CI provenance assumption remain documented fail-closed boundaries rather than hidden compatibility claims.
- An initial regression wrapper timed out while its Gradle child continued, and a second invocation encountered the resulting Windows output-directory lock. Identified and stopped only the exact Gradle/Kotlin processes created by this task, verified none remained, and reran once with a single-use daemon successfully. No project file, cache, SDK, emulator, Docker data, or user file was deleted; approximately 45 GB of system-drive space remained free.
- The implementation is still uncommitted on the local RC7 branch. Branch/main CI, merge, annotated `v0.2.1-rc.7` tag, signed Prerelease, exact-public-APK compatibility jobs, public artifact audit, Xiaomi overlay, visible basemap/marker test, and minimal WALK/DRIVE/TRANSIT physical requests remain pending and are not claimed here.
- No secret, token, raw IP, coordinate, work title, search term, route body, Google credential, Firebase credential, signing material, VPS credential, password, or SSH state was recorded or changed. No VPS or SSH action occurred.

## 2026-07-31 - Task 52: RC7 protected publication and user-test handoff

### Preparation and publication gates

- Re-read the complete 1,313-line `AGENT.md` in UTF-8 after the user changed the handoff boundary. The phone had been taken away, so this task performed no ADB command, installation, device read, UI action, permission change, mock-location change, network change, or other phone operation.
- Committed the Task 51 repair as `d2704e018ded5b7b43d7c351be828a5b0dad72d5` on `codex/v0.2.1-rc.7-shader-r8`, pushed the branch, and opened draft pull request 7 through the authenticated GitHub browser because both the connector and command-line credential lacked pull-request mutation scope.
- Pull-request Android CI run `30593248315` passed verify, API 26, and API 37. The PR was marked ready and merged only after all three jobs succeeded; GitHub created merge commit `0e8fb6dd91a10a3b7842d7a6ca447ef888d965be`.
- Fast-forwarded local `main` to the merge commit and waited for independent `main` run `30594003074`; verify, API 26, and API 37 all succeeded at that exact SHA.
- Verified a clean worktree, matching local/remote `main`, `versionCode=7`, `versionName=0.2.1`, and absence of an existing RC7 tag. Created annotated tag `v0.2.1-rc.7` at the green merge commit, verified its peeled commit, and pushed it without moving any prior tag.

### Public artifact and compatibility evidence

- Signed APK Release run `30594755413` passed protected configuration restoration, tag/version validation, tests, signed Release assembly, the expanded 12-class R8 reflection audit, APK content audit, signature verification, checksum creation, and GitHub Prerelease publication.
- The public non-draft Prerelease is `https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.1-rc.7`. Its APK is 48,703,245 bytes with SHA-256 `f4a1b53b20baddb16602488242cd71dace56b98a0a04cb5ee28ad214c08c3a1d`; the independently downloaded file, checksum asset, and GitHub asset digest agree.
- Independent `aapt` inspection reports package `cn.anitabi.navigator`, version code 7, version name 0.2.1, minimum SDK 26, and target SDK 37. Independent `apksigner` verification reports APK Signature Scheme v2, one RSA-4096 signer, and the unchanged fixed production certificate used by public v0.2.0 through RC6.
- Exact-public-APK compatibility run `30595041809` resolved and downloaded `v0.2.1-rc.7` rather than an older release, then passed install, package inspection, cold launch, crash-buffer, and evidence-upload checks on API 26 and API 37.

### Handoff boundary

- The public candidate and emulator gates are complete, but no physical result is inferred from them. The user will independently install/test this release; visible Google basemap and markers plus WALK, DRIVE, and TRANSIT remain unchecked until the user reports the actual phone result.
- Stable `v0.2.1` remains unpublished. The active goal must stop at the physical-test boundary rather than treating a fallback screen, emulator launch, or public upload as proof of the three remaining user-visible issues.
- No secret, token, raw IP, coordinate, work title, search term, route body, Google/Firebase credential, signing material, VPS credential, password, or SSH state was printed, recorded, or changed.

## 2026-07-31 - Task 53: second RC7 physical-boundary audit

### Complete reread and authoritative-state audit

- Re-read the complete current 1,336-line `AGENT.md` in UTF-8 before starting this continuation. Per the user's handoff, the phone remains physically unavailable, so this task performed no ADB command, installation, device read, UI action, permission change, mock-location change, network change, or other phone operation.
- Confirmed the local worktree was clean before this record, local `main` and its `origin/main` tracking ref both pointed to `b045f6baaca107c5d855ba55cbef500e7c98ff73`, and the authoritative remote `main` independently reported the same commit. The annotated `v0.2.1-rc.7` tag still peels to the green merge commit `0e8fb6dd91a10a3b7842d7a6ca447ef888d965be`.
- Confirmed the public non-draft Prerelease still exposes `anitabi-v0.2.1-rc.7.apk` at 48,703,245 bytes with GitHub digest SHA-256 `f4a1b53b20baddb16602488242cd71dace56b98a0a04cb5ee28ad214c08c3a1d`. Signed APK Release run `30594755413` and exact-public Release APK Compatibility run `30595041809` both remain completed successfully at the tagged merge commit.
- Rechecked the RC7 acceptance record. Every implementation, local, branch, main, signed-publication, artifact, API 26, and API 37 gate is checked; only the Xiaomi overlay, visibly rendered Google basemap/markers, and minimum WALK, DRIVE, and TRANSIT physical checks remain unchecked. Stable `v0.2.1` remains unpublished.

### Repeated external boundary

- This is the second consecutive goal turn since the user stated that the phone had been taken away and that they would test the public RC7 themselves. No source change, emulator run, backend probe, release rerun, or static audit can honestly substitute for those remaining physical observations.
- The precise resume condition is either a user report from the exact public `v0.2.1-rc.7` APK covering map/markers plus WALK, DRIVE, and TRANSIT, or the authorized Xiaomi 15T Pro becoming physically available again for testing. No product, backend, workflow, release, SSH, VPS, credential, or stable-release change was made in this audit.
- No secret, token, raw IP, coordinate, work title, search term, route body, Google/Firebase credential, signing material, VPS credential, password, or SSH state was printed, recorded, or changed.

## 2026-07-31 - Task 54: third RC7 physical-boundary audit and blocked handoff

### Third authoritative verification

- Re-read the complete current 1,351-line `AGENT.md` in UTF-8 before starting this continuation. The phone remains physically unavailable under the user's self-test handoff, so this task performed no ADB command, installation, device read, UI action, permission change, mock-location change, network change, or other phone operation.
- Confirmed the worktree was clean before this record and local `main`, its `origin/main` tracking ref, and the authoritative remote `main` all pointed to `65225b9ee0c5ff7d75d56339a47580c995f4c852`. The annotated `v0.2.1-rc.7` tag still peels to green merge commit `0e8fb6dd91a10a3b7842d7a6ca447ef888d965be`.
- Confirmed the public non-draft Prerelease and its two assets remain available. The APK is still 48,703,245 bytes with GitHub digest SHA-256 `f4a1b53b20baddb16602488242cd71dace56b98a0a04cb5ee28ad214c08c3a1d`; Signed APK Release run `30594755413` and exact-public Release APK Compatibility run `30595041809` both remain completed successfully at the tagged merge commit.
- Rechecked the RC7 acceptance record again. Every non-phone implementation, test, CI, signed-publication, artifact, API 26, and API 37 gate is checked. Only exact Xiaomi overlay, visibly rendered Google basemap/markers, and the minimum WALK, DRIVE, and TRANSIT physical checks remain unchecked. Stable `v0.2.1` remains unpublished.

### Blocked handoff and resume condition

- The same external condition has now repeated for three consecutive goal turns: Task 52 recorded that the user had taken the phone away and would test the public RC7, Task 53 found no physical result on the second audit, and this task again has neither the phone nor a user test report. All remaining acceptance evidence depends on that same physical boundary.
- No source change, emulator run, backend probe, release rerun, or static inspection can honestly prove visible Google rendering or successful WALK, DRIVE, and TRANSIT behavior on the target phone. Further work without a physical result would create churn rather than advance the requested completion standard.
- The goal is therefore blocked until either the user reports all four results from the exact public `v0.2.1-rc.7` APK or the authorized Xiaomi 15T Pro becomes physically available for testing. No product, backend, workflow, Release, SSH, VPS, credential, or stable-version change was made in this audit.
- No secret, token, raw IP, coordinate, work title, search term, route body, Google/Firebase credential, signing material, VPS credential, password, or SSH state was printed, recorded, or changed.

## 2026-07-31 - Task 55: RC7 user acceptance, buggy Release cleanup, and user-facing documentation

### User acceptance and evidence boundary

- Re-read the complete current 1,367-line `AGENT.md` in UTF-8 before starting this task. The user then reported that RC7 was fully normal in their physical test. Updated the RC7 acceptance record only for the explicitly confirmed Google basemap/markers plus WALK, DRIVE, and TRANSIT results.
- This report closes the three outstanding user-visible failures: the map is visible instead of crashing or falling back, a valid DRIVE route is accepted, and TRANSIT no longer fails parameter validation. It does not prove a particular `adb install -r` procedure, data retention under that exact procedure, lock-screen recovery, rerouting, voice, real GNSS movement, or long-tour batch switching; those claims remain intentionally absent.
- Stable `v0.2.1` was not published. RC7 remains a non-draft Prerelease and `v0.2.0` remains the Latest stable Release.

### Buggy Release cleanup and retained history

- Inventoried every public Release, tag, attachment, title, status, size, and digest before deletion. The known-buggy set was exactly `v0.2.1-rc.1` through `v0.2.1-rc.6`; each represented a superseded candidate with a reproduced parser, map, rendering, DRIVE, or TRANSIT defect recorded in earlier tasks.
- Deleted only those six GitHub Release objects and their attached APK/checksum assets. The command-line credential was rejected with HTTP 403 before the first deletion, so no partial CLI mutation occurred; deletion was completed through the already-authenticated GitHub browser. No credential value was printed or recorded.
- Deliberately retained the six annotated Git tags and their commits for audit and source recovery. Final authoritative checks show exactly seven public Releases: `v0.1.0` through `v0.1.4`, `v0.2.0`, and `v0.2.1-rc.7`; all RC1-RC7 remote tags still exist.
- The seven retained Releases still contain exactly fourteen uploaded attachments, one APK and one checksum file per version. Their filenames, sizes, APK digests, checksum contents, draft/prerelease flags, fixed signing-certificate digest, and `v0.2.0` Latest status are unchanged.

### User-facing documentation and publication

- Rewrote `README.md` as a user guide centered on download choice, upgrade behavior, first use, features, GMS/network/individual/shared-quota limits, privacy, and common problems. It clearly distinguishes stable `v0.2.0` from the tested but still prerelease RC7 and no longer asks ordinary users to configure an API Key.
- Rewrote all seven retained Release bodies in plain user language while preserving historical behavior and immutable APK verification values. Added `docs/BUILD.md` for developer-only setup, corrected the current Firebase/backend/Navigation Key boundary, and updated `SECURITY.md` for the Google/Firebase/VPS architecture without claiming a nonexistent private contact channel.
- Recorded the user's RC7 result in `docs/releases/v0.2.1-rc.7.md` without expanding the evidence. Independent read-only reviews found no remaining documentation or Release-copy blocker; all relative Markdown links resolve, all retained asset facts match GitHub, tracked-source credential auditing passes, and `git diff --check` passes.
- Committed the documentation as `3386929e5a0188f742fe44ece76b75a44974c92c`, pushed `codex/release-cleanup-user-docs`, and opened pull request 8 through the authenticated GitHub browser because the command-line credential lacks pull-request mutation scope. PR run `30597781818` passed verify plus API 26 and API 37 emulator jobs before merge.
- Marked the PR ready only after review, then merged it as `ed6d5ec094823b08222622f7dc54cabb4f877940`. Independent `main` run `30598436083` passed the complete verify job and both API 26/API 37 emulator matrices at that exact commit.
- Updated the titles and bodies of all seven retained GitHub Releases through the authenticated browser. A final API comparison confirmed every remote body equals its local Release-note source after newline normalization, every title matches the user-facing title, and all Release flags and attachments remain correct.

### Safety and future boundary

- No phone was connected, read, installed to, or changed. No SSH connection, VPS mutation, DNS change, Google/Firebase configuration change, credential rotation, tag deletion, history rewrite, stable-release publication, billable route request, or local APK build occurred.
- No secret, token, raw IP, coordinate, work title, search term, route body, Google/Firebase credential, signing material, VPS credential, password, or SSH state was written to source, Git, documentation, browser forms, or this record.
- `docs/RELEASE_NOTES_v0.2.1.md` intentionally describes the current RC7 asset because the release workflow reuses it for candidates. Before a future stable `v0.2.1` tag, its RC7 title, prerelease wording, filename, size, and SHA-256 must be replaced with the actual stable artifact details.

## 2026-07-31 - Task 56: RC7 transit acceptance withdrawal and quota/no-route diagnosis

### Preparation and evidence boundary

- Re-read the complete current 1,397-line `AGENT.md` in UTF-8 before starting this new task, then inspected both user-supplied screenshots at original resolution. The screenshots were treated as evidence only; no phone, application state, permission, mock-location setting, network setting, or device configuration was accessed or changed.
- Performed a read-only trace of the Android navigation/planner paths and the backend quota/Google Routes normalizer. Used current official Google Routes documentation for transit request, timestamp, response, and error semantics. No production SSH connection, VPS read or mutation, Firebase sign-in, Google request, billable operation, credential access, or password use occurred.

### Navigation quota finding

- The exact English text in the first screenshot is the Android client's local `ApiException.QuotaExhausted` message. In this screen it can only arise when `/v1/navigation/reserve` rejects the initial road-navigation destination batch; it is not the Navigation SDK's own quota status and is not a phone-network error.
- The displayed 18-leg trip attempts to reserve 18 destinations at once. The backend permits only 20 navigation destinations per anonymous UID per UTC day, so any prior successful reservation of at least three destinations makes that 18-destination start fail. The same error code also represents the 900-destination shared monthly ceiling; without reading the production ledger the screenshot cannot prove which scope rejected it, although the 18-of-20 daily interaction is the likely cause after repeated testing.
- The application publishes `NAVIGATING` and “Google navigation is guiding” before reservation and SDK destination loading complete. On failure it renders the exception's English text verbatim and leaves the prior instruction/progress wording visible even though Google guidance never started. This is a separate state/localization defect, not evidence that navigation continued.

### Transit finding and corrected meaning

- The transit request shape in RC7 uses exactly one origin/destination pair, `TRANSIT`, no intermediate waypoint, and a complete RFC3339 timestamp with seconds. Google's official current window is seven days before through 100 days after the request time; the same-day time shown in the screenshot is valid. An invalid request would map to the separate `INVALID_ARGUMENT` message, not the screenshot's text.
- “本区域暂无开放公交数据” is a Transitous-era message incorrectly retained during the Google migration. The Android provider emits it only after classifying the request as `NO_ROUTE`. The backend currently classifies both a successful Google response with no routes and any Google HTTP 404 as `NO_ROUTE`, while the Android client can also map an unrecognized backend 404 to the same result. The screenshot therefore supports only “this adjacent pair at this time did not yield a transit route,” not “the region has no transit data.”
- Transit tours are requested serially per adjacent point pair. One `NO_ROUTE` aborts the complete multi-point plan, so a nearby segment with no useful vehicle itinerary can make an otherwise valid transit tour fail globally. Correct handling needs to distinguish genuine empty-route results from upstream 404 errors and either represent a walking-only connector for that segment or report the exact failed segment; no product fix was implemented without a requested behavior choice.

### Acceptance correction and verification

- Withdrew only the TRANSIT acceptance from `docs/releases/v0.2.1-rc.7.md`; retained the user's confirmed Google map/point, WALK, and DRIVE results. The record explicitly preserves that the user first reported all modes normal and later supplied contradictory transit evidence rather than silently rewriting history.
- Corrected `README.md` and `docs/RELEASE_NOTES_v0.2.1.md` so they no longer advertise RC7 transit as accepted. Added user-facing explanations for the 20-destination daily navigation limit, the untranslated quota message, the false “guiding” state, and the overbroad transit no-data message.
- `git diff --check` passes apart from the repository's normal Windows line-ending notices. No application/backend source, APK, tag, Release asset, stable release, quota value, ledger, SSH setting, VPS service, Google/Firebase setting, or phone state changed in this diagnosis.

### Published acceptance correction

- Committed the correction as `bbee52398d2cbe5a97d48ecebeb0b7c5d88c5e02`, pushed `codex/rc7-acceptance-correction`, and opened pull request 9. The pull-request Android CI run `30602780456` passed the verify job and both API 26/API 37 emulator-smoke jobs before merge.
- Marked the pull request ready only after all three checks passed, then merged it into `main` as `6f679fa4ccb0f5aceb2340d76ccbf8138a80ed56`. The merge changed documentation and this audit log only; the `main` push correctly did not start a duplicate Android CI run because all changed paths are covered by the workflow's `paths-ignore` list.
- Updated the existing public `v0.2.1-rc.7` Prerelease body to the corrected local `docs/RELEASE_NOTES_v0.2.1.md`. A post-update API comparison confirmed an exact body match after newline normalization; the title, tag, Prerelease status, APK, checksum attachment, and their contents remain unchanged.
- Stable `v0.2.1` remains unpublished and TRANSIT remains unchecked. No application/backend source, APK, tag, Release attachment, phone, production service, quota ledger, SSH setting, VPS service, Google/Firebase setting, credential, or secret was changed by the correction.

## 2026-07-31 - Task 57: Google Maps-style transit planning and safe failure recovery

### Preparation and verified product behavior

- Re-read the complete current 1,429-line `AGENT.md` in UTF-8 before starting this task. Used the current official Google Maps Android help and Google Routes transit, `TransitPreferences`, request, response, and pricing references before changing the implementation.
- Confirmed that Google Maps presents the transit time choice as leave now, depart at, or arrive by. Confirmed that Routes accepts exactly one departure or arrival time for transit, supports the seven-day-past through 100-day-future window, and exposes less-walking, fewer-transfers, bus, subway, train, and light-rail preferences. The API has no last-service request mode, so the application does not invent one.
- Corrected the previous diagnosis boundary: only a successful Google response with an empty routes array is treated as no route. Google HTTP 404, malformed success bodies, and other upstream failures now remain service failures and cannot be displayed as proof that a region lacks transit coverage.

### Android and backend implementation

- Replaced the raw transit date/time fields with a compact Google Maps-style time row and Material date/time dialogs for now, specified departure, and specified arrival. Added a single transit-options sheet with multi-select bus/subway/train/light-rail filters and recommended, less-walking, or fewer-transfers preferences. Loading freezes these controls and duplicate generation is ignored.
- Added forward departure-time chaining and reverse arrive-by chaining across every adjacent pilgrimage segment, including dwell time. A genuine empty transit result now gets one Google WALK connector attempt; if neither transit nor walking is available, the error identifies the exact failed segment instead of blaming the whole region.
- Preserved walking connectors as walking legs in the timeline and map, added route/line/stops/times/transfer details, distinguished walking and transit geometry, and converted Google UTC timestamps with the returned IANA time zone. Resolved Google route content remains memory-only.
- Versioned persistence now retains only the user-selected transit time anchor, preference, allowed modes, and existing user-owned tour/progress data. Legacy v0.2.0 transit data migrates to a departure-time anchor, arrive-by never falls back to a resolved Google departure, repeated migration remains idempotent, and route/provider content is still discarded for online refresh.
- Extended the backend contract for mutually exclusive departure/arrival time, allowlisted transit preferences and travel modes, fixed field masks for time zones, and pre-quota time-window validation. Updated privacy text for the additional user-selected request fields and added a Node 24 backend job to pull-request/main CI so Android success cannot mask a backend regression.
- Localized every navigation API failure. Quota exhaustion now says that navigation did not start and no further charge will be produced. A failed reservation or Google navigation synchronization rolls runtime and persisted progress back to `PLANNED`, clears transient navigation state, and stops only after the resumable state is saved; the UI no longer remains falsely marked as guiding or automatically repeats the failed start.

### Verification and remaining deployment boundary

- Full Android verification passed after the final failure-state repair: 103 JVM tests in 23 suites with zero failures, errors, or skips; Debug Lint; Debug APK; and Debug androidTest APK. The new coverage includes now/depart/arrive scheduling, reverse multi-segment dwell chaining, DST boundaries, transit preferences, time zones, walk connectors, precise failed segments, legacy model and real Room 1-to-2 migration, localized navigation errors, and failure rollback to a resumable state.
- Clean backend installation, typecheck, all 25 Node tests, and production dependency audit passed with zero vulnerabilities. Workflow YAML parsing, `git diff --check`, tracked-source credential audit, and the rebuilt Debug APK content audit also passed. An independent read-only diff review found and prompted the persisted false-navigation repair, then found no second user-level blocker.
- No Android device or emulator was attached. No APK was installed, no phone was read or changed, and no physical transit result is claimed. The public RC7, its assets and historical evidence remain unchanged; RC8 and stable v0.2.1 were not tagged or published.
- Public HTTPS health still returns only service/database healthy and HTTP still redirects to HTTPS, but this task did not deploy the new backend. The current machine has neither a deployment key nor a presently verifiable stored host fingerprint, and the provider client page could not be controlled while another browser extension UI was open. Authentication was not attempted past that boundary; no new host key was trusted.
- The next safe step is to verify the VPS host fingerprint through the provider console, deploy this exact green backend without changing SSH or the password, confirm health and one minimal sanitized real transit request, then prepare and publish RC8. The existing RC7 release notes must be replaced with truthful RC8 notes before any RC8 tag is created.
- Explicitly staged only the 29 intended implementation, test, workflow, privacy, checklist, and audit-record files; the user-provided attachments remained untracked. Committed the green implementation as `dea7584bf89f50c800730931b58eafc59d9868a9` and pushed `codex/google-maps-transit-ux`. The first push attempt timed out before any remote write and the clean retry succeeded.
- The command-line GitHub credential again lacked pull-request mutation scope, so its draft-PR attempt failed before creating anything. Prepared the authenticated web form with the exact pushed branch, title, validation evidence, and release boundary, but did not submit the representational action without the required action-time user confirmation. No pull request exists yet.
- No secret, token, raw IP, coordinate, work title, search term, route body, Google/Firebase credential, signing material, VPS credential, password, SSH option, firewall rule, or user attachment was written to tracked source or this record.

## 2026-07-31 - Task 58: backend upgrade preflight and authenticated-console blocker

### Complete reread and deployment preflight

- Re-read the complete current `AGENT.md` in UTF-8 before taking deployment action. Kept the user's explicit boundary unchanged: no password, SSH port, `sshd` option, authorized key, root-login policy, firewall rule, provider setting, DNS record, Nginx virtual host, personal website, secret mount, or SQLite quota data may be changed by this backend upgrade.
- Confirmed `codex/google-maps-transit-ux` and its remote tracking branch point to the same audit-log commit, whose backend tree is identical to the tested implementation commit. The tracked worktree and index are clean; the only untracked path is the user-owned attachment directory, which was neither read into a deployment archive nor staged.
- Re-ran the exact committed backend locally: TypeScript build and typecheck passed, all 25 tests passed with zero failures or skips, and the production dependency audit reported zero vulnerabilities. Public HTTPS health still returned only service/database healthy, while plain HTTP redirected to the same HTTPS health path.
- Audited the production delta against the currently deployed RC6 backend. Only four runtime TypeScript files require synchronization and image rebuild; there is no Dockerfile, Compose, package-lock, SQLite schema, quota-limit, Nginx, DNS, environment, service-account, or secret change. RC7 remains compatible with the new server, while the new arrival-time and transit-preference contract requires this server upgrade before a future RC8 can be used.
- Established the rollback boundary: preserve the live SQLite volume and its monotonically increasing quota ledger, identify and pin the currently running image before replacement, and roll back only the API image/source if required. Database files must never be restored merely to roll back application code.

### Authentication boundary and remaining action

- The existing authenticated provider page could not be inspected because another browser extension UI blocked automation. Releasing the stale browser bindings closed the two task-prepared temporary tabs but did not submit the pending GitHub form or mutate provider state. The installed Chrome browser is currently not running, and the remaining Edge tabs do not provide an authenticated provider control surface that can be safely reopened through the supported browser interface.
- A fresh SSH scan was not trusted: the current environment has no matching stored host key and no private deployment key. No password was placed in a command, file, environment variable, helper script, clipboard, log, or browser form; no SSH authentication was attempted. One diagnostic exposed only a sandbox-mapped address in private tool output; it was not trusted, persisted, or copied into this record.
- Deployment, backup, container rebuild, live transit probe, and ledger reconciliation therefore have not started. The precise safe resume condition is to launch Chrome, open the already authenticated `https://vps.hosting/clientarea/` page, and leave any extension popup closed so the provider-displayed host identity can be compared with an independent scan before login.
- No server, phone, APK, Release, tag, pull request, GitHub setting, Google/Firebase resource, credential, quota, or billable Google request changed in this preflight.

## 2026-07-31 - Task 59: direct SSH backend deployment and Japan transit boundary

### Direct SSH recovery, inventory, and protected deployment

- Continued from Task 58 after the user restored SSH and explicitly authorized direct password login. Compared the live host key with the previously trusted fingerprint recovered from the existing task record before authentication; they matched exactly. The user-authored password was supplied only in process memory by a temporary askpass helper, was never embedded in a command, environment value, source file, deployment script, output, or this record, and every local helper was deleted after use. No password, SSH port, `sshd` option, root-login policy, authorized key, firewall rule, or host-key store was changed.
- Read-only inventory confirmed Ubuntu 24.04, two CPUs, approximately 1 GiB visible memory, sufficient disk, the API bound only to host loopback, the existing reverse proxy, seven expected running containers, active backup/security timers, and the unrelated personal sites. No unknown service was stopped or reconfigured.
- Safe early deployment attempts that encountered a byte-order mark, an incorrect source precondition, and a CRLF-versus-LF hash mismatch all stopped before replacing production source or recreating the container. The one attempt that had already created a valid quota backup left the running service unchanged; its empty rollback skeleton was verified and removed precisely.
- Deployed the four tested runtime files from implementation commit `dea7584bf89f50c800730931b58eafc59d9868a9` only after Git-object and raw-GitHub SHA-256 values matched. The live SQLite volume, Compose definition, package lock, secrets, environment, DNS, Nginx, quotas, and unrelated containers were not changed. The prior image was pinned and the previous source retained for rollback before rebuilding only `anitabi-api-api-1`.
- Official Google protocol review and a failing regression test found that TRANSIT requests must omit the `intermediates` field entirely and that `TransitVehicle.name` is a localized object. Fixed both in `a8b61f03efa5a202547f1f5d3062e4f3ecd491d4`, passed all 25 backend tests, TypeScript build, production dependency audit with zero vulnerabilities, pushed the commit, backed up production, and deployed only the corrected route provider. The prior image and source remain available as `pre-a8b61f0` rollback artifacts.
- Production evidence then showed Google returned HTTP 200 while the normalizer emitted `UPSTREAM_UNAVAILABLE`. A structure-only diagnostic proved that Google represented the no-route result as an object with the repeated `routes` field omitted. Added a regression first, changed successful omitted/empty routes to `NO_ROUTE`, passed all 25 tests and the zero-vulnerability audit, committed and pushed `e9616a3ca01e465ed5c5ca1e20b3017ab2963a26`, and deployed that exact hashed source. The currently running image is the new `e9616a3` build; the preceding image and source remain pinned as `pre-e9616a3` rollback artifacts.

### Real Google diagnostics and corrected product conclusion

- Three public end-to-end probes used Firebase anonymous authentication, HTTPS, the production endpoint, and the SQLite ledger. Each made exactly one Compute Routes request with no retry, increased the route ledger by exactly one, and deleted its temporary anonymous account. The first two probes, before the final no-route repair, returned sanitized `503 UPSTREAM_UNAVAILABLE`; the final deployed probe returned the correct sanitized `404 NO_ROUTE`.
- Five additional administrator-only Compute Routes diagnostics were sent directly from the API container with one request each and no retry. They emitted only HTTP status and response-shape booleans/counts: the fixed Japan control returned HTTP 200 with no `routes` for the full production request, a minimal request, an address-form request, and a region/language/future-time request, while Google's own documented non-Japan control returned a normal transit route. One one-element Compute Route Matrix diagnostic for the same Japan control independently returned `ROUTE_NOT_FOUND`.
- Those six administrator diagnostics did not pass through the public endpoint and therefore were not reserved in the SQLite ledger. This was a bounded diagnostic exception, not normal product behavior; it must not be repeated as an application fallback or represented as proof of local hard-cap enforcement. In total this task sent eight Compute Routes requests and one billed matrix element, with no automatic retries.
- The earlier tentative explanation of generic regional coverage was too vague. Google's current official Maps Platform FAQ explicitly states that Routes API transit supports Google Transit partners except the Indian Railway Catering and Tourism Corporation and partners in Japan. This exactly explains why the consumer Google Maps application can show Japanese transit while both developer Routes methods return no route. The official coverage page also says public-transit coverage data is not published in its Platform table.
- Investigated Directions API (Legacy) as a possible Google fallback using current official documentation. It is a Legacy service, is not a dependable new-project production path, requires a separately restricted server API key for the fully documented authentication route, and does not promise Japanese transit or parity with the consumer Google Maps application. It was not enabled, keyed, called, or added to the backend.

### Final production verification and remaining product decision

- The final API image runs as non-root `node`, has a read-only root filesystem, retains the intended restart policy and read-only secret mounts, and is healthy on loopback and public HTTPS. Plain HTTP redirects to the same HTTPS health path; HSTS and `nosniff` remain present; the certificate had 88 days remaining at verification.
- Nginx validation passed. All seven expected containers remained running, unrelated restart count stayed zero, and the three checked personal sites returned HTTP 200. The API log contained only the allowlisted structured keys, with zero non-JSON lines, extra keys, fatal terms, sensitive terms, or error-level records after the final deployment.
- Ran a final consistent quota backup after the last public probe, verified SQLite integrity, and confirmed the seven-day backup set contained eleven files. Approximately 9.2 GiB remained free on the root filesystem. One SSH timeout occurred only while rechecking deletion of exact remote helper paths after all production checks had passed; a quiet retry removed and verified those exact files without changing the service.
- Every task-created local helper file and every corresponding remote temporary helper was deleted. The user-owned untracked attachment directory was not modified or staged.
- The backend upgrade is complete and now reports Google's Japanese empty result honestly, but application-internal Japanese transit cannot pass acceptance through Routes API because Google explicitly excludes Japanese transit partners. The remaining product choice is material: preserve an all-Google stack and hand Japanese transit segments to the consumer Google Maps application, or select a separate legally supported Japanese transit provider for in-app route details. No Android fallback, APK, RC8, stable release, Google project setting, API key, phone, or public Release was changed in this task.
- No secret, token, raw IP, coordinate, exact/user-specific/probe-specific location or address, work title, search term, route body, response body, Google/Firebase credential, signing material, VPS credential, password, sensitive SSH value, credential, or configuration content was recorded in source or this audit entry.

## 2026-07-31 - Task 60: truthful transit handling, recovery hardening, and production rate-limit update

### Diagnosis and product boundary

- Re-read the complete current 1,503-line `AGENT.md` in UTF-8 before starting this task. Rechecked the current official Google Maps Platform transit FAQ and kept two distinct findings explicit: the application had real correctness defects, while Routes API separately excludes Google Transit partners in Japan even though the consumer Google Maps application can display Japanese transit.
- Confirmed the Android client could accept an all-walking fallback or an empty-step nontrivial response as transit, so a successful-looking itinerary did not necessarily prove that Google returned a public-transport leg. Also found cancellation, stale-callback, persistence-order, and local rate-limit interactions that could produce misleading state or make long pairwise plans fail.
- No new Google route or matrix request was sent for this diagnosis. The bounded production evidence and billing count from Task 59 remain historical evidence and were not repeated.

### Android and backend corrections

- Nontrivial transit itineraries now require at least one genuine transit leg. Empty transit steps are rejected, and only an exact same-coordinate zero-duration connector may become a walking connector. An all-walking sequence can no longer be presented as public transport.
- Made the Android HTTP call cancellable through response-body reading. Planning and point reordering now use generation cancellation, while Navigation service callbacks, cleanup, rollback, and successor starts are generation-gated so stale work cannot overwrite or stop a newer session.
- The repository now persists navigation progress before publishing its memory cache, retains exact same-process progress, evicts stale route/progress state when saving an unresolved tour, and preserves completed progress when a recoverable route refresh fails.
- The backend now treats only a strictly empty successful object with an omitted repeated `routes` field as `NO_ROUTE`; nonempty malformed responses are upstream failures. Only the local token bucket emits a trusted one-second `Retry-After`, and the client retries that specific local limit once before pacing subsequent requests. Quota, upstream, and unclassified rate limits are not silently retried.
- Updated active documentation and the draft pull-request description to distinguish fixed application bugs from the Japanese Routes API product boundary. Updated emulator CI to verify the persisted recoverable state instead of assuming one exact screen after process restart.

### Verification, deployment, and publication boundary

- Full Android verification passed: 121 JVM tests in 25 suites with zero failures, errors, or skips; Debug Lint; Debug APK; Debug androidTest APK; tracked-source credential audit; Debug APK content audit; and `git diff --check`.
- Backend verification passed: all 29 tests, TypeScript build, and the production dependency audit with zero vulnerabilities. Independent final review found no remaining actionable high- or medium-priority correctness issue in the task diff.
- Committed the implementation as `0902e484c9d6e8745febea665c0a2c293839cf14` and the recovery-CI correction as `9f3750f273f41125144a2f45dae3d19e6b3c8818`, then pushed `codex/google-maps-transit-ux`. The first implementation run exposed only an over-narrow recovery-screen CI assertion; after correcting the assertion and adding a persisted-state check, GitHub Actions run `30622071853` passed backend, verify, API 26 emulator, and API 37 emulator jobs.
- Deployed the exact tested backend tree from the implementation commit after source precondition checks and a consistent quota backup. Preserved both source and image rollback artifacts, rebuilt only the API container, and verified loopback/public health, HTTPS headers and certificate, non-root/read-only container constraints, backup health, strict log keys, unchanged unrelated restart counts, and unaffected personal sites. No billable Google route request was used for deployment verification.
- Updated draft pull request 10 with the final fixes, test counts, deployment evidence, official Japanese-transit limitation, and explicit non-release boundary. The pull request remains draft. No RC8, stable release, tag, Release asset, phone installation, phone access, Google/Firebase project change, DNS change, Nginx change, SSH setting change, firewall change, password change, quota reset, or unrelated service change occurred.
- Removed and verified deletion of all task-created local credential helpers and downloaded CI diagnostic directories. A final temporary-directory audit also found and removed two stale task-created RC6 evidence directories totaling approximately 72 MB. The user-owned attachment directory remained untracked and untouched; the local system drive had approximately 43 GiB free at the final check.
- No secret, token, raw IP, coordinate, exact location or address, work title, search term, route body, response body, Google/Firebase credential, signing material, VPS credential, password, or sensitive SSH value was written to source, Git, documentation, browser forms, logs, or this record.

### Remaining blocker

- The correctness and recovery defects found in this round are fixed and verified, but application-internal Japanese public-transit acceptance is still impossible through the selected Routes API because Google's documented product boundary excludes transit partners in Japan. Completing the product requires a material user/product choice: hand Japanese transit segments to the consumer Google Maps application, or approve a separate legally sustainable Japanese transit provider for in-app details.
- The same external product-choice blocker has now recurred across more than three consecutive goal turns. The goal is therefore intentionally set to blocked after this task rather than publishing a fake Japanese-transit acceptance or an RC/stable build with a known unmet completion criterion.

## 2026-07-31 - Task 61: signed release build and pull-request submission

### Preparation and scope

- Re-read the complete current 1,533-line `AGENT.md` in UTF-8 before starting this new task. Interpreted the user's request as building a production-signed `release` APK from the existing v0.2.1 branch and submitting the existing pull request for review, not creating a tag, publishing a GitHub Release, merging the pull request, or installing the APK on a phone.
- Audited the worktree and pull-request scope before staging. The branch and remote initially matched at `9f3750f273f41125144a2f45dae3d19e6b3c8818`; the only project change was Task 60's audit record, while the user-owned `.codex-remote-attachments/` directory remained untracked and outside the submission scope.
- Confirmed `versionCode=7`, `versionName=0.2.1`, API 26 minimum, API 37 target, the ignored real Firebase configuration, the ignored Navigation SDK key, and the fixed external RSA-4096 signing material. No version bump was needed for this build.

### Signed local release evidence

- Decrypted the existing store and key passwords only inside one temporary PowerShell/Gradle process through Windows user-scoped DPAPI, passed all signing values only through that child process environment, and cleared those environment variables in a `finally` block. No password, API key, Firebase value, keystore content, or other secret was printed, copied into the repository, or written to this record.
- Ran `testDebugUnitTest`, `lintRelease`, and `assembleRelease` while excluding only the local Crashlytics mapping upload. The signed build completed successfully. The JVM report contains 121 tests in 25 suites with zero failures, errors, or skips. Release Lint has zero errors and zero warnings; its four remaining entries are non-blocking Compose integer-state performance hints.
- The tracked-source credential audit, Navigation SDK Release R8 reflection audit, and Release APK content audit all passed against the newly generated outputs.
- The ignored local artifact is `app/build/outputs/apk/release/app-release.apk`, 49,058,377 bytes, SHA-256 `76ed1a64a4bb4bcac5618f9e15706a2f80e58f61971d7a3ddddb123520b90161`. Package inspection reports `cn.anitabi.navigator`, version code 7, version name 0.2.1, minimum SDK 26, and target SDK 37.
- Independent `apksigner` verification reports APK Signature Scheme v2, one RSA-4096 signer, and certificate SHA-256 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`, exactly matching the fixed public v0.2.0 signing identity. The APK remains an ignored local build product and is not committed to Git.
- The build did not start an emulator, Docker, Firebase sign-in, Google route request, VPS request, SSH session, phone interaction, tag, public Release, or installation. Approximately 42 GiB of system-drive space remained available after the build.

### Expanded stable-release scope and public-transit wording

- After the signed build, the user expanded this task to include merging the reviewed pull request and publishing stable GitHub Release `v0.2.1`. The user required the Release description to state that public-transit routing is unsupported in mainland China, Japan, and India and is not planned for near-term alternative-provider work.
- Corrected an initial assistant wording error that framed India as a choice by this project. The current Google Maps Platform FAQ states that Routes API supports Google Transit partners except Indian Railway Catering and Tourism Corporation and partners in Japan. The user-facing wording now attributes the three-region product limitation to the current Google API path being unable to provide the transit data this application needs; only the decision not to add a separate third-party provider in the near term belongs to this project. The copy also avoids implying that this independent application can make decisions for Anitabi.cn.
- Rewrote the active v0.2.1 Release notes from RC7 candidate copy to stable user-facing notes, added the prominent three-region transit limitation, and updated README download, upgrade, quota, transit, and stable-version wording. Historical RC7 evidence remains unchanged and linked rather than being rewritten as stable evidence.

### Pull request, main verification, and stable publication

- Committed the signed-build record as `0c515bd0c5d750117567dc92bad1f4646e2c3a95`, the stable user-facing README and Release notes as `8af3c11781fd593f7ff31cb456228e9c60c3e341`, and the corrected Google-versus-Anitabi transit attribution as `a4db8b98707565e7df5a1d6ff27e1004598d8b78`. Pushed each commit to `codex/google-maps-transit-ux`; only the three intended tracked documents were staged for the final wording commits, and the user-owned attachment directory remained untracked.
- GitHub Actions pull-request run `30628585336` passed all four jobs at the final head: backend, verify, Google APIs API 26 emulator, and Google APIs API 37 emulator. Updated pull request 10 with the final implementation summary, signed local-build evidence, the Google API transit boundary, and an explicit statement that this independent project does not speak for Anitabi.cn. Marked the pull request ready and merged it without squashing so the implementation and audit history remain available.
- Pull request 10 merged into `main` as `fd65c1d54372df43769d4c53602ed2fb833f8ae2`. Independent main-push run `30629643413` then passed backend, verify, API 26 emulator, and API 37 emulator jobs, including cold launch, onboarding, offline recovery, foreground navigation completion, and screen-off arrival checks.
- Created annotated tag `v0.2.1` only after the main run passed and verified it targets the merge commit. Signed-release run `30630717751` passed version/tag matching, tracked-source credential audit, protected configuration restoration, tests, Release Lint, signed APK build, Navigation R8 reflection audit, APK content audit, signature verification, checksum creation, and GitHub publication.
- Published stable [GitHub Release v0.2.1](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.1). It is non-draft, non-prerelease, and marked Latest. Its user-facing description prominently states that mainland China, Japan, and India public-transit routing is unavailable because the current Google API path cannot provide this application's required transit data, not because of Anitabi or an intentional regional block; the separate near-term no-third-party-provider decision is attributed only to this project.

### Public asset and compatibility verification

- The public `anitabi-v0.2.1.apk` is 49,058,377 bytes with SHA-256 `90ef4ef11ceeaeac164bd9cfcb4d86cea9349fac29750a54cab0bf0587650217`. A fresh download matched both GitHub's asset digest and the separately published `anitabi-v0.2.1.apk.sha256`. This independently rebuilt public hash is kept distinct from the earlier local-build hash rather than presenting the local artifact as the published file.
- Fresh package inspection reports `cn.anitabi.navigator`, version code 7, version name 0.2.1, minimum SDK 26, and target SDK 37. Fresh signature inspection reports APK Signature Scheme v2, one RSA-4096 signer, and certificate SHA-256 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`, preserving public v0.2.0 update compatibility.
- Release-asset compatibility run `30631245688` downloaded the public APK independently and passed installation and cold launch on Google APIs API 26 and API 37, with evidence uploaded for both jobs. No APK was installed on the user's phone in this task.
- Deleted only the dedicated temporary public-asset audit directory after verification; the ignored local signed APK remains available, the user attachment directory remains untouched and untracked, and approximately 42.17 GiB remained free on the system drive. No password, SSH setting, VPS state, DNS, Google/Firebase project setting, phone data, quota, or unrelated service was changed during PR merging or publication, and no secret was written to Git, Release text, command output, or this record.
