package app.zdrgon.patches.callrecorder.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.callrecorder.shared.Constants.COMPATIBILITY_CALL_RECORDER

/**
 * Call Recorder — Enable Lifetime.
 *
 * The license verdict is computed natively (libcallrec_v2.so) from
 * the account's entitlements on the developer's server and posted
 * back to Java, where exactly one method stores it:
 * App.i(state, sub) writes the state to the static App.t field, and
 * every check in the app reads it back through App.e(). The state
 * values (read off the About screen's own branch table):
 * 0 = check pending, 1 = trial, 2 = trial expired,
 * 3 = license verified (permanent, no expiry),
 * 4 = subscription (carries an expiry timestamp).
 * Every "is licensed" test in the app accepts exactly {3, 4}.
 *
 * This patch rewrites the state argument at the single writer so
 * the stored state is always 3 — the same permanent verified state
 * the $49.99 Lifetime license produces. No server response, token
 * or entitlement is fabricated; the sync still runs and reports
 * what it reports, the app just never stores a lesser state. The
 * trial time gates consult the same state (through rv0.c()), so
 * they pass at any date.
 *
 * Experimental, honestly: the verdict computation and the
 * recording engine are native. If the native library enforces the
 * license on its own (separate server state) instead of trusting
 * the Java state it sets, the app will look licensed while the
 * engine stays limited — only an on-device test (record past the
 * trial limits) settles which way this build behaves.
 */
@Suppress("unused")
val enableLifetimePatch = bytecodePatch(
    name = "Enable Lifetime",
    description = "Pins the license state to the permanent verified state (the $49.99 " +
        "Lifetime tier) at its single writer, so the app always runs as a lifetime " +
        "license. Experimental: the license verdict is computed in the app's native " +
        "library, so whether the recording engine honors the Java-side state can only " +
        "be settled on a device. Call Recorder 3.7.8 only.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_CALL_RECORDER)

    execute {
        // v2 is the first parameter (the state) — proven from the
        // method itself: sput v2 lands in App.t, sput v3 in App.y.
        // Overwriting it at entry makes every store a store of 3.
        AppSetStateFingerprint.method.addInstructions(
            0,
            """
                const/4 v2, 0x3
            """.trimIndent()
        )
    }
}
