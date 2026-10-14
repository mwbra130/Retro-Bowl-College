package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private const val SET_PLAY_WHEN_READY =
    "Landroidx/media3/common/Player;->setPlayWhenReady(Z)V"

/**
 * Disables video autoplay in BeReal's feeds.
 *
 * The in-feed video composable builds a media3 ExoPlayer and runs a
 * LaunchedEffect lambda on the autoplay flag; for a non-ended player that
 * effect rewinds to 0 and calls Player.setPlayWhenReady(autoplay) — the
 * autoplay trigger. This patch nops those invokes, so videos load paused on
 * their thumbnail. The fingerprint finds the effect behaviorally (it is the
 * only invokeSuspend calling getPlaybackState/seekTo/setPlayWhenReady), so
 * it survives BeReal's per-release obfuscation renames.
 *
 * Deliberately surgical: the autoplay boolean is NOT forced to false,
 * because it may feed other UI logic. Nopping only the invokes leaves all
 * other logic untouched.
 *
 * Tap-to-play keeps working — it goes through a separate onClick call site,
 * not this effect.
 *
 * Scope: this disables autoplay everywhere the video composable is used —
 * the Discovery feed, the friends feed, and profiles — not just Discovery.
 */
@Suppress("unused")
val videoAutoplayPatch = bytecodePatch(
    name = "Disable video autoplay",
    description = "Videos stay paused until you tap them. Works in all feeds and profiles.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        val method = VideoAutoplayFingerprint.method

        // Snapshot first: replaceInstruction mutates the implementation.
        val instructions = method.instructions.toList()
        val triggerIndices = instructions.mapIndexedNotNull { index, instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference ?: return@mapIndexedNotNull null
            if (reference.toString() == SET_PLAY_WHEN_READY) index else null
        }

        if (triggerIndices.isEmpty()) {
            throw PatchException("setPlayWhenReady trigger not found in the autoplay invokeSuspend")
        }

        // The trigger is a void invoke: replacing with nop has no
        // stack or register side effects.
        triggerIndices.forEach { index ->
            method.replaceInstruction(index, "nop")
        }
    }
}
