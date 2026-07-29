# 巡礼手帖 v0.1.2

这是 Anitabi 网络访问边界加固版，包含 v0.1.1 的全部 Android 8 兼容修复、道路路线规划与连续导航能力。

## 本次修复

- 数据请求继续固定为官方 `api.anitabi.cn` 的作品轻量信息与地标详情端点，不请求 `anitabi.cn` 主域。
- Anitabi 封面和地标截图只接受官方 `image.anitabi.cn` 图片 API；非官方或伪装相似域名不会被自动加载。
- Coil 图片请求现在与数据请求使用同一个可识别 User-Agent：`AnitabiNavigator/0.1.2 (https://github.com/realMisakaMikoto)`。
- HTTP 403 提示明确指向当前公网 IP 被拒绝，建议停止重试并更换网络。
- 新增低频 API 边界探针；它仅在手动触发或探针工作流本身变更时执行，每次只请求一次官方数据示例和一次官方缩略图。

## 403 根因结论

此次报告的 Cloudflare 403 来自当前公网 IP 继承的历史封禁，不是应用请求了错误端点，也不是 User-Agent 或请求频率问题。GitHub Actions 的干净出口使用同一应用身份请求官方数据与图片 API 均已成功。应用不会尝试代理、绕过或高频重试 Cloudflare 封禁；受影响用户需要更换网络或联系运营商更换公网 IP。

## 下载校验

- APK：`anitabi-v0.1.2.apk`
- 大小：43,087,229 字节
- SHA-256：`4852fa44abafc7165feceae98f17147106e772ffbfa4824bdf7f391705f84e61`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

Release 同时提供机器可读的 `anitabi-v0.1.2.apk.sha256`。正式 APK 继续使用与 v0.1.0、v0.1.1 相同的固定签名证书。

## 已验证边界

- 34 个 JVM 单元测试、Android SDK 37 编译、release Lint、R8、APK 内容审计和签名校验由 GitHub Actions 执行。
- 同版本源码继续在 Android 8（API 26）与 Android 17（API 37）模拟器执行离线进程恢复、前台导航、息屏模拟 GPS 自动到达和 Room 进度持久化。
- 一次性边界探针仅访问文档授权的 `api.anitabi.cn` 与 `image.anitabi.cn`，两项均成功。

## 仍受外部条件限制

- 已被 Cloudflare 封禁的公网 IP 无法由 APK 修复；请更换网络或联系运营商。
- 实体手机上的真实 GPS、中文 TTS 音频、锁屏、OEM 后台限制、弱网和跨午夜行为仍需真机验收。
- Transitous 公交在此历史版本中因项目误读政策而在编译期关闭，不会发送公交路由请求；官方政策并不要求先取得明确批准，该错误已在 v0.1.3 纠正。

完整逐项证据见仓库中的 `docs/releases/v0.1.2.md`。

验证运行：[API 边界探针 30450517037](https://github.com/realMisakaMikoto/anitabi/actions/runs/30450517037)、[同版本源码运行时 CI 30451151980](https://github.com/realMisakaMikoto/anitabi/actions/runs/30451151980)、[签名发布 30451156419](https://github.com/realMisakaMikoto/anitabi/actions/runs/30451156419)、[签名 APK 兼容性复验 30451752045](https://github.com/realMisakaMikoto/anitabi/actions/runs/30451752045)。
