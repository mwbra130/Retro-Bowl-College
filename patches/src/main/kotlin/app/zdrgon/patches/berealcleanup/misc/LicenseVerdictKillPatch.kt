package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills BeReal's PairIP/Guardsquare license-verdict handler.
 *
 * `LicenseClient.processResponse(I, Bundle)` is the single choke point where a
 * licensing verdict turns into action: response code 1 (NOT_LICENSED) reads
 * the PAYWALL_INTENT and launches the "Get this app from Play" blocking
 * screen; any other unexpected code throws into the error-dialog path. It is
 * only reachable through the AIDL licensing-service callback
 * (`LicenseClient$2.verifyLicense`), which is itself downstream of
 * `checkLicense`.
 *
 * Returning void at its entry means no verdict is ever processed — no
 * paywall, no error dialog, no repeated-check rescheduling — no matter how
 * the callback gets invoked. This is the second layer behind the
 * checkLicense bypass: even if that patch somehow missed, this one alone
 * still prevents the block (the AIDL callback simply becomes a no-op).
 */
@Suppress("unused")
val licenseVerdictKillPatch = bytecodePatch(
    name = "Kill license verdict handler",
    description = "Makes BeReal's PairIP license-verdict handler a no-op so a " +
        "NOT_LICENSED verdict can never trigger the 'Get this app from Play' " +
        "screen. Backup layer behind the license-check bypass.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // processResponse returns void; returning immediately means the
        // verdict code is never examined and the paywall is never launched.
        LicenseVerdictKillFingerprint.method.addInstructions(0, "return-void")
    }
}
