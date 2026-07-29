# 巡礼手帖 v0.1.0

首个公开签名版，实现零预算、无自建后端、全应用内的动漫圣地巡礼道路路线规划与连续导航基础能力。

## 已包含

- 通过 Bangumi 搜索作品，并从 Anitabi 加载、署名和缓存用户访问的巡礼点。
- 在 MapLibre + OpenFreeMap 地图或列表选点，支持聚合、单选和当前地图范围批选。
- 驾车、骑行、步行最多 12 个巡礼点；支持自由终点、指定终点和返回起点。
- 使用用户自己的 ORS Standard Key 进行 Matrix 和多停靠 Directions，请求地址为 `api.heigit.org`。
- 应用内路线预览、手动重排、定位前台服务、中文 TTS、到达/停留/下一站、偏航限频重算和 Room 进度恢复。
- 公交实现保留在源码中，但本版本因项目当时误读 Transitous 政策而在编译期关闭，不会发送公交路由请求；该误读已在 v0.1.3 纠正。

## 下载校验

- APK：`anitabi-v0.1.0.apk`
- SHA-256：`e4c1c66cefaea54fde0c7f3f6bfbba461530465e5ef4530c02fa956bcd12624d`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

## 政策与隐私

政策于 2026-07-29 重新核对：OpenFreeMap 公共实例仍免费且要求地图数据署名；ORS Standard 当前为 Directions 2,000 次/日、Matrix 500 次/日；Transitous 仍要求路由等重资源用法事先联系。应用无广告、分析、账号、云同步和位置日志，不包含共享 ORS Key。

## 已知限制

- 每位用户必须自行申请并在应用内输入免费 ORS Key。
- 公共服务均为 best-effort，没有 SLA；第一版不支持完整离线地图或断网重新规划。
- Android 编译、单测、Lint、R8、签名验证和 APK 内容审计已通过；实际签名 APK 已在 Android 8（API 26）与 Android 17（API 37）模拟器冷启动，同版本源码另已通过 Android 16（API 36）debug 冷启动。实体手机上的 GPS、锁屏通知、后台限制和 TTS 音频仍待验收。
- Transitous 公交在此历史版本中保持关闭，不能将源码中存在公交实现理解为此 APK 已启用公交；这不是官方审批要求。
- 发布后验证环境访问 Anitabi 官方 API 时被 Cloudflare 403 拦截，已向上游提交 [anitabi.cn-document#86](https://github.com/anitabi/anitabi.cn-document/issues/86)；这可能导致部分网络无法加载巡礼点。
- v0.1.0 APK 对该 403 会显示通用 API 错误；主分支已改为明确提示“公共服务拒绝了当前网络的访问”，修复将在后续版本包含。

完整逐项证据见仓库中的 `docs/releases/v0.1.0.md`。

兼容性运行：[当前主分支 CI 30439622908](https://github.com/realMisakaMikoto/anitabi/actions/runs/30439622908)、[签名 v0.1.0 APK 30440179352](https://github.com/realMisakaMikoto/anitabi/actions/runs/30440179352)。
