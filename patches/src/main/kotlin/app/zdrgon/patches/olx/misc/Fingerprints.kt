package app.zdrgon.patches.olx.misc

import app.morphe.patcher.Fingerprint

/**
 * The per-hit sink of the search repository: rrb.emit (a Flow
 * collector in Lrrb;, classes2.dex) deserializes each
 * ElasticSearch hit's "_source" into a ListingModel
 * (Ldl7;) via Lcl7;.a(Lcz6;Lwz6;)Ldl7;, de-duplicates it
 * by id against the repository's LinkedHashSet, and emits it
 * downstream. Every home / search / category listing passes through
 * this one method. The "hits" string belongs to its sibling branch;
 * "_source" is unique to the per-hit branch of this method.
 */
object ListingEmitFingerprint : Fingerprint(
    definingClass = "Lrrb;",
    name = "emit",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;", "Lv13;"),
    strings = listOf("_source", "hits"),
)

/**
 * The promo interleave of the search feed: urb.invokeSuspend
 * (Lurb;, classes2.dex) is the search repository's flow
 * transform; at fixed slots it substitutes/inserts promo items
 * (FeaturedAgency, CarInspectionWidget, the Google Ads
 * ListingAdBanner, InterScroller) into the listing stream. The
 * "slot_index" / "page_index" / "lpv" strings are built only here,
 * for the ad banner's slot bookkeeping.
 */
object FeedTransformFingerprint : Fingerprint(
    definingClass = "Lurb;",
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("slot_index", "page_index", "lpv"),
)
