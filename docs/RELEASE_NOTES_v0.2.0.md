# 巡礼手帳 v0.2.0

> 这是当前稳定版。愿意使用已通过真机验证的 Google 地图预发布候选，可选择 [v0.2.1 RC7](https://github.com/realMisakaMikoto/anitabi/releases/tag/v0.2.1-rc.7)。

## 这个版本带来了什么

- 新增三步首次使用导览，集中说明用途、权限和 ORS Key 设置。
- 支持同时选择多部动画，把所有巡礼点合并到同一张地图中。
- 可以跨多次搜索继续添加作品，也可以随时移除已选作品。
- 多作品巡礼点可以直接生成一条联合路线。
- 改进权限提示、路线请求字段、公交错过换乘后的重算和网络错误显示。
- 新增完整的应用图标。

## 安装与使用

- 支持 Android 8.0 及以上版本。
- 本版本仍使用 MapLibre/OpenFreeMap、ORS 和 Transitous。
- 驾车、骑行和步行需要用户自行申请并填写 ORS Key。
- 道路路线最多选择 12 个巡礼点，公交最多选择 8 个巡礼点。

升级到 v0.2.1 RC7 时请直接覆盖安装，不要卸载或清除数据。升级会保留导览完成状态、作品选择、行程顺序、设置和导航进度，同时移除本机保存的旧 ORS Key 与旧路线缓存。

## 文件校验

- APK：`anitabi-v0.2.0.apk`
- 大小：42,677,141 字节
- SHA-256：`e3d36b47695b452978680726c5eb09133e04c0f207149a6324f3e08ac8f9a9ec`
- 签名证书 SHA-256：`9679c83769368c7150f629d9cba3c0e5d633fa7f1043ce251fdba6c7c64fb00a`

Release 同时提供 `anitabi-v0.2.0.apk.sha256`。详细的历史测试记录见 [`docs/releases/v0.2.0.md`](https://github.com/realMisakaMikoto/anitabi/blob/main/docs/releases/v0.2.0.md)。
