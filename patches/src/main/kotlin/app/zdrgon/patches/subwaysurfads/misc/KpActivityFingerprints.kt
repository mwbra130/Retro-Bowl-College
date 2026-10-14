package app.zdrgon.patches.subwaysurfads.misc

import app.morphe.patcher.Fingerprint

/**
 * SYBO's own ad player activity — every fullscreen/rewarded ad in
 * Subway Surfers is displayed through this Activity, regardless of
 * which mediated SDK (Unity, AdMob, AppLovin, etc.) provides the ad.
 * Hooking onCreate() here is the ultimate chokepoint.
 */
object KPAdPlayerActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lcom/kpadplayer/sdk/ads/fullscreen/KPAdPlayerActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
