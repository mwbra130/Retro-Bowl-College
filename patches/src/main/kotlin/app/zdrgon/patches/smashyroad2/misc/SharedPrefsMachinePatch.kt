package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.patch.bytecodePatch
import app.zdrgon.patches.smashyroad2.shared.Constants.COMPATIBILITY_SMASHY_ROAD_2

/**
 * Forces slot machine unlock via SharedPreferences hook.
 *
 * Hooks SharedPreferencesImpl.getInt() — Unity's PlayerPrefs backend.
 * If the key contains "machine" or "slot", returns 1 (unlocked) immediately.
 * Otherwise, falls through to the original method.
 *
 * This tests whether the game checks PlayerPrefs (just a different key
 * than hasMachine3) vs Google Play at runtime.
 */
@Suppress("unused")
val sharedPrefsMachinePatch = bytecodePatch(
    name = "Force Machine Unlock",
    description = "Forces all machine/slot PlayerPrefs keys to return unlocked.",
    default = false,
) {
    compatibleWith(COMPATIBILITY_SMASHY_ROAD_2)

    execute {
        SharedPrefsGetIntFingerprint.methodOrNull?.let {
            it.addInstructions(
                0,
                """
                    invoke-virtual {p1}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
                    move-result-object v0
                    const-string v1, "machine"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v0
                    if-nez v0, :return_unlocked
                    invoke-virtual {p1}, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
                    move-result-object v0
                    const-string v1, "slot"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v0
                    if-eqz v0, :continue_original
                    :return_unlocked
                    const/4 v0, 0x1
                    return v0
                    :continue_original
                """.trimIndent(),
            )
        }
    }
}
