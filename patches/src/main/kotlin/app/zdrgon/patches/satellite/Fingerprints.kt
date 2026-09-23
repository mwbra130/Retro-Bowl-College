package app.zdrgon.patches.retrobowlcollege.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.string

/**
 * Matches the billing callback that converts Google Play purchase data into the
 * JSON handed to the GameMaker runtime:
 *
 *   GooglePlayBillingService$N.onQueryPurchasesResponse(BillingResult, List)
 *
 * "sku_type" is referenced by exactly one method in the entire APK, so this
 * fingerprint deliberately does NOT pin the anonymous-inner-class number
 * ($1, $2, ...), which can differ between builds of the app.
 */
object QueryPurchasesResponseFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf(
        "Lcom/android/billingclient/api/BillingResult;",
        "Ljava/util/List;",
    ),
    filters = listOf(
        // Stable strings inside the JSON-building branch of the callback.
        // "sku_type" occurs in exactly one method in the whole APK.
        string("sku_type"),
        string("response_json"),
        string("Malformed JSON data from queryPurchases."),
    ),
)
