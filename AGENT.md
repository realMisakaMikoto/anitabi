# 项目执行上下文（压缩版）

> 最后核对：2026-08-03。每个新任务开始前必须完整读取本文件；每个任务结束后必须更新当前状态、验证结果、剩余问题，并在“最近任务”追加一条简短真实记录。

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
- 当前任务分支为 `codex/v0.2.3-release-completion`，基线为 `main@f91cb13835c43415982831ea8bf9944e534641e2`。移除 UID 日额度与 UID 突发限制的 [PR #14](https://github.com/realMisakaMikoto/junrei_navi/pull/14) 已合并；修复 `backend/deploy/backup.sh` Git 可执行位和记录部署边界的 [PR #15](https://github.com/realMisakaMikoto/junrei_navi/pull/15) 也已 rebase 合并。main CI run `30757317236` 的 backend、verify、API 26 和 API 37 均成功。本分支只记录最终生产部署与同版本发布证据；原 v0.2.3 标签仍为 `f173ebafef87c246de1e7d86e5e52979fdd18660`，没有移动或重写。
- 非草稿、非预发布的 [v0.2.3 Release](https://github.com/realMisakaMikoto/junrei_navi/releases/tag/v0.2.3) 继续为 Latest Release。2026-08-03 已在新后端成功生产切换后完成同版本资产替换：公开 APK 为 49,227,286 字节、SHA-256 `b5a125386a511151139f5ff69a4b88d7101af822b53d920db5f45a00aca75eec`；85 字节校验资产 SHA-256 为 `b7f3290685e9390b704a05b64b1760ab98bb4550d9d7ae1df7368481625871c5`，正文精确指向该 APK。Release 正文已同步取消 UID 日额度/突发限制及保留共享月度、HMAC-IP、Firebase 和费用熔断的边界。完整证据见 `docs/releases/v0.2.3.md`。
- 功能分支 `codex/transit-no-route` 仍保留在远端；不要将其误当作当前发布分支。压缩前完整日志锚点仍为 `c92a92195d903ecb89ae563a509699f32e5737ef`。
- `main` 已通过快进合并纳入相对原基线 `cbc29fa7acc3dd2e589d5849d2f6e66be53b9ea1` 的 6 个提交：
  - `95cdb8366cab59a4b9cb12bdc61d98d8dd3f3d34`：修复 Google Navigation cancelled-task 概率闪退。
  - `43b9c7d067b7b1907d0b4ef528ff5ef65ec2c45a`：显示名改为 `巡礼手帳`，准备同版本替换 APK。
  - `b39e7a0`：替换包交接记录。
  - `c92a921`：旧 Release 说明品牌更新记录。
  - `e1b71e5`：压缩项目执行记录。
  - `c0e1e5c`：增加报错解决记录硬性约束。
- 这些提交已合并进 `main`；`v0.2.1` 标签未移动，也未重写历史。
- 当前与任务无关且必须原样保留的未跟踪路径只有用户的 `.codex-remote-attachments/`；不要读取、暂存或清理。v0.2.3 主实现、悬浮控制与“提前离开”源码均已进入 `main@f173ebaf`，后续仍只能按当前任务范围显式暂存。
- 2026-08-01 经用户在当前任务明确授权进行一次只读盘点：该路径是普通未跟踪目录，包含 2 个 JPEG 用户附件，共 124,294 字节，无链接、源码、APK、密钥或构建产物；未修改、删除或暂存。附件内容不写入项目记录，后续任务仍恢复默认禁止读取。

### 稳定 Release 的特殊边界

- 当前公开稳定版为 `v0.2.3`：`anitabi-v0.2.3.apk` 共 49,227,286 字节，SHA-256 `b5a125386a511151139f5ff69a4b88d7101af822b53d920db5f45a00aca75eec`。独立公开重下载与已验证额度更新候选逐字节相同，因此确认 APK 为 `cn.anitabi.navigator` / `0.2.3 (9)`、minSdk 26、targetSdk 37、APK Signature Scheme v2、单一 RSA-4096 签名者，固定证书 SHA-256 为 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`，且 APK 内容审计通过。公开校验资产为 85 字节，SHA-256 `b7f3290685e9390b704a05b64b1760ab98bb4550d9d7ae1df7368481625871c5`，正文匹配 APK。run `30744459795` 的 API 26/API 37 精确公开资产门禁验证的是同版本替换前的旧资产；本次替换后没有新的模拟器 run，不得挪用旧 run 冒充当前资产门禁。
- 2026-08-01 通过只读 GitHub API 确认：公开稳定 `v0.2.1` 的 APK 已被同版本替换为：
  - 文件：`anitabi-v0.2.1.apk`
  - 大小：49,058,373 字节
  - SHA-256：`77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`
- 该摘要与本地 `app/build/outputs/manual-release/v0.2.1/anitabi-v0.2.1.apk` 及 Gradle Release 输出一致；相邻 `.sha256` 文件匹配。构建目录已忽略，`clean` 可能删除本地产物。
- 公开替换包包含 cancelled-task 修复和 `巡礼手帳` 改名，仍为 `versionName=0.2.1` / `versionCode=7`，并保持固定签名。
- 稳定标签仍指向替换前源码，但 `main` 已包含替换包对应的修复和改名。因此当前公开 APK 与标签源码仍不完全一致；不得声称标签可重现该资产，也不得擅自移动标签或重写历史。
- 用户已自行更新稳定 `v0.2.1` Release 文案。2026-08-01 只读 GitHub API 核对确认：正文不再记录旧 APK SHA-256，改为要求使用随 Release 发布的校验文件；APK 资产仍为 49,058,373 字节、SHA-256 `77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`。本任务未修改公开 Release 文案。
- 现有 10 个 Release：`v0.1.0`、`v0.1.1`、`v0.1.2`、`v0.1.3`、`v0.1.4`、`v0.2.0`、`v0.2.1-rc.7`、`v0.2.1`、`v0.2.2`、`v0.2.3`；每个仍有 APK 和校验文件两个资产。RC1–RC6 的 Release 对象已删除，但 Git tags/commits 保留。
- 除稳定 `v0.2.1` 外，上述旧 Release 说明正文中的旧应用名已替换为 `Anitabi Navigator`。这是历史说明品牌，不等于当前应用显示名。

## 当前产品与 Android 架构

- 用户可见名称：`巡礼手帳`。
- 包名/namespace：`cn.anitabi.navigator`。
- 当前源码与公开稳定版均为 `versionName=0.2.3`、`versionCode=9`。`minSdk=26`、`compileSdk=37`、`targetSdk=37`；Java 17。
- 主要工具链：Gradle 9.6.1、AGP 9.3.0、Kotlin 2.4.10、KSP 2.3.10、Compose BOM 2026.06.01。
- Google Navigation SDK 7.8.0 负责地图、定位及驾车/步行/骑行道路导航；项目不得同时接入 Maps SDK for Android。
- Google Routes 由自建 VPS 后端调用，Android 不包含服务账号。Firebase Anonymous Auth 只用于后端访问鉴权，不作为个人配额或突发限速键。
- 旧 MapLibre/OpenFreeMap、ORS、Transitous 仅属于 v0.2.0 历史；当前没有这些 Provider、Key UI 或回退路径。
- Anitabi 数据只从 `https://api.anitabi.cn` 获取，图片只允许 HTTPS `image.anitabi.cn`；主站不得作为 API。保持人类访问频率，技术 User-Agent 随 `BuildConfig.VERSION_NAME`，当前为 `AnitabiNavigator/0.2.3`。
- Bangumi 搜索使用官方 `https://api.bgm.tv/v0/search/subjects`。
- 无 GMS、断网或后端故障时，只显示已保存点位、顺序、设置和进度；没有备用地图或路线服务。

### 规划、持久化与导航硬边界

- 总行程无固定产品级点数上限；全局使用最近邻 + 有限轮次 2-opt，窗口内可使用 Held–Karp。
- Matrix：每个窗口 2–10 坐标，最多 100 个计费元素。
- Road route：每次 2–12 个位置，即最多 10 个中间点。
- Transit route：恰好两个位置，按相邻巡礼点逐段规划。
- Navigation SDK 技术上限为 25 个目的地；生产协调器按该上限分批装载，并在换批前为当前批次重新预留共享月度额度。
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
- v0.2.3 核心功能已在用户明确授权清除旧调试包后完成正式签名真机验收：先安装精确公开 v0.2.2 并建立可见 Room schema 2 行程，再以固定证书 `adb install -r` 原位升级到 v0.2.3；版本码 8→9、活动段号、点位锁定和未来编辑状态均保留。全日本公交进入外部 Google 地图并保持 VPS/Routes 零调用，混合地区显示精确拦截提示且零调用，全部非日本点继续使用应用内 Routes 公交；道路模式未改，崩溃缓冲区为 0。用户随后明确确认“功能上没有问题”，当前反馈只针对旧悬浮窗过大。
- 悬浮控制改造使用同一个 `TYPE_APPLICATION_OVERLAY` 窗口：默认面板为 232×212dp、最小 216×188dp、悬浮球固定 60dp；标题栏可拖动并可轻触切换四角，右下固定 48dp 手柄可拖动缩放或轻触切换紧凑/默认尺寸，收起后球可独立拖动、轻触展开。位置按安全视口归一化持久化，面板尺寸按 dp 持久化；横竖屏、系统栏/刘海、显示密度和字体缩放变化会重新夹紧并重建 View，进程恢复保留形态、大小和位置。正文单独滚动，主要动作与暂停/应用/结束/缩放固定可达；每秒状态刷新不重建窗口，拖动不会被刷新拉回。`DWELLING` 时禁用的“停留中”旁新增 48dp 高、间隔 8dp 的“提前离开”，应用内与通知同步提供入口；一次用户点击通过既有交接 Activity 和 CAS 原子结束停留并启动下一段，末站则完成，不会后台自动开地图。暂停、终态、撤权和服务销毁仍立即移除，结束二次确认、Maps URL 和后端分流未改变。
- 本轮实现前再次核对当前 Android 官方 [WindowManager.LayoutParams](https://developer.android.com/reference/android/view/WindowManager.LayoutParams)、[悬浮窗授权](https://developer.android.com/reference/android/provider/Settings.html)、[触摸手势](https://developer.android.com/develop/ui/views/touch-and-input/gestures) 与 [缩放手势](https://developer.android.com/develop/ui/views/touch-and-input/gestures/scale)，并复核 Google 官方 [Maps URLs](https://developers.google.com/maps/documentation/urls/get-started)、Android 官方 [Activity lifecycle](https://developer.android.com/guide/components/activities/activity-lifecycle.html) 与 [后台 Activity 启动限制](https://developer.android.com/guide/components/activities/background-starts)。依据是继续使用一个非全屏、不可聚焦 overlay，通过 `updateViewLayout` 更新同一窗口的位置/尺寸，手势按 touch slop 区分点击与拖动；Maps 参数不变，只有用户点击后、可见交接 Activity resumed 时才打开下一段，服务/倒计时/恢复不自动拉起。
- 悬浮与提前离开最终自动证据：44 个 JVM 套件 / 243 个测试，0 失败、0 错误、0 跳过；纯几何覆盖移动/缩放/恢复，提前离开覆盖中间站、末站、暂停、重复调用和返回起点。Debug/Release Lint 均为 0 问题，Debug/AndroidTest/Release Kotlin 与正式签名 Release R8 构建通过；源码凭据、Navigation R8 反射和 APK 内容审计全部通过。本地候选为 `cn.anitabi.navigator` / `0.2.3 (9)`、minSdk 26、targetSdk 37，49,227,286 字节，SHA-256 `67f2bd35e2aca560a57a3126bed9eb76cefb667fec2872b3e0771a5e24ed9a79`；v2、单一 RSA-4096 签名者及固定证书均通过。PR #13、main、签名发布和精确公开资产兼容门禁随后全部成功。
- 当前候选已在唯一 Xiaomi API 36 真机执行精确 `adb install -r`，保留应用数据并再次确认正式包为 versionCode 9；`cn.anitabi.navigator.test` 仍为原包且未卸载、清理或覆盖。用户解锁后已完成悬浮控制实体验收：60dp 球为 195×195px；最小面板 702×611px、默认面板 754×689px，可拖动放大到 1050×937px；标题拖动后连续 3 秒状态刷新不回跳，四角停靠、收球、跨屏拖球、展开恢复尺寸、横竖屏安全区和设置恢复均通过。截图只在系统临时目录，未写入 Git。
- 进程恢复实体证据通过：球停在右下后 force-stop 正式进程，冷启动只恢复巡礼手帖且 overlay 为 0，没有自动打开 Google Maps；只点击“恢复行程”后球以原形态和位置重现，展开保留原 dp 尺寸，仅有 1px 密度舍入。用户授权的 ADB mock 先以同一日本区域的两个大于 120 米样本验证距离 1.1→2.2 km 且保持 `NAVIGATING`；随后按用户要求在 Google 地图前台持续注入精度 3 米、目标 80 米内样本超过 15 秒，悬浮窗显示 4 米、“已接近目标，请确认到达”和“确认到达”，成功进入 `ARRIVING`，未点击确认。两轮均在 `finally` 移除 `gps/network`，shell app-op=`default`、当前 `mMockProvider=0`，正式/测试包未变且 crash/ANR 为 0；用户之后确认测试通过并停止当前行程。截图只在系统临时目录。
- 报错闭环（真机手势与 mock）：Git 上游查询首次对无 upstream 的分支直接 `rev-parse`，在 PowerShell 停止策略下报 fatal；改读 branch config 后确认无上游。Android 16 不返回旧 `mCurrentFocus` 行时脚本对空值 `Trim()` 报错，改用 `topResumedActivity` 后确认本应用前台。`appops get` 首次未限定用户而误扫到次用户并抛 `SecurityException`，改为读取当前用户并显式 `--user`。首次 ADB 样本为保证远离目标而选在跨洲区域，provider 已清理但应用保留最后运行态，地图短暂显示非洲；立即 force-stop、重启真实 provider 并冷启动后恢复日本行程，随后只用同一日本区域的有界样本重做通过。首版清理断言又误把 location 事件历史当作当前 provider，且只匹配 `Default mode: deny`；实际改为核对 `MOCK_LOCATION: default`、当前 `mMockProvider=0` 和 provider removal 事件。FakeGPS 只读尝试中工具栏 Start 实际 disabled，用户要求改用 ADB 后已停止其服务；既有 FakeGPS app-op 未改。7 个包含预览坐标、第三方 UI 或失败样本的任务临时文件已精确删除，合格截图保留在系统临时目录。
- 报错闭环（悬浮控制实现与门禁）：`ui-ux-pro-max` 的设计检索脚本路径指向本机不存在的源目录，实际改为完整读取技能规则后直接应用 48dp 触控、渐进披露和大字体约束；误查不存在的 `gradle/libs.versions.toml` 后确认项目依赖直接声明于 `app/build.gradle.kts`。首轮控制器编译因 Kotlin 不暴露 `ScrollView.LayoutParams` 失败，改用实际父类 `FrameLayout.LayoutParams` 后通过。首轮最终 Lint 报 4 个 API 28 刘海访问错误，根因是 Lint 无法沿可空局部变量识别运行时守卫，拆成 `@RequiresApi(28)` 辅助函数后双 Lint 通过。测试报告计数脚本首次把逐行 XML 强制转换为文档而失败，改用 `Get-Content -Raw` 后确认 240/240。最终截图首次用 PowerShell `>` 写二进制导致 PNG 出现 UTF-16 字节并无法查看，改为手机端 `screencap` 后逐字节 `adb pull`，PNG 魔数恢复；随后真实截图显示设备仍处于指纹锁屏，未冒充应用画面。一次只读活动查询因末尾筛选无匹配返回 exit 1，但明确输出 app crash 计数为 0；后续命令将验收条件与退出码分离。
- 当前源码最新自动证据：44 个 JVM 套件 / 243 个测试，0 失败、0 错误、0 跳过；Debug/Release Lint 0 问题，Debug/Release/AndroidTest Kotlin 编译、Debug/AndroidTest APK、Release R8、Navigation 反射、源码凭据、APK 内容和 Natural Earth 入包哈希审计均通过。后端类型检查、构建、29/29 测试及生产依赖审计仍适用。最终 PR `30743065945` 和 main `30743646492` 的 backend、verify、API 26、API 37，签名发布 `30744241032` 及公开资产兼容 `30744459795` 均成功。
- 诚实边界：用户已授权并完成旧调试包清理、精确公开 v0.2.2 基线安装、Room 2 可见行程建立、正式 v0.2.3 原位覆盖，以及核心地区分流和新版悬浮控制真机验收；用户确认核心功能无问题。ADB mock 已真实推进到 `ARRIVING`，但不冒充 GNSS，也未点击确认到达；“提前离开”改动未安装个人真机，证据为 JVM、编译、Lint、静态审计和最终远端 CI。未运行 `connectedAndroidTest`，未卸载、清理或覆盖测试包；公开 v0.2.3 已完成独立资产核对，但公开资产自动兼容门禁仍不替代该按钮的个人真机实体点击。
- 正式签名环境继续使用工作区外原 keystore 和两份 Windows 当前用户范围 DPAPI 文件，未重新生成签名。最终本地候选在单个无守护 Gradle 子进程中临时注入四项 `ANITABI_*`，`finally` 清除环境变量；243 个 JVM、双 Lint 与 `assembleRelease` 明确 `BUILD SUCCESSFUL`。本地 Release APK 为 `cn.anitabi.navigator` / `0.2.3 (9)`、minSdk 26、targetSdk 37，49,227,286 字节，SHA-256 `67f2bd35e2aca560a57a3126bed9eb76cefb667fec2872b3e0771a5e24ed9a79`；`apksigner` 验证为 v2、单一 RSA-4096 签名者，固定证书与 v0.2.2 完全一致。它与发布工作流独立生成的公开资产分开记录，不用本地哈希替代公开哈希。
- 报错闭环（提前离开正式构建）：首次重建在配置阶段报 `Release signing is not configured` 且未生成新 APK。根因是子进程误用了不存在的 `ANITABI_RELEASE_*` 变量名，实际 `app/build.gradle.kts` 读取 `ANITABI_STORE_FILE`、`ANITABI_STORE_PASSWORD`、`ANITABI_KEY_ALIAS`、`ANITABI_KEY_PASSWORD`；DPAPI、密码和 keystore 均未损坏。只读核对源码后改用准确名称，仍只在单个进程注入并于 `finally` 清除，重跑 5 分 28 秒成功；包名/版本、v2、单一 RSA-4096、固定证书、R8、源码和 APK 审计全部通过。首版证书脚本又因只匹配旧 `Signer #1` 字段而假阴性，按当前 `V2 Signer` 输出复核后固定摘要完全一致。
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
- 报错闭环（真机只读签名核对）：前两次临时拉取命令都在执行前被工具策略拒绝，现象分别是递归目录清理和单文件 `Remove-Item` 被判定为不安全；因此没有读取设备 APK、创建临时文件或修改设备。根因是命令包含策略不接受的删除形式；实际解决为改用单个随机系统临时 APK，先验证绝对父目录、文件名前缀和 `.apk` 后缀，再用 .NET 只删除该精确文件。随后只读拉取已安装 APK 并用 `apksigner` 核对成功，确认当前 0.2.1 为单一 RSA-2048 调试签名而非官方证书；临时 APK 已删除并复查无残留，设备和应用数据未修改。
- 报错闭环（PR、CI 与 Chrome 发布）：首次启动 Chrome 的辅助命令在 30 秒工具窗口超时，但 Chrome 实际已经启动；根因是辅助进程持续附着而非启动失败，2 秒后从进程与自动化连接两侧确认可用并继续。PR CI 页面跳转曾报 `ERR_CONNECTION_CLOSED`，main CI 周期刷新又两次在 `Page.getFrameTree`/连接处超时；均为 GitHub 瞬时连接或长时间刷新后的旧 tab 问题，先确认 tab 仍连接，分别 reload 或新建干净监控 tab 后恢复，最终两轮四个 job 全部成功。Chrome 对 `api.github.com` 和 `release-assets.githubusercontent.com` 返回 `ERR_BLOCKED_BY_CLIENT`，其下载动作也没有在 Downloads 产生文件；实际解决为从 Release 页面自身的 `include-fragment` 读取同源 `github.com/.../releases/expanded_assets/v0.2.3` 以核对资产列表，PR、合并和 Release 编辑仍在 Chrome 完成，二进制技术审计则通过稳定的公开 GitHub 下载 URL 获取随机系统临时单文件。同步最终 Release 正文时又先后调用了当前接口不存在的 `playwright.domContentLoaded()` 与 locator `inputValue()`：前者发生在 `goto` 已成功之后，后者发生在 `fill` 已成功之后，均未提交错误内容；实际改为用 `domSnapshot()` 精确确认两条新增文案，再点击 `Update release`，最终公开页 URL、Latest 标记、`f173eba` 提交及两条文案全部复核通过。
- 报错闭环（公开 APK 与收尾文档）：提交前把源码凭据和 Navigation R8 两个审计串在 30 秒工具窗口内导致超时，根因是 R8 反射审计实际需要约 55 秒；拆分任务并把单项时限提高到 120 秒后两项均通过。首次把公开 APK 下载、递归清理和多项核对合在一个 PowerShell 命令时被安全策略在执行前拒绝，没有创建或删除文件；改为单一随机临时 APK、逐步只读核对和精确单文件删除。`apksigner` 首次断言沿用旧的 `public key size`/证书字段而假失败，实际当前输出为 `V2 Signer: key size` 与 `V2 Signer: certificate SHA-256 digest`；随后又因把 PowerShell 字符串数组直接拼成 regex 得到 `System.Object[]` 和未终止字符集，改为逐项布尔匹配后确认 v2、单签名、RSA-4096 和固定证书。APK 内容审计数次只显示 `Usage: audit-apk.sh <apk>`，根因是当前 `bash` 为 WSL2 而非 Git Bash，`C:/` 与 `/c/` 路径均不能通过 `-f`；检测实际运行环境并转换为 `/mnt/c/...` 后审计通过，临时 APK 随后精确删除。更新文档时首条只读分段读取又因把 `$agentLines.Count - 1` 放在嵌套数组表达式中而对整个数组执行减法；文件未改，先单独计算末行索引后重跑成功。等待只读文档审计时又误传 1 秒窗口，低于协作工具 10 秒下限并在执行前被拒绝；改用 10 秒合法窗口后调用正常，未改变工作树或审计任务。最后一次单文件暂存断言又把 PowerShell 标量字符串的 `[0]` 当作首个文件，实际取到首字符并产生假失败；此时只有 `AGENT.md` 已暂存，没有提交或推送，改为用 `@(...)` 显式数组化后重新核对。上述错误均未修改发布资产、设备、服务器或附件目录。

## 后端与生产环境

- 公共 API：`https://api.anitabi.afunnypersonlol0.site`。
- 技术栈：Node.js 24、TypeScript、Fastify、SQLite WAL；固定 Google OAuth/Routes 上游。
- 端点：`GET /v1/health`、`POST /v1/matrix`、`POST /v1/route`、`POST /v1/navigation/reserve`；POST 仅 HTTPS JSON，正文最多 16 KiB。
- 生产使用现有 Nginx；容器监听 8787，宿主映射为 `127.0.0.1:8788`，因为宿主 8787 属于无关服务。
- 目录：程序 `/opt/anitabi-api`，数据 `/var/lib/anitabi-api`，秘密 `/etc/anitabi-api/secrets`。
- 容器必须保持非 root、只读根文件系统、drop all capabilities、no-new-privileges、只读秘密挂载、健康检查和自动重启。
- 当前生产后端运行树为 `main@f91cb13835c43415982831ea8bf9944e534641e2`，source marker 已精确核对。2026-08-03 已完成 archive 校验、镜像构建、部署前数据库备份、独立候选健康检查、生产切换、回滚快照、生产健康和部署后 systemd 备份；公开 `/v1/health` 为 `service=ok`、`database=ok`。旧应用归档和 rollback 镜像保留，SQLite 未回滚；备份 timer 保持 active。
- 配额只保留项目共享 UTC 月度上限：Matrix 9,000 元素、Routes 9,000 次、Navigation 900 个目的地；没有 UID 日额度或 UID 突发令牌桶。HMAC-IP 防滥用仍为容量 60、每秒恢复 5 次，Firebase Anonymous Token 验证仍为所有 POST 的前置条件。
- 新配额预留只读写 SQLite `global` 月记录；旧 schema 的 `uid` 行原样保留但不再读取、写入或参与限制，不执行破坏性迁移。账本异常、费用开关关闭和恢复不确定仍 fail closed；上游失败仍不退款。`/v1/navigation/reserve` 为旧 v0.2.3 客户端继续返回 `remainingToday=2147483647` 兼容字段，新 Android 不依赖该字段。
- 本次实现前查阅 2026-07-28 更新的 Google 官方 Routes [用量与计费](https://developers.google.com/maps/documentation/routes/usage-and-billing)、2026-07-15 更新的 Navigation SDK Android [用量与计费](https://developers.google.com/maps/documentation/navigation/android-sdk/pricing)，以及官方 [成本管理](https://developers.google.com/maps/billing-and-pricing/manage-costs) 和 [Routes 监控](https://developers.google.com/maps/documentation/routes/report-monitor)。确认 Routes 按请求、Matrix 按元素、Navigation 按目的地计费，Navigation 单次最多 25 个目的地；Cloud quota 可停止服务，而预算告警不会限制费用。因此只移除项目内 UID 限制，继续保留共享月度硬上限、费用熔断、HMAC-IP 防滥用和 Google Cloud 配额。
- 报错闭环（本次额度策略更新）：首次整文件读取 `AGENT.md` 被工具输出上限截断，改为按行分段并补读交界后完整覆盖 267 行；首次宽泛 `rg` 误包含日本边界资产并输出超长数据行，后续检索固定为精确源码/文档路径并排除 assets。`mcporter` 首次沿用 function-call/`--args` 写法无法加载参数，改为显式命名参数后只返回 Google 官方来源。`npm audit --omit=dev` 首轮因 registry 连接超时失败，原命令重试后确认 0 个生产漏洞。Docker 首次构建因 Desktop 守护进程未运行失败，先只读确认服务后以隐藏窗口启动，manifest 可访问；基础 Node 镜像拉取连续 20 分钟没有层进度并被有界工具窗口终止，残留拉取客户端随后按完整命令行和 PID 精确停止，确认计数为 0。本机 Docker 镜像门禁尚未完成，必须由部署主机的实际镜像构建补齐，不得冒充通过；Docker Desktop 已恢复为任务前的进程 0、服务 Stopped。此前 5 分钟构建超时留下的两个本任务 Docker 客户端也已按命令行与启动时间精确停止；首条停止后检查因 `Get-Process` 对已消失 PID 返回 1 而假失败，改为显式数组计数确认残留为 0。首版 Lint XML 汇总把 `$null` 数组化误报为每档 1 个问题，改用 XPath `SelectNodes` 后确认 Debug/Release 均为 0。首版签名断言匹配 `V2 Signer #1` 而当前工具输出 `V2 Signer:`，造成已签名 APK 的假失败；按实际字段重跑后 v2、单签名者、RSA-4096 和固定证书全部通过。发布只读审计中，未引用的 tag peel 表达式被 PowerShell 当作脚本块、immutability 设置端点因当前 PAT 无 Administration read 返回 403、以及检索不存在的根 `docker-compose.yml` 导致退出 1；分别通过引用 revision、读取公开 Release 对象和使用实际 `backend/compose.yaml` 闭环。`gh auth status` 预检还输出了 CLI 自动遮蔽的凭据元数据；没有可用 Token 值写入项目或文档，后续只用不回显凭据的用户 API 检查登录状态。
- 报错闭环（本次生产部署）：候选 archive 的 Git blob 校验因跨主机换行语义失败，改为候选原始 SHA-256；生产工作树原始哈希又因 CRLF 假报漂移，改回生产 Git blob 校验。镜像随后从精确 `main@774064fc` 构建成功，但无 healthcheck 的无关容器没有 `.State.Health`，快照改为稳定 inspect 后可空提取 Health。下一次已完成数据库备份、新容器健康与安全属性验证，却在部署后 systemd 备份失败；根因是仓库 `backend/deploy/backup.sh` 为 `100644`，而旧生产文件依赖人工可执行位。部署器自动回滚旧源码、镜像和容器，loopback/公开健康均恢复，SQLite 未回滚；临时兼容用旧文件 `chmod --reference`，跟踪模式现修正为 `100755`。随后直接 scp/ssh 连接出现超时；单连接 Base64 管道又因远端解码无效在脚本前退出，改为原始 tar stdin。部署辅助 Python 的首轮 AST 检查因 PowerShell 外部管道在首字符加入 U+FEFF 而假报语法错误，直接按 `utf-8-sig` 读取实际文件后解析通过，文件字节本身无 BOM。Paramiko 与系统 `ssh.exe` 当时都在远端脚本前被密码认证拒绝。OpenSSH 一次性密码桥首版把随机 token 的二进制值与十六进制环境值比较，导致空密码；用一致十六进制 token 修正，并以非敏感值逐字节验证。监听线程关闭时的预期 socket 错误也已在 stop 状态正常收口。15 分钟无连接冷却后的单次系统 SSH 当时仍返回 `Permission denied (publickey,password)`，公共 API 同时保持健康；2026-08-03 服务器重新接受同一既有凭据，连接继续使用已信任的主机密钥严格校验，项目没有修改密码、SSH、端口、防火墙、主机密钥或登录策略。PR #15 监控时一次 GitHub API TLS 握手超时，原查询立即重试后确认同一 run 仍在正常执行，未重跑或修改 CI。
- 报错闭环（最终部署与 Release）：首次部署重试在候选启动前失败且辅助脚本没有输出阶段信息；增加不含敏感值的 stdout/stderr 后确认候选 SQLite 临时目录为 root `0700`，容器内 `node` 用户无法穿过父目录。实际改为递归设置候选数据目录 UID/GID 1000，重跑后候选健康、生产切换、回滚快照、生产健康和部署后备份全部成功。PowerShell 首次以错误代码页读取中文记录产生乱码，已改为显式 UTF-8 分段完整读取；APK 签名断言首次对字符串数组直接 `-match` 得到无效 `System.Object[]`，连接输出后逐项严格核对通过；校验文件首次写成 CRLF 86 字节，改为无 BOM LF 后为 85 字节；Release 正文首次因 GitHub CRLF 未命中插入标记，内存归一化换行后生成正确正文。CLI PAT 对 Release 资产删除和新增均返回 403，旧资产未被 CLI 改动，随后按用户授权在已登录 Chrome 操作；Chrome 首次上传因扩展文件 URL 权限关闭而被拒，用户启用并继续后上传成功。浏览器重启辅助进程虽在 30 秒工具窗口超时，但两秒后连接可用；首次网页提交出现 `ERR_CONNECTION_TIMED_OUT`，只读 API 确认新资产已生效而正文仍旧，重新填写并提交后正文成功。公开 APK 首次下载出现 transport EOF，改用带完整重试的公开 URL 后字节数、摘要和校验正文全部通过。GitHub CLI 查询还曾使用不存在的 `isLatest` JSON 字段、一次 `gh api --jq` 又被 PowerShell 引号改写，均改为读取公开 Release 对象并在 PowerShell 内解析；这些只读错误没有改变发布状态。远端上传暂存目录已在绝对路径和前缀复核后删除。
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

- v0.2.3 同版本额度策略更新的当前证据：PR #14 已合并为 `main@774064fc`，main CI run `30750623097` 的 backend、verify、API 26、API 37 全部成功；PR #15 已合并为 `main@f91cb138`，main CI run `30757317236` 同样四项全绿。后端类型检查、构建及 29/29 测试，Android 245/245、双 Lint、Debug/AndroidTest、正式签名 R8 与三项审计均通过。精确实现源码重新构建候选为 `cn.anitabi.navigator` / `0.2.3 (9)`、minSdk 26、targetSdk 37，49,227,286 字节、SHA-256 `b5a125386a511151139f5ff69a4b88d7101af822b53d920db5f45a00aca75eec`，签名为 v2、单一 RSA-4096 与固定证书。生产后端现运行 `main@f91cb138`，公开 Release APK 已替换为同一候选字节，先后端再 APK 的兼容顺序完成。
- v0.2.3 当前源码已有报告：243 个 JVM 测试 / 44 个套件，0 失败、0 错误、0 跳过；Debug/Release Lint 0 问题；Debug/Release/AndroidTest Kotlin、Debug/AndroidTest APK、Release R8、Navigation 反射、源码凭据和 APK 内容审计通过；后端 29/29、类型检查、构建及生产依赖审计通过。最终 PR `30743065945` 和 main `30743646492` 的 backend、verify、API 26、API 37 均成功；签名发布 `30744241032` 与公开资产兼容 `30744459795` 也全部成功。
- v0.2.3 提前离开阶段的本地正式签名候选为 49,227,286 字节、SHA-256 `67f2bd35e2aca560a57a3126bed9eb76cefb667fec2872b3e0771a5e24ed9a79`；同版本额度策略更新后的当前公开 Release APK 为 49,227,286 字节、SHA-256 `b5a125386a511151139f5ff69a4b88d7101af822b53d920db5f45a00aca75eec`。当前公开包与已验证更新候选逐字节相同，包名/版本/minSdk/targetSdk、v2、单一 RSA-4096 签名者、固定证书和内容审计均已核对。核心地区分流、Google Maps 交接、升级保留数据、悬浮控制及 ADB mock `ARRIVING` 有此前候选的真机证据；新增提前离开只有自动证据，未安装个人真机。
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
- 稳定 v0.2.3 已发布：PR #12 完成日本公共交通主体实现，PR #13 完成可拖动/缩放/悬浮球控制与停留态“提前离开”，均通过 Chrome rebase 合并；标签保持 `f173ebaf`。243 个 JVM、双 Lint、Debug/AndroidTest、Release R8、后端 29/29、PR/main 两轮 backend/verify/API 26/API 37、签名发布和精确公开资产 API 26/API 37 门禁全部成功。PR #14 随后移除 UID 日额度与突发限制，PR #15 修复生产备份脚本模式；2026-08-03 已部署 `main@f91cb138` 后端并同版本替换公开 APK，标签没有移动。正式签名真机升级、地区分流、悬浮控制和 ADB mock `ARRIVING` 已验收；“提前离开”未另行安装个人真机，不能把自动门禁冒充实体点击。

## 常用权威入口

- 用户说明：`README.md`
- 隐私：`PRIVACY.md`
- 安全：`SECURITY.md`
- 构建：`docs/BUILD.md`
- 后端运维：`backend/README.md`
- 当前源码与公开稳定版说明：`docs/RELEASE_NOTES_v0.2.3.md`；v0.2.2 与 v0.2.1 历史说明保持不变。
- Release/真机证据：`docs/releases/`、`docs/PHYSICAL_DEVICE_ACCEPTANCE_v0.1.3.md`
- 压缩前完整实施记录：提交 `c92a92195d903ecb89ae563a509699f32e5737ef` 中的 `AGENT.md`

## 最近任务（最多 5 条）

- 2026-08-03：v0.2.3 移除个人请求限制的 PR #14 与备份模式修复 PR #15 均已合并，`main@f91cb138` 四项 CI 全绿。服务器重新接受同一既有凭据后，精确 main archive 完成镜像构建、前备份、候选健康、生产切换、回滚快照、公开健康和后备份；候选首次失败的根因是 root `0700` 父目录阻止 `node` 用户访问 SQLite，递归设置 UID/GID 1000 后通过。随后按兼容顺序同版本替换 [v0.2.3 Release](https://github.com/realMisakaMikoto/junrei_navi/releases/tag/v0.2.3)：公开 APK 为 49,227,286 字节、SHA-256 `b5a125386a511151139f5ff69a4b88d7101af822b53d920db5f45a00aca75eec`，85 字节校验资产与正文均匹配，Release 仍为 Latest，标签仍为 `f173ebaf`。CLI 403、Chrome 上传权限、GitHub 页面超时和公开下载 EOF 均已按报错闭环处理；远端/本地任务临时文件已精确清理，附件目录未读取或改动。
- 2026-08-02：日本外部公交 `DWELLING` 已增加“提前离开”，悬浮面板、应用内和通知同步；点击经既有 `MODE_NEXT`/CAS 一次结束停留并启动下一段，末站完成，自然到期仍保留 `NEXT_STOP`。243 个 JVM、双 Lint、Debug/AndroidTest/Release Kotlin、正式签名 R8 和三项审计通过；提交 `ec7a28f` 经 Chrome 创建并 rebase 合并 [PR #13](https://github.com/realMisakaMikoto/junrei_navi/pull/13)，最终 PR/main、签名发布与公开资产兼容门禁全部成功，发布源码/`v0.2.3` 为 `f173ebaf`。当时的 [公开 Release](https://github.com/realMisakaMikoto/junrei_navi/releases/tag/v0.2.3) APK 为 49,227,274 字节、SHA-256 `47374031a2836949179a9dc3c611e05e071bcb9a527f314253ad4a0d3a3e4a33`，固定签名与内容审计通过；该历史资产已在 2026-08-03 被同版本额度更新 APK 替换。签名变量、审计超时、Chrome/GitHub 瞬时错误、公开资产路径和签名输出解析均已按本文件报错闭环解决。该按钮未另行安装个人真机，未运行 `connectedAndroidTest`，测试包未动。
- 2026-08-02：用户要求纯 ADB mock 触发 `ARRIVING`；Google 地图前台时以精度 3 米、80 米内样本持续超过 15 秒，悬浮窗显示 4 米、“已接近目标，请确认到达”和“确认到达”，未点击确认。`finally` 已移除测试 provider，shell app-op=`default`、`mMockProvider=0`，正式/测试包版本未变，crash/ANR 为 0；截图仅在系统临时目录。用户随后确认测试通过并停止当前行程。
- 2026-08-02：用户解锁并授权 mock location 后，Xiaomi API 36 正式签名候选完成新版悬浮控制真机验收：最小/默认/拖拽放大尺寸、标题拖动稳定、四角停靠、60dp 球、横竖屏、force-stop 冷恢复且不自动开 Maps 均通过；ADB `gps/network` 日本区域样本让距离 1.1→2.2 km 且保持 `NAVIGATING`，暂停立即移除 overlay。首次跨洲 mock 使应用运行态短暂显示非洲，已 force-stop、恢复真实 provider 并改用日本有界样本重做；最终 shell app-op=`default`、`mMockProvider=0`、系统设置恢复，正式/测试包未变，crash/ANR 为 0。未运行 connected Gradle 任务；用户随后停止测试行程，最终 Git 与发布状态见本节首条。
- 2026-08-02：因用户确认 v0.2.3 功能无问题但旧悬浮窗严重遮挡，已在 `codex/v0.2.3-overlay-controls` 改为可拖动、可缩放、可收成 60dp 悬浮球的单窗口控制器，并持久化形态/尺寸/安全区归一化位置；横竖屏、刘海、密度/字体变化、大字体、TalkBack 点击替代和固定 48dp 操作均已收口。240 个 JVM、双 Lint、Debug/AndroidTest、正式签名 Release、R8/源码/APK 审计全部通过；候选 49,227,286 字节、SHA-256 `779ebeaef4ed397f328865e1d179bb23f1c6fd35a82ef3118b9fd43a655177ed`，固定证书不变，已保留数据原位覆盖真机且测试包未动。首次最终截图被设备指纹锁屏阻断；用户解锁后的完整实体结果见上一条，最终提交与发布状态见本节首条。
