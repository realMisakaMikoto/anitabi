# 巡礼手帖 v0.1.1

这是 Android 8 导航兼容性修复版，包含 v0.1.0 的全部道路路线规划与连续导航能力。

## 本次修复

- 地图运行时固定使用 MapLibre 官方 OpenGL 构建，避免不支持 Vulkan 的 Android 8 设备或模拟器在进入导航地图时崩溃。
- 补齐旧版 Android `LocationListener` 必需的兼容回调，避免前台导航服务在 API 26 收到定位更新时崩溃。
- Anitabi 公共 API 返回 HTTP 403 时显示明确的服务拒绝访问提示，不再与普通 API 错误混淆。
- 主分支 CI 增加 Android 8（API 26）与 Android 17（API 37）运行时验证：全程断网、强制停止与两次恢复、前台服务通知、两站手动到达、完成状态与 Room 进度持久化。
- APK 内容审计持续拒绝旧 ORS 域名、禁用 SDK、签名密码标记、私钥标记和 keystore 文件。

## 下载校验

- APK：`anitabi-v0.1.1.apk`
- 大小：43,087,229 字节
- SHA-256：`efd41a3f6186e0da5784cfb93cf6816d1a3720ee332dee243058a8b90e1a5787`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

Release 同时提供机器可读的 `anitabi-v0.1.1.apk.sha256`。

## 已验证边界

- 33 个 JVM 单元测试、Android SDK 37 编译、release Lint、R8、APK 内容审计和签名校验由发布工作流执行。
- 修复后的同版本源码已在 API 26 与 API 37 模拟器完成离线进程恢复和前台导航状态流；系统崩溃缓冲与 DropBox 均未发现应用崩溃。
- 发布后的实际签名 APK 已由独立兼容性工作流在 API 26 与 API 37 复验下载、安装、版本信息和冷启动；两端崩溃缓冲为空。

## 仍受外部条件限制

- 实体手机上的真实 GPS、中文 TTS 音频、锁屏、OEM 后台限制、弱网和跨午夜行为仍需真机验收。
- 部分网络访问 Anitabi 官方 API 会被 Cloudflare 403 拦截；上游问题为 [anitabi.cn-document#86](https://github.com/anitabi/anitabi.cn-document/issues/86)。
- Transitous 公交未获得维护者明确同意，仍在编译期关闭，不会发送公交路由请求。

完整逐项证据见仓库中的 `docs/releases/v0.1.1.md`。

验证运行：[同版本源码运行时 CI 30446068593](https://github.com/realMisakaMikoto/anitabi/actions/runs/30446068593)、[签名发布 30446070504](https://github.com/realMisakaMikoto/anitabi/actions/runs/30446070504)、[签名 APK 兼容性复验 30446437144](https://github.com/realMisakaMikoto/anitabi/actions/runs/30446437144)。
