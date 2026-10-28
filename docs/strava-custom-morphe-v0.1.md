# Strava Custom Morphe Bundle v0.1

Status: **Patches implemented for five of the six planned features; CI build and device testing pending**. The additional local pace calculations feature is deferred: see "Feature 6 investigation" below.

This work is being developed in the existing Morphe patches repository. It does not replace the repository's existing patches.

## Target APK inspected

- App: Strava for Android
- Package: `com.strava`
- Version: `486.9`
- Version code: `12400193`
- Minimum Android SDK: 29
- Target SDK: 36
- Input: XAPK with base APK and ARM64, English, and xxxhdpi splits
- Base APK DEX inventory: `classes.dex` through `classes8.dex`
- Patch framework: Morphe
- Build task: `./gradlew buildAndroid` (Windows: `./gradlew.bat buildAndroid`)
- Expected bundle output: `patches/build/libs/patches-*.mpp`

The XAPK manifest identifies the package/version and split inventory. A first-pass DEX class-table scan found relevant namespaces listed in [the APK inventory report](strava-486.9-apk-inventory.md). This is not yet a method-level decompilation, and the candidate classes below are leads rather than verified patch targets.

## v0.1 goals

The initial scope is limited to client-side customization that does not unlock paid/server-side entitlements:

1. Theme or appearance customization, if a reliable local hook exists.
2. Hide selected promotional UI elements, where they can be identified reliably.
3. Hide selected feed cards or sections through local UI filtering.
4. Local branding options only if resource and launcher-icon handling support them.
5. Optional privacy-oriented display changes that do not falsify shared activity data.
6. A local pace/distance helper only if it can be added without interfering with recording or uploaded activity data.

These are candidate features, not claims that they are already implemented or confirmed to work.

## Initial candidate areas from the 486.9 DEX class tables

- Promotions: `com.strava.metering.data.Promotion`, `PromotionTypeInterface`
- Feed: `com.strava.feed.net.FollowingFeedList`, `com.strava.you.feed.YouFeedFragment`, `com.strava.profile.view.SingleAthleteFeedFragment`
- Local app icon feature area: `com.strava.subscriptionscustomappicons.AppIcon`, `CustomAppIconsActivity` (investigate resource/launcher configuration only; do not alter subscription entitlement checks)
- Activity privacy display: `com.strava.activitydetail.universal.data.GetPolylinePrivacyUseCase`, `FilterPolylineForPublicPointsUseCase`
- Recording and pace-related models: `com.strava.recording.data.ActiveActivityStats`, `PaceGuidanceState`, `TimedDistancePoint`
- Subscription UI exists in dedicated `com.strava.subscriptions...` namespaces; it is out of scope to bypass paid access.

These names were discovered in the APK's DEX class tables. Method signatures, call sites, resource IDs, and whether a local hook is safe still need verification against the exact APK before implementation.

## Implementation (verified against the 486.9 APK)

Method-level inspection was done with JADX and DEX analysis on the exact XAPK (base APK 101,126,753 bytes; all four split sizes match the inventory above, and the base reports versionCode 12400193). Implemented patches, all under `app.zdrgon.patches.strava` with an extension in `extensions/strava`:

1. **Custom theme colors** (resource patch): replaces the brand orange literal `#FC5200` (design token `color/core_o3` and derived accent colors) in `res/values/colors.xml` and the night configuration with a selectable accent color. Screens whose colors are hardcoded in code are not affected.
2. **Hide subscription promotions** (bytecode patch + extension): Strava's feed and many screens are server-driven modular framework content (186 classes under `com.strava.modularframework`). The patch intercepts the modular model getters (`getEntries`, `getModules`, `getChildrenEntries`, `getFields`, `getField`, `getMenuItems`, `getProperties`, `getStateMap`, `getSubmodules`) and the extension drops modules named `*_upsell`, `promo`, or `top_of_tab_promo`, plus anything destined for `strava://subscription/checkout`. Toggles: Upselling, Promotions.
3. **Hide feed elements** (bytecode patch, depends on patch 2): same filter, additional toggles for `suggested_follows`, `suggested_challenges`, `challenge`, `club`, and `highlighted_activity_lookback` modules.
4. **Custom app name and icon** (resource patch): the name edits `@string/app_name` (the label used by the app and all launcher aliases, including the `values-en` override from the language split). The icon enables one of the 15 built-in `SplashActivity*Icon` activity-aliases already in the manifest and disables the default SplashActivity launcher component, the same end state the in-app icon picker produces.
5. **Local route privacy trim** (bytecode patch + extension): wraps `FilterPolylineForPublicPointsUseCase.invoke(List, Lpow;)`, which returns the activity map polyline already trimmed by Strava's own per-point privacy (the `pow` range of first/last non-"Only me" points, computed by `GetPolylinePrivacyUseCase`). The wrapper passes the result through a distance-based trimmer that removes a selectable number of meters (default 200) from both ends, on this device only. It never widens the visible range and never touches recorded or uploaded data.

### Feature 6 investigation (additional local pace calculations): deferred

Every surface that displays pace was inspected: the recording UI's `StatType` registry (`com.strava.recordingui.domain.models.StatType$AveragePace` etc., driven by the stats picker), the activity-detail `PaceSplitRow` list (leadingLabel, paceLabel, barWidthRatio), and the modular-framework stats modules fed from the server. All three require multi-point UI surgery (new stat type registration plus a formatter branch, or fabricating server-shaped module data) whose rendering cannot be verified without iterating on a device. Shipping a blind UI patch risks a broken recording or activity screen, so this feature is deferred until it can be built against live device feedback. The other five features have single-point, statically verifiable hooks and are implemented.

## Explicit exclusions

- No bypassing subscriptions, paid features, billing, or server-side access controls.
- No spoofing GPS, activity metrics, or data uploaded to Strava.
- No changing privacy filtering in ways that expose private route/location data.
- No claiming a patch works until it compiles and has been tested against the target APK.

## Validation checklist

- [x] Confirm target package/version and inspect XAPK split and DEX inventory.
- [x] Decompile candidate classes and verify method signatures/call sites.
- [x] Implement one isolated patch at a time using the repository's existing Morphe conventions.
- [ ] Compile the patch bundle with `buildAndroid`.
- [ ] Verify patch metadata and supported-version constraints.
- [ ] Test app launch, login, feed, profile, and activity recording.
- [ ] Test each patch independently and verify disabling it restores default behavior.
- [ ] Document tested Strava version(s) and known limitations.

## Build

From the repository root on Windows:

```powershell
.\\gradlew.bat buildAndroid
```

The generated `.mpp` should appear under `patches/build/libs/`.

## Next step

Use a DEX decompiler (for example JADX) on this exact base APK, then inspect candidate methods and their call sites. The current environment has no JADX/APKTool executable available, so this pass stops at verified package/version and DEX class-table discovery rather than fabricating method-level targets.
