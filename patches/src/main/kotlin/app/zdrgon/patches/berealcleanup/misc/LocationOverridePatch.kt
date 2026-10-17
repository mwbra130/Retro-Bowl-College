package app.zdrgon.patches.berealcleanup.misc

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch

private val COMPATIBILITY_LOCATION_PRIVATE = Compatibility(
    name = "BeReal",
    packageName = "com.bereal.ft",
    apkFileType = ApkFileType.XAPK,
    targets = listOf(
        AppTarget(version = "3.97.1"),
    ),
)

/**
 * BeReal's Location entity constructor.
 *
 * Verified against BeReal 3.97.1: Ldefpackage/teb; is
 * bereal.app.entities.Location with constructor
 *   teb(DD)V  (longitude first, latitude second)
 * toString: "Location(longitude=..., latitude=...)".
 * The constructor is called when building post location data.
 */
object LocationEntityFingerprint : Fingerprint(
    definingClass = "Ldefpackage/teb;",
    name = "<init>",
    returnType = "V",
    parameters = listOf("D", "D"),
)

/**
 * Private location override patch.
 *
 * Reads /sdcard/bereal_location.txt (format: "latitude,longitude" on one line,
 * e.g. "33.8542,-112.1197"). When the file exists, the patch substitutes the
 * file's coordinates for the GPS location when BeReal constructs its Location
 * entity. When the file is absent, BeReal's normal GPS behavior is untouched.
 *
 * Use OpenStreetMap Nominatim (https://nominatim.openstreetmap.org) to resolve
 * a place name to coordinates, then write them to the file.
 *
 * BETA PATCH — test carefully before use.
 */
val locationOverridePatch = bytecodePatch(
    name = "Change location",
    description = "Reads /sdcard/bereal_location.txt and substitutes its coordinates " +
        "for BeReal's GPS location. Only active when the file exists.",
) {
    compatibleWith(COMPATIBILITY_LOCATION_PRIVATE)

    execute {
        LocationEntityFingerprint.method.apply {
            // At the start of teb.<init>(D longitude, D latitude), check for
            // the override file. Registers: p1/p2 = longitude (wide), p3/p4 = latitude (wide).
            // We inject code that reads the file and overwrites p1-p4 if present.
            addInstructions(
                0,
                """
                const-string v0, "/sdcard/bereal_location.txt"
                new-instance v1, Ljava/io/File;
                invoke-direct {v1, v0}, Ljava/io/File;-><init>(Ljava/lang/String;)V
                invoke-virtual {v1}, Ljava/io/File;->exists()Z
                move-result v0
                if-eqz v0, :no_override
                new-instance v0, Ljava/io/BufferedReader;
                new-instance v1, Ljava/io/FileReader;
                const-string v2, "/sdcard/bereal_location.txt"
                invoke-direct {v1, v2}, Ljava/io/FileReader;-><init>(Ljava/lang/String;)V
                invoke-direct {v0, v1}, Ljava/io/BufferedReader;-><init>(Ljava/io/Reader;)V
                invoke-virtual {v0}, Ljava/io/BufferedReader;->readLine()Ljava/lang/String;
                move-result-object v0
                if-eqz v0, :no_override
                const-string v1, ","
                invoke-virtual {v0, v1}, Ljava/lang/String;->split(Ljava/lang/String;)[Ljava/lang/String;
                move-result-object v0
                array-length v1, v0
                const/4 v2, 0x2
                if-lt v1, v2, :no_override
                const/4 v1, 0x0
                aget-object v1, v0, v1
                invoke-static {v1}, Ljava/lang/Double;->parseDouble(Ljava/lang/String;)D
                move-result-wide v1
                const/4 v3, 0x1
                aget-object v0, v0, v3
                invoke-static {v0}, Ljava/lang/Double;->parseDouble(Ljava/lang/String;)D
                move-result-wide v3
                # v1/v2 = latitude, v3/v4 = longitude
                # teb constructor takes (longitude, latitude) — p1/p2 = longitude, p3/p4 = latitude
                move-wide p1, v3
                move-wide p3, v1
                :no_override
                nop
                """.trimIndent(),
            )
        }
    }
}
