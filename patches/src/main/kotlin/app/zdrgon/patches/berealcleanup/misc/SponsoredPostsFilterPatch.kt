package app.zdrgon.patches.berealcleanup.misc

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
    // Snapshot first: replaceInstruction mutates the implementation.
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
 * Filters sponsored posts out of BeReal's feeds at the data layer.
 *
 * BeReal caches feed items in the Room table FeedItemEntity, which has a real
 * `isSponsored` column. The feed-read DAO (Lig7;->invoke) runs two queries of
 * the form `... WHERE feedId = ? ORDER BY orderIndex LIMIT ?`; this patch
 * rewrites both const-string SQL literals in place to
 * `... WHERE feedId = ? AND isSponsored = 0 ...`, so sponsored rows never
 * reach the UI — in every feed that renders from the cache (Friends and
 * Discovery).
 */
@Suppress("unused")
val sponsoredPostsFilterPatch = bytecodePatch(
    name = "Remove sponsored posts",
    description = "Removes sponsored posts from the Friends and Discovery feeds.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // Primary: the feed-item queries (two const-strings in one method).
        val primaryPatched = filterFeedQueries(FeedItemsQueryFingerprint.method)
        if (primaryPatched == 0) {
            throw PatchException("No feed query const-string found to filter")
        }

        // Companion: the post-id list query (Lib2), patched for consistency.
        // Not fatal if the companion lookup changes shape in a future build.
        FeedPostIdsQueryFingerprint.methodOrNull?.let { filterFeedQueries(it) }
    }
}
