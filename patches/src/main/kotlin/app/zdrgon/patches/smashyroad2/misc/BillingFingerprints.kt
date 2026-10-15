package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.Fingerprint

/**
 * BillingClient.launchBillingFlow(Activity, BillingFlowParams).
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
