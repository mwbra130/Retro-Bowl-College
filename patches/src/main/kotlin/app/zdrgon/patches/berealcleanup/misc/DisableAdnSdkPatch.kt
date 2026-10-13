package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills every ad served through Voodoo's ADN SDK in one hook.
 *
 * Voodoo's ADN (`io.adn.sdk`) is BeReal's primary ad stack: interstitials via
 * AdnFullscreenAd/FullscreenAdActivity, sponsored feed cards via
 * AdnNativeAdRenderer, and all mediated networks behind it (AppLovin MAX,
 * AdMob, Pangle, InMobi, Moloco, Vungle). Everything only loads after
 * SdkManager.initialize() runs — returning void at index 0 means the SDK
 * never initializes: isInitialized() stays false, no bids are ever requested,
 * no ad surface can load.
 *
 * Complements (does not replace) the other ad patches: AdSdkInitKillPatch
 * stops BeReal's direct AppLovin init, TrackerProvidersPatch strips the
 * auto-init content providers, and SponsoredPostsFilterPatch removes
 * first-party sponsored rows from the feed cache.
 *
 * Runtime verification checklist:
 *  1. Patch, build, install the APK.
 *  2. `adb logcat | grep AdnSdk` — expect NO init/config lines from
 *     io.adn.sdk.internal.common.logger.Logger.
 *  3. Exercise every known ad surface: cold start, post a BeReal, scroll
 *     the feed — confirm zero ads and zero crashes.
 *  4. Known risk: BeReal code waiting on AdnInitializationCallback never
 *     fires. If anything hangs or crashes, the narrower fallback is to
 *     keep initialize() but hook the getters instead:
 *     getFullscreenAdInstance() -> return null (interstitials only),
 *     getNativeAdInstance() -> return null (sponsored feed cards only).
 */
@Suppress("unused")
val disableAdnSdkPatch = bytecodePatch(
    name = "Disable Voodoo ADN ads",
    description = "No-ops the Voodoo ad SDK init so no ads can load — " +
        "interstitials, sponsored feed cards, and every mediated network behind it.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // initialize(Context, AdnInitializationCallback)V — return-void at
        // index 0 touches no registers; both parameters are simply ignored.
        AdnSdkManagerInitFingerprint.method.addInstructions(0, "return-void")
    }
}
