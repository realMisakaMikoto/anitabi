# 第三方服务、数据与署名

本项目代码采用 GPL-3.0-or-later；以下服务、数据和依赖保留其各自权利与许可。

- 地图渲染：MapLibre Native。
- 地图服务：OpenFreeMap；地图样式包含 OpenMapTiles 数据，底层地图数据为 © OpenStreetMap contributors。应用地图界面固定显示署名。
- 道路路线：openrouteservice / HeiGIT，路线基于 © OpenStreetMap contributors。路线预览和导航界面显示来源。
- 公共交通：Transitous / MOTIS。具体 GTFS、NeTEx、实时数据和运营方许可依地区不同，完整来源以 <https://transitous.org/sources/> 为准。
- 动漫元数据：Bangumi API。
- 巡礼点与截图：Anitabi API，仅用于非商业用途；数据采用 CC BY-NC-SA 4.0。应用保留每张截图的 `origin` 和 `originURL`，且只缓存用户访问的数据。

运行应用会直接访问这些公共服务。它们是独立服务，不由本项目运营，也不提供可用性保证。
