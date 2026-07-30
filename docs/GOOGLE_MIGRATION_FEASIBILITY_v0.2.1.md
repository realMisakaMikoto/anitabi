# Google migration feasibility record for v0.2.1

Checked against current official documentation on 2026-07-30. This record freezes the implementation assumptions for v0.2.1; it does not rewrite any v0.2.0 evidence.

## Navigation SDK for Android

- Navigation SDK replaces the map layer as well as navigation. It must not be packaged alongside Maps SDK. Version 7.x requires minimum API 24 and target API 36, Google Play services, at least 2 GiB RAM, and OpenGL ES 2.0. The existing API 26 minimum and API 37 target remain compatible. [Setup requirements](https://developers.google.com/maps/documentation/navigation/android-sdk/setup-overview)
- The SDK supports driving, walking, cycling, and motorized two-wheel modes. v0.2.1 uses driving, walking, and human-powered cycling. [Travel modes](https://developers.google.com/maps/documentation/navigation/android-sdk/reference/com/google/android/libraries/navigation/RoutingOptions.TravelMode)
- A call accepts at most 25 destinations and billing is per destination. The Navigation Request SKU currently has 1,000 free destinations per month, so the local 90% ceiling is 900. Guidance start, traffic refresh, and deviation rerouting are not separately billed after the destinations have been fetched. [Usage and billing](https://developers.google.com/maps/documentation/navigation/android-sdk/pricing), [multi-destination routes](https://developers.google.com/maps/documentation/navigation/android-sdk/multi-destination)
- A VPS reservation cannot technically prevent a modified Android client from calling Navigation SDK directly. The production control is therefore layered: Android must reserve first, the VPS ledger stops at 900, the Android API key is package/signature/API restricted, and Google Cloud quota must be set independently.

## Routes API

- The only service endpoint is `https://routes.googleapis.com`; the REST methods are `/distanceMatrix/v2:computeRouteMatrix` and `/directions/v2:computeRoutes`. [REST reference](https://developers.google.com/maps/documentation/routes/reference/rest)
- Compute Routes Essentials permits at most 10 intermediate waypoints. This is the reason for the v0.2.1 road-preview batch of 12 locations: one origin, ten intermediate locations, and one destination. A request with more intermediate waypoints changes the SKU or exceeds the product plan. [Usage and billing](https://developers.google.com/maps/documentation/routes/usage-and-billing)
- Route Matrix bills per origin-destination element. Non-transit matrices allow up to 625 elements, while transit or `TRAFFIC_AWARE_OPTIMAL` matrices allow only 100. v0.2.1 deliberately uses square windows of at most 10 coordinates, always reserving at most 100 elements. [Matrix limits](https://developers.google.com/maps/documentation/routes/compute_route_matrix)
- WALK and BICYCLE Routes API results are beta and Google requires a warning anywhere those routes are displayed. The Android preview will include that warning. [Route travel modes](https://developers.google.com/maps/documentation/routes/reference/rest/v2/RouteTravelMode)
- Transit routes do not support intermediate waypoints. v0.2.1 therefore requests exactly one adjacent pair at a time and prioritizes the current and next pair. [Transit routes](https://developers.google.com/maps/documentation/routes/transit-route)
- The transit response exposes stop names/locations, arrival/departure times, line, vehicle, headsign, and stop count. It has no separate platform-number field, so v0.2.1 must not promise or synthesize platform data. [Transit response schema](https://developers.google.com/maps/documentation/routes/reference/rest/v2/TopLevel/computeRoutes)
- The current Essentials free cap is 10,000 monthly billable events for Compute Routes and 10,000 monthly elements for Compute Route Matrix. The VPS limits each bucket to 9,000. Navigation Request is an Enterprise SKU with a 1,000 free cap and is limited locally to 900. [Core pricing list](https://developers.google.com/maps/billing-and-pricing/pricing)
- Routes accepts an API key or OAuth token. The VPS uses a service-account JWT with the Cloud Platform scope to obtain a short-lived OAuth token; Android receives neither credential. [Routes usage and billing](https://developers.google.com/maps/documentation/routes/usage-and-billing), [Google Cloud REST authentication](https://cloud.google.com/docs/authentication/rest)

## Firebase and consent

- Firebase ID tokens sent to the VPS must be verified for RS256 signature, expiration, audience/project, issuer, and non-empty subject/UID. v0.2.1 additionally requires `firebase.sign_in_provider=anonymous`. Firebase public signing keys are cached according to their HTTP cache lifetime. [Verify ID tokens](https://firebase.google.com/docs/auth/admin/verify-id-tokens)
- Analytics collection is disabled in the manifest until the user opts in, can be enabled/disabled at runtime, and advertising ID plus ad-personalization signals remain disabled. [Analytics collection controls](https://firebase.google.com/docs/analytics/android/configure-data-collection)
- Crashlytics automatic collection is disabled in the manifest and enabled only after separate consent. Firebase documents that disabled Crashlytics can retain reports locally for later transmission; the implementation must delete unsent reports when consent is absent or withdrawn so pre-consent reports are not sent after a later opt-in. [Crashlytics opt-in reporting](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports#enable_opt-in_reporting)

## Frozen v0.2.1 decisions

- Road matrix and preview stay on Essentials features; traffic-aware Pro options, wildcard field masks, route optimization APIs, and response caching are out of scope.
- The API accepts departure time for contract stability, but road ordering uses returned distance/duration fields without a paid traffic-aware feature. Transit departure time is forwarded.
- Google route content remains in memory only. Room stores user-owned tour state and marks route details as requiring refresh after process death, upgrade, network failure, or quota failure.
