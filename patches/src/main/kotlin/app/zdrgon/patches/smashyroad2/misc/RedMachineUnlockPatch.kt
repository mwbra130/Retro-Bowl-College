package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.rawResourcePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Unlocks all slot machines (machine1/2/3) without Google Play.
 *
 * Patches the ownership check in slotManager to always grant the machines.
 * At startup, the game sets hasMachine1/2/3=0, queries Google Play, and
 * only sets them to 1 if the purchase is found. This patch NOPs the
 * conditional branch so the "owned" path always executes.
 *
 * Target: b.ne at RVA 0x162D450 (v1.54)
 *   Original: 61 00 00 54 (b.ne #0x162d45c)
 *   Patched:  1F 20 03 D5 (nop)
 *
 * Also patches purchaseMachine3() to instantly grant on buy tap.
 */
private val MACHINE_CHECK_ORIGINAL = byteArrayOf(
    0x61, 0x00, 0x00, 0x54,
)

private val MACHINE_CHECK_PATCHED = byteArrayOf(
    0x1F, 0x20, 0x03, 0xD5.toByte(),
)

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
    description = "Unlocks all slot machines (1/2/3) without Google Play purchase. " +
        "Patches the native ownership check to always grant the machines.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        fun findPattern(pattern: ByteArray): Int {
            for (i in 0..bytes.size - pattern.size) {
                var match = true
                for (j in pattern.indices) {
                    if (bytes[i + j] != pattern[j]) {
                        match = false
                        break
                    }
                }
                if (match) return i
            }
            return -1
        }

        // Patch 1: NOP the b.ne so machines are always marked owned at startup
        val checkOffset = findPattern(MACHINE_CHECK_ORIGINAL)
        if (checkOffset == -1) {
            throw Exception(
                "RedMachineUnlock: Could not find machine check pattern. " +
                    "APK version may not be supported (expected v1.54)."
            )
        }
        for (j in MACHINE_CHECK_PATCHED.indices) {
            bytes[checkOffset + j] = MACHINE_CHECK_PATCHED[j]
        }

        // Patch 2: purchaseMachine3() instantly grants on buy tap
        val purchaseOffset = findPattern(PURCHASE_MACHINE3_ORIGINAL)
        if (purchaseOffset != -1) {
            for (j in PURCHASE_MACHINE3_PATCHED.indices) {
                bytes[purchaseOffset + j] = PURCHASE_MACHINE3_PATCHED[j]
            }
        }

        soFile.writeBytes(bytes)
    }
}
