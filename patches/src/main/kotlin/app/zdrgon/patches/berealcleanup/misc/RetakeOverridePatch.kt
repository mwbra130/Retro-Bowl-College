package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction

private val COMPATIBILITY_RETAKE_PRIVATE = Compatibility(
    name = "BeReal",
    packageName = "com.bereal.ft",
    apkFileType = ApkFileType.XAPK,
    targets = listOf(
        AppTarget(version = "3.97.1"),
    ),
)

/**
 * BeReal's PostDraftEntity constructor — the local database entity that holds
 * the retake count before the post is finalized and uploaded.
 *
 * Verified against BeReal 3.97.1: Ldefpackage/pig; is the PostDraftEntity
 * (Room table with retakeCounter column, see lig.java:360 INSERT statement).
 * The constructor takes 47 parameters; the retakeCounter is the int parameter
 * at position 26 (after locationLatitude:Double and locationLongitude:Double),
 * which maps to p30 in smali registers.
 *
 * Fingerprint uses a custom check on parameter count to avoid hardcoding the
 * full 48-parameter signature.
 */
object PostDraftEntityFingerprint : Fingerprint(
    definingClass = "Ldefpackage/pig;",
    name = "<init>",
    returnType = "V",
    custom = { method, _ ->
        // Match the 47-parameter PostDraftEntity constructor
        method.parameters.size == 47
    },
)

/**
 * Private retake count override patch.
 *
 * Reads /sdcard/bereal_retakes.txt (single integer on one line, e.g. "0").
 * When the file exists, the patch substitutes its value for BeReal's retake
 * counter when constructing the PostDraftEntity. This affects the count stored
 * locally, sent to the API, and displayed to viewers.
 *
 * When the file is absent, BeReal's normal retake counting is untouched.
 *
 * BETA PATCH — test carefully before use.
 */
val retakeOverridePatch = bytecodePatch(
    name = "Set retake count",
    description = "Reads /sdcard/bereal_retakes.txt and substitutes its integer value " +
        "for BeReal's retake counter. Only active when the file exists.",
) {
    compatibleWith(COMPATIBILITY_RETAKE_PRIVATE)

    execute {
        PostDraftEntityFingerprint.method.apply {
            // p30 = retakeCounter (int), the 26th constructor parameter
            // (after p26/p27=locationLatitude:Double, p28/p29=locationLongitude:Double)
            addInstructions(
                0,
                """
                const-string v0, "/sdcard/bereal_retakes.txt"
                new-instance v1, Ljava/io/File;
                invoke-direct {v1, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V
                invoke-virtual {v1}, Ljava/io/File;->exists()Z
                move-result v0
                if-eqz v0, :no_override
                new-instance v0, Ljava/io/BufferedReader;
                new-instance v1, Ljava/io/FileReader;
                const-string v2, "/sdcard/bereal_retakes.txt"
                invoke-direct {v1, v2}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V
                invoke-direct {v0, v1}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
                invoke-virtual {v0}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :no_override
                invoke-virtual {v0}, Ljava/lang/String;->trim()Ljava/lang/String;
                move-result-object v0
                invoke-static {v0}, Ljava/lang/Integer;->parseInt(Ljava/lang/String;)I
                move-result v0
                # Override p30 (retakeCounter) with the file value
                move p30, v0
                :no_override
                nop
                """.trimIndent(),
            )
        }
    }
}
