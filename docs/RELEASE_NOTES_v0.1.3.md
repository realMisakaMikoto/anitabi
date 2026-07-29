# 巡礼手帖 v0.1.3

这是 Transitous 公交启用纠正版，包含 v0.1.2 的全部网络边界加固与 Android 8 兼容修复。

## 本次纠正

- Transitous 官方政策没有“必须取得明确批准后才能启用”的条件；此前将建议联系维护者误写成硬审批门槛，是项目实现与文档错误。
- 已删除 `ANITABI_TRANSITOUS_APPROVED` 构建变量、`BuildConfig` 开关、API 拦截异常及 UI 禁用逻辑，公交规划现在可直接使用。
- 项目此前已通过 Matrix 说明低频用途；这项沟通符合官方对潜在高负载用法的建议，但不再被当作授权流程。

## 请求负载边界

- 只访问正式地址 `https://api.transitous.org/api/v6/plan`。
- 发送 `AnitabiNavigator/0.1.3 (https://github.com/realMisakaMikoto)` User-Agent。
- 公交最多 8 个巡礼点，按路段串行查询，不并发请求。
- 仅在用户生成路线、手动重算或到站/取消事件触发时请求；不后台轮询、不批量下载、不爬取、无自动重试。
- 已生成路线保存在本机，断网时继续使用旧路线；关于页与路线结果保留 Transitous 数据来源链接。

## 已知限制

- Transitous 为 best-effort 服务，在日本只覆盖部分地区；无行程时应用会提示“本区域暂无开放公交数据”。
- 已被 Cloudflare 封禁的公网 IP 无法由 APK 修复；请更换网络或联系运营商。
- 实体手机上的真实 GPS、中文 TTS 音频、锁屏、OEM 后台限制、弱网和跨午夜行为仍需真机验收。

完整逐项证据见仓库中的 `docs/releases/v0.1.3.md`。
