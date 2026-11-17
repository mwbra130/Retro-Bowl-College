package app.zdrgon.patches.luminar.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.luminar.shared.Constants.COMPATIBILITY_LUMINAR
import java.util.logging.Logger

/**
 * Luminar — Enable Lifetime.
 *
 * Luminar sells Lifetime ($69.99 one-time) and Yearly
 * ($29.99/yr). Entitlement is served by Adapty: its backend
 * validates receipts and serves the profile, but no
 * server-computed premium flag is ever read — Skylum code
 * derives the whole gate locally from the profile. The store
 * (Ls57;) holds a StateFlow of UserPaymentData (Llka;) whose
 * isPremium / isPrime / planType fields feed ~24 feature and UI
 * readers, and Superwall's entitlement set is only a mirror of
 * that state (see Fingerprints.kt for the full derivation map).
 *
 * Every derivation — Ls9;->d, the store's own profile callbacks
 * Le57;/Lf57;, the Superwall purchase controller Lw9; — funnels
 * through the premium access level's two DTO getters: the level
 * (IDs "premium" / "premium&prime") counts when isActive() is
 * true, and the plan is Lifetime when isLifetime() is true.
 * This patch pins both getters to true, plus
 * AdaptyProfile$Subscription.isActive() so primeSku and the
 * persisted has_store_prime_subscription flag light up too.
 * Every profile refresh then derives isPremium = true with
 * planType = LIFETIME through the app's own code, and the
 * store persists that as the last-known state, so later cold
 * starts are premium from the seed. The patch creates no
 * purchase and touches nothing in Adapty's networking or
 * billing; it only changes what the local derivation reads.
 */
@Suppress("unused")
val enableLifetimePatch = bytecodePatch(
    name = "Enable Lifetime",
    description = "Turns on Lifetime permanently: every premium feature unlocks as if the one-time Lifetime purchase (Mobile + Web) were bought, and stays on across restarts.",
    default = true
) {
    compatibleWith(COMPATIBILITY_LUMINAR)

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val returnTrue =
            """
                const/4 v0, 0x1
                return v0
            """.trimIndent()

        AccessLevelIsActiveFingerprint.method.addInstructions(0, returnTrue)
        logger.info("Luminar: AdaptyProfile.AccessLevel.isActive pinned to true.")

        AccessLevelIsLifetimeFingerprint.method.addInstructions(0, returnTrue)
        logger.info("Luminar: AdaptyProfile.AccessLevel.isLifetime pinned to true (planType derives LIFETIME).")

        SubscriptionIsActiveFingerprint.method.addInstructions(0, returnTrue)
        logger.info("Luminar: AdaptyProfile.Subscription.isActive pinned to true (primeSku / persisted Prime state).")
    }
}
