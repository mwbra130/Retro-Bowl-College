# Strava Custom Morphe Bundle v0.1

Status: **APK inventory complete; patch implementation pending method-level decompilation and validation**.

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

## Explicit exclusions

- No bypassing subscriptions, paid features, billing, or server-side access controls.
- No spoofing GPS, activity metrics, or data uploaded to Strava.
- No changing privacy filtering in ways that expose private route/location data.
- No claiming a patch works until it compiles and has been tested against the target APK.

## Validation checklist

- [x] Confirm target package/version and inspect XAPK split and DEX inventory.
- [ ] Decompile candidate classes and verify method signatures/call sites.
- [ ] Implement one isolated patch at a time using the repository's existing Morphe conventions.
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
