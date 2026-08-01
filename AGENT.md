# 项目执行上下文（压缩版）

> 最后核对：2026-08-01。每个新任务开始前必须完整读取本文件；每个任务结束后必须更新当前状态、验证结果、剩余问题，并在“最近任务”追加一条简短真实记录。

## 维护方式

- 本文件是当前权威工作摘要，不再保存完整逐步骤流水。只保留仍会影响后续工作的事实、约束和证据边界。
- “最近任务”最多保留 5 条；超过后把较旧内容归并到“里程碑”，完整细节由 Git 历史保存。
- 压缩前的 1,637 行完整记录固定在提交 `c92a92195d903ecb89ae563a509699f32e5737ef`：
  - 本地：`git show c92a92195d903ecb89ae563a509699f32e5737ef:AGENT.md`
  - 远端：`https://github.com/realMisakaMikoto/junrei_navi/blob/c92a92195d903ecb89ae563a509699f32e5737ef/AGENT.md`
- 长期证据应写入对应的 `docs/` 文件；不要重新把本文件扩张成重复的 CI、浏览器、ADB 或失败重试日志。

## 最高执行约束

### Google 官方文档前置要求

- 任何涉及 Google Maps Platform 的设计、代码或修改，包括 Google 地图、Navigation SDK、Routes API、地图/路线/导航生命周期和相关接口，开始实现前必须查阅当时最新的 Google 官方开发者文档。
- 适用任务必须记录：查阅的官方文档范围、确认的关键约束、以及它们对实现的影响。
- 官方文档不可访问、互相矛盾或不能确认所需行为时，停止相关实现并报告；不得凭记忆、猜测或第三方示例补写行为。

### 报错解决记录要求

- 如果程序、构建、测试、CI、部署或运行环境遇到报错，解决后必须在本文件记录报错现象、确认的根因、实际解决方法和验证结果；不得只写“已修复”或省略解决方法。尚未解决时，记录当前阻断、已排除项和安全的恢复条件。

### 安全与证据边界

- 不得把密码、Token、API Key、服务账号内容、签名材料、原始 IP、坐标、动画名、搜索词、路线/响应正文写入源码、Git、文档、日志、命令输出或本文件。
- 不得伪造真机、GNSS、声音、锁屏、路线、账单或外部服务验收。模拟器、mock location、静态审计和用户口头确认必须分别标明证据边界。
- 用户明确禁止修改密码和 SSH。不得更改密码、SSH 端口、`sshd`、root 登录策略、authorized keys、防火墙或主机密钥库。获得明确授权时，也只能临时使用既有凭据；不得落盘、回显或记录。
- 未获当前任务明确授权，不安装、覆盖或卸载 APK，不清应用数据，不改权限、mock location、输入法、网络或设备配置。覆盖升级默认不得卸载或清数据。
- `.codex-remote-attachments/` 是用户所有的未跟踪目录；除非用户在当前任务明确引用，不读取，更不得修改、删除或暂存。
- Windows PowerShell 脚本必须保持 ASCII。删除仅限经过绝对路径和范围验证的任务生成物；不得碰用户文件、无关 Docker 数据、容器或服务。
- Google 路线内容不得持久化。配额数据库、磁盘或账单状态不确定时必须 fail closed；应用/镜像回滚不得回滚 SQLite 配额账本。
- 修改应保持手术式范围；保留用户和无关工作树改动，只暂存本任务文件。

## 当前 Git 与发布状态

- 规范仓库：`realMisakaMikoto/junrei_navi`。
- 本地 `origin` 仍配置为旧地址 `https://github.com/realMisakaMikoto/anitabi.git`，目前由 GitHub 重定向；远程 API 和新链接应使用规范仓库。
- 当前发布分支：`codex/transit-no-route`，以 `origin/main` 的 `6993f1ce6d638ec2d1600b7a079d0a2f928cbb7e` 为发布候选基线；v0.2.2 的提交、PR、标签和 Release 状态以本节后续记录及 `docs/releases/v0.2.2.md` 为准。功能分支交接提交为 `c0e1e5c04afb55cae76ca93f2a1920e1e62b20bb`；压缩前完整日志锚点为 `c92a92195d903ecb89ae563a509699f32e5737ef`。
- `main` / `origin/main` 已包含功能分支交接提交 `c0e1e5c04afb55cae76ca93f2a1920e1e62b20bb`；本次状态记录提交位于其后。
- 稳定标签 `v0.2.1` 指向 `fd65c1d54372df43769d4c53602ed2fb833f8ae2`。
- `main` 已通过快进合并纳入相对原基线 `cbc29fa7acc3dd2e589d5849d2f6e66be53b9ea1` 的 6 个提交：
  - `95cdb8366cab59a4b9cb12bdc61d98d8dd3f3d34`：修复 Google Navigation cancelled-task 概率闪退。
  - `43b9c7d067b7b1907d0b4ef528ff5ef65ec2c45a`：显示名改为 `巡礼手帳`，准备同版本替换 APK。
  - `b39e7a0`：替换包交接记录。
  - `c92a921`：旧 Release 说明品牌更新记录。
  - `e1b71e5`：压缩项目执行记录。
  - `c0e1e5c`：增加报错解决记录硬性约束。
- 这些提交已合并进 `main`；`v0.2.1` 标签未移动，也未重写历史。
- 当前唯一允许保留的未跟踪路径是用户的 `.codex-remote-attachments/`；不要暂存或清理。
- 2026-08-01 经用户在当前任务明确授权进行一次只读盘点：该路径是普通未跟踪目录，包含 2 个 JPEG 用户附件，共 124,294 字节，无链接、源码、APK、密钥或构建产物；未修改、删除或暂存。附件内容不写入项目记录，后续任务仍恢复默认禁止读取。

### 稳定 Release 的特殊边界

- 2026-08-01 通过只读 GitHub API 确认：公开稳定 `v0.2.1` 的 APK 已被同版本替换为：
  - 文件：`anitabi-v0.2.1.apk`
  - 大小：49,058,373 字节
  - SHA-256：`77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`
- 该摘要与本地 `app/build/outputs/manual-release/v0.2.1/anitabi-v0.2.1.apk` 及 Gradle Release 输出一致；相邻 `.sha256` 文件匹配。构建目录已忽略，`clean` 可能删除本地产物。
- 公开替换包包含 cancelled-task 修复和 `巡礼手帳` 改名，仍为 `versionName=0.2.1` / `versionCode=7`，并保持固定签名。
- 稳定标签仍指向替换前源码，但 `main` 已包含替换包对应的修复和改名。因此当前公开 APK 与标签源码仍不完全一致；不得声称标签可重现该资产，也不得擅自移动标签或重写历史。
- 用户已自行更新稳定 `v0.2.1` Release 文案。2026-08-01 只读 GitHub API 核对确认：正文不再记录旧 APK SHA-256，改为要求使用随 Release 发布的校验文件；APK 资产仍为 49,058,373 字节、SHA-256 `77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`。本任务未修改公开 Release 文案。
- 现有 8 个 Release：`v0.1.0`、`v0.1.1`、`v0.1.2`、`v0.1.3`、`v0.1.4`、`v0.2.0`、`v0.2.1-rc.7`、`v0.2.1`；每个仍有 APK 和校验文件两个资产。RC1–RC6 的 Release 对象已删除，但 Git tags/commits 保留。
- 除稳定 `v0.2.1` 外，上述旧 Release 说明正文中的旧应用名已替换为 `Anitabi Navigator`。这是历史说明品牌，不等于当前应用显示名。

## 当前产品与 Android 架构

- 用户可见名称：`巡礼手帳`。
- 包名/namespace：`cn.anitabi.navigator`。
- 发布候选版本：`versionName=0.2.2`、`versionCode=8`；`minSdk=26`、`compileSdk=37`、`targetSdk=37`；Java 17。公开稳定版在本次发布完成前仍为 v0.2.1。
- 主要工具链：Gradle 9.6.1、AGP 9.3.0、Kotlin 2.4.10、KSP 2.3.10、Compose BOM 2026.06.01。
- Google Navigation SDK 7.8.0 负责地图、定位及驾车/步行/骑行道路导航；项目不得同时接入 Maps SDK for Android。
- Google Routes 由自建 VPS 后端调用，Android 不包含服务账号。Firebase Anonymous Auth 只用于后端鉴权和配额归属。
- 旧 MapLibre/OpenFreeMap、ORS、Transitous 仅属于 v0.2.0 历史；当前没有这些 Provider、Key UI 或回退路径。
- Anitabi 数据只从 `https://api.anitabi.cn` 获取，图片只允许 HTTPS `image.anitabi.cn`；主站不得作为 API。保持人类访问频率和技术 User-Agent `AnitabiNavigator/0.2.2`。
- Bangumi 搜索使用官方 `https://api.bgm.tv/v0/search/subjects`。
- 无 GMS、断网或后端故障时，只显示已保存点位、顺序、设置和进度；没有备用地图或路线服务。

### 规划、持久化与导航硬边界

- 总行程无固定产品级点数上限；全局使用最近邻 + 有限轮次 2-opt，窗口内可使用 Held–Karp。
- Matrix：每个窗口 2–10 坐标，最多 100 个计费元素。
- Road route：每次 2–12 个位置，即最多 10 个中间点。
- Transit route：恰好两个位置，按相邻巡礼点逐段规划。
- Navigation SDK 技术上限为 25 个目的地；生产协调器因 UID 每日额度按最多 20 个目的地装载并在换批前重新预留。
- 用户拖动点位只刷新受影响的相邻腿/窗口，不应全量重算。
- Room schema 2 / `StoredTourV2` 只保存用户拥有的数据：作品/点、顺序、起终点、模式、停留与公交选项、完成点和导航状态。
- Google 矩阵、折线、步骤、ETA、公交详情和 Provider 响应只驻留内存。v0.2.0 升级保留导览、选择、顺序和进度，删除旧 ORS Key 与旧路线；迁移失败保留原记录并显示恢复错误。
- Analytics 与 Crashlytics 独立选择加入、默认关闭；撤回时分别清理本地 Analytics 数据或未发送 Crashlytics 报告。
- 非平凡公交行程必须至少包含一个真实 transit leg；不得把全步行结果伪装成公交。真正空路线、畸形响应和服务错误必须分开。
- 公交时间只允许 departure 或 arrival 其中一个，Google 窗口为过去 7 天至未来 100 天。

### Google Navigation Release 关键规则

- Release R8 必须保留反射创建的 `CreatorImpl` 公共零参构造、Navigation registry 的包兼容，以及 SDK 7.8.0 中 12 个 `ej` shader 子类的零参构造；对应审计必须 fail closed。
- `Navigator.setDestinations()` 不得再注册会在取消时抛主线程异常的结果监听器，也不得调用 SDK Future 的 `cancel()`。当前分支使用 `Dispatchers.IO` 上的可中断阻塞等待，并正确解包失败原因。
- 道路导航由 Google 负责语音、偏航和原生指令；应用自有 TTS 仅用于公交状态文本。

## 公交地区边界

- Consumer Google Maps 能显示当地公交，不代表 Routes API 会返回相同数据。
- Google 官方说明明确排除日本 Transit partners 与印度 IRCTC；当前 API 路径也无法提供本应用所需的中国大陆公交数据。
- 稳定产品明确不支持中国大陆、日本和印度的应用内公交；短期不计划接入第三方公交 Provider。印度限制来自 Google API，不是项目主动屏蔽。
- 旧的“公交全部验收通过”结论已撤销；不要重新引用。其他地区也只能在 Google Routes 实际返回公交路线时使用。

### 2026-08-01 公交 `NO_ROUTE` 定向诊断（修复范围待确认）

- 已在用户给定的真机配置中稳定复现首段失败。生产安全日志初始 24 小时窗口共有 29 个 `/v1/route` 完成事件：20 个 `404/NO_ROUTE`、9 个成功、0 个上游异常；定向重试后 `NO_ROUTE` 恰好增加 2，成功与上游异常均未增加，符合“公交空结果后步行兜底也为空”的现有控制流。日志只读取端点模板、状态和错误码，没有读取或保存请求/响应正文。
- 一次仅驻留内存的 loopback 请求形状检查确认：实际公交请求恰好两个 raw `latLng` waypoint；“最佳路线 / 全部方式”对应字段确实省略；时间锚与服务端当前 instant 偏差小于 60 秒。没有把坐标、时间值、Token、点位/作品文本或正文打印、落盘或写入本文档。
- 三次有界 Google 对照均先写入现有配额账本且不退款：仅给原坐标增加 `vehicleStopover` 仍为无路线；改用 address waypoint 能找到路线，路线几何两端仍处于原点附近的有界距离桶。三条诊断 UID 预留均为 1，SQLite `integrity_check` 为 `ok`。这些结果把根因收敛到 raw 坐标的道路/接入点吸附假阴性，而非时区、公交筛选、网络、鉴权、配额、HTTP 错误或后端 `NO_ROUTE` 归一化。Consumer Google Maps 的用户确认与 Routes API 对照仍分别标明，不能互相冒充。
- 实现前重新查阅了 Google 官方 [Specify locations](https://developers.google.com/maps/documentation/routes/specify_location)、[Waypoint reference](https://developers.google.com/maps/documentation/routes/reference/rest/v2/Waypoint)、[Compute Routes reference](https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes)、[Review the route response](https://developers.google.com/maps/documentation/routes/understand-route-response)、[Search for destinations](https://developers.google.com/maps/documentation/geocoding/search-for-destinations) 与 [Navigation point tokens](https://developers.google.com/maps/documentation/geocoding/navigation-point-tokens)。确认 raw 坐标只会吸附到最近道路且可能不是物业接入点；Place ID 才是官方优先的带接入点语义；`address` 是人类可读地址或 Plus Code，模糊值可能被误解；从精确坐标寻找导航点的正式路径是 SearchDestinations。
- 因数据模型没有 Place ID、完整地址或已选接入点，不能把公共点位名称自动当作 `address`。SearchDestinations 可能返回多个 destination / entrance / navigation point，官方 token 流程要求用户选择 preferred location；navigation point 仅标注 `DRIVE` / `WALK`，官方没有给出自动用于公交 waypoint 的选择规则。它还需要新启用并单独核算 Geocoding API，且会把路线到达点从原巡礼坐标改为道路旁接入点。按官方文档前置要求和“不得盲移坐标”边界，本次试做的自动 address-label 回退已完整撤销，Android/后端源码与测试均无残留差异。
- 当前没有可诚实声称完成的无交互最小修复。安全的后续范围只能二选一：只把错误文案改为“精确点位不可接入”并指导用户改用当前位置/可通行入口；或另立产品任务，让用户显式选择 Google 返回的接入点，同时建模“巡礼目标坐标”和“路线接入点”、增加 Geocoding 独立配额/账单/隐私边界，并先取得公交兼容性的官方依据。生产未部署或改配置，公开和 loopback 健康检查均仍为 200。
- 诊断工具报错均已闭环：`mcporter` 的 function-call/PowerShell `--args` 形式无法加载或解析参数，改为显式命名参数后只返回 Google 官方文档；首轮设备时钟脚本误用只读 `$Host`，改用任务专用变量后确认漂移不超过 5 秒；UIAutomator 首次 XML 含尾随提示导致解析失败，限定到 `</hierarchy>` 后通过；长轮询因每次 UI dump 较慢而超时，拆成触发与短检查后复现；设备熄屏、同一真机同时出现 USB/无线两条 ADB transport、以及误假设宿主使用默认端口分别导致前置检查、ADB 和内存捕获失败，实际通过唤醒设备、显式选择 transport、只读查询 Compose 端口后重试成功；内存诊断脚本首次因嵌套字符串定界冲突在本地语法阶段退出，修正定界后运行，失败轮次均未发 Google 请求或预留配额。设备无 `sqlite3`，因此没有导出包含用户点位的数据库；VPS 无抓包/trace 工具，最终使用仅驻留内存的原始 socket 检查，没有安装工具或创建捕获文件。
- 用户要求后续真机验收先完整读取 `C:\Users\csy15\.codex\AGENTS.md`，并使用其中规定的无线 ADB 流程。本次无线路详情任务已按该流程完成显式无线 transport 验证、APK 包名核对和 `install -r` 覆盖安装；未卸载、清数据、改权限、改定位提供者或运行 connected Gradle 任务。

### 2026-08-01 无线路详情与 Google 地图交接

- 仅为 `TransitSegmentUnavailableException` 增加失败段的内存端点，并在 `PlannerViewModel` 将其映射为名称与坐标的展示模型；没有解析错误文案，也没有改动 Routes 请求、公交转步行回退、排序、持久化、权限、服务或导航行为。只有公交与步行都明确返回无线路的分段错误会在原红色错误卡内显示蓝色、加粗、下划线的“详细信息”；普通网络或泛化错误不会显示伪详情，页面原有底部固定按钮未改。
- 详情使用现有 Material 设计语言的底部面板，显示失败段号、起点和终点名称、各自纬度与经度；坐标仅来自当前内存错误状态，未新增持久化或日志。用户显式点击“在 Google 地图中查看路线”后，应用使用精确端点构造公交 Directions URL，优先交给已安装的 Google 地图，未安装时才回退浏览器。
- 实现前查阅 Google 官方 [Maps URLs](https://developers.google.com/maps/documentation/urls/get-started) 与 Android [Google Maps intents](https://developer.android.com/guide/components/google-maps-intents)。确认 Directions URL 必须包含 `api=1`，`origin` / `destination` 可使用 URL 编码的经纬度，`travelmode=transit` 会预选公交且不需要 API Key；Android 上可用 `ACTION_VIEW` 定向 Google 地图并在应用缺失时回退通用 HTTPS。因 `google.navigation:` 不支持这类自定义公交起终点，本实现没有使用它，也没有加会直接开始导航的 `dir_action`。
- 验证结果：127 个 JVM 测试 / 26 个套件全部通过；`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` 和 AndroidTest Kotlin 编译通过。新增单元测试覆盖异常端点、点位名称/当前位置映射与精确 URL 编码；新增 Compose 测试覆盖红框入口、详情内容和 Google 地图动作，定向 instrumentation 在 Xiaomi API 36 真机为 1/1 通过。主 APK 与测试 APK 均经 `aapt` 核对包名后使用唯一无线 transport 显式 `adb -s ... install -r` 覆盖安装。人工复现现有首个失败段后，入口、名称和坐标显示正确；点击动作实际唤起手机 Google 地图、填入两个端点并选中公交模式。截图只保存在工作区外的临时目录，未写入 Git 或本文档。
- 本任务只改善错误可解释性和外部核对路径，raw 坐标接入点吸附导致的 Routes API 假阴性仍然存在；Consumer Google Maps 能找到线路不代表应用当前 Routes API 请求也会成功，不得把本界面改动描述成路线算法修复。
- 报错闭环：技术检索首次沿用 function-call 风格的 `mcporter call`，因未显式命名参数而无法加载工具元数据；改用 `mcporter call exa.web_search_exa 'query=...' numResults=5 --output markdown` 后只取得 Google 官方结果。首次 Kotlin 编译出现 `showUnavailableRouteDetails`、`state` 和 `openGoogleMaps` 未解析，根因是机械补丁把详情面板块放进了无这些状态的 `SettingsSection`；将该块移回 `PlannerSettingsScreen` 作用域后，单测、Lint、Debug 构建和 AndroidTest 编译均通过。首次 APK 核对脚本因当前 shell 未设置 `ANDROID_SDK_ROOT` / `ANDROID_HOME` 而在安装前停止；改用本机已配置的明确 Android SDK Build Tools 路径完成 `aapt dump badging` 后才执行覆盖安装。

### 2026-08-01 v0.2.2 发布准备

- 用户完成 Debug 候选人工验收后明确授权提交、创建 PR 并发布小版本。Android 已提升为 `versionName=0.2.2` / `versionCode=8`；后端版本与协议保持 0.2.1，Room schema、路线算法、权限流程和 `NavigationService` 行为不变。README、隐私说明、发布说明、发布清单与 `docs/releases/v0.2.2.md` 已同步；v0.2.1 历史材料未改写。
- 最终本地门禁通过：127 个 JVM 测试 / 26 个套件，0 失败、0 错误、0 跳过；Debug/Release Lint 0 个问题；Debug、正式签名 Release 与 AndroidTest APK 构建成功。后端 29 个测试、TypeScript 构建和生产依赖审计通过，0 个生产依赖漏洞；跟踪源码凭据、Navigation R8 与 Release APK 内容审计全部通过。
- 本地正式候选经 `aapt` 核对为 `cn.anitabi.navigator` / `0.2.2 (8)` / minSdk 26 / targetSdk 37；`apksigner` 验证为单一 RSA-4096、v2 签名，证书 SHA-256 仍为公开固定值。最终候选大小 49,102,773 字节、SHA-256 `17fdf79ed9789a6631430f9cbe329a4bd7b42a0840614479157705fa714d8bae`；它不是精确 GitHub Release 资产证据，公开资产必须在工作流发布后重新下载核对。
- 第一次合并运行最终 Gradle 门禁时，任务图在编译前报“Release signing is not configured”。根因是新终端没有加载四个 `ANITABI_*` 正式签名环境变量，不是源码、keystore 或密码损坏。实际恢复方式是只在单个 PowerShell/Gradle 子进程内使用当前 Windows 用户 DPAPI 解密工作区外既有密码文件，把完整签名值注入子进程环境，并在 `finally` 中清除；重跑两次完整门禁均成功，未输出、复制或提交密码和 keystore。
- Release Lint 首轮成功但提示新 Maps URL 使用处可改用 AndroidX `String.toUri`。首次清理补丁因预期的 import 邻接行与实际文件不一致而完整拒绝，没有形成部分修改；按实际导入位置重新应用一行等价替换后，Debug/Release Lint 都为 0 个问题，完整 Debug/Release 构建与单测再次通过。构建期间 Google Navigation SDK 自带多语言字符串打印非阻断 AAPT 提示，项目 Lint 报告没有对应问题。
- 独立发布审查指出旧清单要求正式签名 APK 在 Xiaomi 真机覆盖安装后才能发稳定版，而本轮只有 Debug 候选人工验收。用户已经明确表示该候选任务完成，随后要求提交、PR 和稳定小版本；因此 v0.2.2 的边界已显式调整为“用户接受的 Debug 真机 UI 验收 + 本地/CI 固定签名审计 + 发布后精确公开 APK API 26/API 37 门禁”。正式签名真机覆盖没有执行且保持未验证，不得在证据中补勾或冒充。
- 同一审查还发现：定向 Google 地图 Intent 失败后，通用 HTTPS fallback 若也没有处理器会再次抛 `ActivityNotFoundException`。现已让两次启动都返回明确成功状态，第二次失败不再崩溃；详情面板会以内联错误色 live region 提示设备无法打开地图或网页链接。Compose 契约测试增加失败回调断言；实际 Google 地图成功交接的既有真机证据边界不变。
- 上述失败提示回归测试首轮 AndroidTest Kotlin 编译报 `assertTextContains` 未解析，根因是新增断言时漏导入 Compose 测试扩展；补入 `androidx.compose.ui.test.assertTextContains` 后按相同完整门禁重新验证。该失败发生在测试源码编译阶段，没有安装 APK、运行设备测试或产生发布资产。
- 用户的 `.codex-remote-attachments/` 仍未读取、修改、删除或暂存。PR、远端 CI、标签、GitHub Release 和精确公开 APK 兼容工作流尚待本次发布流程完成，当前不得声称已经发布。

## 后端与生产环境

- 公共 API：`https://api.anitabi.afunnypersonlol0.site`。
- 技术栈：Node.js 24、TypeScript、Fastify、SQLite WAL；固定 Google OAuth/Routes 上游。
- 端点：`GET /v1/health`、`POST /v1/matrix`、`POST /v1/route`、`POST /v1/navigation/reserve`；POST 仅 HTTPS JSON，正文最多 16 KiB。
- 生产使用现有 Nginx；容器监听 8787，宿主映射为 `127.0.0.1:8788`，因为宿主 8787 属于无关服务。
- 目录：程序 `/opt/anitabi-api`，数据 `/var/lib/anitabi-api`，秘密 `/etc/anitabi-api/secrets`。
- 容器必须保持非 root、只读根文件系统、drop all capabilities、no-new-privileges、只读秘密挂载、健康检查和自动重启。
- 最后记录的已部署后端运行树来自 `0902e484c9d6e8745febea665c0a2c293839cf14`，公开与 loopback 健康检查当时正常。任何新部署前都要重新只读盘点，不能把历史健康状态当成实时结果。
- 配额：Matrix 全局 9,000 元素/月、每 UID 2,000/UTC 日；Routes 全局 9,000 次/月、每 UID 200/UTC 日；Navigation 全局 900 目的地/月、每 UID 20/UTC 日。
- SQLite 事务原子预留；上游失败不退款。每日一致性备份保留 7 天；恢复后计费默认关闭，直到证明账本没有回退。
- 日志只允许端点模板、状态、延迟区间和错误码；不得包含 Token、原始 IP、坐标、作品/搜索文本、请求或响应正文。
- VPS 服务账号仅有 Service Usage Consumer 权限；私钥只存在于外部受限位置和 VPS 只读挂载。

## 密钥、签名和外部系统状态

- Routes API、Navigation SDK、Firebase Anonymous Auth 已启用；Maps SDK for Android 已禁用。
- Firebase 和 Navigation Android Key 已轮换；旧 Key 已删除，GitHub secret-scanning alert 已按 revoked 关闭。
- 当前 Android Key 限制为包名和正式/调试签名；Navigation Key 仅允许 Navigation SDK。
- `app/google-services.json`、Navigation Key、服务账号和签名材料均不得进入 Git；CI 只能通过加密 Secrets 恢复。
- 固定 Release 签名为 RSA-4096、APK Signature Scheme v2、单一签名者；证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`。
- 签名材料在工作区外并由 Windows 用户范围保护；不得打印密码或复制 keystore 到仓库。
- 生产变更不得影响现有个人网站或无关容器。连接 VPS 前重新验证主机指纹；不要从本文件猜测或复制敏感 SSH 值。

## 当前验证基线与诚实边界

- 当前分支已有报告：127 个 JVM 测试 / 26 个套件，0 失败、0 错误、0 跳过。
- Debug 与 Release Lint 均通过，仅有 4 条 `AutoboxingStateCreation` 性能提示；Debug/Release 构建、Navigation R8 审计、源码凭据审计和 APK 内容审计通过。
- 后端最后记录为 29 个测试通过，TypeScript 构建通过，生产依赖审计 0 漏洞；本次压缩没有重跑后端测试。
- 原稳定发布流程曾通过：PR `30628585336`、main `30629643413`、signed release `30630717751`、旧公共资产兼容 `30631245688`。
- `30631245688` 验证的是后来被替换前的公共资产，不能作为当前 `77e263…` APK 的 exact-public-asset 证据。
- cancelled-task 修复曾在 Xiaomi 15T Pro 上完成 6 次有界启动/快速停止，未再出现 `Task was cancelled`，地图与最终 Google 导航正常；该测试使用既有 mock-location，且测试包早于显示名改动。
- 当前公开替换 APK 与本地完整测试/签名产物哈希一致，但没有记录新的 exact-public-asset API 26/API 37 工作流或该精确 APK 的真机覆盖验收。需要此类结论时必须重新验证。
- 手机当前安装、页面、网络、mock location 和选点状态都可能已被用户改变；开始新真机任务时重新只读确认，不沿用旧状态。

### 2026-08-01 前端重设计与真机验收状态

- Jetpack Compose 展示层、品牌色、应用内标识和 adaptive/monochrome 图标已重设计；未修改 ViewModel、repository、Room schema、网络请求、路线算法或 `NavigationService` 行为。搜索页真机横屏曾出现固定底栏被挤出屏幕的问题，根因是搜索表单、选择轨和底栏同时作为不可滚动 `Column` 子项；现已改为固定 Header、单一 `LazyColumn` 主体和固定 Footer，并新增 320dp 短高度回归测试。
- Xiaomi 真机（Android 16 / API 36）已通过新增 7 个 Compose UI 契约测试和既有 10 个 instrumentation 测试。既有测试覆盖首次引导、设置迁移、Room 迁移、离线进程恢复、离线手动到达及熄屏 mock GPS 自动到达；第三方 FakeGPS 首次干扰自动到达，用户取消该提供者后复测通过。人工检查覆盖竖屏、修复前后横屏、200% 字体、IME resize 与自适应图标安全区。
- 新增 Compose 测试首轮无法启动 `ComponentActivity`，根因是缺少 Compose 测试宿主 manifest；已增加仅 debug 生效的 `androidx.compose.ui:ui-test-manifest`，重建后 7/7 通过。横屏回归测试首版无法定位未组合的 LazyColumn 屏外节点；改为从列表容器执行 `performScrollToNode` 后通过。
- 一次被中止的 `connectedDebugAndroidTest` 在 debug APK 因签名不匹配安装失败后，仍于 17:12 自动卸载了正式包，系统记录为 `PACKAGE_FULLY_REMOVED`；因 `allowBackup=false`，本地数据无法通过 Android 备份恢复。用户确认没有重要数据并授权安装测试版。后续禁止在含正式签名包的个人设备上运行该 Gradle 任务，只允许显式校验包名后执行单一 APK 安装。
- 正式包名的 debug 测试版已重新构建；首次安装被 MIUI 的 USB 安装确认以 `INSTALL_FAILED_USER_RESTRICTED` 取消，用户确认弹窗后重试成功。当前手机安装 `cn.anitabi.navigator`、`versionName=0.2.1`、`versionCode=7`。PR、Release、标签和公开资产均未改动，等待用户手工验收通过；验收后仍需重跑最终 JVM、Release Lint、Release 构建及发布审计。
- 地图异常现象与根因：真机选点页一度只显示地图背景和 Google 水印，底图、道路、地名、Marker 与自动取景均缺失；修复前截图为系统截图目录中的 `anitabi-map-issue-20260801.png`。同一进程的 Logcat 明确报告 Maps 与 Navigation SDK `Authorization failure` / `INVALID_ARGUMENT`，并显示 API Key 为空。确认原因是本机 Key 仅存在于 Git 忽略的 `local.properties`，而原 Gradle 逻辑只读取 Gradle property 或环境变量，普通 `assembleDebug` 因此把空值写入 `com.google.android.geo.API_KEY`。现有 UI 契约测试强制列表模式，没有实例化真实地图，因而无法发现授权缺口。
- 地图异常实际修复：`app/build.gradle.kts` 仅为 `ANITABI_NAVIGATION_API_KEY` 增加 `local.properties` 后备读取，仍保持 Gradle property、环境变量优先，Release 签名凭据来源语义不变；`local.properties` 继续不受 Git 跟踪，源码和文档未写入实际 Key。`docs/BUILD.md` 已同步本地配置方式。未修改地图生命周期、Marker、相机、ViewModel、repository、网络请求、路线算法或 `NavigationService`。
- 本次实现前查阅了 2026-07-15 更新的 Google 官方 [Navigation SDK Android Studio 设置](https://developers.google.com/maps/documentation/navigation/android-sdk/android-studio-setup) 与 [Navigation SDK 概览](https://developers.google.com/maps/documentation/navigation/android-sdk/overview)。确认 Key 应从未提交的本地配置安全注入 `com.google.android.geo.API_KEY`；Navigation SDK 已包含地图层且不得与 Maps SDK 同时依赖。因此保留现有 Navigation SDK 7.8.0 和 manifest 注入方式，不增加 Maps SDK、运行时取 Key 或新的地图初始化路径。
- 地图修复验证：最终 debug merged manifest 的 Key 非空且本地配置未被跟踪；`testDebugUnitTest`、`lintDebug`、`assembleDebug` 通过；APK 经 `aapt` 核对为 `cn.anitabi.navigator` / `0.2.1 (7)` 后使用 `adb install -r` 覆盖安装，未卸载、未清数据。同一真机选点页随后恢复真实底图、道路、地名、全部 Marker 和自动取景，修复后截图为 `anitabi-map-fixed-20260801.png`；本次启动后的授权失败、`INVALID_ARGUMENT` 和 `PilgrimageMap ... failed` 计数均为 0。真实 Google 地图仍属于依赖凭据与网络的真机人工证据，不声称由 CI/UI 契约测试覆盖。
- 本次首次 APK 包名核对命令把 `local.properties` 中转义后的 Windows 盘符误解析为 `C\:`，导致 `aapt` 未执行；PowerShell 将其视为非终止错误，后续 `adb install -r` 仍执行成功。发现后立即改用已解析的 Android SDK 路径，从 APK 与设备两侧补验包名和版本；最终覆盖安装前再次成功执行 `aapt dump badging`，确认目标仍为正式包名。期间没有卸载、清数据或安装其他包。
- 路线设置页的出行方式卡片曾出现图标和文字贴在上沿、下方留出大块空白的问题，修复前真机截图为 `anitabi-ui-too-high-20260801.png`。根因不是状态栏 inset 或整页位置：紧凑布局使用纵向滚动，子项高度约束无界；外层 `Card.heightIn(min = 58.dp)` 只在内容测量后撑高卡片，内部 `Row.fillMaxSize()` 无法填充无界高度，因此内容仍按自身高度排在顶部。实际修复是把 58dp 最小高度移到负责排列内容的内部 Row，并使用 `fillMaxWidth()` 与既有 `Alignment.CenterVertically`；允许大字体自然增高。按用户确认，底部固定按钮、起终点、滚动区和系统栏处理均未修改。
- 出行方式对齐验证：新增稳定 `planner-mode-*` 语义标签，并在 Compose UI 契约测试中比较卡片中心与未合并文字节点中心，允许最多 2 像素舍入差；该定向 instrumentation 测试在 Xiaomi API 36 真机通过。`testDebugUnitTest`、`lintDebug`、`assembleDebug`、`assembleDebugAndroidTest` 与 AndroidTest Kotlin 编译通过；主 APK 和测试 APK 经 `aapt` 核对后分别显式 `adb install -r`，未运行 connected Gradle 任务、未卸载或清数据。修复后同页真机截图为 `anitabi-ui-centered-20260801.png`，四个选项内容均垂直居中。

## 里程碑摘要

- v0.1.0–v0.1.4：完成最初 Android、Room、搜索、MapLibre/ORS/Transitous、连续导航和固定签名发布；这些 Provider 已不属于当前架构。
- v0.2.0：加入多作品联合巡礼、首次导览、真机/模拟器覆盖和可升级生产基线。
- v0.2.1：迁移到 Google Navigation + VPS Google Routes + Firebase，加入无限点分批、Room 2 数据迁移、遥测选择加入、配额与生产部署。
- RC1–RC6 的 Release 因已知缺陷被删除，tags 保留；RC7 修复地图/R8/道路/公交参数问题。后续公交证据促成更严格的 transit 真实性与错误处理。
- 稳定 v0.2.1 已发布；随后修复 Navigation Future 取消崩溃、改名为 `巡礼手帳`，并生成/替换同版本 APK。相关提交现已合并进 `main`，但标签源码仍未同步。

## 常用权威入口

- 用户说明：`README.md`
- 隐私：`PRIVACY.md`
- 安全：`SECURITY.md`
- 构建：`docs/BUILD.md`
- 后端运维：`backend/README.md`
- 当前发布候选说明：`docs/RELEASE_NOTES_v0.2.2.md`；v0.2.1 历史说明保持不变。
- Release/真机证据：`docs/releases/`、`docs/PHYSICAL_DEVICE_ACCEPTANCE_v0.1.3.md`
- 压缩前完整实施记录：提交 `c92a92195d903ecb89ae563a509699f32e5737ef` 中的 `AGENT.md`

## 最近任务（最多 5 条）

- 2026-08-01：为确切的公交/步行无线路分段增加红框内“详细信息”、失败两端名称与经纬度底部面板，以及预填两端并选中公交模式的 Google 地图跳转；未改路线行为，完成 JVM、Lint、Debug/AndroidTest 构建、定向 Compose 真机测试与真实 Google 地图交接验证。
- 2026-08-01：完成公交首段 `NO_ROUTE` 的真机、生产安全日志和 Google 有界对照诊断，确认 raw 坐标接入点吸附是假阴性根因；因官方方案需要用户选择导航点、独立 Geocoding 配额及新的到达点语义，自动 address-label 试做已完整撤销，当前只保留 `AGENT.md` 证据并等待用户确认后续产品范围，未部署或覆盖 APK。
- 2026-08-01：经用户明确授权，只读检查 `.codex-remote-attachments/`；确认其中只有 2 个 JPEG 用户附件，未修改、删除或暂存。检查完成后恢复默认禁止读取边界，本任务只修改 `AGENT.md`。
- 2026-08-01：截图定位并修复 debug 真机地图空白：本地 Key 未被 Gradle 读取导致 manifest 占位符为空；现仅为 Navigation Key 增加未跟踪 `local.properties` 后备读取，完成单测、Lint、构建、覆盖安装、授权日志和同页地图截图验证。PR 与 Release 继续等待用户验收。
- 2026-08-01：真机截图定位并修复路线设置页出行方式内容贴上沿；将最小高度约束从外层卡片移到内部 Row，底栏与其他页面结构不变，新增中心对齐 bounds 回归测试并在 API 36 真机通过，已覆盖安装修复版并保存前后截图。
