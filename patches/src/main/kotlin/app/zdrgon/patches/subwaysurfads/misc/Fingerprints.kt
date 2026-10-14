package app.zdrgon.patches.subwaysurfads.misc

import app.morphe.patcher.Fingerprint

private const val UNITY_ADS = "Lcom/unity3d/ads/UnityAds;"
private const val SHOW_LISTENER = "Lcom/unity3d/ads/IUnityAdsShowListener;"
private const val SHOW_OPTIONS = "Lcom/unity3d/ads/UnityAdsShowOptions;"

/**
 * UnityAds.show(Activity, String) — no listener, no options.
 * Just return; nothing to complete.
 */
object UnityAdsShowFingerprint : Fingerprint(
    definingClass = UNITY_ADS,
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
    ),
)

/**
 * UnityAds.show(Activity, String, IUnityAdsShowListener).
 * Fires onUnityAdsShowComplete(COMPLETED) on the listener.
 */
object UnityAdsShowListenerFingerprint : Fingerprint(
    definingClass = UNITY_ADS,
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        SHOW_LISTENER,
    ),
)

/**
 * UnityAds.show(Activity, String, UnityAdsShowOptions) — no listener.
 * Just return; nothing to complete.
 */
object UnityAdsShowOptionsFingerprint : Fingerprint(
    definingClass = UNITY_ADS,
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        SHOW_OPTIONS,
    ),
)

/**
 * UnityAds.show(Activity, String, UnityAdsShowOptions, IUnityAdsShowListener).
 * Fires onUnityAdsShowComplete(COMPLETED) on the listener.
 */
object UnityAdsShowOptionsListenerFingerprint : Fingerprint(
    definingClass = UNITY_ADS,
    name = "show",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        SHOW_OPTIONS,
        SHOW_LISTENER,
    ),
)
