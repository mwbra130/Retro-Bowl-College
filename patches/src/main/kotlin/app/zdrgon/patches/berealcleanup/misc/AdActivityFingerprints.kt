package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint

/**
 * BeReal's interstitial ad activity (Voodoo ADN SDK).
 * Every fullscreen ad renders through here. Hooking onCreate() to finish()
 * immediately blocks the ad UI without touching SDK initialization
 * (which caused the ~5s crash loop when hooked).
 */
object FullscreenAdActivityOnCreateFingerprint : Fingerprint(
    definingClass = "Lio/adn/sdk/internal/ui/fullscreen/FullscreenAdActivity;",
    name = "onCreate",
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)

/**
 * ADN fullscreen ad show method.
 * No-op'ing this prevents the ad from ever displaying, without touching
 * SDK init.
 */
object AdnFullscreenAdShowFingerprint : Fingerprint(
    definingClass = "Lio/adn/sdk/publisher/AdnFullscreenAd;",
    name = "show",
    returnType = "V",
    parameters = listOf(),
)
