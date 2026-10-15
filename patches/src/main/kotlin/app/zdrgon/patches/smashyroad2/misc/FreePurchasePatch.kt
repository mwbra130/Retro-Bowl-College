package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Fakes Google Play Billing purchase flow — "click buy and it goes through."
 *
 * Hooks BillingClient.launchBillingFlow() to immediately return a success
 * BillingResult without ever showing Google Play UI.
 *
 * This is the simplest form of purchase piracy: the game thinks the billing
 * flow launched successfully. Whether the game grants the item depends on
 * whether it waits for the onPurchasesUpdated callback (which we don't fake
 * here) or grants optimistically on flow launch.
 *
 * If this doesn't grant items, the full BillingFaker approach (faking the
 * callback with a dummy Purchase) will be needed.
 */
@Suppress("unused")
val freePurchasePatch = bytecodePatch(
    name = "Free purchases",
    description = "Clicking buy on any IAP immediately succeeds without " +
        "payment — Google Play UI never appears.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        // launchBillingFlow(Activity, BillingFlowParams): p0=this, p1=activity, p2=params.
        // Build BillingResult.newBuilder().setResponseCode(OK).build() and return it.
        // OK = 0 (BillingClient.BillingResponseCode.OK).
        BillingLaunchFlowFingerprint.methodOrNull?.let {
            it.addInstructions(
                0,
                """
                    invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult$Builder;
                    move-result-object v0
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult$Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult$Builder;
                    move-result-object v0
                    invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult$Builder;->build()Lcom/android/billingclient/api/BillingResult;
                    move-result-object v0
                    return-object v0
                """.trimIndent(),
            )
        }
    }
}
