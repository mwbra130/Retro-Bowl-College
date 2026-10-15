package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val WHERE_CLAUSE = "WHERE feedId = ?"
private const val WHERE_CLAUSE_FILTERED = "WHERE feedId = ? AND isSponsored = 0"

private fun smaliEscape(value: String): String {
    val sb = StringBuilder(value.length)
    for (c in value) {
        when (c) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(c)
        }
    }
    return sb.toString()
}

/**
 * Rewrites every `WHERE feedId = ?` const-string in [method] to also require
 * `isSponsored = 0`, by overwriting the same register the original
 * const-string loaded. Returns the number of queries rewritten.
 */
private fun filterFeedQueries(method: MutableMethod): Int {
    val instructions = method.instructions.toList()
    var patched = 0

    instructions.forEachIndexed { index, instruction ->
        val original = ((instruction as? ReferenceInstruction)?.reference as? StringReference)
            ?.string ?: return@forEachIndexed
        if (!original.contains(WHERE_CLAUSE)) return@forEachIndexed

        val register = (instruction as? OneRegisterInstruction)?.registerA
            ?: throw PatchException(
                "const-string at index $index is not a single-register instruction",
            )
        val rewritten = original.replace(WHERE_CLAUSE, WHERE_CLAUSE_FILTERED)
        method.replaceInstruction(
            index,
            "const-string v$register, \"${smaliEscape(rewritten)}\"",
        )
        patched++
    }
    return patched
}

/**
 * Blocks all ads in BeReal.
 *
 * Combines two approaches:
 * 1. Fullscreen interstitials: No-op AdnFullscreenAd.show() (Layer 1) and
 *    finish FullscreenAdActivity.onCreate() immediately (Layer 2). The SDK
 *    init is left alone to avoid the ~5s crash loop.
 * 2. Sponsored feed posts: Rewrites the Room feed queries to filter
 *    `isSponsored = 0`, so sponsored rows never reach the UI.
 */
@Suppress("unused")
val blockAdsPatch = bytecodePatch(
    name = "Block ads",
    description = "Blocks fullscreen interstitial ads and removes sponsored posts from feeds.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // Part 1: Block interstitial ad display.
        // Layer 1: No-op AdnFullscreenAd.show().
        try {
            AdnFullscreenAdShowFingerprint.method?.addInstructions(0, "return-void")
        } catch (e: Exception) {
            // Skip if not found.
        }

        // Layer 2: Finish FullscreenAdActivity immediately.
        try {
            FullscreenAdActivityOnCreateFingerprint.method?.addInstructions(
                0,
                """
                    invoke-virtual {p0}, Lio/adn/sdk/internal/ui/fullscreen/FullscreenAdActivity;->finish()V
                    return-void
                """.trimIndent(),
            )
        } catch (e: Exception) {
            // Skip if not found.
        }

        // Part 2: Filter sponsored posts from feed queries.
        val primaryPatched = filterFeedQueries(FeedItemsQueryFingerprint.method)
        if (primaryPatched == 0) {
            throw PatchException("No feed query const-string found to filter")
        }
        FeedPostIdsQueryFingerprint.methodOrNull?.let { filterFeedQueries(it) }
    }
}
