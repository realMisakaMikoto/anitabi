# 巡礼手帖 v0.1.4

> 这是历史版本，仅供兼容与版本记录。偏好稳定版请安装 [v0.2.0](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.0)；愿意使用已通过真机验证的预发布候选，可选择 [v0.2.1 RC7](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.1-rc.7)。

## 这个版本修复了什么

- 顶部内容不再被状态栏遮挡，底部按钮也会避开系统手势区域。
- 提高 Anitabi 可选字段变化时的兼容性，避免一条异常记录导致整部作品无法加载。
- Transitous 未提供距离时，会根据路线几何计算并显示合理距离。
- 过滤无法绘制的退化路线，避免无效几何警告。
- 公交时间按上下车站的当地时区显示。

## 安装与使用

- 支持 Android 8.0 及以上版本。
- 驾车、骑行和步行仍需要用户自行申请并填写 ORS Key。
- Transitous 是 best-effort 公共服务，在日本只覆盖部分地区。

## 文件校验

- APK：`anitabi-v0.1.4.apk`
- 大小：43,103,613 字节
- SHA-256：`4a95482bdc9bdec9e357d334339f9a401f558b00f19b4160b519ea9af586240e`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

Release 同时提供 `anitabi-v0.1.4.apk.sha256`。详细的历史测试记录见 [`docs/releases/v0.1.4.md`](https://github.com/realMisakaMikoto/anitabi/blob/main/docs/releases/v0.1.4.md)。
