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

@Suppress("unused")
val redMachineUnlockPatch = rawResourcePatch(
    name = "Slot Machine Unlock",
    description = "Unlocks all slot machines (1/2/3) without Google Play purchase. " +
        "Patches the native ownership checks to always grant the machines at startup.",
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
                    "SlotMachineUnlock: Expected 1 match for $name, found $count. " +
                        "APK version may not be supported. Aborting safely."
                )
            }
            return foundOffset
        }

        // Patch all three machine checks
        val patterns = listOf(
            MACHINE1_PATTERN to "machine1",
            MACHINE2_PATTERN to "machine2",
            MACHINE3_PATTERN to "machine3",
        )

        for ((pattern, name) in patterns) {
            val offset = findUnique(pattern, name)
            // Replace b.ne (at offset 8) with NOP
            val patchOffset = offset + 8
            for (j in NOP.indices) {
                bytes[patchOffset + j] = NOP[j]
            }
        }

        soFile.writeBytes(bytes)
    }
}
