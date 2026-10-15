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
 * AdMob, Pangle, InMobi, Moloco, Vungle).
 *
 * The init hook does NOT just return-void: BeReal's ad code runs on a
 * periodic refresh timer, and a never-firing AdnInitializationCallback
 * leaves it in a broken state that crashes (observed: ~5s crash loop on
 * 3.97.1, gone in airplane mode). Instead, initialize() immediately fires
 * the callback with AdnInitializationStatus.Failure — BeReal then takes its
 * normal "ads unavailable" path.
 *
 * Note: the ad factory getters are intentionally NOT hooked. Forcing them
 * to return null caused crashes when BeReal didn't null-check; letting the
 * uninitialized SDK handle it is safer.
 *
 * Complements (does not replace) the other ad patches: AdSdkInitKillPatch
 * stops BeReal's direct AppLovin init, TrackerProvidersPatch strips the
 * auto-init content providers, and SponsoredPostsFilterPatch removes
 * first-party sponsored rows from the feed cache.
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
        // initialize(Context, AdnInitializationCallback)V — fire the callback
        // with Failure instead of nopping. BeReal handles init failure
        // gracefully (same as airplane mode); a never-firing callback does
        // not. p0=this, p1=Context, p2=callback; v0 is free to clobber
        // since we return immediately after.
        AdnSdkManagerInitFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lio/adn/sdk/publisher/AdnInitializationStatus;->Failure:Lio/adn/sdk/publisher/AdnInitializationStatus;
                invoke-interface {p2, v0}, Lio/adn/sdk/publisher/AdnInitializationCallback;->onCompletion(Lio/adn/sdk/publisher/AdnInitializationStatus;)V
                return-void
            """.trimIndent(),
        )
    }
}
