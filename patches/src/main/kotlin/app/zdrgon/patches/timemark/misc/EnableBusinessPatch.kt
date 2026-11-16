package app.zdrgon.patches.timemark.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.timemark.shared.Constants.COMPATIBILITY_TIMEMARK

/**
 * Timemark — Enable Business.
 *
 * The Business plan's local distinction from Plus is one boolean
 * config flag, "key_team_space_is_business_plan_user", read
 * through the config store Lcom/xhey/xcamera/util/i1;->a(I Z)Z
 * (which reads the same prefs store as "key_is_vip"). A full
 * census found exactly 8 literal read sites; the state is
 * produced by one sync writer:
 *
 *   Lb50/y0;->W(Z)V — called with the server-synced business
 *   value, it writes BOTH the cached static Lb50/y0;->k Z and
 *   the persisted config flag (through i1.g) whenever they
 *   differ, then posts the membership-state refresh. The account
 *   coroutine Lb50/p0; calls W(false) when the account has no
 *   team space, and the team-space refreshers Lb50/y0;->H(...)/I()
 *   re-read the flag into the cache.
 *
 * This patch pins W's argument to true at entry (const/4 v3, 0x1;
 * W is a 4-register static, so its boolean argument is v3). The
 * app's own sync then persists the business flag through its own
 * writer, and every local read site — the dominant gate
 * Ldb0/n;->e(), the membership enum Ldb0/n;->g(), the team-space
 * cache and its readers (the save quota, the paywall, the ad
 * service), and the renewal-offer suppression — answers as a
 * business-plan user. On top of that, this patch applies the full
 * Plus pin set (see EnablePlusPatch), because Business includes
 * Plus; it is self-contained, so selecting Business alone works,
 * and selecting both patches in one run applies the shared pins
 * twice — value-identical blocks that stack harmlessly.
 *
 * Honest scope: what is covered is every LOCAL business gate.
 * The Business headline features are account/server state and
 * cannot materialize from local pins: shared team storage
 * (300,000 photos), unlimited projects and team data itself
 * (the team-space model is downloaded from the server; without a
 * business account there are no projects to show), centralized
 * team billing, admin control, SharePoint team drives, and the
 * Map & Spreadsheet views of team data. Those stay unavailable,
 * exactly as they are for a Plus user; this patch does not fake
 * server data.
 */
@Suppress("unused")
val enableBusinessPatch = bytecodePatch(
    name = "Enable Business",
    description = "Turns on the app's Business plan state locally (everything in Enable Plus, plus the business-plan flag the team-space and paywall gates read). Team storage, team billing, admin control and other server-side business features cannot be created by a local patch.",
    default = true
) {
    compatibleWith(COMPATIBILITY_TIMEMARK)

    execute {
        // Business includes Plus: the full shared pin set first.
        applyPlusPins()

        // The business-plan producer: force the sync writer's
        // argument to true so the app persists the business flag
        // (and its cached mirror) through its own code paths.
        BusinessSyncFingerprint.method.addInstructions(
            0,
            """
                const/4 v3, 0x1
            """.trimIndent()
        )
    }
}
