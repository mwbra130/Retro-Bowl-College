package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.rawResourcePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * DISABLED - Caused app crashes (non-unique byte pattern patched wrong location).
 * Kept as placeholder to prevent the broken v1.22.0/v1.22.1 patch from applying.
 */
@Suppress("unused")
val redMachineUnlockPatch = rawResourcePatch(
    name = "Red Slot Machine Unlock (DISABLED)",
    description = "DISABLED: Caused crashes. Do not use.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        // Disabled - does nothing
    }
}
