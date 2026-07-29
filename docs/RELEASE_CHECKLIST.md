# 发布检查清单

以下项目全部满足后才能创建 `v*` tag。公交没有获得明确同意不阻止道路版发布，但必须保持关闭。

本文件是每次发布使用的模板，不直接勾选。每个版本应在 `docs/releases/` 保存一份带证据的实际验收记录；未验证项必须明确保留为未完成。

## 政策与许可

- [ ] 重新核对 OpenFreeMap 署名、隐私和使用条款；地图仍显示 OpenFreeMap、OpenMapTiles、© OpenStreetMap contributors。
- [ ] 重新核对 ORS Standard 套餐、限制、API 域名和服务条款；没有项目共享 Key。
- [ ] 重新核对 Transitous API 使用政策和数据来源。
- [ ] 若启用公交，已保存维护者明确同意与附加限制，并设置 `ANITABI_TRANSITOUS_APPROVED=true`；否则变量为 `false` 或未设置。
- [ ] Anitabi 数据请求仅访问 `api.anitabi.cn`，自动图片请求仅访问 `image.anitabi.cn`，两者都发送可识别 User-Agent；截图仍显示 `origin` 并保留 `originURL`；未打包全量数据。
- [ ] `LICENSE`、`NOTICE.md`、关于页和 GPL-3.0-or-later 声明一致。

## 自动验证

- [ ] `./gradlew testDebugUnitTest lintRelease assembleRelease --stacktrace` 通过。
- [ ] R8 release 构建没有缺失类或序列化/Room/MapLibre 反射错误。
- [ ] `apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk` 通过，证书 SHA-256 与上一版本一致。
- [ ] APK SHA-256 已生成并随 GitHub Release 发布。
- [ ] 扫描源码和解包 APK：没有 ORS Key、keystore、密码、Google Billing、Firebase/分析/广告 SDK。
- [ ] 扫描源码：没有旧 `api.openrouteservice.org`，没有跳转 Google Maps/Organic Maps 等外部导航应用的 Intent。

## 模拟器与真机

- [ ] Android 8、当前稳定 Android 和 target SDK 对应 Android 版本至少各启动一次。
- [ ] 搜索动漫 → 选择作品 → 地图区域批选 → 生成道路路线 → 一次开始连续导航完整走通。
- [ ] 2、8、12 点的自由终点、指定终点、返回起点均验证。
- [ ] 无效 ORS Key、429 配额耗尽、404/5xx 和弱网提示可理解，不泄露响应或 Key。
- [ ] GPS 跳点未立即重算；步行/骑行 60 米、驾车 100 米持续 15 秒后才重算，60 秒冷却有效。
- [ ] 到达、停留、自动下一站、手动到达、偏航、锁屏、杀进程、重启恢复和跨午夜均验证。
- [ ] 断网后旧路线可继续；重算失败保留旧路线；未批量下载 OpenFreeMap 瓦片。
- [ ] 拒绝定位或通知权限时应用不崩溃且提示明确。
- [ ] 公交启用时，在有覆盖区域验证步行、线路、方向、站台、中途站、换乘、取消、无实时、地下无 GPS、错过班次和严重延误。
- [ ] 无公交覆盖区域明确显示“本区域暂无开放公交数据”，没有伪造路线。

## 发布

- [ ] `versionCode` 递增，`versionName` 与 tag 一致。
- [ ] 固定签名 keystore 在工作区外且已有离线加密备份。
- [ ] GitHub Actions Secrets 完整，日志中没有打印密码或 Base64 keystore。
- [ ] 仓库公开，Release 免费分发，不上传应用商店。
- [ ] 发布说明列出政策核对日期、已知覆盖限制、隐私变化和升级注意事项。
