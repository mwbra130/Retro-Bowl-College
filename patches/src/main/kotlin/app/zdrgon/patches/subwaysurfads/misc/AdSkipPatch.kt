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
 *
 * Note: fingerprints use safe calls (?.) so if an SDK isn't present in
 * a variant, that hook is skipped instead of crashing the patch.
 */
@Suppress("unused")
val adSkipPatch = bytecodePatch(
    name = "Skip Unity Ads",
    description = "Blocks all ad SDKs — no ad ever displays. Legacy Unity rewarded ads still grant their reward.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SUBWAYSURF_ADS, COMPATIBILITY_SUBWAYSURF_ADS_ALT)

    execute {
        fun tryHook(label: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                // Skip SDKs whose show() has no patchable code (abstract, etc.).
                // println("AdSkip: skipping $label: ${e.message}")
            }
        }

        // ===== Unity Ads (legacy API) =====
        tryHook("UnityAds.show/2") {
            UnityAdsShowFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("UnityAds.show/3-options") {
            UnityAdsShowOptionsFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("UnityAds.show/3-listener") {
            UnityAdsShowListenerFingerprint.method?.addInstructions(
                0,
                """
                    sget-object v0, $COMPLETION_STATE->COMPLETED:$COMPLETION_STATE
                    invoke-interface {p2, p1, v0}, $SHOW_LISTENER->onUnityAdsShowComplete(Ljava/lang/String;$COMPLETION_STATE)V
                    return-void
                """.trimIndent(),
            )
        }
        tryHook("UnityAds.show/4") {
            UnityAdsShowOptionsListenerFingerprint.method?.addInstructions(
                0,
                """
                    sget-object v0, $COMPLETION_STATE->COMPLETED:$COMPLETION_STATE
                    invoke-interface {p3, p1, v0}, $SHOW_LISTENER->onUnityAdsShowComplete(Ljava/lang/String;$COMPLETION_STATE)V
                    return-void
                """.trimIndent(),
            )
        }

        // ===== AdMob =====
        tryHook("AdMob.Interstitial") {
            AdMobInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("AdMob.Rewarded") {
            AdMobRewardedFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("AdMob.RewardedInterstitial") {
            AdMobRewardedInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }

        // ===== Unity Ads (new API) =====
        tryHook("UnityNew.Interstitial") {
            UnityNewInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("UnityNew.Rewarded") {
            UnityNewRewardedFingerprint.method?.addInstructions(0, "return-void")
        }

        // ===== Facebook Ads (show() returns boolean) =====
        tryHook("Facebook.Interstitial") {
            FacebookInterstitialFingerprint.method?.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        }
        tryHook("Facebook.RewardedInterstitial") {
            FacebookRewardedInterstitialFingerprint.method?.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        }

        // ===== Pangle =====
        tryHook("Pangle.Interstitial") {
            PangleInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("Pangle.Rewarded") {
            PangleRewardedFingerprint.method?.addInstructions(0, "return-void")
        }

        // ===== ironSource =====
        tryHook("IronSource.Interstitial") {
            IronSourceInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("IronSource.RewardedVideo") {
            IronSourceRewardedVideoFingerprint.method?.addInstructions(0, "return-void")
        }

        // ===== Unity ironSource wrapper =====
        tryHook("UnityIronSource.Interstitial") {
            UnityIronSourceInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        tryHook("UnityIronSource.Rewarded") {
            UnityIronSourceRewardedFingerprint.method?.addInstructions(0, "return-void")
        }
    }
}
