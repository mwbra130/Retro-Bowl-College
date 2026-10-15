package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.berealcleanup.shared.Constants.COMPATIBILITY_BEREAL_CLEANUP

/**
 * Kills AppLovin MAX initialization.
 *
 * BeReal's ad entry point (Lcq;->invokeSuspend) builds an
 * AppLovinSdkInitializationConfiguration and calls AppLovinSdk.initialize.
 * Returning Kotlin's Unit singleton at method entry runs none of that, so
 * the AppLovin SDK is never initialized. Also remove the AppLovinInitProvider
 * manifest entry (see TrackerProvidersPatch) so the SDK cannot self-initialize
 * through its content provider.
 *
 * Note: this stops SDK-served ads, but BeReal's sponsored feed posts are
 * first-party items — those are handled by SponsoredPostsFilterPatch.
 *
 * DISABLED as of v1.14.3: the obfuscated Lcq fingerprint may match the wrong
 * method, potentially causing the ~5s crash. Testing without it.
 */
@Suppress("unused")
val adSdkInitKillPatch = bytecodePatch(
    name = "Disable AppLovin ads",
    description = "Blocks AppLovin ads from loading.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_BEREAL_CLEANUP)

    execute {
        // DISABLED as of v1.14.3 for crash diagnosis.
        // The obfuscated Lcq fingerprint may match the wrong method.
    }
}
