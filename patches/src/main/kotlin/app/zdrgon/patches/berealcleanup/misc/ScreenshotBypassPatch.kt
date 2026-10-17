package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private val COMPATIBILITY_SCREENSHOT_PRIVATE = Compatibility(
    name = "BeReal",
    packageName = "com.bereal.ft",
    apkFileType = ApkFileType.XAPK,
    targets = listOf(
        AppTarget(version = "3.97.1"),
    ),
)

/**
 * OkHttp request builder — intercepts outgoing requests.
 *
 * Strategy: BeReal reports screenshots via POST to "content/screenshots".
 * Rather than finding the obfuscated detector, we hook OkHttp's Request.Builder
 * and drop any request whose URL contains "content/screenshots".
 *
 * Verified against BeReal 3.97.1: "content/screenshots" exists in classes3.dex
 * string pool. The Retrofit interface didn't survive decompilation, but the
 * endpoint string is confirmed.
 */
object OkHttpRequestBuilderFingerprint : Fingerprint(
    definingClass = "Lokhttp3/Request\$Builder;",
    name = "url",
    returnType = "Lokhttp3/Request\$Builder;",
    parameters = listOf("Ljava/lang/String;"),
)

/**
 * Private screenshot notification suppression patch.
 *
 * Intercepts OkHttp requests and silently drops any POST to BeReal's
 * screenshot reporting endpoint ("content/screenshots"). Screenshots still
 * work locally — the image is captured — but the server is never notified,
 * so the poster sees no screenshot indicator.
 *
 * BETA PATCH — test carefully before use.
 */
val screenshotBypassPatch = bytecodePatch(
    name = "Hide screenshot notification",
    description = "Blocks BeReal's screenshot reports from reaching the server. " +
        "Screenshots work normally locally, but the poster is not notified.",
) {
    compatibleWith(COMPATIBILITY_SCREENSHOT_PRIVATE)

    execute {
        OkHttpRequestBuilderFingerprint.method.apply {
            // At the start of Request.Builder.url(String), check if the URL
            // contains "content/screenshots". If so, replace it with a null
            // route that goes nowhere.
            //
            // p1 = the URL string parameter
            addInstructions(
                0,
                """
                const-string v0, "content/screenshots"
                invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :no_block
                # URL contains screenshot endpoint — redirect to invalid URL
                # This causes the request to fail silently without notifying the server
                const-string p1, "https://127.0.0.1/screenshot-blocked"
                :no_block
                nop
                """.trimIndent(),
            )
        }
    }
}
