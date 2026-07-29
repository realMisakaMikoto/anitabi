# 安全策略

## 报告问题

请通过 <https://github.com/realMisakaMikoto> 联系维护者。报告中不要粘贴真实 ORS Key、签名私钥、精确家庭位置或其他敏感信息。

## 密钥边界

- 应用不提供共享 ORS Key。每位用户在应用内输入自己的 Key。
- ORS Key 经 Android Keystore AES-GCM 加密后保存；不进入日志、备份、崩溃报告或源码。
- release keystore 必须位于项目工作区外。GitHub Actions 只从加密 Secrets 恢复到 runner 临时目录。
- 丢失固定签名私钥后无法为现有安装提供可升级 APK；请离线保存至少一份加密备份。

## 隐私边界

- 应用没有分析、广告、账号、云同步或位置上传日志。
- 规划和偏航重算需要把相关坐标发送给 ORS；公交启用后发送给 Transitous；地图请求发送给 OpenFreeMap。
- Room 路线和进度、Anitabi 缓存均留在应用私有空间，且应用关闭系统备份与设备迁移。

## 发布要求

每个发布必须通过测试、Android Lint、R8 release 构建、APK 签名校验、共享密钥扫描和 [发布检查清单](docs/RELEASE_CHECKLIST.md)。
