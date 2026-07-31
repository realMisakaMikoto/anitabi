# 安全策略

## 报告安全问题

仓库目前没有公开的私密联系地址。如需报告漏洞，请先新建一个不含漏洞细节的公开 Issue，只说明希望与维护者私密沟通，并等待维护者提供后续渠道。不要在公开 Issue、截图或日志中粘贴 Token、API Key、服务账号文件、VPS 凭据、签名材料、密码或精确家庭位置。

报告时请尽量提供应用版本、Android 版本、可重复步骤和已经脱敏的错误类型，不要提供真实坐标、动画名称、搜索词或路线正文。

## 客户端与服务端密钥边界

- APK 只包含受应用包名、签名证书和 Navigation SDK API 限制的 Android 客户端 Key。
- Firebase Android 配置不作为服务端凭据使用，并由源码审计阻止误提交。
- Google 服务账号私钥只保存在受限的服务端或部署密钥位置，不进入 APK、源码、Git、应用日志或发布附件。
- Release keystore 必须位于项目工作区外。GitHub Actions 只从加密 Secrets 恢复到临时目录。
- 丢失固定签名私钥后无法为现有安装提供可覆盖升级的 APK，因此必须保存受保护的离线备份。

## 数据与隐私边界

- 应用使用 Firebase 匿名鉴权，不要求邮箱、姓名或密码。
- Firebase Analytics 和 Crashlytics 默认关闭，分别取得用户同意后才启用，并支持撤回。
- 路线请求所需的坐标、模式、优化目标和时间经项目自建 HTTPS API 发送到固定的 Google Routes 上游。
- Google Navigation SDK 会在设备上直接处理当前位置与道路导航交互；具体数据处理方式见隐私政策。
- Google 路线矩阵、折线、步骤、预计时间和公交详情仅保存在进程内存中，不持久化到 Room、SharedPreferences、文件或备份。
- 应用的行程持久化只保存用户选择的作品、巡礼点、顺序、设置、完成状态和导航状态。本机还会保存导览与遥测同意设置、公共数据缓存和 Firebase 匿名鉴权状态；应用关闭 Android 系统备份与设备迁移。
- 服务端日志不得记录 Token、原始 IP、坐标、动画名称、搜索词、请求正文或 Google 响应正文。

详细数据处理方式见 [隐私政策](PRIVACY.md)。

## 发布要求

每个公开 APK 必须通过测试、Android Lint、R8 Release 构建、反射兼容审计、源码与 APK 密钥审计、固定签名验证、SHA-256 校验以及 [发布检查清单](docs/RELEASE_CHECKLIST.md)。
