package app.zdrgon.patches.subwaysurfads.misc

import app.morphe.patcher.Fingerprint

// ============ AdMob (Google) ============

object AdMobInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/interstitial/InterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

object AdMobRewardedFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/rewarded/RewardedAd;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;",
    ),
)

object AdMobRewardedInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/ads/rewardedinterstitial/RewardedInterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/google/android/gms/ads/OnUserEarnedRewardListener;",
    ),
)

// ============ Unity Ads (new API) ============

object UnityNewInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/InterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/unity3d/ads/ShowConfiguration;",
        "Lcom/unity3d/ads/InterstitialShowListener;",
    ),
)

object UnityNewRewardedFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ads/RewardedAd;",
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/unity3d/ads/ShowConfiguration;",
        "Lcom/unity3d/ads/RewardedShowListener;",
    ),
)

// ============ Facebook Ads ============

object FacebookInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/InterstitialAd;",
    name = "show",
    returnType = "Z",
    parameters = listOf(),
)

object FacebookRewardedInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/facebook/ads/RewardedInterstitialAd;",
    name = "show",
    returnType = "Z",
    parameters = listOf(),
)

// ============ Pangle (ByteDance) ============

object PangleInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/sdk/openadsdk/api/interstitial/PAGInterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

object PangleRewardedFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/sdk/openadsdk/api/reward/PAGRewardedAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

// ============ ironSource ============

object IronSourceInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showISDemandOnlyInterstitial",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

object IronSourceRewardedVideoFingerprint : Fingerprint(
    definingClass = "Lcom/ironsource/mediationsdk/IronSource;",
    name = "showISDemandOnlyRewardedVideo",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

// ============ Unity ironSource wrapper ============

object UnityIronSourceInterstitialFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ironsourceads/interstitial/InterstitialAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

object UnityIronSourceRewardedFingerprint : Fingerprint(
    definingClass = "Lcom/unity3d/ironsourceads/rewarded/RewardedAd;",
    name = "show",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)
