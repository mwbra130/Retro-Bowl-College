package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP
import org.w3c.dom.Element

/**
 * Content providers that auto-initialize ad/tracker SDKs at app startup,
 * confirmed present in BeReal 3.96.0's AndroidManifest.xml.
 */
private val TRACKER_PROVIDERS = setOf(
    // Google Mobile Ads
    "com.google.android.gms.ads.MobileAdsInitProvider",
    // InMobi ads
    "com.inmobi.sdk.InMobiInitProvider",
    // AppLovin MAX (also killed at the init call site by AdSdkInitKillPatch)
    "com.applovin.sdk.AppLovinInitProvider",
    // Datadog real-user monitoring
    "com.datadog.android.rum.DdRumContentProvider",
    // Vungle/Liftoff ads
    "com.vungle.ads.VungleProvider",
    // Adjust lifecycle hooks (Adjust init itself is killed by AdjustInitKillPatch)
    "com.adjust.sdk.SystemLifecycleContentProvider",
)

/**
 * Removes auto-init content providers for ad and tracker SDKs from the manifest.
 *
 * Deleting these <provider> entries stops the SDKs from self-initializing at
 * startup. Deliberately kept: AndroidX Startup, FCM push
 * (BeRealMessagingService — push notifications depend on it), Picasso,
 * Zendesk, ML Kit, and Shake.
 */
@Suppress("unused")
val trackerProvidersPatch = resourcePatch(
    name = "Remove ad/tracker auto-init providers",
    description = "Stops ad and tracker SDKs from starting with the app " +
        "(Mobile Ads, InMobi, AppLovin, Datadog, Vungle, Adjust). Notifications keep working.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        document("AndroidManifest.xml").use { document ->
            val applications = document.getElementsByTagName("application")
            if (applications.length == 0) {
                throw PatchException("No <application> element found in AndroidManifest.xml")
            }
            val application = applications.item(0) as Element

            // Collect first: the NodeList is live and would shift under removal.
            val providers = application.getElementsByTagName("provider")
            val toRemove = mutableListOf<Element>()
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as Element
                if (provider.getAttribute("android:name") in TRACKER_PROVIDERS) {
                    toRemove.add(provider)
                }
            }

            toRemove.forEach { application.removeChild(it) }
        }
    }
}
