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
