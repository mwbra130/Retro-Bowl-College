package app.zdrgon.patches.subwaysurfads.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.subwaysurfads.shared.Constants.COMPATIBILITY_SUBWAYSURF_ADS
import app.zdrgon.patches.subwaysurfads.shared.Constants.COMPATIBILITY_SUBWAYSURF_ADS_ALT

/**
 * Blocks ads in Subway Surfers 3.70.0 by no-opping the ad show methods.
 *
 * Based on analysis of the 3.70.2 mod APK which blocks ads via class replacement.
 * This patch uses the BeReal formula: block at the display layer (show() no-op),
 * never hook SDK initialization.
 *
 * Hooks UnityAds.show() overloads to return immediately without displaying ads.
 * Verified: these methods exist in 3.70.0 (com.unity3d.ads.UnityAds).
 *
 * Note: Rewarded ads will not grant rewards (the show() is no-oped before the
 * callback can fire). This is a tradeoff for complete ad blocking.
 */
@Suppress("unused")
val blockAdsPatch = bytecodePatch(
    name = "Block Ads",
    description = "Blocks all Unity Ads (interstitial and rewarded) by no-opping the show() methods. No ads will display.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SUBWAYSURF_ADS, COMPATIBILITY_SUBWAYSURF_ADS_ALT)

    execute {
        // UnityAds.show(Activity, String) - interstitials
        try {
            UnityAdsShowFingerprint.method?.addInstructions(0, "return-void")
        } catch (e: Exception) {
            // Skip if not found
        }

        // UnityAds.show(Activity, String, UnityAdsShowOptions) - interstitials
        try {
            UnityAdsShowOptionsFingerprint.method?.addInstructions(0, "return-void")
        } catch (e: Exception) {
            // Skip if not found
        }

        // UnityAds.show(Activity, String, IUnityAdsShowListener) - rewarded
        // No-op to block rewarded ads too (user won't get reward, but no ad shows)
        try {
            UnityAdsShowListenerFingerprint.method?.addInstructions(0, "return-void")
        } catch (e: Exception) {
            // Skip if not found
        }

        // UnityAds.show(Activity, String, UnityAdsShowOptions, IUnityAdsShowListener) - rewarded
        try {
            UnityAdsShowOptionsListenerFingerprint.method?.addInstructions(0, "return-void")
        } catch (e: Exception) {
            // Skip if not found
        }
    }
}
