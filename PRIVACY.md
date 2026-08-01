# 巡礼手帳 v0.2.1 隐私说明

最后更新：2026-07-30

## 本机保存的数据

应用只持久化用户拥有或明确选择的数据：首次导览状态、多作品选择、Anitabi 巡礼点、手动顺序、起终点、出行模式、停留设置、已完成点和导航状态。应用禁止 Android 系统备份和设备迁移。

Google 路线矩阵、路线、折线、步骤、预计时间和公交详情仅保存在当前进程内存中，不写入 Room、SharedPreferences、文件或备份。v0.2.0 升级时会移除旧 ORS Key 密文、IV 和旧路线内容；无法迁移的原记录会保留并向用户显示恢复错误，不会静默清库。

## 路线请求

规划或刷新路线时，应用会将完成请求所需的坐标、出行模式、优化目标、出发或到达时间以及公交偏好经 HTTPS 发送到项目自建 API。Android 使用 Firebase Anonymous Auth 获取匿名 ID Token；不需要提供邮箱、姓名或密码。

自建 API 验证匿名 Token 后，通过服务器端 Google 服务账号调用固定的 Google Routes 上游。服务账号私钥不会进入 APK。服务器不缓存 Google 矩阵、路线、折线、步骤或公交正文。

服务器配额账本按匿名 UID 记录计费类别、日期/月度周期和已预留数量。内存限速主要按匿名 UID 计算；原始 IP 只在内存中转换为带服务器秘密的 HMAC 值，用于更宽松的辅助限速。应用与服务器日志不得包含 Token、原始 IP、坐标、动漫名、搜索词、请求正文或 Google 响应正文。

## Google Navigation 与 Firebase

驾车、骑行和步行道路导航由设备上的 Google Navigation SDK 提供。Google SDK 可能根据其适用条款处理设备位置和导航交互；项目无法替代 Google 的隐私政策。

Firebase Analytics 与 Firebase Crashlytics 为两个独立、可选的开关：

- 两项默认关闭，只有用户明确选择加入后才启用。
- Analytics 只允许版本、设备能力、模式、点数区间、延迟区间和错误类型；不允许坐标、动漫名、搜索词、路线正文或用户 ID。
- 首次启用 Analytics 前会清理同意前的本机分析状态；撤回后关闭收集并重置本机分析数据。
- 首次启用 Crashlytics 前会删除同意前产生的未发送报告；撤回后立即删除未发送报告，并在下次启动完全停止收集。

用户可随时在“关于、隐私与数据来源”页面分别撤回两项同意。

## Bangumi 与 Anitabi

动漫搜索直接访问 Bangumi API。巡礼点 JSON 和图片由 Android 以可识别的 `AnitabiNavigator/0.2.1` User-Agent、按用户操作低频访问 Anitabi 官方 API 与图片域名，不经过项目 VPS。应用不抓取 Anitabi 主站，也不打包或批量下载全站数据。

## 保留、故障与联系

自建 API 的 SQLite 配额账本与审计状态位于项目 VPS，并执行七日一致性备份。若恢复后无法证明额度没有回退，计费请求保持关闭。路线内容不进入这些备份。

断网、VPS 故障或费用熔断时，已保存的行程和进度仍留在设备上，路线暂时无法刷新。卸载应用会按 Android 行为删除其本机私有数据。

问题与安全报告方式见 [SECURITY.md](SECURITY.md)，项目联系人为 [realMisakaMikoto](https://github.com/realMisakaMikoto)。Google 与 Firebase 的数据处理还受其各自服务条款和隐私政策约束。
