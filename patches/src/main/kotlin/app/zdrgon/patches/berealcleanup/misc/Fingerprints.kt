package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

/**
 * Room DAO lambda that reads feed items from the FeedItemEntity cache table.
 *
 * Verified against BeReal 3.96.0 (com.bereal.ft): Lig7;->invoke holds the two
 * feed queries, both parameterized by feedId and ordered by orderIndex:
 *   "SELECT DISTINCT * FROM FeedItemEntity WHERE feedId = ? ORDER BY orderIndex LIMIT ?"
 *   and a whitespace-padded variant of "SELECT * FROM FeedItemEntity WHERE ...".
 * FeedItemEntity has a real `isSponsored` column (see Lay4;->a CREATE TABLE),
 * so filtering on it is valid SQLite.
 */
object FeedItemsQueryFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "SELECT DISTINCT * FROM FeedItemEntity WHERE feedId = ? ORDER BY orderIndex LIMIT ?",
    ),
)

/**
 * Companion Room DAO lambda returning the post-id list for a feed.
 * Same table, same WHERE clause shape; patched for consistency.
 */
object FeedPostIdsQueryFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf(
        "SELECT DISTINCT postId FROM FeedItemEntity WHERE feedId = ? ORDER BY orderIndex LIMIT ?",
    ),
)

/**
 * BeReal's AppLovin MAX initialization entry point.
 *
 * Verified against BeReal 3.96.0: Lcq;->invokeSuspend builds
 * AppLovinSdkInitializationConfiguration (mediation provider "max"), then
 * calls AppLovinSdk.initialize after the unique "INIT_ADS with segments "
 * log string.
 */
object AppLovinInitFingerprint : Fingerprint(
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("INIT_ADS with segments "),
)

/**
 * BeReal's Adjust SDK initialization entry point.
 *
 * Verified against BeReal 3.96.0: Lm6a;->a(Ljv4;) constructs AdjustConfig,
 * calls Adjust.initSdk, then registers BeReal's own Adjust lifecycle
 * callbacks (Lsx). The custom check pins the match to the method that
 * actually references AdjustConfig, since the name "a" alone is too generic.
 */
object AdjustInitFingerprint : Fingerprint(
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljv4;"),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            (instruction as? ReferenceInstruction)?.reference.toString() ==
                "Lcom/adjust/sdk/AdjustConfig;"
        } == true
    },
)

/**
 * The "suggested people" card composable.
 *
 * Verified against BeReal 3.96.0: Luhi;->a renders the card identified by the
 * "suggested_people_card" string; it is invoked from the feed-item renderer
 * Lfd8;->a when the item content is of type Lhc8.
 */
object SuggestedPeopleCardFingerprint : Fingerprint(
    name = "a",
    returnType = "V",
    strings = listOf("suggested_people_card"),
)

/**
 * The LaunchedEffect lambda inside the in-feed video composable (Le5o;->a)
 * that triggers autoplay.
 *
 * Verified against BeReal 3.96.0: Lcvd;->invokeSuspend reads the autoplay
 * boolean (iget-boolean ..., Lcvd;->t Z) and, for a non-ended player, calls
 * Player;->setPlayWhenReady(Z)V — that invoke is the autoplay trigger.
 * Tap-to-play (Luf;->onClick) uses separate call sites and is untouched.
 */
object VideoAutoplayFingerprint : Fingerprint(
    definingClass = "Lcvd;",
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)

/**
 * PairIP/Guardsquare license check entry point.
 *
 * Verified against BeReal 3.96.0: Lcom/pairip/licensecheck/LicenseClient;->checkLicense
 * runs from the PairIP Application wrapper's attachBaseContext (before the real
 * app attaches) and from LicenseContentProvider.onCreate. On a re-signed APK the
 * verdict comes back NOT_LICENSED, and processResponse launches LicenseActivity
 * ("Get this app from Play" blocking screen) via startPaywallActivity.
 * The class name is the SDK's own (not obfuscated); the string pins the match.
 */
object LicenseCheckBypassFingerprint : Fingerprint(
    definingClass = "Lcom/pairip/licensecheck/LicenseClient;",
    name = "checkLicense",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
    strings = listOf("Cannot check license with null context."),
)
