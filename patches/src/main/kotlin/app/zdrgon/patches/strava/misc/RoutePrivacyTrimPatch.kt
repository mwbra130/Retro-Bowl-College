package app.zdrgon.patches.strava.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.zdrgon.patches.strava.shared.Constants.COMPATIBILITY_STRAVA
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod

private const val FILTER_POLYLINE_CLASS =
    "Lcom/strava/activitydetail/universal/data/FilterPolylineForPublicPointsUseCase;"
private const val TRIM_EXTENSION_CLASS =
    "Lapp/template/extension/strava/RoutePrivacyTrimPatch;"
private const val METHOD_SUFFIX = "\$original"

/**
 * Trims extra distance from both ends of the activity-detail map
 * polyline, on this device only.
 *
 * In Strava 486.9 the activity map draws
 * `FilterPolylineForPublicPointsUseCase.invoke(points, privacyRange)`,
 * which returns the sublist of points inside the range Strava's own
 * per-point privacy settings allow (leading/trailing "Only me" points
 * are already excluded by GetPolylinePrivacyUseCase). This patch wraps
 * that method: the original is renamed to `invoke$original`, and the
 * replacement passes its result through the extension's distance-based
 * trimmer before returning it.
 *
 * The trim only ever shrinks the visible range. Recorded and uploaded
 * data are untouched, and anyone else viewing the activity sees exactly
 * what the owner's Strava privacy settings dictate.
 */
@Suppress("unused")
val routePrivacyTrimPatch = bytecodePatch(
    name = "Local route privacy trim",
    description = "Trims extra route distance from both ends of the activity map on this device " +
        "only, on top of Strava's own privacy filtering. Display-only: recorded and uploaded data " +
        "are unchanged, and other people still see the activity exactly as your Strava privacy " +
        "settings dictate.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_STRAVA)

    extendWith(STRAVA_EXTENSION)

    val trimMetersOption = intOption(
        key = "trimMeters",
        title = "Meters to trim",
        description = "Extra meters of route hidden from each end of the activity map on this device.",
        default = 200,
        required = true,
    )

    execute {
        val trimMeters = trimMetersOption.value ?: 200

        val classDef = classDefBy(FILTER_POLYLINE_CLASS)
        val mutableClass = mutableClassDefBy(classDef)

        val original = mutableClass.methods.first { method ->
            method.name == "invoke" &&
                method.returnType == "Ljava/util/List;" &&
                method.parameterTypes.size == 2
        }

        if (!mutableClass.virtualMethods.remove(original)) {
            throw PatchException("FilterPolyline invoke method is not a virtual method")
        }

        // Build the $original reference from the method's real signature:
        // the second parameter type is obfuscated in 486.9 (Lpow;), so it
        // must never be hardcoded.
        val originalReference = "${classDef.type}->invoke$METHOD_SUFFIX" +
            "(${original.parameterTypes.joinToString("")})${original.returnType}"

        val clone = ImmutableMethod.of(original).toMutable()
        mutableClass.virtualMethods += clone

        clone.addInstructions(
            0,
            """
                invoke-virtual {p0, p1, p2}, $originalReference
                move-result-object v0
                const v1, $trimMeters
                invoke-static {v0, v1}, $TRIM_EXTENSION_CLASS->trimFurther(Ljava/util/List;I)Ljava/util/List;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )

        original.name += METHOD_SUFFIX
        mutableClass.virtualMethods += original
    }
}
