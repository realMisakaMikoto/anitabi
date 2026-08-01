# Transitous 路由使用沟通记录

Transitous 官方政策要求项目为 FOSS/非营利，并要求正确的 User-Agent、正式 API 地址和可见的数据来源链接；对于可能造成较高负载或对负载有疑虑的用法，官方建议提前联系。政策没有要求等待明确批准后才能启用。

以下原始说明已于 2026-07-29 通过官网列出的 Matrix 社区发送，用于主动告知项目的低频路由用途。联系是负载沟通记录，不是应用功能的审批门槛。原消息最后错误地把“获得批准”写成启用条件；这是发送者对政策的误读，不是 Transitous 的要求，已在 v0.1.3 中纠正。

## 沟通内容（产品品牌名已更新）

Subject: Permission request for low-volume routing use by 巡礼手帳

Hello Transitous maintainers,

I am developing 巡礼手帳, a personal, non-commercial Android pilgrimage navigation app. The source code is public under GPL-3.0-or-later at https://github.com/realMisakaMikoto/anitabi, and APKs are distributed for free through GitHub Releases only.

The app would use `https://api.transitous.org/api/v6/plan` only after explicit user actions:

- at most 8 selected stops per tour;
- sequential point-to-point requests to build one itinerary;
- cached generated tours for offline continuation;
- rerouting only after arrival, a missed connection, or sustained deviation;
- at least 60 seconds between deviation reroutes;
- no bulk data download, background crawling, analytics, advertising, or commercial use;
- User-Agent: `AnitabiNavigator/0.1.2 (https://github.com/realMisakaMikoto)`.

The public transit feature is disabled by default and will remain disabled unless you approve this routing usage. Please let me know whether this usage is acceptable and whether you require additional limits or attribution.

Thank you,

realMisakaMikoto / https://github.com/realMisakaMikoto

## 后续处理

1. 保存维护者提出的附加限制，并落实到代码、发布检查清单和文档。
2. 不公开对方不希望公开的个人信息。
3. 每次发布重新检查 <https://transitous.org/api/>；如果政策或项目请求负载变化，重新评估并主动沟通。
