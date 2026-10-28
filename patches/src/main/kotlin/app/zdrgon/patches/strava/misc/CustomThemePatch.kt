package app.zdrgon.patches.strava.misc

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import app.zdrgon.patches.strava.shared.Constants.COMPATIBILITY_STRAVA
import org.w3c.dom.Element

// Strava's brand orange. In 486.9 resources the design token
// color/core_o3 and the brand/accent colors derived from it use the
// literal value #FFFC5200 (plus alpha variants such as #66FC5200).
private val brandOrangeRegex = Regex("""#([0-9a-fA-F]{2})?[Ff][Cc]5200""")

/**
 * Replaces Strava's brand orange accent color in the app's resource
 * colors (default and night configurations) with a chosen color.
 * Covers native screens that draw from color resources; screens whose
 * colors are hardcoded in code keep the original orange.
 */
@Suppress("unused")
val customThemePatch = resourcePatch(
    name = "Custom theme colors",
    description = "Replaces Strava's brand orange (#FC5200) accent color in the app's native " +
        "screens with a color of your choice.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_STRAVA)

    val accentColorOption = stringOption(
        key = "accentColor",
        default = "#FC5200",
        values = mapOf(
            "Strava orange" to "#FC5200",
            "Sunset orange" to "#FF6B00",
            "Blue" to "#0066FF",
            "Violet" to "#7C4DFF",
            "Teal" to "#00BFA5",
            "Green" to "#00C853",
            "Pink" to "#FF4081",
            "Red" to "#E53935",
            "Slate" to "#546E7A",
        ),
        title = "Accent color",
        description = "The accent color that replaces Strava orange.",
        required = true,
    )

    execute {
        val accent = accentColorOption.value!!.trim().removePrefix("#")
        if (!accent.matches(Regex("[0-9a-fA-F]{6}"))) {
            throw PatchException(
                "Accent color must be a 6-digit hex value, got: ${accentColorOption.value}",
            )
        }
        val rgb = accent.uppercase()

        fun replaceBrandOrange(path: String) {
            document(path).use { document ->
                val nodes = document.getElementsByTagName("color")

                for (i in 0 until nodes.length) {
                    val element = nodes.item(i) as Element
                    val match = brandOrangeRegex.matchEntire(element.textContent.trim())
                        ?: continue
                    val alpha = match.groupValues[1].ifEmpty { "FF" }.uppercase()
                    element.textContent = "#$alpha$rgb"
                }
            }
        }

        replaceBrandOrange("res/values/colors.xml")

        // Optional configuration files: present in 486.9 for night mode,
        // but a missing file must not fail the patch.
        listOf("res/values-night/colors.xml", "res/values-v31/colors.xml").forEach { path ->
            try {
                replaceBrandOrange(path)
            } catch (e: Exception) {
                // File not present in this APK.
            }
        }
    }
}
