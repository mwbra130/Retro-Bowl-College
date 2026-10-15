package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Makes Unity IAP report all purchases as PURCHASED.
 *
 * Hooks Unity's PurchaseBridge.getPurchaseState() (not Google's BillingClient)
 * to always return 1 (PURCHASED). If the game checks purchase state via Unity
 * IAP, this will unlock the red slot machine.
 *
 * This tests whether Unity wrapper classes avoid the build failure that
 * Google's billing classes trigger.
 */
@Suppress("unused")
val unityPurchaseStatePatch = bytecodePatch(
    name = "Unity IAP Purchase State",
    description = "Forces Unity IAP to report all purchases as purchased.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        UnityPurchaseBridgeFingerprint.methodOrNull?.let {
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
