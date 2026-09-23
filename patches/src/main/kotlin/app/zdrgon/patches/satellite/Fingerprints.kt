package app.zdrgon.patches.retrobowlcollege.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import app.morphe.patcher.string

/**
 * Targets GooglePlayBillingService$1.onQueryPurchasesResponse(BillingResult, List).
 *
 * This callback receives the Play Billing purchase list, serializes it to JSON
 * (keys "success", "purchases", each entry the purchase's originalJson) and
 * forwards it to the GameMaker runtime via
 * RunnerJNILib.CreateAsynEventWithDSMap under the key "response_json"
 * (async event id 68). The game's GML reads that JSON to decide whether the
 * full version is unlocked.
 */
object QueryPurchasesResponseFingerprint : Fingerprint(
    definingClass = "Lcom/newstargames/retrobowlcollege/GooglePlayBillingService\$1;",
    returnType = "V",
    parameters = listOf(
        "Lcom/android/billingclient/api/BillingResult;",
        "Ljava/util/List;",
    ),
    filters = listOf(
        string("response_json"),
        string("sku_type"),
        string("Malformed JSON data from queryPurchases."),
        methodCall("Lorg/json/JSONObject;->toString()Ljava/lang/String;"),
        methodCall("Lcom/yoyogames/runner/RunnerJNILib;->CreateAsynEventWithDSMap(II)V"),
    ),
)
