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
 * Blocks interstitial ads in Subway Surfers, leaves rewarded ads working.
 *
 * Hooks the show() methods for interstitial (fullscreen) ads across every
 * ad SDK bundled in 3.70.0: Unity Ads (legacy + new API), AdMob, Facebook,
 * Pangle, ironSource, Unity ironSource wrapper. Interstitial show() calls
 * return immediately without displaying anything.
 *
 * Rewarded ads are intentionally NOT hooked — faking reward callbacks
 * across 7 SDKs proved unreliable (the game expects specific callback
 * sequences). Rewarded ads play normally and grant their reward; only
 * the forced interstitials between runs are killed.
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
    description = "Blocks forced interstitial ads — no fullscreen ad between runs. Rewarded ads still work normally and grant their reward.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SUBWAYSURF_ADS, COMPATIBILITY_SUBWAYSURF_ADS_ALT)

    execute {
        fun tryHook(label: String, block: () -> Unit) {
            try {
                block()
            } catch (e: Exception) {
                // Skip SDKs whose show() has no patchable code (abstract, etc.).
            }
        }

        // ===== KPAdPlayerActivity — THE chokepoint =====
        // Every ad in Subway Surfers displays through this Activity.
        // Hook onCreate(): set RESULT_OK (game treats it as "ad completed",
        // granting rewarded-ad benefits) then finish() immediately.
        // No ad UI ever appears.
        // onCreate(Bundle): p0=this, p1=savedInstanceState.
        tryHook("KPAdPlayerActivity.onCreate") {
            KPAdPlayerActivityOnCreateFingerprint.method?.addInstructions(
                0,
                """
                    const/4 v0, -0x1
                    invoke-virtual {p0, v0}, Lcom/kpadplayer/sdk/ads/fullscreen/KPAdPlayerActivity;->setResult(I)V
                    invoke-virtual {p0}, Lcom/kpadplayer/sdk/ads/fullscreen/KPAdPlayerActivity;->finish()V
                    return-void
                """.trimIndent(),
            )
        }

        // ===== kpadplayer FullScreenAdManager (backup) =====
        // Capture the rewarded listener when it's set (let original code run too).
        tryHook("Kpad.SetRewardListener") {
            FullScreenAdManagerSetRewardListenerFingerprint.method?.addInstructions(
                0,
                """
                    sput-object p1, Lapp/zdrgon/patches/subwaysurfads/misc/RewardListenerStorage;->listener:Ljava/lang/Object;
                """.trimIndent(),
            )
        }
        // Hook show(): fire onRewardReceived() if listener exists, then return
        // without showing the ad. This blocks the ad UI and grants the reward.
        tryHook("Kpad.Show") {
            FullScreenAdManagerShowFingerprint.method?.addInstructions(
                0,
                """
                    sget-object v0, Lapp/zdrgon/patches/subwaysurfads/misc/RewardListenerStorage;->listener:Ljava/lang/Object;
                    if-eqz v0, :no_reward
                    check-cast v0, Lcom/kpadplayer/sdk/ads/fullscreen/FullScreenAdManager${'$'}IOnInterstitialRewardedEventListener;
                    invoke-interface {v0}, Lcom/kpadplayer/sdk/ads/fullscreen/FullScreenAdManager${'$'}IOnInterstitialRewardedEventListener;->onRewardReceived()V
                    :no_reward
                    return-void
                """.trimIndent(),
            )
        }

        // ===== Unity Ads (legacy API) — interstitials only =====
        // show(Activity, String) — no listener, used for interstitials.
        tryHook("UnityAds.show/2") {
            UnityAdsShowFingerprint.method?.addInstructions(0, "return-void")
        }
        // show(Activity, String, UnityAdsShowOptions) — no listener, interstitials.
        tryHook("UnityAds.show/3-options") {
            UnityAdsShowOptionsFingerprint.method?.addInstructions(0, "return-void")
        }
        // NOTE: show() overloads WITH IUnityAdsShowListener are rewarded ads —
        // intentionally not hooked so rewards work normally.

        // ===== AdMob — interstitials only =====
        tryHook("AdMob.Interstitial") {
            AdMobInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        // NOTE: AdMob Rewarded/RewardedInterstitial not hooked — rewards work.

        // ===== Unity Ads (new API) — interstitials only =====
        tryHook("UnityNew.Interstitial") {
            UnityNewInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        // NOTE: Unity new RewardedAd not hooked — rewards work.

        // ===== Facebook Ads (show() returns boolean) — interstitials =====
        tryHook("Facebook.Interstitial") {
            FacebookInterstitialFingerprint.method?.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        }
        // NOTE: Facebook RewardedInterstitial not hooked.

        // ===== Pangle — interstitials only =====
        tryHook("Pangle.Interstitial") {
            PangleInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        // NOTE: Pangle Rewarded not hooked.

        // ===== ironSource — interstitials only =====
        tryHook("IronSource.Interstitial") {
            IronSourceInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        // NOTE: ironSource RewardedVideo not hooked.

        // ===== Unity ironSource wrapper — interstitials only =====
        tryHook("UnityIronSource.Interstitial") {
            UnityIronSourceInterstitialFingerprint.method?.addInstructions(0, "return-void")
        }
        // NOTE: Unity ironSource Rewarded not hooked.
    }
}
