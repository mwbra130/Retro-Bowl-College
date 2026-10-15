package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Diagnostic: makes every Purchase report as PURCHASED.
 *
 * Hooks Purchase.getPurchaseState() to always return 1 (PURCHASED).
 * This is the simplest possible billing hook — if this builds, the
 * issue is with the complex smali, not the fingerprint approach.
 * If this fails, the issue is fundamental to billing hooks.
 */
@Suppress("unused")
val purchaseStateTestPatch = bytecodePatch(
    name = "TEST Purchase State",
    description = "Diagnostic: forces all purchases to report as purchased.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        PurchaseStateFingerprint.methodOrNull?.let {
            it.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """.trimIndent(),
            )
        }
    }
}
