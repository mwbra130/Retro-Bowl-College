package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills Adjust SDK initialization.
 *
 * BeReal's Adjust entry point (Lm6a;->a) constructs an AdjustConfig, calls
 * Adjust.initSdk, and registers BeReal's own Adjust lifecycle callbacks.
 * Returning Kotlin's Unit singleton at method entry runs none of that, so no
 * Adjust attribution/tracking session is ever started.
 *
 * Note: deep links processed for attribution (Adjust.processDeeplink) stop
 * reporting attribution; normal link-opening behavior is unaffected.
 */
@Suppress("unused")
val adjustInitKillPatch = bytecodePatch(
    name = "Disable Adjust tracking",
    description = "Stops Adjust analytics tracking.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // The method returns Object; Kotlin Unit is the Lndn; singleton.
        // Returning immediately means no later instruction runs, so
        // overwriting v0 here is safe.
        AdjustInitFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, Lndn;->a:Lndn;
                return-object v0
            """.trimIndent(),
        )
    }
}
