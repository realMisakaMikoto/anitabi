# v0.2.0 真机验收记录

日期：2026-07-30（Asia/Shanghai）

设备：Xiaomi 15T Pro（2506BPN68G），Android 16 / API 36，安全补丁 2026-06-01

应用：`cn.anitabi.navigator` v0.2.0（versionCode 6）首个固定签名候选；该候选完成多作品检查，随后源码又加入首次启动导览

## 已通过

- 使用与公开 v0.1.4 相同的固定签名证书原位升级，没有卸载应用或清空应用数据；安装返回 `Success`。
- Android 包信息报告 `versionCode=6` / `versionName=0.2.0`；冷启动返回 `Status: ok` / `LaunchState: COLD`，总耗时 456 ms。
- 导览加入前的候选 APK 为 42,660,757 字节，SHA-256 为 `4d8698f5bab17246274450466e751b287178999a8d6edc72783b0dce679aa3c9`；证书 SHA-256 为 `9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`。此哈希不代表加入导览后的最终 APK。
- 真机页面显示“3 部作品联合巡礼”“178 个可用地点”，合并地图正常铺满多作品点位；底部仍显示已选数量、列表/清空/规划操作，系统状态栏和手势栏无重叠。
- Android 最近任务界面显示新的巡礼定位针启动图标；应用崩溃缓冲区为空。
- 用户明确确认此前同一真机连续导航的中文 TTS 可以听到；这里只记录可听，不评价发音质量。

## 公开 v0.2.0 无安装复验

- [x] 用户已自行安装公开 v0.2.0；本轮遵守用户要求，没有执行 APK 安装、覆盖安装、卸载、清除数据、修改权限或更换输入法。
- [x] ADB 确认手机中的 `base.apk` SHA-256 为 `e3d36b47695b452978680726c5eb09133e04c0f207149a6324f3e08ac8f9a9ec`，与 GitHub Release 资产完全一致；包信息为 `versionName=0.2.0` / `versionCode=6`。
- [x] 在应用没有运行、没有导航服务和活动通知时执行不清数据冷启动；返回 `Status: ok` / `LaunchState: COLD`，总耗时 302 ms，`MainActivity` 成为前台且崩溃缓冲无本应用记录。
- [x] 已完成的首次导览和个人配置在冷启动后仍然有效，应用直接进入 Bangumi 搜索主页；定位、粗略定位和通知权限均为 `granted=true`。
- [x] 公开包通过现有界面向 Bangumi 发起实网搜索并返回 20 部作品；选择无 Anitabi 数据的结果时明确显示“Anitabi 暂无这部作品的巡礼数据”，进程保持存活且无崩溃。
- [x] 关于页在真机显示 GPL、无广告/分析/账号/云同步/位置日志的隐私说明，以及 OpenFreeMap、OpenMapTiles、OSM、ORS/HeiGIT、Transitous、Bangumi 和 Anitabi 数据来源。
- [x] 真实定位功能可用：用户明确确认即使系统仍配置 `com.blogspot.newapphorizons.fakegps`，切回真实定位后已经亲自验证正常；ADB 同时确认系统定位、GPS/网络 Provider 和本应用定位权限已启用。此项是用户现场验收，不宣称 ADB 能在保留 mock 授权时独立判定每个定位样本来源。
- [ ] 尚未把上述定位可用性扩大为“真实 GNSS 8–12 点整段路线已通过”；该项仍需实际完成全部点的自动到站、停留和下一站推进。

## 证据边界

- 真机仍显示 `com.blogspot.newapphorizons.fakegps` 获得 `android:mock_location`，而本应用自身没有该能力；没有擅自修改用户的假定位配置。
- 本轮没有使用聊天中暴露过的 ORS Key，也没有向正式 ORS 发出新请求。
- 手机出现其他前台应用/浮层抢焦点后，停止继续注入触控，避免影响用户的其他应用；多作品标题、总点数、合并地图和选点/规划入口已有直接画面证据，作品 ID 防冲突和标题前缀另由单元测试覆盖。
- 真实定位功能已按用户亲测结论单独勾选；8–12 点整段实走、长时间 OEM 省电和现实中的错过班次仍不由远程 ADB 伪造为通过。
- 加入首次启动导览后，本地 49 个 JVM 测试、debug/androidTest 编译和零项 Lint 已通过；但当时手机未连接 ADB，尚未把导览首屏、安全区、权限弹窗和 Key 页面记为真机通过。
- 主分支 CI `30474785852` 的 Android 8/API 26 与 Android 17/API 37 全新安装均显示首次导览；下载的 320×640 截图中状态栏避让正确，三步进度与正文可读，“开始设置”按钮完整位于首屏和底部安全区内。这是模拟器证据，不替代上述小米真机边界。
- 最终公开签名 APK 由发布运行 `30475526252` 生成，公开兼容性运行 `30475867612` 又在 Android 8/API 26 与 Android 17/API 37 下载并安装真实 Release 资产。逐张检查两张 320×640 首屏截图后，确认公开包同样没有状态栏遮挡，“开始设置”按钮完整可见，崩溃缓冲为空。
- 公开 `anitabi-v0.2.0.apk` 为 42,677,141 字节，SHA-256 为 `e3d36b47695b452978680726c5eb09133e04c0f207149a6324f3e08ac8f9a9ec`；这替代上文仅用于导览加入前候选的旧哈希。发布地址：`https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.0`。

## 后续现场采证

- 真实 GNSS 8–12 点、至少两小时小米/OEM 后台运行、真实错过公交班次三项的统一前置条件、步骤和通过判据见 [`PHYSICAL_FIELD_TEST_RUNBOOK_v0.2.0.md`](PHYSICAL_FIELD_TEST_RUNBOOK_v0.2.0.md)。
- `scripts/capture-field-evidence.ps1` 只读采集 ADB 证据并把原始结果留在 Windows 临时目录；它不修改假定位或省电设置，不读取 ORS Key，也不把未发生的现场事件判定为通过。
