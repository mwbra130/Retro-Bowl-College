package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Prevents BeReal's PairIP/Guardsquare blocking activity from ever showing.
 *
 * `LicenseActivity` is the "Get this app from Play" screen (and the license
 * error dialog). Its `onStart` reads the `activitytype` extra and either
 * shows the paywall or the error dialog. It has no legitimate purpose beyond
 * blocking unlicensed installs.
 *
 * Finishing the activity at the entry of `onStart` means that even if it is
 * launched through some unforeseen path, it closes before rendering
 * anything. `finish()` on an Activity is safe to call before
 * `super.onStart()`; the subsequent `return-void` skips the rest.
 */
@Suppress("unused")
val licenseActivityKillPatch = bytecodePatch(
    name = "Kill license blocking activity",
    description = "Makes BeReal's 'Get this app from Play' blocking activity " +
        "finish itself immediately on start, so it can never be displayed. " +
        "Backup layer behind the license-check bypass.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // p0 is the Activity instance; finish() + return-void skips the
        // paywall/error-dialog branch entirely.
        LicenseActivityKillFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                return-void
            """.trimIndent(),
        )
    }
}
