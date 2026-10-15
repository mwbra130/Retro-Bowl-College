package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills every ad served through Voodoo's ADN SDK.
 *
 * Voodoo's ADN (`io.adn.sdk`) is BeReal's primary ad stack: interstitials via
 * AdnFullscreenAd/FullscreenAdActivity, sponsored feed cards via
 * AdnNativeAdRenderer, and all mediated networks behind it (AppLovin MAX,
 * AdMob, Pangle, InMobi, Moloco, Vungle).
 *
 * NOTE: As of v1.14.2, the SdkManager.initialize() hook is DISABLED.
 * Both nopping it (v1.9.2) and firing onCompletion(Failure) (v1.9.3+) caused
 * a ~5s crash loop. The other ad patches (AppLovin kill, provider strip,
 * sponsored post filter) are sufficient to block ads without touching the
 * ADN init.
 *
 * This patch is kept as a placeholder for future ADN-specific hooks.
 * Currently it does nothing.
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
        // SdkManager.initialize() hook DISABLED as of v1.14.2.
        // Both nopping (v1.9.2) and firing Failure (v1.9.3-v1.14.1) caused
        // a ~5s crash loop. The other ad patches handle blocking without it.
    }
}
