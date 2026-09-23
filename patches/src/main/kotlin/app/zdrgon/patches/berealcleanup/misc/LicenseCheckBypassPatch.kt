package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Bypasses BeReal's PairIP/Guardsquare license check.
 *
 * BeReal ships the PairIP (Guardsquare) app-protection SDK. Its Application
 * wrapper (Lcom/pairip/application/Application) calls
 * LicenseClient.checkLicense in attachBaseContext — before the real BeReal
 * application even attaches — and LicenseContentProvider calls it again in
 * onCreate. On a re-signed (patched) APK the verdict comes back NOT_LICENSED,
 * and processResponse launches LicenseActivity's "Get this app from Play"
 * blocking screen via startPaywallActivity, so the patched app can never be
 * used.
 *
 * This patch returns void at the entry of checkLicense, so the check never
 * runs: no licensing service is bound, no verdict is computed, and the
 * blocking activity is never launched. It covers all three call sites at
 * once (attachBaseContext, LicenseContentProvider.onCreate, and the internal
 * service-reconnect retry).
 *
 * Why this is safe:
 * - Nothing outside the license-check flow reads its state. The verdict,
 *   response payload, and check-state fields are only consumed by
 *   processResponse / initializeLicenseCheck, which are only reachable from
 *   within the check flow itself. BeReal's own code never touches
 *   LicenseClient.
 * - PairIP's separate SignatureCheck.verifyIntegrity runs *before*
 *   checkLicense in attachBaseContext, and it passes for re-signed APKs
 *   (verified empirically: the v1.2.0 patched build sailed past it and only
 *   stopped at the license screen), so no signature-check bypass is needed.
 * - The method returns void, so an entry return-void has no register or
 *   stack side effects.
 */
@Suppress("unused")
val licenseCheckBypassPatch = bytecodePatch(
    name = "Bypass license check",
    description = "Skips BeReal's PairIP license check so the re-signed app isn't blocked " +
        "by the 'Get this app from Play' screen. Required for the patched app to launch at all.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // checkLicense returns void; returning immediately means the
        // licensing service is never bound and no verdict is ever computed.
        LicenseCheckBypassFingerprint.method.addInstructions(0, "return-void")
    }
}
