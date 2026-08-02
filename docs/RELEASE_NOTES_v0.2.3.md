# 巡礼手帳 v0.2.3

v0.2.3 为日本公共交通新增独立的外部 Google 地图分段执行策略，同时保留日本以外地区原有的 Google Routes 应用内公交和全部道路模式。同版本更新移除了按匿名用户计算的日额度与突发限制，并把道路导航生产批次恢复为 Google Navigation SDK 的 25 目的地技术上限。版本号仍为 `versionName=0.2.3`、`versionCode=9`；Room schema 仍为 2，后端公开请求与响应结构保持兼容。

## 日本公交分流

- 应用在取得定位或发送路线请求前，只按用户选择的巡礼点坐标汇总地区。
- 全部日本点使用外部 Google 地图分段模式；全部非日本点继续使用现有应用内路线；混合选择会显示“**不支持此操作，请去除日本或日本以外的点。**”。
- 日本边界来自随 APK 固定携带的 Natural Earth 1:10m Admin-0 Countries v5.1.1 日本 MultiPolygon，支持外接框快速排除、孔洞和边界点；资产缺失、损坏或版本错误时停止规划。
- 日本行程只在本机使用球面距离生成访问顺序和占位段，不请求 `/v1/matrix`、`/v1/route` 或 Google Routes。100 个及以上日本点不受 Google Maps URL 途经点限制，因为每次只交接一段的两个端点。

Google 官方 FAQ 明确说明 Routes 的公交数据不包含日本 Transit 合作方：[Google Maps Platform FAQ](https://developers.google.com/maps/faq#transit_directions_countries)。固定边界数据及公共领域条款见 [Natural Earth 10m Admin-0 Countries](https://www.naturalearthdata.com/downloads/10m-cultural-vectors/10m-admin-0-countries/) 和 [Terms of Use](https://www.naturalearthdata.com/about/terms-of-use/)。

## 单段 Google 地图交接

- 每次由用户点击“开始日本公交行程”“打开本段”或“开始下一段”，应用才生成 `https://www.google.com/maps/dir/?api=1` URL，并只附带 `origin`、`destination` 和 `travelmode=transit`。
- 优先打开 `com.google.android.apps.maps`；不可用时用同一 HTTPS URL 回退到浏览器或其他处理应用。两者均失败时保留当前段并提供重试。
- URL 不包含 API Key、途经点、出发/到达时间或 `dir_action=navigate`。应用不读取或推断 Google 地图的路线、班次、用户选择或导航状态。
- 首段在用户前台点击开始后取得 30 秒内、精度不差于 100 米的精确定位；后续段从上一个已确认巡礼点出发，返回起点段使用保存的首次起点。

实现依据：[Google Maps URLs](https://developers.google.com/maps/documentation/urls/get-started) 和 [Android Google Maps intents](https://developer.android.com/guide/components/google-maps-intents)。

## 连续控制、到达和恢复

- 先启动 `location` 类型前台服务，再由未导出的交接 Activity 持久化当前段并打开 Google 地图；服务不会在后台自动拉起第一段或下一段。
- 日本模式可使用可拖动、可缩放、可收起为 60dp 悬浮球的不可聚焦系统悬浮窗；形态、尺寸和安全区内位置会本地保留。未授权时使用前台服务通知。Android 13 及以上会同时检查通知权限、全局通知开关和导航频道。两种入口都不可用时不打开外部地图。
- 精度不差于 50 米且连续 15 秒位于目标 80 米内时只进入接近提示；超过 120 米会清除候选。用户仍须确认到达，提前确认会再次询问。
- 确认后进入停留；停留中可由用户点击“提前离开”，立即持久化并打开下一段，最后一站则完成行程。自然停留结束仍保持“下一站”，必须由用户点击才推进。暂停会冻结停留截止时间并停止到达判定，结束需要二次确认并进入 `ENDED`；暂停或结束一经接受即先移除悬浮窗，保存失败时才按回滚状态恢复。
- 进程死亡后按持久化 tour ID、当前目标、段号、完成点和暂停/停留状态恢复控制，不自动打开 Google 地图；设备重启且通知可见时只为日本外部公交发布恢复通知，不改变道路或应用内公交的既有恢复行为，也不启动定位前台服务或 Activity。若重启后只有悬浮窗权限而通知不可见，Android 后台限制不允许自动建立可见控制入口，用户需手动打开应用恢复。

实现依据：[location 前台服务](https://developer.android.com/develop/background-work/services/fgs/service-types#location)、[后台启动 FGS 限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)、[后台 Activity 限制](https://developer.android.com/guide/components/activities/background-starts) 和 [通知运行时权限](https://developer.android.com/develop/ui/views/notifications/notification-permission)。

## 活跃行程编辑与兼容

- 已完成点及当前前往/停留目标锁定；后续点可以插入、删除和重排。
- 保存前重新分类全部点；混合地区、地区资产错误或路线重建失败都不会覆盖原行程。
- 只重建受影响的未来段，保留 tour ID、当前段和完整进度；日本未来段仍完全本地生成。
- 导航进度与编辑同时保存时以进度为准，不把旧段号套入已经重排的计划；服务重新加载超时或被新的导航操作接管时保留编辑草稿，不会误报已经保存。
- `StoredTourV2` 新字段都有兼容默认值。v0.2.2 记录缺少执行策略时按已保存坐标重新分类，不执行 Room 迁移。

## 安装与升级

- 支持 Android 8.0（API 26）及以上版本。
- 从 v0.2.2 更新时请直接覆盖安装，不要卸载或清除应用数据。
- 已安装较早 v0.2.3 资产时，也请下载当前 Release APK 直接覆盖安装；版本号不变，请以 Release 当前提供的校验文件为准。
- Room schema 仍为 2，不执行数据库迁移；现有作品、点位、顺序、设置和导航进度继续保留。
- 地图、道路导航和非日本路线仍需要联网及设备上可用的 Google Play 服务。日本公交交接需要 Google 地图应用或能够处理同一 HTTPS 链接的应用。

## 网络、费用与隐私

- 全日本公交的地区分类、排序和占位分段完全在本机完成，不调用项目 VPS、`/v1/matrix`、`/v1/route` 或 Google Routes，因此不消耗项目 Routes 配额。
- 只有用户主动打开某一段时，应用才把该段精确起终点和公交模式交给 Google 地图或 HTTPS 处理应用；应用不读取外部路线、班次或导航状态。
- 非日本公交与道路路线继续使用现有项目 API、Firebase 匿名鉴权和 Google 服务；Firebase 匿名身份只验证访问资格，不再用于每日额度或突发限速。
- 项目只保留共享月度额度：Matrix 9,000 个元素、Route 9,000 次、Navigation 900 个目的地。道路导航按 Navigation SDK 的技术上限每批最多预留 25 个目的地。
- 服务继续使用基于 HMAC-IP 的防滥用限速，原始 IP 不进入日志或配额账本。共享月度额度、网络状态、费用熔断或防滥用限速仍可能限制刷新，已保存行程与进度不会因此删除。
- Google 路线、折线、步骤、ETA 和公交详情仍只保存在当前进程内存。Analytics 与 Crashlytics 继续默认关闭，可分别选择加入和撤回。

完整边界见 [隐私说明](https://github.com/realMisakaMikoto/junrei_navi/blob/main/PRIVACY.md)。

## 已知限制

- 日本公交由 Google 地图提供路线、班次和换乘；应用只维护访问顺序、到达确认、停留与分段进度，无法判断 Google 地图是否找到路线。
- 日本与非日本巡礼点不能混合生成同一公交行程；保存活跃行程编辑前也会执行相同检查。
- 没有 Google Play 服务时，没有备用地图或道路导航。没有 Google 地图且设备也不能打开 HTTPS 链接时，当前日本公交分段会保留并允许重试。
- 外部地图期间必须至少有可见通知或悬浮窗控制入口。设备重启后若通知不可见，Android 后台限制要求用户手动打开应用恢复。
- 步行和骑行道路导航仍会显示 Google 要求的测试版提示；请以现场道路、交通规则和安全状况为准。

## 文件校验与证据

Release 同时提供 `anitabi-v0.2.3.apk` 和 `anitabi-v0.2.3.apk.sha256`。请以随 Release 发布的校验文件核对 APK。

固定签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

本版本的实际 CI、签名、兼容性、公开资产与真机证据记录在 [`docs/releases/v0.2.3.md`](https://github.com/realMisakaMikoto/junrei_navi/blob/main/docs/releases/v0.2.3.md)；未验证项保持未勾选，不会预先声明通过。
