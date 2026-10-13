package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills Adjust SDK initialization.
 *
 * Hooks the Adjust SDK's own `initSdk(AdjustConfig)` (void method), so no
 * Adjust attribution/tracking session is ever started, regardless of how
 * BeReal's obfuscated wrapper code changes between versions.
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
        // initSdk returns void: return-void at index 0 skips the entire
        // method body. No registers are touched.
        AdjustInitFingerprint.method.addInstructions(0, "return-void")
    }
}
