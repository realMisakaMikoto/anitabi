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
- 当前工作分支：`codex/navigation-task-cancel-crash`。压缩前 HEAD 和完整日志锚点为 `c92a92195d903ecb89ae563a509699f32e5737ef`。
- `main` / `origin/main`：`cbc29fa7acc3dd2e589d5849d2f6e66be53b9ea1`。
- 稳定标签 `v0.2.1` 指向 `fd65c1d54372df43769d4c53602ed2fb833f8ae2`。
- 当前分支在压缩前比 `main` 多 4 个提交：
  - `95cdb8366cab59a4b9cb12bdc61d98d8dd3f3d34`：修复 Google Navigation cancelled-task 概率闪退。
  - `43b9c7d067b7b1907d0b4ef528ff5ef65ec2c45a`：显示名改为 `巡礼手帳`，准备同版本替换 APK。
  - `b39e7a0`：替换包交接记录。
  - `c92a921`：旧 Release 说明品牌更新记录。
- 这些源码提交尚未合并进 `main`，也未移动 `v0.2.1` 标签。
- 当前唯一允许保留的未跟踪路径是用户的 `.codex-remote-attachments/`；不要暂存或清理。

### 稳定 Release 的特殊边界

- 2026-08-01 通过只读 GitHub API 确认：公开稳定 `v0.2.1` 的 APK 已被同版本替换为：
  - 文件：`anitabi-v0.2.1.apk`
  - 大小：49,058,373 字节
  - SHA-256：`77e2634ae3e22d663cc25bbf74b28fd3682074f2a1aa8cad51ab5a4615855d9a`
- 该摘要与本地 `app/build/outputs/manual-release/v0.2.1/anitabi-v0.2.1.apk` 及 Gradle Release 输出一致；相邻 `.sha256` 文件匹配。构建目录已忽略，`clean` 可能删除本地产物。
- 公开替换包包含 cancelled-task 修复和 `巡礼手帳` 改名，仍为 `versionName=0.2.1` / `versionCode=7`，并保持固定签名。
- 但稳定标签和 `main` 仍指向替换前源码。因此当前公开 APK 与标签源码不完全一致；不得声称标签可重现该资产，也不得擅自移动标签或重写历史。
- 稳定 `v0.2.1` Release 说明按用户要求未修改，正文 SHA-256 仍为 `f5ef07346eee097efe7bdfd1e2c00ae2012707af4a43c143132c4ef5a9b59afe`。它可能未完整描述后来替换的 APK；未经用户要求不要修改。
- 现有 8 个 Release：`v0.1.0`、`v0.1.1`、`v0.1.2`、`v0.1.3`、`v0.1.4`、`v0.2.0`、`v0.2.1-rc.7`、`v0.2.1`；每个仍有 APK 和校验文件两个资产。RC1–RC6 的 Release 对象已删除，但 Git tags/commits 保留。
- 除稳定 `v0.2.1` 外，上述旧 Release 说明正文中的旧应用名已替换为 `Anitabi Navigator`。这是历史说明品牌，不等于当前应用显示名。

## 当前产品与 Android 架构

- 用户可见名称：`巡礼手帳`。
- 包名/namespace：`cn.anitabi.navigator`。
- 版本：`versionName=0.2.1`、`versionCode=7`；`minSdk=26`、`compileSdk=37`、`targetSdk=37`；Java 17。
- 主要工具链：Gradle 9.6.1、AGP 9.3.0、Kotlin 2.4.10、KSP 2.3.10、Compose BOM 2026.06.01。
- Google Navigation SDK 7.8.0 负责地图、定位及驾车/步行/骑行道路导航；项目不得同时接入 Maps SDK for Android。
- Google Routes 由自建 VPS 后端调用，Android 不包含服务账号。Firebase Anonymous Auth 只用于后端鉴权和配额归属。
- 旧 MapLibre/OpenFreeMap、ORS、Transitous 仅属于 v0.2.0 历史；当前没有这些 Provider、Key UI 或回退路径。
- Anitabi 数据只从 `https://api.anitabi.cn` 获取，图片只允许 HTTPS `image.anitabi.cn`；主站不得作为 API。保持人类访问频率和技术 User-Agent `AnitabiNavigator/0.2.1`。
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

- 当前分支已有报告：125 个 JVM 测试 / 26 个套件，0 失败、0 错误、0 跳过。
- Debug 与 Release Lint 均通过，仅有 4 条 `AutoboxingStateCreation` 性能提示；Debug/Release 构建、Navigation R8 审计、源码凭据审计和 APK 内容审计通过。
- 后端最后记录为 29 个测试通过，TypeScript 构建通过，生产依赖审计 0 漏洞；本次压缩没有重跑后端测试。
- 原稳定发布流程曾通过：PR `30628585336`、main `30629643413`、signed release `30630717751`、旧公共资产兼容 `30631245688`。
- `30631245688` 验证的是后来被替换前的公共资产，不能作为当前 `77e263…` APK 的 exact-public-asset 证据。
- cancelled-task 修复曾在 Xiaomi 15T Pro 上完成 6 次有界启动/快速停止，未再出现 `Task was cancelled`，地图与最终 Google 导航正常；该测试使用既有 mock-location，且测试包早于显示名改动。
- 当前公开替换 APK 与本地完整测试/签名产物哈希一致，但没有记录新的 exact-public-asset API 26/API 37 工作流或该精确 APK 的真机覆盖验收。需要此类结论时必须重新验证。
- 手机当前安装、页面、网络、mock location 和选点状态都可能已被用户改变；开始新真机任务时重新只读确认，不沿用旧状态。

## 里程碑摘要

- v0.1.0–v0.1.4：完成最初 Android、Room、搜索、MapLibre/ORS/Transitous、连续导航和固定签名发布；这些 Provider 已不属于当前架构。
- v0.2.0：加入多作品联合巡礼、首次导览、真机/模拟器覆盖和可升级生产基线。
- v0.2.1：迁移到 Google Navigation + VPS Google Routes + Firebase，加入无限点分批、Room 2 数据迁移、遥测选择加入、配额与生产部署。
- RC1–RC6 的 Release 因已知缺陷被删除，tags 保留；RC7 修复地图/R8/道路/公交参数问题。后续公交证据促成更严格的 transit 真实性与错误处理。
- 稳定 v0.2.1 已发布；随后在功能分支修复 Navigation Future 取消崩溃、改名为 `巡礼手帳`，并生成/替换同版本 APK。标签源码尚未同步这些分支提交。

## 常用权威入口

- 用户说明：`README.md`
- 隐私：`PRIVACY.md`
- 安全：`SECURITY.md`
- 构建：`docs/BUILD.md`
- 后端运维：`backend/README.md`
- 当前稳定说明：`docs/RELEASE_NOTES_v0.2.1.md`
- Release/真机证据：`docs/releases/`、`docs/PHYSICAL_DEVICE_ACCEPTANCE_v0.1.3.md`
- 压缩前完整实施记录：提交 `c92a92195d903ecb89ae563a509699f32e5737ef` 中的 `AGENT.md`

## 最近任务（最多 5 条）

- 2026-08-01：修复 `Navigator.setDestinations()` Future 取消导致的概率主线程崩溃；新增 4 个回归测试，完整 Android 验证和有界 Xiaomi 测试通过。提交 `95cdb83`。
- 2026-08-01：显示名与当前文档改为 `巡礼手帳`，保持包名、版本和签名不变；生成同版本替换 APK。提交 `43b9c7d`。
- 2026-08-01：将稳定版以外 7 个历史 Release 的说明正文应用名改为 `Anitabi Navigator`；稳定 `v0.2.1` 与 `README.md` 未改。提交 `c92a921`。
- 2026-08-01：完整读取并审计旧 1,637 行 / 274,142 字节工作树记录，将其替换为当前状态摘要；用 Git 锚点保留全部历史，并通过只读 GitHub API 发现并记录稳定 APK 已被替换、但标签源码未同步的边界。本任务只修改 `AGENT.md`。
