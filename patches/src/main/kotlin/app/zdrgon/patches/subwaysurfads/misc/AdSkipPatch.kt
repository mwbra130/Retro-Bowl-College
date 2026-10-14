package app.zdrgon.patches.subwaysurfads.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.subwaysurfads.shared.Constants.COMPATIBILITY_SUBWAYSURF_ADS
import app.zdrgon.patches.subwaysurfads.shared.Constants.COMPATIBILITY_SUBWAYSURF_ADS_ALT

// Escaped $ for the inner enum class name.
private const val COMPLETION_STATE =
    "Lcom/unity3d/ads/UnityAds\$UnityAdsShowCompletionState;"
private const val SHOW_LISTENER = "Lcom/unity3d/ads/IUnityAdsShowListener;"

/**
 * Skips Unity Ads in Subway Surfers.
 *
 * Hooks all four UnityAds.show() overloads. The two overloads that take an
 * IUnityAdsShowListener immediately fire
 * onUnityAdsShowComplete(placementId, COMPLETED) — the game then grants the
 * reward (revive, coins, hoverboard, etc.) exactly as if the ad had played
 * to completion. The two overloads without a listener just return; there is
 * no ad to show and nothing waiting on a callback.
 *
 * Result: tapping anything that would play an ad passes straight through —
 * no ad UI, no wait, reward granted.
 *
 * Verified against Subway Surfers 3.70.0 (com.kiloo.subwaysurfers): the four
 * show() overloads are static methods on com.unity3d.ads.UnityAds, and
 * UnityAdsShowCompletionState has the COMPLETED enum value.
 *
 * Note: this covers Unity Ads only. If Subway Surfers serves ads through
 * AppLovin, AdMob, or ironSource in some placements, those will still show.
 */
@Suppress("unused")
val adSkipPatch = bytecodePatch(
    name = "Skip Unity Ads",
    description = "Ads pass straight through — rewarded ads grant their reward instantly, no ad ever plays.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SUBWAYSURF_ADS, COMPATIBILITY_SUBWAYSURF_ADS_ALT)

    execute {
        // show(Activity, String) — no listener: just return.
        UnityAdsShowFingerprint.method.addInstructions(0, "return-void")

        // show(Activity, String, UnityAdsShowOptions) — no listener: just return.
        UnityAdsShowOptionsFingerprint.method.addInstructions(0, "return-void")

        // show(Activity, String, IUnityAdsShowListener) — static, so
        // p0=Activity, p1=placementId, p2=listener. Fire COMPLETED.
        UnityAdsShowListenerFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $COMPLETION_STATE->COMPLETED:$COMPLETION_STATE
                invoke-interface {p2, p1, v0}, $SHOW_LISTENER->onUnityAdsShowComplete(Ljava/lang/String;$COMPLETION_STATE)V
                return-void
            """.trimIndent(),
        )

        // show(Activity, String, UnityAdsShowOptions, IUnityAdsShowListener) —
        // p0=Activity, p1=placementId, p2=options, p3=listener. Fire COMPLETED.
        UnityAdsShowOptionsListenerFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $COMPLETION_STATE->COMPLETED:$COMPLETION_STATE
                invoke-interface {p3, p1, v0}, $SHOW_LISTENER->onUnityAdsShowComplete(Ljava/lang/String;$COMPLETION_STATE)V
                return-void
            """.trimIndent(),
        )
    }
}
