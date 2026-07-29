# Transitous 路由使用联系模板

以下内容已填入公开仓库、版本和联系人，可直接通过 Transitous API 页面列出的 Matrix 社区发送。

## English template

Subject: Permission request for low-volume routing use by Anitabi Navigator

Hello Transitous maintainers,

I am developing Anitabi Navigator, a personal, non-commercial Android pilgrimage navigation app. The source code is public under GPL-3.0-or-later at https://github.com/realMisakaMikoto/anitabi, and APKs are distributed for free through GitHub Releases only.

The app would use `https://api.transitous.org/api/v6/plan` only after explicit user actions:

- at most 8 selected stops per tour;
- sequential point-to-point requests to build one itinerary;
- cached generated tours for offline continuation;
- rerouting only after arrival, a missed connection, or sustained deviation;
- at least 60 seconds between deviation reroutes;
- no bulk data download, background crawling, analytics, advertising, or commercial use;
- User-Agent: `AnitabiNavigator/0.1.0 (https://github.com/realMisakaMikoto)`.

The public transit feature is disabled by default and will remain disabled unless you approve this routing usage. Please let me know whether this usage is acceptable and whether you require additional limits or attribution.

Thank you,

realMisakaMikoto / https://github.com/realMisakaMikoto

## 取得回复后

1. 保存维护者的明确同意和附加限制，不要只保存“已发送”。
2. 把日期、联系渠道和限制写入项目私有发布记录；不要公开对方不希望公开的个人信息。
3. 只有明确同意后，才设置 `ANITABI_TRANSITOUS_APPROVED=true`。
4. 每次发布重新检查 <https://transitous.org/api/>；如政策改变或维护者撤回同意，立即关闭该变量。
