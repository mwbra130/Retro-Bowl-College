package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Empty test patch — does nothing.
 * If this builds, the issue is with fingerprint/smali content.
 * If this fails, the issue is with the patch file itself.
 */
@Suppress("unused")
val emptyTestPatch = bytecodePatch(
    name = "Empty Test",
    description = "Does nothing. Build system test.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        // Intentionally empty.
    }
}
