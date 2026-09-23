package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills BeReal's PairIP/Guardsquare delayed app shutdown.
 *
 * When the license flow decides the install is unlicensed,
 * `LicenseClient.scheduleAppShutdown()` posts the `exitAction` runnable
 * (`System.exit(0)`) on a delay — killing the process while the blocking
 * activity is foregrounded, which surfaces as Android's "Something went
 * wrong / app has a bug" crash dialog.
 *
 * Returning void at its entry removes the delayed kill. It is only ever
 * called from the paywall/error paths, so neutering it cannot affect normal
 * app behavior.
 */
@Suppress("unused")
val licenseShutdownKillPatch = bytecodePatch(
    name = "Kill license delayed shutdown",
    description = "Backup: stops the app from force-closing itself after the license check.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // scheduleAppShutdown returns void; returning immediately means the
        // exit runnable is never scheduled.
        LicenseShutdownKillFingerprint.method.addInstructions(0, "return-void")
    }
}
