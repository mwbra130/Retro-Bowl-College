package app.zdrgon.patches.smashyroad2.misc

import app.morphe.patcher.Fingerprint

/**
 * SharedPreferencesImpl.getInt(String, int).
 * Unity's PlayerPrefs backend on Android. Hook this to force
 * machine unlock keys to return 1.
 */
object SharedPrefsGetIntFingerprint : Fingerprint(
    definingClass = "Landroid/app/SharedPreferencesImpl;",
    name = "getInt",
    returnType = "I",
    parameters = listOf(
        "Ljava/lang/String;",
        "I",
    ),
)
