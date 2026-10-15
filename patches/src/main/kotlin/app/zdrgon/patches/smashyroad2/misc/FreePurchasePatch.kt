package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

private const val BILLING_FAKER =
    "Lapp/template/extension/smashyroad2/BillingFaker;"

/**
 * Fakes Google Play Billing purchases — "click buy and it goes through."
 *
 * Hooks:
 * 1. BillingClient.Builder.setListener() — captures the game's
 *    PurchasesUpdatedListener so we can invoke it later.
 * 2. BillingClient.launchBillingFlow() — instead of showing Google Play UI,
 *    immediately reports a successful purchase to the listener.
 *
 * The fake Purchase uses a dummy signature. Games that don't verify
 * signatures locally (most Unity games, including Smashy Road 2) accept it
 * and grant the item.
 *
 * This is purchase piracy: it grants paid items without payment. The patch
 * code does no network I/O and exfiltrates no data.
 */
@Suppress("unused")
val freePurchasePatch = bytecodePatch(
    name = "Free purchases",
    description = "Clicking buy on any IAP immediately succeeds without " +
        "payment — Google Play UI never appears.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    // Note: extendWith is NOT called here — UnlimitedCoinsPatch already merges
    // extensions/extension.mpe (which contains BillingFaker). Calling extendWith
    // twice for the same extension breaks the build.

    execute {
        // Hook 1: Capture the listener when the game sets it.
        // Builder.setListener(PurchasesUpdatedListener): p0=this(Builder), p1=listener.
        // Call BillingFaker.setListener(p1) via Object param.
        try {
            BillingSetListenerFingerprint.method.addInstructions(
                0,
                """
                    invoke-static {p1}, $BILLING_FAKER->setListener(Ljava/lang/Object;)V
                """.trimIndent(),
            )
        } catch (e: Exception) {
            // If the fingerprint doesn't match, skip (don't break the build).
        }

        // Hook 2: Intercept launchBillingFlow and fake success.
        // BillingClient.launchBillingFlow(Activity, BillingFlowParams): p0=this, p1=activity, p2=params.
        // Call BillingFaker.fakeLaunch(p0, p1, p2) which returns Object (BillingResult); return it.
        try {
            BillingLaunchFlowFingerprint.method.addInstructions(
                0,
                """
                    invoke-static {p0, p1, p2}, $BILLING_FAKER->fakeLaunch(Ljava/lang/Object;Landroid/app/Activity;Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
        } catch (e: Exception) {
            // If the fingerprint doesn't match, skip.
        }
    }
}
