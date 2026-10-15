package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.rawResourcePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Unlocks the red slot machine (machine3) without Google Play.
 *
 * Patches slotManager.purchaseMachine3() in libil2cpp.so to set
 * hasMachine3 = true and return immediately, skipping the
 * Google Play Billing purchase flow.
 *
 * Target: purchaseMachine3() at RVA 0x162E400 (v1.54)
 * Field: hasMachine3 at object offset 0x392
 *
 * Original (12 bytes):
 *   str x30, [sp, #-0x20]!
 *   stp x20, x19, [sp, #0x10]
 *   adrp x20, #0x335f000
 *
 * Patched (12 bytes):
 *   mov w8, #1
 *   strb w8, [x0, #0x392]  ; hasMachine3 = true
 *   ret                    ; skip IAP, return
 */
private val PURCHASE_MACHINE3_ORIGINAL = byteArrayOf(
    0xFE.toByte(), 0x0F, 0x1E.toByte(), 0xF8.toByte(),
    0xF4.toByte(), 0x4F, 0x01, 0xA9.toByte(),
    0x94.toByte(), 0xE9.toByte(), 0x00, 0xB0.toByte(),
)

private val PURCHASE_MACHINE3_PATCHED = byteArrayOf(
    0x28, 0x00, 0x80.toByte(), 0x52,
    0x08, 0x48, 0x0E, 0x39,
    0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
)

@Suppress("unused")
val redMachineUnlockPatch = rawResourcePatch(
    name = "Red Slot Machine Unlock",
    description = "Unlocks the red slot machine (machine3) without Google Play purchase. " +
        "Patches the native purchase method to grant the machine instantly.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        // Find the original byte pattern
        var foundOffset = -1
        for (i in 0..bytes.size - PURCHASE_MACHINE3_ORIGINAL.size) {
            var match = true
            for (j in PURCHASE_MACHINE3_ORIGINAL.indices) {
                if (bytes[i + j] != PURCHASE_MACHINE3_ORIGINAL[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                foundOffset = i
                break
            }
        }

        if (foundOffset == -1) {
            throw Exception(
                "RedMachineUnlock: Could not find purchaseMachine3 pattern in libil2cpp.so. " +
                    "The APK version may not be supported (expected v1.54)."
            )
        }

        // Apply the patch
        for (j in PURCHASE_MACHINE3_PATCHED.indices) {
            bytes[foundOffset + j] = PURCHASE_MACHINE3_PATCHED[j]
        }

        soFile.writeBytes(bytes)
    }
}
