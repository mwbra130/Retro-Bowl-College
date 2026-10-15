package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.Fingerprint

/**
 * Purchase.getPurchaseState().
 * Returns int: 0=UNSPECIFIED, 1=PURCHASED, 2=PENDING.
 */
object PurchaseStateFingerprint : Fingerprint(
    definingClass = "Lcom/android/billingclient/api/Purchase;",
    name = "getPurchaseState",
    returnType = "I",
    parameters = listOf(),
)
