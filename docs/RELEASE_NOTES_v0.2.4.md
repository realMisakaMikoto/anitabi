# 巡礼手帳 v0.2.4

v0.2.4 改进首次启动的权限导览，并修复日本外部公交在用户静止于目标附近时可能无法进入到达确认的问题。版本号为 `versionName=0.2.4`、`versionCode=10`；Room schema 仍为 2，后端协议、Google 地图/Routes 行为、日本公交分流和路线额度策略均未改变。

## 首次导览后台设置

- 定位和通知授权之后，权限页会显示“关闭电池优化”“在后台锁定应用”和“允许悬浮窗”三个后台导航建议。
- 电池优化和悬浮窗使用 Android 提供的真实状态；从系统设置返回应用后会立即刷新。
- 电池优化入口打开系统通用优化列表，不直接请求忽略电池优化白名单，也没有新增相关 manifest 权限。
- “在后台锁定应用”明确说明不同厂商的最近任务入口可能不同，没有统一入口时可以跳过；应用不会伪造完成状态。
- 悬浮窗仍是日本公交切换到 Google 地图后的可选控制入口；不开启时可以继续使用可见通知。三个建议都不会阻止完成首次导览。
- 系统没有对应设置 Activity 时会回退到应用详情页；若仍无法打开，会在当前页面显示错误而不是崩溃。

Android 行为依据：[Settings API](https://developer.android.com/reference/android/provider/Settings.html)、[Doze 与应用待机](https://developer.android.com/training/monitoring-device-state/doze-standby) 和 [后台启动前台服务限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)。

## 日本公交到达判定

- 日本外部公交现在允许在用户静止时继续接收定位更新，避免最小位移门槛反复中断到达候选计时。
- 到达规则保持不变：定位精度不差于 50 米、在目标 80 米内连续 15 秒后进入到达确认，超过 120 米时重置候选。
- 道路导航和非日本公交的定位订阅保持原行为；Google 地图交接 URL、Routes 请求和后端均未修改。

## 安装与升级

- 支持 Android 8.0（API 26）及以上版本。
- 从 v0.2.3 更新时请直接覆盖安装，不要卸载应用或清除数据。
- Room schema 仍为 2，不执行数据库迁移；导览状态、作品与巡礼点、行程顺序、设置和导航进度会继续保留。
- 地图、道路导航和非日本路线仍需要联网及设备上可用的 Google Play 服务。

## 网络、费用与隐私

- 本次导览改动只读取本机权限和系统设置状态，不新增网络请求、账号字段、日志字段或持久化的路线内容。
- Google 路线、折线、步骤、ETA 和公交详情仍只保存在当前进程内存。
- Firebase Analytics 和 Crashlytics 继续默认关闭，可分别选择加入和撤回。
- 日本公交仍在任何 VPS/Routes 请求前离线分流；每一段只有用户主动点击后才交给 Google 地图。
- 项目继续只保留共享月度路线额度和 HMAC-IP 防滥用，不设置按匿名用户计算的每日额度或突发限制。

完整边界见 [隐私说明](https://github.com/realMisakaMikoto/junrei_navi/blob/main/PRIVACY.md)。

## 已知限制与证据边界

- 不同 Android 厂商的“后台锁定”名称、手势和可用性不同，Android 没有可供应用统一调用或验证的公开接口。
- 关闭电池优化或开启悬浮窗不能绕过 Android 对 while-in-use 定位、后台 Activity 和前台服务的限制。
- 自动测试和模拟器不能替代个人真机上的厂商后台管理、真实 Google 地图、通知、悬浮窗和真实定位验收；未执行的个人真机项目会在发布记录中保持未勾选。

## 文件校验

Release 同时提供 `anitabi-v0.2.4.apk` 和 `anitabi-v0.2.4.apk.sha256`。请以随 Release 发布的校验文件核对 APK。

固定签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

本版本的实际 CI、签名、公开资产与证据边界记录在 [`docs/releases/v0.2.4.md`](https://github.com/realMisakaMikoto/junrei_navi/blob/main/docs/releases/v0.2.4.md)。
