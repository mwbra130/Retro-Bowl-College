package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.Fingerprint

/**
 * Unity's PurchaseBridge.getPurchaseState().
 * Unity IAP wrapper — not Google's BillingClient, so it may not trigger
 * the build failure.
 */
object UnityPurchaseBridgeFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/services/store/gpbl/bridges/PurchaseBridge;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = listOf(),
)
