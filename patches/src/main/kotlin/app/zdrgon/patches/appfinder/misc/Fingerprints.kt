package app.zdrgon.patches.appfinder.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

// ── Plan tier gate — App Finder 1.5.1 ────────────────────────────────
//
// App Finder sells three paid plans (Advance / Ultimate /
// Professional) as Play Billing products. The current plan is a
// single int on the user-state object (verified 2026-11-16 across
// both app dexes):
//
//   Lscadica/aq/UU;   field f:I — the plan tier
//     0 = free, 1/2 = Advance, 3 = Ultimate, 4 = Professional
//     (tier names from Lscadica/aq/BllKt;->pns)
//
// The tier is produced by Lscadica/aq/BlKt;->chkP, which parses the
// user's Play purchases (productId char 0 = period w/m/s/x, char 1
// = tier digit), stores one Lscadica/aq/Pla; per tier in UU.pla and
// derives UU.f from the plans whose end timestamp is still ahead.
// A lifetime ("x") purchase is stored with an end ~200 years out;
// nothing anywhere distinguishes it beyond that timestamp.
//
// At READ time nothing asks a server and nothing re-checks the Pla
// list: every feature asks the state object directly —
//
//   Lscadica/aq/UU;->getF()I   ~39 call sites across 14 files
//
// (filter/sort gates, other-country apps, local ratings lists,
// developer-country filter, min/target-API display, and the CSV
// export limit, which reads getF() > 3 ? 20,000 : 2,000 locally at
// export time). The only direct iget of UU.f in the app is inside
// getF()/setF() themselves. UsrKt.valiChg, despite the name, only
// refreshes gated UI when the value changes; it makes no network
// calls.
//
// NOTE: Morphe fingerprints compare access flags by EXACT equality;
// getF is declared "public final" (0x0011).

/** The plan-tier reader. Pinning it pins every paid feature. */
object PlanTierFingerprint : Fingerprint(
    returnType = "I",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    definingClass = "Lscadica/aq/UU;",
    name = "getF"
)
