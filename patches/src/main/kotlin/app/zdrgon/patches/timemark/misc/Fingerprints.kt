package app.zdrgon.patches.timemark.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Plus entitlement — Timemark v10.0.316 ────────────────────────────
//
// Timemark sells "Plus" ($49.99/yr personal premium) and a Business
// plan. Billing is the Play Billing Library directly (product
// plus_1year_onetime); there is no RevenueCat/Superwall, no PairIP
// and no signature self-check. Entitlement is issued by the
// developer's server and cached locally:
//
//   GET member/vip/info  ->  VipInfo { isVip, vipExpireTime,
//     vipType (0 none / 1 basic / 2 premium), vipVersion,
//     watermarkSaveQuota }
//
// The refresh coroutine Ldb0/l;->invokeSuspend is the bean's main
// consumer: it writes the bean into the state holder Ldb0/n;
//
//   Ldb0/n;->a Z   = VipInfo.isVip()          (in-memory mirror)
//   Ldb0/n;->b J   = VipInfo.getVipExpireTime()
//   prefs "key_is_vip" = isVip, "key_vip_expire_time" = expireTime
//   (persisted through Lcom/xhey/xcamera/util/f;)
//
// and hands the quota bean to Leb0/d;->f(...). From then on every
// local gate reads the state back through three readers on Ldb0/n;
// (verified by a full 8-dex caller census, 2026-11-16):
//
//   Ldb0/n;->d()Z  = key_is_vip && expireTime > now   (15 callers,
//     incl. WatermarkRemovedPreviewActivity)
//   Ldb0/n;->e()Z  = d() || config["key_team_space_is_business_
//     plan_user"]                                     (70 callers:
//     settings, custom filename, watermark style, markup editor,
//     camera widgets, paywall — the dominant reader)
//   Ldb0/n;->g()Lp50/e; = (d() || business) ? ACTIVE :
//     queried ? NON_MEMBER : UNKNOWN                  (5 callers)
//
// vipType is never persisted and no feature gate reads it; locally
// the app has exactly one premium boolean, and that boolean IS the
// Plus state. Business is a separate server-pushed account flag
// plus workgroup server APIs (shared storage, team billing, admin)
// and has no local representation to pin.
//
// The free-tier photo-save quota is a second state machine:
// Leb0/d;->a()Leb0/a; derives UNLIMITED / NON_MEMBER /
// QUOTA_AVAILABLE / QUOTA_EXHAUSTED from Ldb0/n;->d()Z plus the
// server-issued WatermarkSaveQuota bean (type/limit/remaining)
// stored by Leb0/d;. Its monitor() sibling delegates to the same
// reader family.
//
// NOTE: Morphe fingerprints compare access flags by EXACT equality.
// VipInfo getters are "public final" (0x0011); the Ldb0/n; and
// Leb0/d; readers are "public static" (0x0009).

/** The server bean's isVip getter. Pinning it makes the app's own refresh persist the Plus state. */
object VipIsVipFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Lcom/xhey/xcamera/data/model/bean/vip/VipInfo;",
    name = "isVip"
)

/** The server bean's expiry getter. Pinned to 2100-01-01 so the persisted state never lapses. */
object VipExpireTimeFingerprint : Fingerprint(
    returnType = "J",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Lcom/xhey/xcamera/data/model/bean/vip/VipInfo;",
    name = "getVipExpireTime"
)

/** The plain local VIP reader (persisted prefs + clock). */
object VipReaderDFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Ldb0/n;",
    name = "d"
)

/** The dominant VIP reader (d() OR business-plan flag), ~70 call sites. */
object VipReaderEFingerprint : Fingerprint(
    returnType = "Z",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Ldb0/n;",
    name = "e"
)

/** The membership-state enum reader used by the paywall/state UI. */
object VipStateFingerprint : Fingerprint(
    returnType = "Lp50/e;",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Ldb0/n;",
    name = "g"
)

/** The watermark photo-save quota reader. Pinned to UNLIMITED. */
object SaveQuotaFingerprint : Fingerprint(
    returnType = "Leb0/a;",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Leb0/d;",
    name = "a"
)

// ── Location funnel — Timemark v10.0.316 ────────────────────────────
//
// Every GPS fix flows through one class: Lcom/xhey/xcamera/location/
// GlobalLocationService; is the ONLY subclass of the location engine
// Lcom/xhey/xcamera/location/a;, and its refreshPlace is called only
// from that engine. The fix object is Lry/c;:
//
//   Lry/c;->a D = latitude,  Lry/c;->b D = longitude
//
// (proven by refreshPlace building new double[]{fix.a, fix.b} as the
// lat/lng pair for both the server place-list request and the local
// Geocoder). refreshPlace resolves the fix into LocationInfoData
// (largeposition / smallposition / addressList / EXIF
// LocationDetail) — the exact strings the watermark prints — so
// overwriting a/b at refreshPlace entry redirects the server place
// lookup, the geocoder, the geo-address cache keys and the composed
// watermark text in one move.
//
// refreshPlace is "public" (0x0001), 36 registers, 4 ins: with
// parameters (Lry/c;, List, List) the fix sits in v33. The patch
// writes it through dexlib2 builder instructions, because the
// inline smali compiler silently dropped this snippet's iput-wide
// instructions on two consecutive desktop verification runs.

/** The single location funnel entry. The spoof overwrites the fix's lat/lng here. */
object RefreshPlaceFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Lry/c;", "Ljava/util/List;", "Ljava/util/List;"),
    accessFlags = listOf(AccessFlags.PUBLIC),
    definingClass = "Lcom/xhey/xcamera/location/GlobalLocationService;",
    name = "refreshPlace"
)

// ── Telemetry — Timemark v10.0.316 ──────────────────────────────────
//
// Census verified 2026-11-16 across all 8 dexes and the manifest:
//
// SensorsData is the analytics platform (694 SDK class refs, plus
// 88 AB-test and 186 Sensors Focus classes; SensorsDataContent-
// Provider + SensorsFocusContentProvider in the manifest). The app
// sends its product analytics — clicks, watermark edits, purchase
// funnel, and location-page events that carry lat/lng and address
// properties — through exactly two funnels, both ending in
// SensorsDataAPI.track:
//
//   1. Lcom/xhey/xcamera/util/SensorAnalyzeUtil;->wrapSensorsTrack
//      (both overloads) — the analytics facade's own sinks; every
//      one of its dozens of event methods ends here.
//   2. Lcom/xhey/xcamera/services/LogService;->track (both
//      overloads) — the implementation behind the framework
//      logger's track() (Xlog / Lty/h;), called all over the app;
//      LogService.monitor() delegates to it as well.
//
// Firebase is present (Analytics, Crashlytics, Messaging,
// Sessions) but the app code does not call FirebaseAnalytics
// directly (13 referencing classes are SDK registrars and camera
// classes referencing the registrar chain); its upload plumbing is
// Google datatransport, whose three components are declared in the
// manifest. Install-referrer API classes are present for ad/SDK
// attribution. The ad SDK sprawl (AppLovin, AdMob, FAN, Vungle,
// Mintegral, Bigo, Pangle, Unity, Tramini, Qumeng, AdSurge) is ads,
// not telemetry, and is out of scope for this patch.
//
// First-party API reporters on the feature client
// (reportDeviceFeature, reportActiveVersion, addEventCount,
// saveUserAttribution) are deliberately NOT pinned: they are
// suspend calls returning response beans that feature code
// consumes (the event counters feed server-side feature
// accounting), so they are dual-use feature traffic, not a clean
// telemetry funnel. See RECON.md for the full census.
//
// Access flags: wrapSensorsTrack overloads are "public static"
// (0x0009); LogService.track overloads are "public" (0x0001).

/** SensorsData sink #1: the analytics facade's event sink (with properties). */
object WrapSensorsTrackPropsFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/json/JSONObject;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Lcom/xhey/xcamera/util/SensorAnalyzeUtil;",
    name = "wrapSensorsTrack"
)

/** SensorsData sink #1b: the analytics facade's event sink (no properties). */
object WrapSensorsTrackFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    definingClass = "Lcom/xhey/xcamera/util/SensorAnalyzeUtil;",
    name = "wrapSensorsTrack"
)

/** SensorsData sink #2: the framework logger's backing track (with properties). */
object LogServiceTrackPropsFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/json/JSONObject;"),
    accessFlags = listOf(AccessFlags.PUBLIC),
    definingClass = "Lcom/xhey/xcamera/services/LogService;",
    name = "track"
)

/** SensorsData sink #2b: the framework logger's backing track (no properties). */
object LogServiceTrackFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    accessFlags = listOf(AccessFlags.PUBLIC),
    definingClass = "Lcom/xhey/xcamera/services/LogService;",
    name = "track"
)
