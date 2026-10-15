package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.Fingerprint

/**
 * BillingClient.Builder.setListener(PurchasesUpdatedListener).
 * We hook this to capture the game's purchase listener.
 */
object BillingSetListenerFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/BillingClient$Builder;",
    name = "setListener",
    returnType = "Lcom/android/billingclient/api/BillingClient$Builder;",
    parameters = listOf("Lcom/android/billingclient/api/PurchasesUpdatedListener;"),
)

/**
 * BillingClient.launchBillingFlow(Activity, BillingFlowParams).
 * We hook this to intercept purchase attempts and fake success.
 */
object BillingLaunchFlowFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/BillingClient;",
    name = "launchBillingFlow",
    returnType = "Lcom/android/billingclient/api/BillingResult;",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/android/billingclient/api/BillingFlowParams;",
    ),
)
