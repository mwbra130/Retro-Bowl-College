# Strava 486.9 APK inventory

## Verified package metadata

Read from the uploaded XAPK's `manifest.json`:

- Package: `com.strava`
- Version: `486.9`
- Version code: `12400193`
- min SDK: 29
- target SDK: 36
- Splits: base, `config.arm64_v8a`, `config.en`, `config.xxxhdpi`
- Base APK size: 101,126,753 bytes
- ARM64 split size: 43,574,359 bytes
- English split size: 254,162 bytes
- xxxhdpi split size: 16,191,227 bytes
- Base APK DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`, `classes4.dex`, `classes5.dex`, `classes6.dex`, `classes7.dex`, `classes8.dex`

## Candidate class names found in DEX class tables

These are class-table observations, not yet verified method targets.

| Feature area | Candidate classes |
|---|---|
| Promotions | `com.strava.metering.data.Promotion`, `com.strava.metering.data.PromotionTypeInterface` |
| Following feed | `com.strava.feed.net.FollowingFeedList`, `com.strava.you.feed.YouFeedFragment` |
| Athlete feed | `com.strava.profile.view.SingleAthleteFeedFragment` |
| App icon UI | `com.strava.subscriptionscustomappicons.AppIcon`, `com.strava.subscriptionscustomappicons.CustomAppIconsActivity` |
| Privacy-aware route display | `com.strava.activitydetail.universal.data.GetPolylinePrivacyUseCase`, `com.strava.activitydetail.universal.data.FilterPolylineForPublicPointsUseCase` |
| Recording/pace models | `com.strava.recording.data.ActiveActivityStats`, `com.strava.recording.data.PaceGuidanceState`, `com.strava.recording.data.TimedDistancePoint` |

## Interpretation and caveats

- These names establish useful investigation areas only. They do not establish that a given class controls the visible UI element, nor do they reveal the correct patch method/signature.
- Subscription-related UI and billing code is present. The project will not alter entitlement checks or grant paid/server-side features.
- Privacy code must be treated carefully: any display customization must preserve Strava's existing privacy protections.
- The local inspection environment did not contain JADX or APKTool, so method-level decompilation and resource mapping remain outstanding.
