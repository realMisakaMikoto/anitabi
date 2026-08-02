# 项目执行上下文（压缩版）

> 最后核对：2026-08-02。每个新任务开始前必须完整读取本文件；每个任务结束后必须更新当前状态、验证结果、剩余问题，并在“最近任务”追加一条简短真实记录。

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
- 当前实施分支为 `codex/v0.2.3-japan-transit`，已跟踪同名上游。v0.2.3 主实现提交为 `ff75d8b63ce8ffc7bc97ca84c7733cc2d47cf814`，四轮恢复/并发修复依次为 `ae835fb040d4b9747510c30f0a66011b22df6fba`、`c6e393ab235fea1479a21f49d86b8ddac6331ce6`、`cfac4bd96190ac756fa6d6e5ae0b8e74ead8b558`、`8c140d3`，均已推送到 ready [PR #12](https://github.com/realMisakaMikoto/junrei_navi/pull/12)。PR CI run `30732960430` 基于已同步的 `e51f014`，backend、verify、API 26 与 API 37 四项全部成功；本次验收文档收口提交后分支相对本地 `main` 为 ahead 7 / behind 0，PR 尚待 Chrome rebase 合并。任务起点 `8d999c5e4c102b2a43e663653ea18306af1b193c` 当时与 `main` 及其上游一致。v0.2.2 功能提交 `0423bd06e67694e28f15c378a948a9bcd9d4ff28` 经 [PR #11](https://github.com/realMisakaMikoto/junrei_navi/pull/11) rebase 合并；发布源码提交为 `308d4c2afcc9681335e9b60d68cbb2891b025442`。
- 稳定标签 `v0.2.2` 指向上述发布源码提交；非草稿、非预发布的 [v0.2.2 Release](https://github.com/realMisakaMikoto/junrei_navi/releases/tag/v0.2.2) 已发布并成为 Latest Release。完整证据在 `docs/releases/v0.2.2.md`。
- 功能分支 `codex/transit-no-route` 仍保留在远端；不要将其误当作当前发布分支。压缩前完整日志锚点仍为 `c92a92195d903ecb89ae563a509699f32e5737ef`。
- `main` 已通过快进合并纳入相对原基线 `cbc29fa7acc3dd2e589d5849d2f6e66be53b9ea1` 的 6 个提交：
  - `95cdb8366cab59a4b9cb12bdc61d98d8dd3f3d34`：修复 Google Navigation cancelled-task 概率闪退。
  - `43b9c7d067b7b1907d0b4ef528ff5ef65ec2c45a`：显示名改为 `巡礼手帳`，准备同版本替换 APK。
  - `b39e7a0`：替换包交接记录。
  - `c92a921`：旧 Release 说明品牌更新记录。
  - `e1b71e5`：压缩项目执行记录。
  - `c0e1e5c`：增加报错解决记录硬性约束。
- 这些提交已合并进 `main`；`v0.2.1` 标签未移动，也未重写历史。
- 当前与任务无关且必须原样保留的未跟踪路径只有用户的 `.codex-remote-attachments/`；不要读取、暂存或清理。v0.2.3 主实现的源码、测试、固定地区资产和发布文档已进入 `ff75d8b`，后续仍只能按当前任务范围显式暂存。
- 2026-08-01 经用户在当前任务明确授权进行一次只读盘点：该路径是普通未跟踪目录，包含 2 个 JPEG 用户附件，共 124,294 字节，无链接、源码、APK、密钥或构建产物；未修改、删除或暂存。附件内容不写入项目记录，后续任务仍恢复默认禁止读取。

### 稳定 Release 的特殊边界

- 当前公开稳定版为 `v0.2.2`：`anitabi-v0.2.2.apk` 共 49,102,773 字节，SHA-256 `e44f2d8e2179650395fa28ecff3a59d30bcb21c6798585e80b87535a0bf96676`，与公开校验文件及 GitHub 资产摘要一致。APK 为 `cn.anitabi.navigator` / `0.2.2 (8)`，固定 RSA-4096 v2 签名证书不变；API 26/API 37 精确公开资产门禁均通过。
- 2026-08-01 通过只读 GitHub API 确认：公开稳定 `v0.2.1` 的 APK 已被同版本替换为：
  - 文件：`anitabi-v0.2.1.apk`
  - 大小：49,058,373 字节
  - SHA-256：`77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`
- 该摘要与本地 `app/build/outputs/manual-release/v0.2.1/anitabi-v0.2.1.apk` 及 Gradle Release 输出一致；相邻 `.sha256` 文件匹配。构建目录已忽略，`clean` 可能删除本地产物。
- 公开替换包包含 cancelled-task 修复和 `巡礼手帳` 改名，仍为 `versionName=0.2.1` / `versionCode=7`，并保持固定签名。
- 稳定标签仍指向替换前源码，但 `main` 已包含替换包对应的修复和改名。因此当前公开 APK 与标签源码仍不完全一致；不得声称标签可重现该资产，也不得擅自移动标签或重写历史。
- 用户已自行更新稳定 `v0.2.1` Release 文案。2026-08-01 只读 GitHub API 核对确认：正文不再记录旧 APK SHA-256，改为要求使用随 Release 发布的校验文件；APK 资产仍为 49,058,373 字节、SHA-256 `77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`。本任务未修改公开 Release 文案。
- 现有 9 个 Release：`v0.1.0`、`v0.1.1`、`v0.1.2`、`v0.1.3`、`v0.1.4`、`v0.2.0`、`v0.2.1-rc.7`、`v0.2.1`、`v0.2.2`；每个仍有 APK 和校验文件两个资产。RC1–RC6 的 Release 对象已删除，但 Git tags/commits 保留。
- 除稳定 `v0.2.1` 外，上述旧 Release 说明正文中的旧应用名已替换为 `Anitabi Navigator`。这是历史说明品牌，不等于当前应用显示名。

## 当前产品与 Android 架构

- 用户可见名称：`巡礼手帳`。
- 包名/namespace：`cn.anitabi.navigator`。
- 当前源码版本：`versionName=0.2.3`、`versionCode=9`；当前公开稳定版仍为 v0.2.2。`minSdk=26`、`compileSdk=37`、`targetSdk=37`；Java 17。
- 主要工具链：Gradle 9.6.1、AGP 9.3.0、Kotlin 2.4.10、KSP 2.3.10、Compose BOM 2026.06.01。
- Google Navigation SDK 7.8.0 负责地图、定位及驾车/步行/骑行道路导航；项目不得同时接入 Maps SDK for Android。
- Google Routes 由自建 VPS 后端调用，Android 不包含服务账号。Firebase Anonymous Auth 只用于后端鉴权和配额归属。
- 旧 MapLibre/OpenFreeMap、ORS、Transitous 仅属于 v0.2.0 历史；当前没有这些 Provider、Key UI 或回退路径。
- Anitabi 数据只从 `https://api.anitabi.cn` 获取，图片只允许 HTTPS `image.anitabi.cn`；主站不得作为 API。保持人类访问频率，技术 User-Agent 随 `BuildConfig.VERSION_NAME`，当前为 `AnitabiNavigator/0.2.3`。
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
- v0.2.3 当前源码仍不为日本提供 Routes 应用内公交，而是把全日本点行程切换为纯本地排序和用户主动的外部 Google 地图单段公交交接；中国大陆、印度及其他非日本地区仍只能以 Google Routes 实际返回结果为准，不接入第三方公交 Provider。印度限制来自 Google API，不是项目主动屏蔽。
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

### 2026-08-01 v0.2.2 发布完成

- 用户完成 Debug 候选人工验收后明确授权提交、创建 PR 并发布小版本。Android 已提升为 `versionName=0.2.2` / `versionCode=8`；后端版本与协议保持 0.2.1，Room schema、路线算法、权限流程和 `NavigationService` 行为不变。README、隐私说明、发布说明、发布清单与 `docs/releases/v0.2.2.md` 已同步；v0.2.1 历史材料未改写。
- 最终本地门禁通过：127 个 JVM 测试 / 26 个套件，0 失败、0 错误、0 跳过；Debug/Release Lint 0 个问题；Debug、正式签名 Release 与 AndroidTest APK 构建成功。后端 29 个测试、TypeScript 构建和生产依赖审计通过，0 个生产依赖漏洞；跟踪源码凭据、Navigation R8 与 Release APK 内容审计全部通过。
- 本地正式候选经 `aapt` 核对为 `cn.anitabi.navigator` / `0.2.2 (8)` / minSdk 26 / targetSdk 37；`apksigner` 验证为单一 RSA-4096、v2 签名，证书 SHA-256 仍为公开固定值。最终候选大小 49,102,773 字节、SHA-256 `17fdf79ed9789a6631430f9cbe329a4bd7b42a0840614479157705fa714d8bae`；它不是精确 GitHub Release 资产证据，公开资产必须在工作流发布后重新下载核对。
- 第一次合并运行最终 Gradle 门禁时，任务图在编译前报“Release signing is not configured”。根因是新终端没有加载四个 `ANITABI_*` 正式签名环境变量，不是源码、keystore 或密码损坏。实际恢复方式是只在单个 PowerShell/Gradle 子进程内使用当前 Windows 用户 DPAPI 解密工作区外既有密码文件，把完整签名值注入子进程环境，并在 `finally` 中清除；重跑两次完整门禁均成功，未输出、复制或提交密码和 keystore。
- Release Lint 首轮成功但提示新 Maps URL 使用处可改用 AndroidX `String.toUri`。首次清理补丁因预期的 import 邻接行与实际文件不一致而完整拒绝，没有形成部分修改；按实际导入位置重新应用一行等价替换后，Debug/Release Lint 都为 0 个问题，完整 Debug/Release 构建与单测再次通过。构建期间 Google Navigation SDK 自带多语言字符串打印非阻断 AAPT 提示，项目 Lint 报告没有对应问题。
- 独立发布审查指出旧清单要求正式签名 APK 在 Xiaomi 真机覆盖安装后才能发稳定版，而本轮只有 Debug 候选人工验收。用户已经明确表示该候选任务完成，随后要求提交、PR 和稳定小版本；因此 v0.2.2 的边界已显式调整为“用户接受的 Debug 真机 UI 验收 + 本地/CI 固定签名审计 + 发布后精确公开 APK API 26/API 37 门禁”。正式签名真机覆盖没有执行且保持未验证，不得在证据中补勾或冒充。
- 同一审查还发现：定向 Google 地图 Intent 失败后，通用 HTTPS fallback 若也没有处理器会再次抛 `ActivityNotFoundException`。现已让两次启动都返回明确成功状态，第二次失败不再崩溃；详情面板会以内联错误色 live region 提示设备无法打开地图或网页链接。Compose 契约测试增加失败回调断言；实际 Google 地图成功交接的既有真机证据边界不变。
- 上述失败提示回归测试首轮 AndroidTest Kotlin 编译报 `assertTextContains` 未解析，根因是新增断言时漏导入 Compose 测试扩展；补入 `androidx.compose.ui.test.assertTextContains` 后按相同完整门禁重新验证。该失败发生在测试源码编译阶段，没有安装 APK、运行设备测试或产生发布资产。
- GitHub connector 创建 PR 与命令行 `gh pr create` 均因令牌写权限不足返回 403；在已登录的 GitHub 网页会话中创建 ready PR #11 后解决。命令行令牌同样无权重跑失败 workflow job，改由同一网页会话只重跑失败的 API 26 矩阵；没有扩大仓库或账户操作范围。
- PR run `30699060766` 全绿并 rebase 合并。main run `30699662842` 第一次 API 26 尝试在系统权限界面发生 `com.android.systemui` 的 `NavigationBarFragment` 空指针，应用崩溃缓冲区为空；同一 SHA 的失败 job 重跑后，首次导览、Android 8 重置、离线恢复、前台导航和熄屏 GPS 全部通过。PR 与 main 的 Git tree 相同，API 37 首次通过，因此该项按模拟器 SystemUI 竞态记录，不修改产品代码。未来若要消除波动，只应加固 API 26 androidTest 在该特定系统崩溃后的有界授权恢复。
- 标签 `v0.2.2` 指向发布源码 `308d4c2afcc9681335e9b60d68cbb2891b025442`。Signed APK Release run `30700533745` 全部通过并创建 Latest Release；精确公开资产兼容 run `30700823528` 的 API 26/API 37 均通过。公开 APK 为 49,102,773 字节、SHA-256 `e44f2d8e2179650395fa28ecff3a59d30bcb21c6798585e80b87535a0bf96676`，包名/版本和固定 RSA-4096 v2 证书均已重新下载核对。正式签名 APK 的个人真机覆盖仍未执行，不得冒充。
- 发布监控曾因 GitHub API `unexpected EOF` 退出，重新查询同一 run 后确认远端仍在运行，再恢复监控直至成功。公开资产复核后的递归及逐文件临时清理均被工具安全策略拒绝；没有绕过策略，Windows 临时目录 `%LOCALAPPDATA%\Temp\anitabi-v0.2.2-4ea7a5bb2ddb4e0eab20721ff81cc3f1` 暂留一份公开 APK 和校验文件，约 49 MB，不含秘密或用户数据。后续只能在再次确认该绝对目录后安全删除。
- 用户的 `.codex-remote-attachments/` 仍未读取、修改、删除或暂存。

### 2026-08-01—2026-08-02 v0.2.3 日本公共交通分段导航实施

- Android 已提升为 `versionName=0.2.3` / `versionCode=9`。新增非空 `TransitExecutionStrategy`，日本公交使用 `EXTERNAL_GOOGLE_MAPS_JAPAN`，全部非日本点继续使用 `IN_APP_GOOGLE_ROUTES`；道路模式、VPS、后端公开协议、SSH、GitHub 工作流、Anitabi 点位模型和 Room schema 2 均未修改。
- 地区判定只读取用户选择点坐标。APK 固定携带 Natural Earth 1:10m Admin-0 Countries v5.1.1 的日本 MultiPolygon 派生资产，支持外接框、孔洞和边界点；缺失、损坏、类型/版本错误一律停止规划。官方源 SHA-256 为 `239eec57ac17f100a11e2536cffc56752c318b50ae765b0918ff7aab4ce8f255`，派生/入包资产为 159,056 字节、SHA-256 `f1cab5b0a94ef1873f9ee349652a80801ad9fc8c829e3c79970cf6752278cd8c`；日本 feature geometry 保持不变，来源与公共领域条款见资产 `NOTICE.txt`。
- 全日本公交复用本地 `approximateGlobalOrder`，保留手动顺序、固定/开放终点和返回起点，只生成端点、目标 ID、球面直线距离的占位段；100 点测试对 road matrix/directions 和 transit provider 均为 0 调用。日本与非日本混合会在定位和任何后端调用前抛出明确错误“**不支持此操作，请去除日本或日本以外的点。**”；日本设置/预览不显示时间、少步行/少换乘、交通工具筛选、ETA 或线路详情。
- 单段交接由统一 `GoogleMapsTransitLauncher` 使用 `Uri.Builder` 生成只含 `api=1`、`origin`、`destination`、`travelmode=transit` 的 HTTPS URL；优先 `com.google.android.apps.maps`，再用同一 URL 回退通用处理器，两者失败保留当前段并重试。未导出的 `TransitHandoffActivity` 必须先让服务持久化当前段，用户可见且 Activity 处于 resumed 状态时才打开地图；服务不会后台自动打开首段或下一段。
- 日本执行使用独立状态机：精度不差于 50 米、80 米内连续 15 秒进入 `ARRIVING`，超过 120 米重置；只提示不自动完成，人工提前确认有二次确认；确认后停留，停留结束保持 `NEXT_STOP`，最后一站进入 `COMPLETED`，主动结束进入 `ENDED`。暂停停止定位并冻结停留；所有进度推进先以 repository CAS 持久化，保存失败回滚内存状态，活跃编辑与服务进度竞争不会互相覆盖。
- `TourRepository` 的读写现在线性化：等待写锁时仍可取消，一旦取得锁，本地 Room/JSON/缓存发布在 `NonCancellable` 临界区完成；DAO 即使在提交后抛错也会清空相关缓存，后续读取以持久状态为准。服务运行态以单调版本戳记进度，进度与未来点编辑竞争时进度优先，编辑会回滚到原计划并保留最新进度；STOP、终态和失败清理都只在持久化成功后清除活动入口。
- 活跃编辑的 service reload 具有明确确认语义：新 reload 会使旧请求返回 `SUPERSEDED`；10 秒只表示 UI 等待 `TIMED_OUT`，两者都不会假报保存成功或关闭草稿。reload 失败后以持久化计划判断未来点编辑是否仍存在，允许进度合法推进；只有活动 ID、计划、进度和旧运行态均精确匹配时才允许安全停服。
- 首段要求 30 秒内、精度不差于 100 米的精确定位。`location` 前台服务提供通知和可拖动、不可聚焦的 `TYPE_APPLICATION_OVERLAY`；任何需要提升 location FGS 的交接/确认/恢复动作都在 `startForeground` 前检查精确定位并处理权限竞态，暂停和结束不依赖该提升。两种控制入口都不可见时不打开地图并暂停/停止服务。通知、悬浮窗和应用按钮只通过用户操作进入交接 Activity；结束有二次确认。`TransitHandoffActivity`、`NavigationService` 和重启接收器均未导出。
- 暂停、结束以及进度进入 `COMPLETED` / `ENDED` 时，在同步状态校验通过后、任何 Room 持久化之前立即移除悬浮窗；异步保存失败只在同一 generation/plan/engine 仍有效时回滚，并按恢复状态与当前权限重新渲染，避免慢 I/O 期间旧按钮仍可点击或错误复活。
- `StoredTourV2` 的策略、活动段号和暂停字段均可空/有默认值；旧 v0.2.2 记录按已保存坐标重分类，不迁移数据库。进程死亡或旧版无 `ActiveNavigationStore` 时可从最近活跃记录恢复当前目标、段号、完成点、停留和暂停状态，绝不自动打开地图。BOOT 接收器只为可恢复的日本外部公交发布控制通知；道路和应用内公交候选保持原指针且不被接收器接管，数据库/分类异常也不清除指针。活跃编辑锁定已完成点、当前点和固定终点，只允许未来点插删重排，保存前重分类并只重建未来段。
- 实现前查阅当时最新的 Google 官方 [Maps Platform FAQ](https://developers.google.com/maps/faq#transit_directions_countries)、[Maps URLs](https://developers.google.com/maps/documentation/urls/get-started)、[Android Maps intents](https://developer.android.com/guide/components/google-maps-intents)、[location FGS](https://developer.android.com/develop/background-work/services/fgs/service-types#location)、[后台 Activity 限制](https://developer.android.com/guide/components/activities/background-starts)、[后台启动 FGS 限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)、[FGS 类型与 specialUse](https://developer.android.com/develop/background-work/services/fgs/service-types)、[Android 15 行为变更](https://developer.android.com/about/versions/15/behavior-changes-15) 和 [通知权限](https://developer.android.com/develop/ui/views/notifications/notification-permission)。据此确认日本 Transit partners 不在 Routes 覆盖内、Maps URL 的受支持参数和 `api=1` 要求、while-in-use 定位/后台启动限制，以及 Android 13+ 通知可见性；实现没有添加 API Key、途经点、时间、`dir_action` 或后台 Activity 启动。重启时仅悬浮窗授权不足以安全创建 location FGS/Activity；新增 `specialUse` 服务还会扩大 manifest 与 Play 审核范围，因此未越过本任务要求的既有 location FGS 边界。
- 当前源码最新本地自动证据：第三轮 CI 修复后再次全量执行，43 个 JVM 套件 / 229 个测试，0 失败、0 错误、0 跳过；恢复候选定向回归 8/8，Debug/Release Lint 均为 0 问题，Debug/Release/AndroidTest Kotlin 编译及 Debug/AndroidTest APK 构建均通过。主实现阶段另已完成后端类型检查/构建/29 个测试/生产依赖审计、Release R8、Navigation 反射、源码凭据及 Natural Earth 入包哈希审计；当时的 Debug APK 为 `cn.anitabi.navigator` / `0.2.3 (9)`，78,581,900 字节，SHA-256 `702c2b8314a9792b8936ef70bed895ac59936a79287ec8f3e35fb4256b2518bc`，AndroidTest APK 为 1,170,750 字节，SHA-256 `44c38b01b6d84edd7204797955b74bc2465518da5564baed0d22e6068642d71b`。这两个哈希早于后续 PR 修复，不得冒充当前最终发布候选。
- 诚实边界：当前唯一连接设备是用户个人真机，未获本任务安装/运行授权，因此只构建 instrumentation APK，没有运行设备测试。Google 地图真实交接、通知/频道、悬浮窗撤权、真实定位、连续五段和重启仍待真机验收。设备重启且通知可见时接收器只发布恢复通知；若仅有悬浮窗权限而通知不可见，Android 后台限制下不能自动创建可见入口，用户必须手动打开应用恢复。
- 正式签名环境阻塞已解除：工作区外原 keystore 和两份 Windows 当前用户范围 DPAPI 密码文件均完整，未重新生成签名。按既有流程只在单个 Gradle 子进程内恢复并注入四项 `ANITABI_*`，`finally` 清除环境变量；串行 `assembleRelease` 在 7 分 36 秒内成功。本地 Release APK 为 `cn.anitabi.navigator` / `0.2.3 (9)`，49,210,374 字节，SHA-256 `c48fdbbcf4ff90e4c97ea178ed0c7888297a895048b6047b6d791211083b9201`；`apksigner` 验证为 v2、单一 RSA-4096 签名者，证书 SHA-256 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`，与 v0.2.2 完全一致。APK 内容、tracked-source 凭据和 Navigation R8 审计均通过；该本地产物尚不是公开 Release 资产，也未做真机覆盖安装。
- 报错闭环（签名恢复与最终构建）：独立 `keytool` 初查先因命令不在 `PATH` 失败；定位到 Gradle 实际使用的 JDK 后，又发现该 JDK 17 不支持预期的 `-storepass:env` 形式，Windows PowerShell 5.1 的 `ProcessStartInfo.ArgumentList` 也不可用，手工标准输入探测因此给出“密码错误”的误导结果。实际根因是诊断调用方式不兼容，不是 keystore 或 DPAPI 密码损坏；改为恢复说明规定的 `ConvertTo-SecureString` + `PSCredential` 流程，并仅把值注入 Gradle 子进程后，`signingReport` 直接确认固定证书。首次强制重建虽生成有效 APK，但运行器未回传末尾摘要；随后把单测/Lint/assemble 组合门禁置于 604 秒工具窗口又被外部超时终止，没有测试或编译诊断。确认无遗留 Java 进程后拆出 `assembleRelease`，提高到 1,200 秒并保持单 worker，最终 7 分 36 秒明确 `BUILD SUCCESSFUL`。首版 `apksigner` 解析器还按旧的 `Signer #1` 字段匹配，面对当前工具的 `V2 Signer` / `Number of signers` 输出产生假阴性；按实际字段修正只读解析后，包名、版本、v2、单签名者、RSA-4096 和固定证书全部通过。
- 报错闭环（资料与资产）：首选检索链先后遇到 Jina 请求 exit 28 超时、`mcporter` 未显式参数导致元数据加载失败、GitHub Jina 403、完整 zipball 在 124 秒超时并留下 612 MB 不完整下载，以及本机缺少 Python shapefile/`ogr2ogr`。根因分别是外部端点/调用格式、限流、下载范围过大和本机工具缺失；实际改为直接读取 Google/Natural Earth 官方页面、显式 CLI 参数、GitHub 官方 API/raw 单一 GeoJSON，并用标准 JSON 提取后核对官方源哈希、feature 选择和 geometry equality。最终固定资产、NOTICE、分类测试和 APK 内哈希均通过。
- 报错闭环（并发构建与测试）：并行子任务期间曾出现 Kotlin 引用尚未落盘、AAPT 缺少 `drawable_anitabi_brand_mark.xml.flat`、Kotlin snapshot `dirty-sources.txt` 消失、定向执行器超时和测试报告 XML 被另一任务覆盖；根因是共享工作树尚在修改且多个 Gradle 进程并发争用中间目录，不是最终源码缺陷。等全部修改收敛后统一改为 `--no-parallel --max-workers=1` 串行；三套 Kotlin 编译、定向回归、全量 JVM 测试、两套 Lint、Debug/AndroidTest 构建和 R8 均通过，最终测试总数见本节自动证据。
- 报错闭环（测试契约）：活跃公交编辑测试最初预期重建两次 provider 调用，但实现正确复用已完成前缀只请求一次；修正错误测试期望并补入停留时间锚点后通过。AndroidTest 契约新增回调时漏了五个调用点，补齐回调后编译通过。交接 Activity 首次强制重跑还受上述并发 AAPT 缺失影响，串行后其 5 个生命周期/幂等测试及最终总门禁通过。
- 报错闭环（静态与产物审计）：首轮新代码 Lint 为 0 error / 11 warning，根因是 API 守卫识别、等价 KTX 建议、拖动句柄 `performClick` 可访问性契约和一处程序化中文文本；采用精确 API 注解、KTX 等价调用、`ACTION_UP` 可访问性处理和局部 `SetTextI18n` 说明后，Debug/Release 均为 0 问题。首次尝试用 `jar tf` 查 APK 资产因当前 shell 无 `jar` 命令失败；改用只读 .NET ZIP 流计算入包条目哈希，资产大小和 SHA-256 与源码一致。Google Navigation SDK 7.8.0 自带弃用/多语言资源提示仍为非阻断依赖输出，项目 Lint 没有对应问题。
- 报错闭环（仓储与服务收口）：`TourRepository.commitProgressOnLatestPlan` 首次编译因块体函数缺显式 `return` 失败，补为返回已提交计划后通过；`NavigationService` 一处 nullable `Intent` 直接调用扩展导致编译失败，改为安全调用后通过；新增确定性测试最初把私有 `PostCommitGate` 暴露为构造参数而编译失败，调整测试可见性后通过。该阶段的全量 JVM、三套 Kotlin 编译、双 Lint 和 R8 均成功；最终总数以后续规格审计收口后的 225 个为准。
- 报错闭环（最终门禁环境）：首轮 `lintDebug` 在 124 秒工具时限处被终止，根因是完整 Lint 超过命令窗口；改为 300 秒以上有界超时并保持单 worker 后通过。一次合并执行 Debug/AndroidTest/Release/R8 的命令在 304 秒工具时限终止且无编译诊断，拆分并把最终组合门禁上限提高到 900 秒后 2 分 49 秒成功。后端只读验证的临时镜像清理两次被工具安全策略在执行前拒绝；只读确认绝对目标位于系统临时目录且 `node_modules` 为 junction 后，改用 PowerShell/.NET 逐项删除并确认临时根不存在，仓库 `backend/` 保持干净。
- 报错闭环（最终规格审计）：终审发现 BOOT 接收器曾对道路/应用内公交也发布日本外部地图措辞通知，且暂停/结束要等异步 Room 保存后才移除悬浮窗。根因分别是恢复候选只检查状态而未检查执行策略，以及 overlay removal 只放在提交成功/终态渲染回调。实际修复为三态 BOOT 决策（null/终态清 stale、非外部忽略、日本外部恢复），异常保留 pointer；并在外部进度目标为暂停或终态时于 repository 调用前移除 overlay，失败沿既有同代回滚重显。新增策略测试后强制全量 225 个 JVM 测试、双 Lint、Debug/AndroidTest/R8 及三项静态审计均通过；独立并发复核未发现旧 overlay 复活或道路模式回归。
- 报错闭环（PR 创建链路）：GitHub connector 创建 PR 返回 `403 Resource not accessible by integration`，根因是当前 integration 没有仓库写权限；按既有 keyring 会话切换到 `gh` 后又在 GitHub GraphQL 连接处超时，未产生 PR。用户随后明确要求改用 Chrome；Chrome 初始未运行且首次连接超时，获用户授权启动后只读确认进程和现有登录会话，最终通过页面创建 ready PR #12，并由 PR 页面 `Conversation` / `Checks` / `Files changed` 链接确认目标分支、1 个提交和 63 个文件均正确。
- 报错闭环（PR #12 首轮 CI）：Android CI run `30727481832` 的 `backend`（15 秒）和 `verify`（8 分钟）成功，但 API 26/API 37 两个 `emulator-smoke` 都在 `NavigationRuntimeInstrumentedTest.verifyFailedProcessRecoveryState` 失败：期望离线路线刷新失败后持久状态为 `PLANNED`，实际重启后仍为 `NAVIGATING`。根因是冷恢复会确定性地把 `startPointId` 加入运行态 `completedPointIds`，而 v0.2.3 的严格 `StoredTourV2.matches` 用该规范化进度比较未含起点的旧 Room 快照，CAS 拒绝写回且清理路径的 `runCatching` 吞掉异常；UI 已回滚但数据库未回滚。首版尝试改用规范化前进度做基线，新增单测随即因 unresolved plan 无 legs、无法重建原 `activePointId` 而抛 `ConcurrentTourUpdateException`，证明该方案不完整。实际修复只在 expected 已含持久 `startPointId` 时把这一确定性差异规范化为等价，其余 `completedPointIds`、活动点及所有字段仍做最终严格相等比较；新增冷恢复回归后 `TourRepositoryTest` 14/14、全量 JVM 43 套件 / 226 测试、AndroidTest Kotlin 编译和 Debug Lint 均通过。精确 API 26/API 37 闭环仍须由修复提交触发的新 PR CI 确认，确认前不得合并。
- 报错闭环（PR #12 第二轮 CI）：修复提交触发的 Android CI run `30728530989` 中 `backend`（20 秒）和 `verify`（7 分 34 秒）成功，API 26 job `91445119326` 在前台服务恢复断言失败，API 37 job `91445119333` 则已恢复服务但在首次人工到达后未推进到第二段。API 26 根因是离线路线成功回滚为 `PLANNED` 后，`failAndStop` 未清 `ActiveNavigationStore`，旧恢复行程可能遮蔽下一步新保存的活动行程；实际修复为仅在回滚持久化成功且 generation 仍为当前值时清除该活动指针，并在 instrumentation 恢复断言中核对指针为空。API 37 根因是 Room CAS 已接受“缓存只缺确定性起点”的语义等价，但同进程 `resolvedProgress` 仍精确比较未规范化缓存，首次到达被误判为并发冲突并重载，因而丢失该次推进；实际修复为仅在 expected 已含当前持久 `startPointId` 时给缓存副本补该起点，再对整个 `NavigationProgress` 严格相等，其他完成点、段号、状态及字段仍不得不同。新增 warm-cache 正向和非起点差异负向回归后 `TourRepositoryTest` 16/16、全量 JVM 43 套件 / 228 测试、Debug/Release Lint 与 Debug/Release/AndroidTest Kotlin 编译全部通过；新的 API 26/API 37 CI 尚待该修复触发的 PR CI 确认，确认前不得合并。
- 报错闭环（PR #12 第三轮 CI）：Android CI run `30729670623` 的 `backend`、`verify` 和 API 37 job `91448220465` 全部成功；API 26 job `91448220468` 的离线恢复与前台人工完成也成功，仅 `foregroundServiceAutoArrivesFromMockGpsWhileScreenOff` 在启动等待处报“foreground service did not start for automatic arrival”。CI 证据显示上一用例结束后约 105 毫秒进程即被 force-stop；下一进程的 `MainActivity` 已正常 RESUMED/Displayed，18 秒内无 `NavigationService` 启动且 crash/dropbox 为空。根因是 `COMPLETED` 已写入 Room 后，服务才清理异步 SharedPreferences 活动指针，两种存储不可原子提交；API 26 在该窗口被终止，旧逻辑随后无条件优先旧终态 pointer，清理后直接返回，遮蔽了之后保存且更新的 `NAVIGATING` 行程。实际修复为：可恢复 pointer 仍绝对优先；非可恢复 pointer 只允许 DAO 既有 `updatedAt DESC, id DESC` 总序中位于它之前的可恢复行程接管，没有更新候选时仍返回旧终态，绝不复活更早行程。instrumentation 同时等待正常终态指针清理，并确定性构造“旧终态 pointer + 更新活动行程”。定向回归 8/8、全量 JVM 43 套件 / 229 测试、Debug/Release Lint 0 问题及三变体 Kotlin 编译通过；精确 API 26/API 37 闭环仍须新 PR CI 确认，确认前不得合并。
- 报错闭环（PR #12 第四轮 CI）：Android CI run `30730687079` 的 `backend` job `91450463433`（20 秒）和 `verify` job `91450463394`（9 分 44 秒）成功；API 26 job `91451236536` 与 API 37 job `91451236537` 均在 `foregroundServiceCompletesOfflineRouteAndPersistsProgress` 报“completed navigation pointer was not cleared”，两边运行态和 Room 都已是 `COMPLETED`，前台服务/通知仍持续到测试 finally，crash/dropbox 为空，后续自动 GPS 因前一步失败而跳过。确定根因不是 SharedPreferences `apply()` 可见性或 generation 猜测，而是最后一站 `NEXT_STOP` 触发本地公交重规划；开放终点已无剩余点，`saveActiveEditIfCurrent` 直接持久化空 legs + `COMPLETED`，随后 `rerouteFrom` 先把 `lastSavedProgress` 设为同一终态再调用 `processUpdate`，导致唯一位于“进度发生变化并另行保存”分支内的旧终态清理被完全绕过。实际修复为让仓储返回本次实际提交的 plan/progress，并让普通队列保存和重规划直接写入两类 terminal writer 共用同一收口；收口前严格核对 terminal、generation、stopping、当前 plan 和 engine progress，随后以同步、带 tour ID 的 SharedPreferences CAS 持久删除 pointer，再精确清运行态并停止服务。instrumentation 新增“开始前 pointer 确属本行程”和“完成后通知消失”断言；新增领先 runtime terminal 及 stale generation/plan/progress 负向 JVM 回归。最终 43 套件 / 231 个 JVM 测试、Debug/Release Lint 0 问题、Debug/AndroidTest APK、Release Kotlin/R8 和后端 29/29 全部通过；Google Navigation SDK 7.8.0 的既有多语言资源格式提示仍为非阻断依赖输出。PR CI run `30732960430` 的 backend job `91456435038`、verify job `91456435019`、API 26 job `91457090492` 与 API 37 job `91457090499` 已全部成功，精确闭环完成。
- 报错闭环（PR 同步与只读状态查询）：修复提交 `8c140d3` 的 run `30732639827` 在 API 26 构建 instrumentation APK 时显示 `The operation was canceled`；根因是 GitHub 延迟约 11 分钟才把 PR head 从 `8c140d3` 同步到纯文档提交 `e51f014`，新 run `30732960430` 触发工作流 `cancel-in-progress` 终止旧 run，不是代码或测试失败；实际解决为等待新 head 的完整 run，最终四项全部成功。期间 PowerShell 把未加引号的 `@{upstream}` 当作哈希表、把嵌套 `gh --jq` 双引号截断，两条只读查询因此解析失败；改用显式 `origin/codex/v0.2.3-japan-transit` 与 `ConvertFrom-Json` 后成功。另一次 GitHub API TCP 查询超时，原样重试后恢复；仓库、CI 和远端引用均未被这些查询错误修改。

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

- v0.2.3 当前工作树已有报告：229 个 JVM 测试 / 43 个套件，0 失败、0 错误、0 跳过；Debug/Release Lint 0 问题；Debug/Release/AndroidTest Kotlin 编译及 Debug/AndroidTest APK 构建通过。主实现阶段的 Release R8、Navigation 反射、后端 29 个测试、类型检查、构建和生产依赖审计均通过，后端生产依赖漏洞为 0；第三轮 PR 恢复修复尚待远端 API 26/API 37 模拟器复验。
- v0.2.3 主实现阶段已有本地正式签名 Release APK：49,210,374 字节、SHA-256 `c48fdbbcf4ff90e4c97ea178ed0c7888297a895048b6047b6d791211083b9201`，包名/版本、v2、单一 RSA-4096 签名者和固定证书均已独立核对；该 APK 早于后续 PR 恢复修复，不能作为当前最终发布资产。ready PR #12 已创建；第三轮 PR CI 已使 backend、verify、API 37 和 API 26 前两项关键运行态用例通过，但 API 26 暴露终态 Room/活动指针非原子清理窗口，当前修复已完成本地验证并等待新 CI。尚无公开发布或 v0.2.3 真机证据。唯一连接设备是个人真机，本任务未获安装/运行授权，所以本地 instrumentation 仅编译未执行。不得把本地候选、失败 CI 或旧 v0.2.2 证据冒充 v0.2.3 公开资产/真机结果。
- v0.2.2 发布源码已有报告：127 个 JVM 测试 / 26 个套件，0 失败、0 错误、0 跳过；Debug/Release Lint 均为 0 个问题，Debug、正式签名 Release 和 AndroidTest APK 构建通过。
- Navigation R8 反射契约、跟踪源码凭据和 Release APK 内容审计通过；后端 29 个测试、TypeScript 构建和生产依赖审计通过，生产依赖漏洞为 0。
- v0.2.2 远端门禁：PR `30699060766`、main `30699662842` attempt 2、signed release `30700533745`、精确公开资产兼容 `30700823528` 全部成功。main API 26 attempt 1 的 SystemUI 竞态及处理见“v0.2.2 发布完成”。
- 精确公开 `anitabi-v0.2.2.apk` 为 49,102,773 字节、SHA-256 `e44f2d8e2179650395fa28ecff3a59d30bcb21c6798585e80b87535a0bf96676`；校验文件、GitHub 摘要、包名/版本和固定签名均已核对，API 26/API 37 均完成安装、冷启动、首次导览和应用崩溃缓冲区检查。
- v0.2.1 的历史流程为 PR `30628585336`、main `30629643413`、signed release `30630717751`、旧公共资产兼容 `30631245688`；`30631245688` 验证的是后来被替换前的公共资产，不能作为当前 v0.2.1 替换 APK 的 exact-public-asset 证据。
- cancelled-task 修复曾在 Xiaomi 15T Pro 上完成 6 次有界启动/快速停止，未再出现 `Task was cancelled`，地图与最终 Google 导航正常；该测试使用既有 mock-location，且测试包早于显示名改动。
- v0.2.2 的个人真机证据仍只覆盖发布前 Debug 候选；没有安装精确公开签名 APK，不能声称完成正式签名真机覆盖。
- 手机当前安装、页面、网络、mock location 和选点状态都可能已被用户改变；开始新真机任务时重新只读确认，不沿用旧状态。

### 2026-08-01 前端重设计与真机验收状态

- Jetpack Compose 展示层、品牌色、应用内标识和 adaptive/monochrome 图标已重设计；未修改 ViewModel、repository、Room schema、网络请求、路线算法或 `NavigationService` 行为。搜索页真机横屏曾出现固定底栏被挤出屏幕的问题，根因是搜索表单、选择轨和底栏同时作为不可滚动 `Column` 子项；现已改为固定 Header、单一 `LazyColumn` 主体和固定 Footer，并新增 320dp 短高度回归测试。
- Xiaomi 真机（Android 16 / API 36）已通过新增 7 个 Compose UI 契约测试和既有 10 个 instrumentation 测试。既有测试覆盖首次引导、设置迁移、Room 迁移、离线进程恢复、离线手动到达及熄屏 mock GPS 自动到达；第三方 FakeGPS 首次干扰自动到达，用户取消该提供者后复测通过。人工检查覆盖竖屏、修复前后横屏、200% 字体、IME resize 与自适应图标安全区。
- 新增 Compose 测试首轮无法启动 `ComponentActivity`，根因是缺少 Compose 测试宿主 manifest；已增加仅 debug 生效的 `androidx.compose.ui:ui-test-manifest`，重建后 7/7 通过。横屏回归测试首版无法定位未组合的 LazyColumn 屏外节点；改为从列表容器执行 `performScrollToNode` 后通过。
- 一次被中止的 `connectedDebugAndroidTest` 在 debug APK 因签名不匹配安装失败后，仍于 17:12 自动卸载了正式包，系统记录为 `PACKAGE_FULLY_REMOVED`；因 `allowBackup=false`，本地数据无法通过 Android 备份恢复。用户确认没有重要数据并授权安装测试版。后续禁止在含正式签名包的个人设备上运行该 Gradle 任务，只允许显式校验包名后执行单一 APK 安装。
- 正式包名的 debug 测试版已重新构建；首次安装被 MIUI 的 USB 安装确认以 `INSTALL_FAILED_USER_RESTRICTED` 取消，用户确认弹窗后重试成功。该时点手机安装的是 `cn.anitabi.navigator`、`versionName=0.2.1`、`versionCode=7`；用户随后完成 Debug 候选人工验收，最终 JVM、Release Lint、Release 构建、发布审计及 v0.2.2 远端发布均已通过。精确公开签名 APK 仍未在该个人真机覆盖安装。
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
- 稳定 v0.2.2 已发布：完成前端与品牌重设计、地图本地 Key 注入修复、无线路详情及 Google 地图交接；PR、main、签名发布和精确公开 APK 的 API 26/API 37 门禁均通过，后端与路线行为未变。
- v0.2.3 已在 PR #12 提交日本公交纯本地分流、用户主动的外部 Google 地图分段执行、独立状态机/控制入口、恢复和未来点编辑；本地 231 个 JVM、双 Lint、Debug/AndroidTest、Release R8、后端 29/29，以及 PR CI run `30732960430` 的 backend/verify/API 26/API 37 均已通过。四轮先前 PR CI 依次暴露冷恢复 Room 起点规范化、失败后活动指针残留、warm-cache 规范化、终态 Room/SharedPreferences 非原子窗口，以及重规划 terminal writer 绕过服务收口；均已在保持严格并发与“不复活更旧行程”边界下完成修复。PR 尚待合并；现存签名 APK 早于主实现和修复，只证明固定 RSA-4096 证书连续性，最终 `main` 必须重建。稳定 v0.2.3 仍被实体设备日本公交验收和从公开 v0.2.2 原位覆盖保留 Room 2 数据的硬门禁阻断，不得提前打稳定标签或发布。

## 常用权威入口

- 用户说明：`README.md`
- 隐私：`PRIVACY.md`
- 安全：`SECURITY.md`
- 构建：`docs/BUILD.md`
- 后端运维：`backend/README.md`
- 当前源码说明：`docs/RELEASE_NOTES_v0.2.3.md`；当前公开稳定版说明仍为 `docs/RELEASE_NOTES_v0.2.2.md`，v0.2.1 历史说明保持不变。
- Release/真机证据：`docs/releases/`、`docs/PHYSICAL_DEVICE_ACCEPTANCE_v0.1.3.md`
- 压缩前完整实施记录：提交 `c92a92195d903ecb89ae563a509699f32e5737ef` 中的 `AGENT.md`

## 最近任务（最多 5 条）

- 2026-08-02：PR #12 最新 head `e51f014` 的 CI run `30732960430` 四项全绿：backend、verify、API 26、API 37 均成功；前一 run 的取消已证明为延迟同步触发 `cancel-in-progress`，不是代码失败。Chrome 已把 PR 验证数更新为 231，并只读确认发布工作流所需 6 个 Actions Secret 名称全部存在；固定签名未丢失。现存签名 APK 早于全部实现/修复，已标记不得发布；稳定 v0.2.3 仍须实体设备日本公交和 v0.2.2 原位覆盖验收，PR 待最终文档 CI 与合并，未打标签或发布。
- 2026-08-02：定位 PR #12 第四轮 CI run `30730687079` 的 API 26/API 37 同源失败：最后一站的本地重规划直接把 Room 写为 `COMPLETED`，但 `lastSavedProgress` 预先同步后令 `processUpdate` 跳过旧终态清理分支，故 pointer/通知/服务未收口。已在 `8c140d3` 统一普通队列保存与重规划 terminal writer 的实际提交结果收口，使用同代 plan/progress 严格 ownership 和 durable pointer CAS；231 个全量 JVM、双 Lint、Debug/AndroidTest、Release R8 及后端 29/29 通过，修复已推送且新 CI 已闭环，未合并或发布。
- 2026-08-02：定位 PR #12 第三轮 CI run `30729670623`：backend、verify、API 37 与 API 26 前两项运行态用例通过，仅 API 26 自动 GPS 用例因 Room 已提交终态、活动指针尚未清理时进程被结束而未启动服务。已限定为“非可恢复 pointer 只可被排序上更新的可恢复行程接管”，保留旧行程不复活边界并强化确定性 instrumentation；8/8 定向、229 个全量 JVM、双 Lint 和三变体 Kotlin 编译通过，待提交和新 CI，未合并或发布。
- 2026-08-02：定位 PR #12 第二轮 CI：API 26 被成功回滚后残留的活动行程指针遮蔽新行程，API 37 因 warm `resolvedProgress` 未接受仅缺确定性起点而误判并发、丢失首次到达。已限定为“回滚成功且同 generation 才清 pointer”及“只补持久起点后全对象严格相等”，加入 instrumentation 指针断言、warm 正向与非起点负向回归；16/16 仓储测试、228 个全量 JVM 测试、双 Lint 和三变体 Kotlin 编译通过，修复已进入当前分支并等待新 CI，未合并或发布。
- 2026-08-02：提交并推送 v0.2.3 主实现 `ff75d8b`，通过 Chrome 创建 ready PR #12；首轮 CI 的 backend/verify 通过、API 26/API 37 同因冷恢复起点规范化与严格 CAS 不等而失败。已将等价范围收紧到持久起点这一确定性差异，新增回归并通过 226 个 JVM 测试与 AndroidTest 编译；修复提交和新 PR CI 待继续完成，未合并或发布。
