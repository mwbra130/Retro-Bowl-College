package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Room DAO lambda that reads feed items from the FeedItemEntity cache table.
 *
 * Verified against BeReal 3.97.1 (com.bereal.ft): Lig7;->invoke holds the two
 * feed queries, both parameterized by feedId and ordered by orderIndex:
 *   "SELECT DISTINCT * FROM FeedItemEntity WHERE feedId = ? ORDER BY orderIndex LIMIT ?"
 *   and a whitespace-padded variant of "SELECT * FROM FeedItemEntity WHERE ...".
 * FeedItemEntity has a real `isSponsored` column (see Lay4;->a CREATE TABLE),
 * so filtering on it is valid SQLite.
 */
object FeedItemsQueryFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "SELECT DISTINCT * FROM FeedItemEntity WHERE feedId = ? ORDER BY orderIndex LIMIT ?",
    ),
)

/**
 * Companion Room DAO lambda returning the post-id list for a feed.
 * Same table, same WHERE clause shape; patched for consistency.
 *
 * Verified against BeReal 3.97.1: the query lost its trailing `LIMIT ?`
 * compared to 3.96.0 (now ends at `ORDER BY orderIndex`).
 */
object FeedPostIdsQueryFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "SELECT DISTINCT postId FROM FeedItemEntity WHERE feedId = ? ORDER BY orderIndex",
    ),
)

/**
 * BeReal's AppLovin MAX initialization entry point.
 *
 * Verified against BeReal 3.97.1: Lcq;->invokeSuspend builds
 * AppLovinSdkInitializationConfiguration (mediation provider "max"), then
 * calls AppLovinSdk.initialize after the unique "INIT_ADS with segments "
 * log string.
 */
object AppLovinInitFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("INIT_ADS with segments "),
)

/**
 * Adjust SDK initialization entry point.
 *
 * Hooks the SDK's own `Adjust.initSdk(AdjustConfig)` instead of BeReal's
 * obfuscated wrapper (which was `Lm6a;->a(Ljv4;)` on 3.96.0 — the `Ljv4`
 * class was renamed on 3.97.1, breaking the old fingerprint). The SDK method
 * is stable across BeReal versions: BeReal builds the AdjustConfig and calls
 * this, which delegates to AdjustInstance.initSdk. No-oping it means no
 * Adjust attribution/tracking session is ever started.
 *
 * Verified against BeReal 3.97.1 with androguard: exactly one method
 * `Lcom/adjust/sdk/Adjust;->initSdk(Lcom/adjust/sdk/AdjustConfig;)V`
 * exists app-wide.
 */
object AdjustInitFingerprint : Fingerprint(
    definingClass = "Lcom/adjust/sdk/Adjust;",
    name = "initSdk",
    returnType = "V",
    parameters = listOf("Lcom/adjust/sdk/AdjustConfig;"),
)

/**
 * The "suggested people" card composable.
 *
 * Verified against BeReal 3.97.1: Luhi;->a renders the card identified by the
 * "suggested_people_card" string; it is invoked from the feed-item renderer
 * Lfd8;->a when the item content is of type Lhc8.
 */
object SuggestedPeopleCardFingerprint : Fingerprint(
    name = "a",
    returnType = "V",
    strings = listOf("suggested_people_card"),
)

/**
 * The LaunchedEffect lambda inside the in-feed video composable that
 * triggers autoplay.
 *
 * Behavioral fingerprint — no obfuscated class names are hardcoded because
 * they are reassigned every BeReal release (on 3.96.0 this was
 * `Lcvd;->invokeSuspend`; on 3.97.1 `Lcvd` is an unrelated class and the
 * same logic lives in `Llqd;->invokeSuspend`). It matches the single
 * app-wide `invokeSuspend` coroutine lambda that reads the player playback
 * state, rewinds via seekTo, and calls setPlayWhenReady.
 *
 * Uniqueness verified against BeReal 3.97.1 with androguard: the only other
 * methods calling this triple (getPlaybackState/seekTo/setPlayWhenReady)
 * are tap-to-play (`Lwe;->onClick`) and two non-coroutine helpers — none is
 * an `invokeSuspend`, so this matches exactly one method. Tap-to-play is
 * untouched.
 */
object VideoAutoplayFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    custom = { method, _ ->
        val refs = method.implementation?.instructions?.mapNotNull { instruction ->
            (instruction as? ReferenceInstruction)?.reference?.toString()
        }.orEmpty()
        refs.contains("Landroidx/media3/common/Player;->getPlaybackState()I") &&
            refs.contains("Landroidx/media3/common/Player;->seekTo(J)V") &&
            refs.contains("Landroidx/media3/common/Player;->setPlayWhenReady(Z)V")
    },
)

/**
 * PairIP/Guardsquare license check entry point.
 *
 * Verified against BeReal 3.97.1: Lcom/pairip/licensecheck/LicenseClient;->checkLicense
 * runs from the PairIP Application wrapper's attachBaseContext (before the real
 * app attaches) and from LicenseContentProvider.onCreate. On a re-signed APK the
 * verdict comes back NOT_LICENSED, and processResponse launches LicenseActivity
 * ("Get this app from Play" blocking screen) via startPaywallActivity.
 * The class name is the SDK's own (not obfuscated); the string pins the match.
 * Verified: the string appears in exactly one method app-wide, and the class
 * is defined in exactly one dex file — no ambiguity, no overloads.
 */
object LicenseCheckBypassFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context."),
)

/**
 * PairIP/Guardsquare license verdict handler.
 *
 * Verified against BeReal 3.97.1: Lcom/pairip/licensecheck/LicenseClient;->processResponse
 * is the single choke point where a licensing verdict becomes action —
 * response code 1 (NOT_LICENSED) launches the "Get this app from Play" screen
 * via startPaywallActivity. Only reachable through the AIDL callback
 * (LicenseClient$2.verifyLicense), downstream of checkLicense. The string
 * pins the match (verified: appears only in this method).
 */
object LicenseVerdictKillFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "processResponse",
    returnType = "V",
    parameters = listOf("I", "Landroid/os/Bundle;"),
    strings = listOf("License check succeeded."),
)

/**
 * PairIP/Guardsquare blocking activity ("Get this app from Play" screen).
 *
 * Verified against BeReal 3.97.1: Lcom/pairip/licensecheck/LicenseActivity;->onStart
 * reads the "activitytype" extra and shows either the paywall or the error
 * dialog. Runs in the main process, exported=false, referenced only from the
 * license flow. The string pins the match (verified: appears only here).
 */
object LicenseActivityKillFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseActivity;",
    name = "onStart",
    returnType = "V",
    parameters = listOf(),
    strings = listOf("Couldn't process license activity correctly."),
)

/**
 * PairIP/Guardsquare delayed app shutdown.
 *
 * Verified against BeReal 3.97.1: Lcom/pairip/licensecheck/LicenseClient;->scheduleAppShutdown
 * posts the exitAction runnable (System.exit(0)) on a delay after an
 * unlicensed verdict — the source of the "app has a bug" crash dialog.
 * Only called from the paywall/error paths. Method name verified unique
 * app-wide.
 */
object LicenseShutdownKillFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "scheduleAppShutdown",
    returnType = "V",
    parameters = listOf(),
)

/**
 * PairIP/Guardsquare (now "Phoenix") native library load.
 *
 * Derived from the 2026-09-24 logcat, not from dex inspection: the device's
 * nativeloader log shows the protection .so being loaded ~0.4s after launch,
 * and ~0.12s later the process dies with a SIGSEGV whose single-frame
 * backtrace is inside that same .so (deterministic across launches — a
 * deliberate native tamper-response kill, not a Java exception). The
 * Java-layer license patches can never fire because the process dies in
 * native code during library init.
 *
 * Verified against BeReal 3.97.1: the native library was renamed from
 * `libpairipcore.so` (3.96.0) to `libPhoenixAndroid.so` (Guardsquare's
 * "Phoenix" rebrand — confirmed present in the arm64 split's lib/ folder).
 * The const-string "PhoenixAndroid" appears in classes3.dex.
 *
 * The string must appear at the load call site — either as the
 * System.loadLibrary argument or as a substring of a full path passed to
 * System.load(".../libPhoenixAndroid.so"). The custom check pins the match
 * to the method that actually invokes System.loadLibrary/System.load, so a
 * stray log string can't false-positive. The patch no-ops only that invoke,
 * leaving the rest of the method (e.g. the rest of <clinit>) intact.
 */
object PairipNativeLoadFingerprint : Fingerprint(
    strings = listOf("PhoenixAndroid"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Ljava/lang/System;" &&
                (reference.name == "loadLibrary" || reference.name == "load")
        } == true
    },
)

/**
 * Voodoo ADN SDK master initialization — the single chokepoint for ALL ads.
 *
 * Verified against BeReal 3.97.1 with androguard on classes3.dex: this is
 * the only `initialize` method on
 * Lio/adn/sdk/internal/domain/management/SdkManager;, with the exact
 * signature
 *   initialize(Landroid/content/Context; Lio/adn/sdk/publisher/AdnInitializationCallback;)V
 * No-oping it means the SDK never initializes: isInitialized() stays false,
 * no bids are ever requested from bidder.voodoo-adn.com, no fullscreen or
 * native ad can load, and every mediated network behind ADN (AppLovin MAX,
 * AdMob, Pangle, InMobi, Moloco, Vungle) stays dead.
 */
object AdnSdkManagerInitFingerprint : Fingerprint(
    definingClass = "Lio/adn/sdk/internal/domain/management/SdkManager;",
    name = "initialize",
    returnType = "V",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lio/adn/sdk/publisher/AdnInitializationCallback;",
    ),
)
