package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Removes the "suggested people" card from the feed.
 *
 * The card is rendered by the composable Luhi;->a (identified by the
 * "suggested_people_card" string), invoked from the feed-item renderer
 * Lfd8;->a whenever an item's content is a suggested-people model. Returning
 * void at method entry skips the card entirely: the callee opens its own
 * composer group after entry, so returning before that point leaves nothing
 * unbalanced and the caller continues normally.
 */
@Suppress("unused")
val suggestedPeopleCardPatch = bytecodePatch(
    name = "Remove suggested-people card",
    description = "Hides the 'suggested people / suggested friends' card from the feed by " +
        "making its renderer a no-op.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        SuggestedPeopleCardFingerprint.method.addInstructions(0, "return-void")
    }
}
