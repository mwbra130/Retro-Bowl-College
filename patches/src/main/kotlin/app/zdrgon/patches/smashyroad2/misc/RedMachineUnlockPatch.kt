package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.rawResourcePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Unlocks all slot machines (machine1/2/3) without Google Play.
 *
 * The game has three separate ownership checks, one per machine.
 * Each sets hasMachineN=0, queries Google Play, and only sets it to 1
 * if the purchase is found. This patch NOPs all three conditional
 * branches so the "owned" path always executes.
 *
 * All patterns verified unique (1 occurrence each in 52MB binary).
 * Aborts safely if any pattern count isn't exactly 1.
 */
private val MACHINE1_PATTERN = byteArrayOf(
    0x0F, 0x03, 0x5D.toByte(), 0x94.toByte(),
    0x1F, 0x04, 0x00, 0x71,
    0x61, 0x00, 0x00, 0x54,
    0x28, 0x00, 0x80.toByte(), 0x52,
    0x68, 0x42, 0x0E, 0x39,
    0x80.toByte(), 0x02, 0x40, 0xF9.toByte(),
)

private val MACHINE2_PATTERN = byteArrayOf(
    0x06, 0x03, 0x5D.toByte(), 0x94.toByte(),
    0x1F, 0x04, 0x00, 0x71,
    0x61, 0x00, 0x00, 0x54,
    0x28, 0x00, 0x80.toByte(), 0x52,
    0x68, 0x46, 0x0E, 0x39,
    0x80.toByte(), 0x02, 0x40, 0xF9.toByte(),
)

private val MACHINE3_PATTERN = byteArrayOf(
    0xFF.toByte(), 0x02, 0x5D.toByte(), 0x94.toByte(),
    0x1F, 0x04, 0x00, 0x71,
    0x61, 0x00, 0x00, 0x54,
    0x28, 0x00, 0x80.toByte(), 0x52,
    0x68, 0x4A, 0x0E, 0x39,
    0xF7.toByte(), 0xD7.toByte(), 0x00, 0xD0.toByte(),
)

// NOP instruction (ARM64) - replaces b.ne at offset 8 in each pattern
private val NOP = byteArrayOf(0x1F, 0x20, 0x03, 0xD5.toByte())

// Vehicle/character unlock via checkIfUnlocked - DISABLED
// The mov w1,#1 patch was not effective. Research ongoing.
// Keeping patterns for reference but not applying.
private val CHECK_VEHICLE_PATTERN = byteArrayOf(
    0x94.toByte(), 0x94.toByte(), 0x05, 0x94.toByte(),
    0xA0.toByte(), 0x0A, 0x00, 0xB4.toByte(),
    0x08, 0x58, 0x40, 0xB9.toByte(),
    0x1F, 0x01, 0x13, 0x6B,
)

private val CHECK_PERSON_PATTERN = byteArrayOf(
    0x83.toByte(), 0xFE.toByte(), 0xFF.toByte(), 0x97.toByte(),
    0x34, 0x00, 0x00, 0x14,
    0xE0.toByte(), 0x03, 0x14, 0xAA.toByte(),
    0xA6.toByte(), 0xF0.toByte(), 0xFF.toByte(), 0x97.toByte(),
)

// buyBtn() - called when tapping a locked car's buy button
// Pattern: 16 bytes unique
private val BUY_BTN_PATTERN = byteArrayOf(
    0x71, 0x71, 0x5B.toByte(), 0x94.toByte(),
    0x60, 0x62, 0x40, 0xF9.toByte(),
    0x80.toByte(), 0x0A, 0x00, 0xB4.toByte(),
    0xA1.toByte(), 0x02, 0x40, 0xF9.toByte(),
)

// unlockAll() - unlocks all vehicles
// Pattern: 16 bytes unique (for calculating bl target)
private val UNLOCK_ALL_PATTERN = byteArrayOf(
    0x56, 0xD4.toByte(), 0x00, 0xD0.toByte(),
    0xD6.toByte(), 0x26, 0x47, 0xF9.toByte(),
    0xC1.toByte(), 0x02, 0x40, 0xF9.toByte(),
    0xA1.toByte(), 0xAC.toByte(), 0x05, 0x94.toByte(),
)

private val FORCE_UNLOCKED = byteArrayOf(
    0x21, 0x00, 0x80.toByte(), 0x52,
)

@Suppress("unused")
val redMachineUnlockPatch = rawResourcePatch(
    name = "Unlock All (Machines + Vehicles)",
    description = "Unlocks all slot machines and all vehicles. " +
        "Machines: patches native ownership checks. " +
        "Vehicles: buy button now calls unlockAll() instead of Google Play.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        val soFile = get("lib/arm64-v8a/libil2cpp.so", true)
        val bytes = soFile.readBytes()

        fun findUnique(pattern: ByteArray, name: String): Int {
            var foundOffset = -1
            var count = 0
            for (i in 0..bytes.size - pattern.size) {
                var match = true
                for (j in pattern.indices) {
                    if (bytes[i + j] != pattern[j]) {
                        match = false
                        break
                    }
                }
                if (match) {
                    count++
                    if (foundOffset == -1) foundOffset = i
                }
            }
            if (count != 1) {
                throw Exception(
                    "UnlockAll: Expected 1 match for $name, found $count. " +
                        "APK version may not be supported. Aborting safely."
                )
            }
            return foundOffset
        }

        // Patch 1-3: Slot machines (NOP the b.ne at offset 8)
        val machinePatterns = listOf(
            MACHINE1_PATTERN to "machine1",
            MACHINE2_PATTERN to "machine2",
            MACHINE3_PATTERN to "machine3",
        )
        for ((pattern, name) in machinePatterns) {
            val offset = findUnique(pattern, name)
            val patchOffset = offset + 8
            for (j in NOP.indices) {
                bytes[patchOffset + j] = NOP[j]
            }
        }

        // Patch 4: Vehicles - buyBtn() calls unlockAll() instead of IAP
        // Find both patterns, calculate dynamic bl offset
        val buyBtnOffset = findUnique(BUY_BTN_PATTERN, "buyBtn")
        val unlockAllOffset = findUnique(UNLOCK_ALL_PATTERN, "unlockAll")
        
        // Calculate bl offset: (target - pc) / 4
        // bl is at buyBtnOffset, target is unlockAllOffset
        // Note: file offsets, but bl uses relative addressing which is same
        val blOffset = (unlockAllOffset - buyBtnOffset) / 4
        if (blOffset < -0x2000000 || blOffset > 0x1FFFFFF) {
            throw Exception("UnlockAll: bl offset out of range: $blOffset")
        }
        // bl encoding: 0x94000000 | (offset & 0x3FFFFFF)
        val blInstr = 0x94000000 or (blOffset and 0x3FFFFFF)
        val blBytes = byteArrayOf(
            (blInstr and 0xFF).toByte(),
            ((blInstr shr 8) and 0xFF).toByte(),
            ((blInstr shr 16) and 0xFF).toByte(),
            ((blInstr shr 24) and 0xFF).toByte(),
        )
        // ret; nop; nop (12 bytes to fill 16 total)
        val retNops = byteArrayOf(
            0xC0.toByte(), 0x03, 0x5F, 0xD6.toByte(),
            0x1F, 0x20, 0x03, 0xD5.toByte(),
            0x1F, 0x20, 0x03, 0xD5.toByte(),
        )
        for (j in blBytes.indices) {
            bytes[buyBtnOffset + j] = blBytes[j]
        }
        for (j in retNops.indices) {
            bytes[buyBtnOffset + 4 + j] = retNops[j]
        }

        // Vehicle/character patches DISABLED - not effective
        // Research ongoing for proper unlock method
        /*
        val vehicleOffset = findUnique(CHECK_VEHICLE_PATTERN, "checkIfUnlocked")
        for (j in FORCE_UNLOCKED.indices) {
            bytes[vehicleOffset + j] = FORCE_UNLOCKED[j]
        }

        val personOffset = findUnique(CHECK_PERSON_PATTERN, "checkIfUnlockedPerson")
        for (j in FORCE_UNLOCKED.indices) {
            bytes[personOffset + j] = FORCE_UNLOCKED[j]
        }
        */

        soFile.writeBytes(bytes)
    }
}
