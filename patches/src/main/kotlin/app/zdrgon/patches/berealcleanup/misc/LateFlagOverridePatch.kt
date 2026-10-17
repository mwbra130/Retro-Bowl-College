package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

private val COMPATIBILITY_LATE_PRIVATE = Compatibility(
    name = "BeReal",
    packageName = "com.bereal.ft",
    apkFileType = ApkFileType.XAPK,
    targets = listOf(
        AppTarget(version = "3.97.1"),
    ),
)

/**
 * BeReal's PostDraftEntity constructor — the single persistence point for the
 * late flag before API submission.
 *
 * Verified against BeReal 3.97.1: Ldefpackage/pig; is the PostDraftEntity
 * Room entity with a 47-parameter constructor. The `isLate` boolean is the
 * 31st parameter (field E), at smali register p37.
 *
 * Data flow: PostDraftEntity(isLate) -> Room DB -> API JSON "isLate" ->
 * displayed to viewers as the late indicator.
 */
object LateFlagFingerprint : Fingerprint(
    definingClass = "Ldefpackage/pig;",
    name = "<init>",
    returnType = "V",
    custom = { method, _ ->
        method.parameters.size == 47
    },
)

/**
 * Private late-flag override patch.
 *
 * Reads /sdcard/bereal_late.txt (single line: "true" or "false").
 * When the file exists, the patch substitutes its value for BeReal's
 * calculated isLate flag when constructing the post draft. This affects
 * the value stored locally, sent to the API, and displayed to viewers.
 *
 * When the file is absent, BeReal's normal late calculation is untouched.
 *
 * BETA PATCH — test carefully before use.
 */
val lateFlagOverridePatch = bytecodePatch(
    name = "Remove late flag",
    description = "Reads /sdcard/bereal_late.txt and substitutes its value " +
        "for BeReal's late flag. Only active when the file exists.",
) {
    compatibleWith(COMPATIBILITY_LATE_PRIVATE)

    execute {
        LateFlagFingerprint.method.apply {
            // p37 = isLate boolean (31st constructor parameter).
            // Read /sdcard/bereal_late.txt; if it contains "true"/"false",
            // overwrite p37 before the original constructor body runs.
            addInstructions(
                0,
                """
                const-string v0, "/sdcard/bereal_late.txt"
                new-instance v1, Ljava/io/File;
                invoke-direct {v1, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V
                invoke-virtual {v1}, Ljava/io/File;->exists()Z
                move-result v0
                if-eqz v0, :no_override
                new-instance v0, Ljava/io/BufferedReader;
                new-instance v1, Ljava/io/FileReader;
                const-string v2, "/sdcard/bereal_late.txt"
                invoke-direct {v1, v2}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V
                invoke-direct {v0, v1}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
                invoke-virtual {v0}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :no_override
                invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;
                move-result-object v0
                const-string v1, "true"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v1
                if-nez v1, :set_true
                const-string v1, "false"
                invoke-virtual {v0, v1}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
                move-result v1
                if-eqz v1, :no_override
                const/4 p37, 0x0
                goto :no_override
                :set_true
                const/4 p37, 0x1
                :no_override
                nop
                """.trimIndent(),
            )
        }
    }
}
