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
 * Blocks all ad SDKs in Subway Surfers.
 *
 * Hooks the show() methods across every ad SDK bundled in 3.70.0:
 * - Unity Ads (legacy UnityAds.show API): rewarded ads fire
 *   onUnityAdsShowComplete(COMPLETED) so the game grants the reward;
 *   plain interstitials just return.
 * - Unity Ads (new InterstitialAd/RewardedAd API), AdMob, Facebook,
 *   Pangle, ironSource, Unity ironSource wrapper: show() returns
 *   immediately without displaying anything.
 *
 * Result: no ad ever displays. Rewarded-ad buttons that use the legacy
 * Unity Ads API still grant their reward; other rewarded placements
 * will simply do nothing (no ad, no reward).
 *
 * Verified against Subway Surfers 3.70.0 (com.kiloo.subwaysurf /
 * com.kiloo.subwaysurfers).
 */
@Suppress("unused")
val adSkipPatch = bytecodePatch(
    name = "Skip Unity Ads",
    description = "Blocks all ad SDKs — no ad ever displays. Legacy Unity rewarded ads still grant their reward.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SUBWAYSURF_ADS, COMPATIBILITY_SUBWAYSURF_ADS_ALT)

    execute {
        // ===== Unity Ads (legacy API) =====
        // show(Activity, String) — no listener: just return.
        UnityAdsShowFingerprint.method.addInstructions(0, "return-void")

        // show(Activity, String, UnityAdsShowOptions) — no listener: just return.
        UnityAdsShowOptionsFingerprint.method.addInstructions(0, "return-void")

        // show(Activity, String, IUnityAdsShowListener) — fire COMPLETED.
        UnityAdsShowListenerFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $COMPLETION_STATE->COMPLETED:$COMPLETION_STATE
                invoke-interface {p2, p1, v0}, $SHOW_LISTENER->onUnityAdsShowComplete(Ljava/lang/String;$COMPLETION_STATE)V
                return-void
            """.trimIndent(),
        )

        // show(Activity, String, UnityAdsShowOptions, IUnityAdsShowListener) — fire COMPLETED.
        UnityAdsShowOptionsListenerFingerprint.method.addInstructions(
            0,
            """
                sget-object v0, $COMPLETION_STATE->COMPLETED:$COMPLETION_STATE
                invoke-interface {p3, p1, v0}, $SHOW_LISTENER->onUnityAdsShowComplete(Ljava/lang/String;$COMPLETION_STATE)V
                return-void
            """.trimIndent(),
        )

        // ===== AdMob =====
        AdMobInterstitialFingerprint.method.addInstructions(0, "return-void")
        AdMobRewardedFingerprint.method.addInstructions(0, "return-void")
        AdMobRewardedInterstitialFingerprint.method.addInstructions(0, "return-void")

        // ===== Unity Ads (new API) =====
        UnityNewInterstitialFingerprint.method.addInstructions(0, "return-void")
        UnityNewRewardedFingerprint.method.addInstructions(0, "return-void")

        // ===== Facebook Ads (show() returns boolean) =====
        FacebookInterstitialFingerprint.method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        FacebookRewardedInterstitialFingerprint.method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")

        // ===== Pangle =====
        PangleInterstitialFingerprint.method.addInstructions(0, "return-void")
        PangleRewardedFingerprint.method.addInstructions(0, "return-void")

        // ===== ironSource =====
        IronSourceInterstitialFingerprint.method.addInstructions(0, "return-void")
        IronSourceRewardedVideoFingerprint.method.addInstructions(0, "return-void")

        // ===== Unity ironSource wrapper =====
        UnityIronSourceInterstitialFingerprint.method.addInstructions(0, "return-void")
        UnityIronSourceRewardedFingerprint.method.addInstructions(0, "return-void")
    }
}
