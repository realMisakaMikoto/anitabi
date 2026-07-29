# 巡礼手帖 v0.1.4

这是一次针对真机反馈的补丁版，包含 v0.1.3 的全部功能。

## 修复

- 顶部标题栏现在避开 Android 状态栏，底部操作区避开系统手势导航栏；搜索、点位选择、规划、路线预览、连续导航和关于页面均已在 1280×2772 真机复核。
- Anitabi 点位解析不再把未使用且类型不稳定的 `ep`、`s` 字段绑定为整数；正式接口返回例如字符串 `"CD"` 时也能正常加载。
- Transitous 未提供路段距离时，应用会从 polyline6 几何计算距离并同步到路段指令，不再把有效铁路段显示为 `0 m`。
- 路线几何会去除连续重复坐标，少于两个不同坐标的退化线不会交给 MapLibre，避免 `Invalid geometry in line layer`。
- Transitous 返回 UTC 时刻时，按上车站和下车站的 IANA 时区显示当地时间，修正日本行程相差九小时的问题。

## 安全与兼容

- ORS Key 仍只由每位用户自行申请，并经 Android Keystore 加密保存在本机；APK 和仓库不包含共享 Key。
- 新增字段均有默认值，已有本地路线缓存仍可读取。
- Transitous 请求边界未改变：正式 API、可识别 User-Agent、最多 8 点、逐段串行、无轮询、无并发抓取、无自动重试。

## 验证

- 38 个 JVM 单元测试、Android SDK 37 编译和 Lint 在本地通过。
- Xiaomi 15T Pro（Android 16 / API 36）真机完成搜索、74 个 Anitabi 点位加载、真实 ORS 道路路线、真实 Transitous 7 段路线、路线预览和连续导航检查。
- 真机 Transitous 结果为 47.2 km；末两段分别为 2.5 km 和 781 m，完整路线无 `0 m`，日志中无 MapLibre 无效几何及应用崩溃。
- [主分支 Android CI 30466264424](https://github.com/realMisakaMikoto/anitabi/actions/runs/30466264424) 通过 38 个测试、SDK 37、Lint、APK 内容审计，以及 Android 8/17 的冷启动、离线恢复、前台服务、息屏自动到达和持久化回归。
- [签名发布 30467039704](https://github.com/realMisakaMikoto/anitabi/actions/runs/30467039704) 通过 release Lint、R8、APK 审计、固定签名校验并公开发布；[签名 APK 兼容性复验 30467427032](https://github.com/realMisakaMikoto/anitabi/actions/runs/30467427032) 在 Android 8/17 全部通过。

## 下载校验

- APK：`anitabi-v0.1.4.apk`
- 大小：43,103,613 字节
- SHA-256：`4a95482bdc9bdec9e357d334339f9a401f558b00f19b4160b519ea9af586240e`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

## 已知边界

- 已发送到聊天中的 ORS Key 应由其所有者撤销并重新生成；发布包没有记录该值。
- 真机仍启用了系统 mock-location 配置，因此本轮验证定位与导航管线，不冒充真实 GNSS 实走。
- Transitous 为 best-effort 服务，只覆盖日本部分地区；无数据时应用会明确提示。

Release 同时提供机器可读的 APK SHA-256 校验文件。详细证据见仓库中的 `docs/releases/v0.1.4.md` 和 `docs/PHYSICAL_DEVICE_ACCEPTANCE_v0.1.4.md`。
