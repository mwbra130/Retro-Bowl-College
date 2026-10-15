package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Blocks BeReal's interstitial ads at the display layer.
 *
 * Voodoo's ADN (`io.adn.sdk`) renders fullscreen ads through
 * FullscreenAdActivity. Hooking onCreate() to finish() immediately prevents
 * any ad UI from appearing, without touching SDK initialization.
 *
 * Why not hook SdkManager.initialize()? Both nopping it (v1.9.2) and firing
 * onCompletion(Failure) (v1.9.3-v1.14.1) caused a ~5s crash loop. The SDK
 * init is left alone; we just block the display.
 *
 * Sponsored feed cards (first-party) are handled by SponsoredPostsFilterPatch.
 */
@Suppress("unused")
val disableAdnSdkPatch = bytecodePatch(
    name = "Disable Voodoo ADN ads",
    description = "Blocks fullscreen ad display — no interstitial ad UI appears.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // FullscreenAdActivity.onCreate(Bundle): p0=this, p1=savedInstanceState.
        // Finish immediately so no ad UI appears. The SDK init is untouched.
        try {
            FullscreenAdActivityOnCreateFingerprint.method?.addInstructions(
                0,
                """
                    invoke-virtual {p0}, Lio/adn/sdk/internal/ui/fullscreen/FullscreenAdActivity;->finish()V
                    return-void
                """.trimIndent(),
            )
        } catch (e: Exception) {
            // If the activity isn't found, skip.
        }
    }
}
