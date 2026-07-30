# v0.2.1 发布说明

v0.2.1 将地图、道路导航和路线规划全面迁移到 Google，并通过项目自建 VPS 实施匿名鉴权、费用硬熔断与隐私边界。这是从公开 v0.2.0 原位升级的版本，沿用同一 RSA-4096 签名。

## 主要变化

- Google Navigation SDK 负责地图、定位以及驾车、骑行、步行的原生道路导航、偏航处理和语音。
- Google Routes API 负责矩阵、道路预览和相邻两点的公交路线；服务端响应会被规范化，不向 Android 透传完整 Google 正文。
- 行程总点数不再有 8/12 点产品上限。应用在手机本地生成全局顺序，并自动拆成最多 10 点矩阵窗口、12 位置道路预览批次和 25 目的地 SDK 批次。
- Firebase Anonymous Auth 为自建 API 提供匿名身份。Analytics 与 Crashlytics 分别默认关闭、独立选择加入，并支持撤回。
- 新增 `StoredTourV2`：只持久化用户点位、顺序、设置和导航进度。Google 矩阵、路线、折线、步骤、预计时间与公交详情仅驻留内存。
- 移除 MapLibre、OpenFreeMap、openrouteservice、Transitous、ORS Key 输入与存储，以及旧公交授权逻辑。

## v0.2.0 升级

- 必须直接覆盖安装，不要卸载或清除数据。
- 保留首次导览完成状态、多作品选择、行程顺序、设置和导航进度。
- 删除旧 ORS Key 密文、IV 与旧路线内容；升级后的路线需要联网刷新。
- 迁移失败时保留原记录并显示恢复错误，不静默清空数据库。

## 服务、费用与隐私

- 生产 API：`https://api.anitabi.afunnypersonlol0.site`。
- VPS 月度硬上限为 Matrix 9,000 个计费元素、Route 9,000 次、Navigation 900 个目的地，并叠加 UID 每日额度与突发限速。达到上限或账本状态不确定时停止请求。
- VPS 不缓存 Google 路线内容；日志不包含 Token、原始 IP、坐标、动漫名、搜索词或请求/响应正文。
- Anitabi 继续由 Android 以 `AnitabiNavigator/0.2.1` 低频访问官方 API 和图片域名，不经过 VPS。

## 要求与限制

- 地图和路线功能要求可用的 Google Play 服务与网络；无 GMS 设备只能查看已保存的点位、顺序和进度。
- 公交按相邻两点逐段规划，不启动 Google 原生公交导航；站台只显示 Google 上游实际提供的文字。
- “无限点”只表示行程总点数不限，Google 单次请求和项目日/月额度仍严格受限。
- 断网、VPS 故障或费用熔断时保留行程与进度，但路线暂时不能刷新。

## RC 说明

`v0.2.1-rc.N` 是正式签名预发布候选，不是稳定版。RC2 修复异常 Anitabi 可选字段与多作品联合选择；RC3 修复 Navigation SDK 与 R8 类合并造成的正式版崩溃。RC3 随后在另一台真机暴露地图工厂未初始化崩溃。RC4 删除客户端位图标记工厂依赖并在地图操作失败时安全回退到列表，但 Xiaomi 15T Pro 复验确认真实地图仍未出现。RC5 尝试在创建 `NavigationView` 前调用 `MapsInitializer`；精确公开 APK 的模拟器兼容门禁通过，但 Xiaomi 15T Pro 首次打开和重试仍只进入列表回退。后续核对官方 Navigation SDK 参考及 7.8.0 字节码确认该 API 对 Navigation SDK 不适用，且 RC5 Release R8 产物删除了动态 `CreatorImpl` 的反射无参构造，因此 RC5 不得晋升稳定版。后续候选必须移除该错误提前调用、窄保留反射构造器，并同时修复地图视图尚未完成布局时执行边界相机更新的竞态；只有真实底图、点位和最小两点路线真机验收通过后，才能继续稳定版发布。

许可证：GPL-3.0-or-later，并附 `LICENSE` 中仅针对 Google Navigation/Firebase SDK 的窄范围链接例外。第三方服务与数据署名见 `NOTICE.md`。
