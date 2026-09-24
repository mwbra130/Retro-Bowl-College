package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Stops BeReal's PairIP/Guardsquare native library from loading.
 *
 * The 2026-09-24 logcat proved the v1.3.0/v1.3.1 Java-layer bypasses were
 * aimed at the wrong layer: the process dies with a native SIGSEGV inside
 * `libpairipcore.so` ~0.12s after the library loads — a deterministic,
 * deliberate tamper-response kill that fires during native init, before any
 * Java license-check code can matter. There is no Java stack trace because
 * the JVM never throws; the process simply dies in native code.
 *
 * This patch no-ops the `System.loadLibrary("pairipcore")` (or
 * `System.load(".../libpairipcore.so")`) call, so the native tamper check
 * never runs. Only the load invoke itself is replaced with `nop` — the rest
 * of the method (e.g. the remainder of `<clinit>`) is left intact.
 *
 * Known follow-up risk: if BeReal's Java code calls native methods declared
 * in the PairIP bridge classes during startup, those calls will now throw
 * UnsatisfiedLinkError instead of segfaulting. That is strictly better for
 * diagnosis — the next logcat will show a normal Java stack trace naming
 * the exact method that needs stubbing, and a follow-up patch can no-op it.
 */
@Suppress("unused")
val pairipNativeKillPatch = bytecodePatch(
    name = "Kill PairIP native library",
    description = "Stops the PairIP tamper-check native library from loading, " +
        "so the app can't be killed by its native code. Required for the app to launch.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        val method = PairipNativeLoadFingerprint.method

        // Snapshot first: replaceInstruction mutates the implementation.
        // invoke-static(-range) is a single instruction, so replacing it
        // with nop keeps every other index valid.
        val instructions = method.instructions.toList()
        var patched = 0

        instructions.forEachIndexed { index, instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
                ?: return@forEachIndexed
            if (reference.definingClass == "Ljava/lang/System;" &&
                (reference.name == "loadLibrary" || reference.name == "load")
            ) {
                method.replaceInstruction(index, "nop")
                patched++
            }
        }

        if (patched == 0) {
            throw PatchException(
                "PairIP native-load method found, but no System.loadLibrary/System.load " +
                    "invoke to neutralize",
            )
        }
    }
}
