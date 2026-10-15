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
 * Uses a 24-byte unique pattern (verified: 1 occurrence in 52MB binary).
 *
 * Pattern (24 bytes at file offset 0x16292E8):
 *   0F 03 5D 94 1F 04 00 71 61 00 00 54 28 00 80 52
 *   68 42 0E 39 80 02 40 F9
 *                                  ^^^^^^^^^^^
 *                                  b.ne -> nop here (offset 8)
 *
 * Patched: byte at offset 8..11 changes from 61 00 00 54 to 1F 20 03 D5
 */
private val MACHINE_CHECK_PATTERN = byteArrayOf(
    0x0F, 0x03, 0x5D.toByte(), 0x94.toByte(),
    0x1F, 0x04, 0x00, 0x71,
    0x61, 0x00, 0x00, 0x54,
    0x28, 0x00, 0x80.toByte(), 0x52,
    0x68, 0x42, 0x0E, 0x39,
    0x80.toByte(), 0x02, 0x40, 0xF9.toByte(),
)

// NOP instruction (ARM64)
private val NOP = byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte())

@Suppress("unused")
val redMachineUnlockPatch = rawResourcePatch(
    name = "Slot Machine Unlock",
    description = "Unlocks all slot machines (1/2/3) without Google Play purchase. " +
        "Patches the native ownership check to always grant the machines at startup.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        // Find the unique 24-byte pattern
        var foundOffset = -1
        var matchCount = 0
        for (i in 0..bytes.size - MACHINE_CHECK_PATTERN.size) {
            var match = true
            for (j in MACHINE_CHECK_PATTERN.indices) {
                if (bytes[i + j] != MACHINE_CHECK_PATTERN[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                matchCount++
                if (foundOffset == -1) foundOffset = i
            }
        }

        if (matchCount != 1) {
            throw Exception(
                "SlotMachineUnlock: Expected 1 pattern match, found $matchCount. " +
                    "APK version may not be supported (expected v1.54). Aborting safely."
            )
        }

        // Replace the b.ne (at offset 8 in pattern) with NOP
        val patchOffset = foundOffset + 8
        for (j in NOP.indices) {
            bytes[patchOffset + j] = NOP[j]
        }

        soFile.writeBytes(bytes)
    }
}
