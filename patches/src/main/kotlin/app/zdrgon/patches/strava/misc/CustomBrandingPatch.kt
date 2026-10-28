package app.zdrgon.patches.strava.misc

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.zdrgon.patches.strava.shared.Constants.COMPATIBILITY_STRAVA
import org.w3c.dom.Element

/**
 * Changes the app's launcher name and launcher icon.
 *
 * Name: the manifest label of the app and of every launcher alias is
 * `@string/app_name`, so editing that one string renames the app
 * everywhere it appears.
 *
 * Icon: Strava 486.9 ships 15 alternate launcher icons as disabled
 * activity-alias components for SplashActivity (the in-app icon picker
 * normally enables one and disables the rest). This patch sets the
 * same end state in the manifest: the chosen alias is enabled and the
 * default SplashActivity launcher component is disabled. Only icons
 * already built into the app are used.
 */
@Suppress("unused")
val customBrandingPatch = resourcePatch(
    name = "Custom app name and icon",
    description = "Changes the app's displayed name and launcher icon. The icon choices are " +
        "Strava's own built-in alternate icons.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_STRAVA)

    val appNameOption = stringOption(
        key = "appName",
        default = "Strava",
        title = "App name",
        description = "The name shown under the launcher icon.",
        required = true,
    )

    val appIconOption = stringOption(
        key = "appIcon",
        default = "default",
        values = mapOf(
            "Default" to "default",
            "Local Legend" to "LocalLegend",
            "XOM" to "Xom",
            "Trophy" to "Trophy",
            "Medal" to "Medal",
            "Hiking" to "Hiking",
            "Cycling" to "Cycling",
            "Running" to "Running",
            "Kudos" to "Kudos",
            "Sticker" to "Sticker",
            "Pride" to "Pride",
            "Road" to "Road",
            "Gravel" to "Gravel",
            "Teal" to "Teal",
            "Red" to "Red",
            "Black" to "Black",
        ),
        title = "App icon",
        description = "The launcher icon. Uses Strava's own built-in alternate icons.",
        required = true,
    )

    execute {
        val appName = appNameOption.value!!
        if (appName != "Strava" && appName.isNotBlank()) {
            fun renameInStrings(path: String) {
                document(path).use { document ->
                    val nodes = document.getElementsByTagName("string")
                    var renamed = false

                    for (i in 0 until nodes.length) {
                        val element = nodes.item(i) as Element
                        if (element.getAttribute("name") == "app_name") {
                            element.textContent = appName
                            renamed = true
                            break
                        }
                    }

                    if (!renamed) {
                        val stringElement = document.createElement("string")
                        stringElement.setAttribute("name", "app_name")
                        stringElement.textContent = appName
                        document.getElementsByTagName("resources").item(0)
                            .appendChild(stringElement)
                    }
                }
            }

            renameInStrings("res/values/strings.xml")

            // The XAPK's config.en split can override the default strings
            // on English devices, so rename there too when present.
            try {
                renameInStrings("res/values-en/strings.xml")
            } catch (e: Exception) {
                // File not present in this APK.
            }
        }

        val appIcon = appIconOption.value!!
        if (appIcon != "default") {
            val aliasName = "com.strava.SplashActivity${appIcon}Icon"

            document("AndroidManifest.xml").use { document ->
                val aliases = document.getElementsByTagName("activity-alias")
                for (i in 0 until aliases.length) {
                    val element = aliases.item(i) as Element
                    if (element.getAttribute("android:name") == aliasName) {
                        element.setAttribute("android:enabled", "true")
                    }
                }

                val activities = document.getElementsByTagName("activity")
                for (i in 0 until activities.length) {
                    val element = activities.item(i) as Element
                    if (element.getAttribute("android:name") == "com.strava.SplashActivity") {
                        element.setAttribute("android:enabled", "false")
                    }
                }
            }
        }
    }
}
