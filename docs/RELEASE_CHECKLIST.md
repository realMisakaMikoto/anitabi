# v0.2.2 发布检查清单

稳定 `v0.2.2` 发布前必须完成版本、自动验证、固定签名和用户接受的 Debug 真机界面验收。精确公开 Release APK 的 API 26/API 37 验证只能在资产发布后进行，属于发布完成门禁；失败时必须立即停止宣称发布完成并修复。用户明确接受 Debug 候选后要求发布，因此 v0.2.2 不把“正式签名 APK 在同一真机覆盖安装”列为发布前条件，也不得把 Debug 验收冒充正式包真机证据。

每个版本或 RC 在 `docs/releases/` 保存一份带证据的实际记录；未验证项保持未勾选。

## 版本、许可与文档

- [ ] `versionCode=8`、`versionName=0.2.2`，RC 标签为 `v0.2.2-rc.N`，稳定标签为 `v0.2.2`。
- [ ] README、NOTICE、隐私说明、关于页和发布说明只描述 Google/Firebase/VPS 当前架构；MapLibre、OpenFreeMap、ORS 与 Transitous 只出现在明确标注的历史记录中。
- [ ] GPL-3.0-or-later 与 `LICENSE` 中仅针对 Google Navigation/Firebase SDK 的窄范围链接例外一致。
- [ ] Google Maps Platform、Navigation SDK、Routes API、Firebase、Bangumi 与 Anitabi 的署名和链接可见。
- [ ] v0.2.0、v0.2.1 及 RC 的发布记录、哈希与真机证据保持历史原文，没有改写成 v0.2.2 结果。

## Google、Firebase 与费用控制

- [ ] Navigation SDK Android Key 只允许 `cn.anitabi.navigator`、正式/调试 SHA-1 和 Navigation SDK。
- [ ] Firebase Android Key 只允许该包名、正式/调试 SHA-1 和所需 Firebase API；旧泄露 Key 保持删除状态。
- [ ] VPS 服务账号只有所需最小权限，JSON 不在 APK、Git、日志或构建产物中。
- [ ] Google Cloud 预算告警有效；VPS 月度硬上限仍为 Matrix 9,000 元素、Route 9,000 次、Navigation 900 个目的地。
- [ ] UID 每日上限仍为 Matrix 2,000 元素、Route 200 次、Navigation 20 个目的地；突发令牌桶生效。
- [ ] Analytics 与 Crashlytics 默认关闭，分别选择加入并可撤回；撤回时执行既定本机清理。

## 后端与安全

- [ ] `npm test` 全部通过，包含 Firebase JWT、固定 OAuth/Routes 上游、single-flight、输入边界、并发 SQLite 配额、周期切换、脱敏与错误映射。
- [ ] 12 个并发 SQLite 连接证明 9,000 元素月度上限不会被突破。
- [ ] 公网 `GET /v1/health` 仅返回服务与数据库健康；HTTP 跳转 HTTPS，证书有效。
- [ ] 容器非 root、只读根文件系统、无额外 capabilities、只读密钥、健康检查、自动重启和 loopback-only 端口均保持。
- [ ] SQLite 一致性备份定时器有效、保留七日；恢复后的计费默认关闭逻辑经过测试。
- [ ] 现有个人网站和原有容器仍正常；没有修改用户的密码、SSH 配置、端口、登录方式或防火墙。

## Android 自动验证

- [ ] `testDebugUnitTest`、`lintRelease`、`assembleRelease` 通过；JVM 测试零失败、零错误、零跳过。
- [ ] Room 使用真实 v0.2.0 schema/记录完成 1→2 迁移：保留导览、行程和进度，删除旧 Key/路线，失败记录可恢复，重复迁移幂等。
- [ ] 大行程覆盖 200 点排序、10 点矩阵窗口、12 位置预览批次、25 目的地 SDK 批次、20 目的地生产配额批次、公交逐段、拖动受影响窗口和不可达点。
- [ ] 公交覆盖现在出发、指定出发、指定到达、停留时间倒推、交通方式/少走路/少换乘偏好、跨时区显示、步行接驳与精确失败段提示。
- [ ] Google 成功空路线只映射为无路线；HTTP 404、上游异常与格式异常不得误报为地区无公交；额度错误不得泄漏英文或保留虚假的引导状态。
- [ ] API 26/API 37 均通过冷启动、完整权限导览、迁移、遥测设置、两次离线恢复、前台服务、息屏模拟位置到达、崩溃检查和证据上传。
- [ ] tracked-source audit 与 APK audit 通过；APK 不含服务端 Google 私钥、VPS 凭据、签名密码、ORS Key、Transitous/ORS/OpenFreeMap/MapLibre 请求路径或 keystore。
- [ ] `apksigner verify --verbose --print-certs` 通过，证书 SHA-256 与公开 v0.2.0 相同；APK SHA-256 随 Release 发布。

## 真机与正式签名证据边界

- [ ] Xiaomi 15T Pro 的 Debug 发布候选完成界面、地图、无线路详情及 Google 地图交接验收，用户明确接受该候选并要求发布。
- [ ] 本地和 GitHub Actions 正式包均沿用固定签名，`versionName=0.2.2` / `versionCode=8`，并通过签名、R8、源码和 APK 内容审计。
- [ ] 若另行执行正式签名 v0.2.2 真机覆盖安装，必须记录是否从公开 v0.2.1 原位升级、是否保留数据；未执行时保持未勾选，不影响本次已明确接受的 UI 小版本发布边界。
- [ ] 用户明确同意 Google Navigation 条款，GMS 地图与当前位置正常。
- [ ] 驾车、骑行或步行完成 Google 原生语音、锁屏、偏航重路由、到达/停留/下一站和服务结束。
- [ ] 长行程跨生产 20 目的地配额批次，批次边界前完成下一次原子预留；额度耗尽时不绕过。
- [ ] 公交逐段路线展示线路、站点、换乘、时间和步行接驳，不朗读 Google 路线指令、不启动原生公交导航。
- [ ] 真实定位、断网/VPS 故障、额度错误和恢复路径符合隐私与本地进度保留要求。

## 发布

- [ ] RC 在 GitHub 标记为 Prerelease；稳定 v0.2.2 仅在发布前自动门禁、固定签名核验和用户明确接受的 Debug 真机 UI 验收完成后发布。
- [ ] 固定签名 keystore 位于工作区外且有离线加密备份；Actions Secrets 完整，日志未输出秘密或 Base64 keystore。
- [ ] 发布说明列出升级迁移、GMS 要求、联网要求、费用熔断、隐私变化、已知限制和证据边界。
- [ ] 精确 Release APK 在 API 26/API 37 完成下载、版本检查、安装、冷启动、首次导览和空崩溃缓冲区验证。
- [ ] 稳定 `v0.2.2` 发布后更新“Latest”状态和最终验收记录；RC 不标记为 Latest。
