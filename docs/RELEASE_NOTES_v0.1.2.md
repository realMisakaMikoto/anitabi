# 巡礼手帳 v0.1.2

> 这是历史版本，仅供兼容与版本记录。偏好稳定版请安装 [v0.2.0](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.0)；愿意使用已通过真机验证的预发布候选，可选择 [v0.2.1 RC7](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.1-rc.7)。

## 这个版本修复了什么

- Anitabi 数据只从官方 API 域名加载，图片只从官方图片域名加载。
- 数据和图片请求统一使用可识别的应用身份。
- 当前网络出口被公共服务拒绝时，会提示停止重复请求并尝试更换网络。
- 应用不会代理、绕过或高频重试 Cloudflare 封锁。

## 安装与使用

- 支持 Android 8.0 及以上版本。
- 道路路线仍需要用户自行申请并填写 ORS Key。
- 本版本未启用公交路线。
- 如果公网 IP 已被 Anitabi 的防护系统封锁，APK 本身无法解除封锁。

## 文件校验

- APK：`anitabi-v0.1.2.apk`
- 大小：43,087,229 字节
- SHA-256：`4852fa44abafc7165feceae98f17147106e772ffbfa4824bdf7f391705f84e61`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

Release 同时提供 `anitabi-v0.1.2.apk.sha256`。详细的历史测试记录见 [`docs/releases/v0.1.2.md`](https://github.com/realMisakaMikoto/anitabi/blob/main/docs/releases/v0.1.2.md)。
