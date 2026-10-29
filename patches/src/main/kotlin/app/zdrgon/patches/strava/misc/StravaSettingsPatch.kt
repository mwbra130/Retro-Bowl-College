package app.zdrgon.patches.strava.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.zdrgon.patches.strava.shared.Constants.COMPATIBILITY_STRAVA
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import org.w3c.dom.Element

private const val SETTINGS_CLASS = "Lapp/template/extension/strava/StravaSettings;"
private const val SETTINGS_ACTIVITY_CLASS = "Lapp/template/extension/strava/StravaSettingsActivity;"
private const val SETTINGS_ACTIVITY_NAME = "app.template.extension.strava.StravaSettingsActivity"

private const val APPLICATION_CLASS = "Lcom/strava/StravaApplication;"
private const val SETTINGS_FRAGMENT_CLASS = "Lcom/strava/settings/view/settings/SettingsFragment;"
private const val BOTTOM_NAV_ACTIVITY_CLASS =
    "Lcom/strava/view/bottomnavigation/BottomNavigationActivity;"
private const val METHOD_SUFFIX = "\$original"

/**
 * Registers the extension's settings activity in the manifest. The
 * extension is merged as dex only, so its components are not declared
 * anywhere until a resource patch adds them here.
 */
@Suppress("unused")
private val stravaSettingsManifestPatch = resourcePatch {
    compatibleWith(COMPATIBILITY_STRAVA)

    execute {
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            val activity = document.createElement("activity")
            activity.setAttribute("android:name", SETTINGS_ACTIVITY_NAME)
            activity.setAttribute("android:exported", "false")
            activity.setAttribute("android:theme", "@style/StravaPreferenceTheme")
            application.appendChild(activity)
        }
    }
}

/**
 * Adds an in-app settings screen for the Strava patches, modelled on
 * hxreborn's BeReal settings panel: the extension ships a settings
 * activity backed by a SharedPreferences store, and this patch wires
 * three entry points into the app:
 *
 * 1. Context: StravaApplication.onCreate is wrapped so the extension
 *    can capture the application context (needed to open
 *    SharedPreferences from the static filter/trim code).
 *
 * 2. Settings entry: Strava's settings screen is a Jetpack Compose
 *    screen (SettingsFragment.onCreateView returns a ComposeView), so
 *    there is no preference list or layout to inject a row into. The
 *    least invasive reliable hook is to wrap the fragment's
 *    onCreateView: the original view is placed in a FrameLayout above a
 *    bottom bar whose button opens the settings activity. The Compose
 *    content itself is untouched and keeps its full area.
 *
 * 3. Tab visibility: BottomNavigationActivity.onCreate is wrapped so
 *    that, once the bottom navigation menu exists, the extension can
 *    hide the tabs the user switched off in the settings screen.
 *
 * Each wrap uses the same rename-and-clone pattern as the route trim
 * patch: the original method becomes `<name>$original` and a
 * same-signature replacement calls it, then the extension.
 */
@Suppress("unused")
val stravaSettingsPatch = bytecodePatch(
    name = "Add settings",
    description = "Adds a 'Morphe patch settings' entry to Strava's settings screen. Lets you " +
        "toggle feed cleanup, route trim, bottom bar tabs and the launcher name/icon at " +
        "runtime, without re-patching.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_STRAVA)

    dependsOn(stravaSettingsManifestPatch)

    extendWith(STRAVA_EXTENSION)

    execute {
        wrapMethod(
            classType = APPLICATION_CLASS,
            methodName = "onCreate",
            parameterTypes = emptyList(),
            returnType = "V",
        ) { originalReference ->
            """
                invoke-virtual {p0}, $originalReference
                invoke-static {p0}, $SETTINGS_CLASS->attach(Landroid/content/Context;)V
                return-void
            """.trimIndent()
        }

        // The wrapper body reuses the parameter registers p1/p2 as
        // scratch space (they are dead once the original has run)
        // instead of local registers: dexlib2's mutable implementation
        // exposes the register count read-only, so a wrapper that
        // needs extra locals cannot be built reliably.
        wrapMethod(
            classType = SETTINGS_FRAGMENT_CLASS,
            methodName = "onCreateView",
            parameterTypes = listOf(
                "Landroid/view/LayoutInflater;",
                "Landroid/view/ViewGroup;",
                "Landroid/os/Bundle;",
            ),
            returnType = "Landroid/view/View;",
        ) { originalReference ->
            """
                invoke-virtual {p0, p1, p2, p3}, $originalReference
                move-result-object p1
                invoke-virtual {p0}, Landroidx/fragment/app/Fragment;->getContext()Landroid/content/Context;
                move-result-object p2
                invoke-static {p2, p1}, $SETTINGS_ACTIVITY_CLASS->wrapSettingsView(Landroid/content/Context;Landroid/view/View;)Landroid/view/View;
                move-result-object p1
                return-object p1
            """.trimIndent()
        }

        // BottomNavigationActivity.onCreate has 22 registers, so its
        // parameter registers are v20/v21. invoke-virtual/invoke-static
        // (format 35c) can only encode registers up to v15, and the
        // instruction assembler silently drops instructions that name
        // p0/p1 here (verified in the v1.31.1 output: only the body's
        // return-void survived, which would have bricked the activity).
        // Copy the parameters into low registers with move-object/from16
        // (16-bit register fields) first, then call through them.
        wrapMethod(
            classType = BOTTOM_NAV_ACTIVITY_CLASS,
            methodName = "onCreate",
            parameterTypes = listOf("Landroid/os/Bundle;"),
            returnType = "V",
        ) { originalReference ->
            """
                move-object/from16 v0, p0
                move-object/from16 v1, p1
                invoke-virtual {v0, v1}, $originalReference
                invoke-static {v0}, $SETTINGS_ACTIVITY_CLASS->applyTabVisibility(Landroid/app/Activity;)V
                return-void
            """.trimIndent()
        }
    }
}

/**
 * Renames the target method to `<name>$original` and installs a
 * same-signature replacement whose body is produced by [body] from the
 * reference to the renamed original. The replacement body is expected
 * to call the original and return; anything after the return is dead
 * code copied from the original implementation and never executes.
 */
private fun BytecodePatchContext.wrapMethod(
    classType: String,
    methodName: String,
    parameterTypes: List<String>,
    returnType: String,
    body: (originalReference: String) -> String,
) {
    val classDef = classDefBy(classType)
    val mutableClass = mutableClassDefBy(classDef)

    val original = mutableClass.methods.first { method ->
        method.name == methodName &&
            method.returnType == returnType &&
            method.parameterTypes == parameterTypes
    }

    if (!mutableClass.virtualMethods.remove(original)) {
        throw PatchException("$classType->$methodName is not a virtual method")
    }

    val originalReference = "$classType->$methodName$METHOD_SUFFIX" +
        "(${parameterTypes.joinToString("")})$returnType"

    val clone = ImmutableMethod.of(original).toMutable()
    mutableClass.virtualMethods += clone

    clone.addInstructions(0, body(originalReference))

    original.name += METHOD_SUFFIX
    mutableClass.virtualMethods += original
}
