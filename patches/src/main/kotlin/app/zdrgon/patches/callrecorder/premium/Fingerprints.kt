package app.zdrgon.patches.callrecorder.premium

import app.morphe.patcher.Fingerprint

/**
 * The license state's single writer: App.i(state, sub) stores the
 * state into the static App.t field (and the sub-state into App.y),
 * then broadcasts the change. Every reader in the app goes through
 * App.e(), which returns App.t. The only other write to App.t in
 * the whole dex is the class initializer setting it to 0
 * (check pending).
 */
object AppSetStateFingerprint : Fingerprint(
    definingClass = "Lorg/skvalex/cr/App;",
    name = "i",
    returnType = "V",
    parameters = listOf("I", "I"),
)
