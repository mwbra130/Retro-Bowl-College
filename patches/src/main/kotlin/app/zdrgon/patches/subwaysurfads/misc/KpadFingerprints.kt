package app.zdrgon.patches.subwaysurfads.misc

import app.morphe.patcher.Fingerprint

/**
 * Central ad manager used by Subway Surfers (kpadplayer SDK).
 * This is the chokepoint — all fullscreen/rewarded ads go through here.
 */
object FullScreenAdManagerShowFingerprint : Fingerprint(
    definingClass = "Lcom/kpadplayer/sdk/ads/fullscreen/FullScreenAdManager;",
    name = "show",
    returnType = "V",
    parameters = listOf(),
)

object FullScreenAdManagerSetRewardListenerFingerprint : Fingerprint(
    definingClass = "Lcom/kpadplayer/sdk/ads/fullscreen/FullScreenAdManager;",
    name = "setOnInterstitialRewardedEventListener",
    returnType = "V",
    parameters = listOf(
        "Lcom/kpadplayer/sdk/ads/fullscreen/FullScreenAdManager\$IOnInterstitialRewardedEventListener;",
    ),
)
