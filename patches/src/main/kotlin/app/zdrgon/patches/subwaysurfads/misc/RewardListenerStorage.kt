package app.zdrgon.patches.subwaysurfads.misc

/**
 * Static storage for the rewarded ad listener.
 * The patch captures it in setOnInterstitialRewardedEventListener
 * and fires it in show().
 */
object RewardListenerStorage {
    @JvmStatic
    var listener: Any? = null
}
