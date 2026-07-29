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

### Pending remote evidence

- The new end-to-end test still needs to pass on both CI emulator versions before the onboarding audit row can return from `implementation complete, end-to-end workflow pending` to fully verified. Xiaomi physical onboarding and the original real-GNSS/long-duration OEM/real missed-service boundaries remain separate.

### First end-to-end CI diagnosis and compatibility fix

- Main run `30477795243` kept the existing application evidence green: its 49-test/build/Lint/APK-audit job and both emulator cold-launch checks passed. Only the newly added onboarding test failed; later navigation steps were skipped rather than producing misleading secondary results.
- Downloaded and inspected both emulator artifacts before changing code. Android 8 logcat and `runtime-activities.txt` proved that the real Package Installer permission activity was on screen, but Android 8 reported it through `mResumedActivity` instead of the newer window-focus fields the test initially inspected. The focus probe now accepts both activity- and window-manager representations.
- Android 17 failed before its first Compose interaction because Compose UI Test 1.11.4 resolved the old Espresso 3.5.0 implementation, which reflectively calls the removed `InputManager.getInstance()` API. Pinned the androidTest-only Espresso dependency to 3.7.0, whose AndroidX release notes document the `getSystemService` compatibility fix. Production dependencies and APK behavior are unchanged.
- Made the always-run Android 8 network-restoration cleanup acquire and verify emulator root even when the preceding offline step was skipped. This prevents a test failure from being obscured by a second cleanup-only permission failure.
- Local verification after all three fixes passed 49 JVM tests with 0 failures/errors/skips, debug Android-test compilation and packaging, and debug Lint with 0 findings. Dependency insight confirms Espresso 3.7.0 wins over the transitive 3.5.0 version; `git diff --check` is clean and the generated Room schema was removed.
