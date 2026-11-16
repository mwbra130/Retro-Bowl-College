package app.zdrgon.patches.timemark.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.zdrgon.patches.timemark.shared.Constants.COMPATIBILITY_TIMEMARK
import java.util.logging.Logger

/**
 * Timemark — Spoof watermark location.
 *
 * Every GPS fix in Timemark flows through a single funnel:
 * Lcom/xhey/xcamera/location/GlobalLocationService; (the only
 * subclass of the location engine Lcom/xhey/xcamera/location/a;) and
 * its refreshPlace(Lry/c;, List, List) entry, which the engine calls
 * with each new fix. The fix object Lry/c; carries the coordinates
 * in two public fields — a:D = latitude, b:D = longitude — and
 * refreshPlace resolves exactly that pair into the watermark's
 * place data (LocationInfoData: large position, small position,
 * address list, EXIF location detail) through the developer's
 * server place list and the Android Geocoder.
 *
 * This patch overwrites the fix's latitude/longitude at the top of
 * refreshPlace with the coordinates set in the patch options, so
 * the server lookup, the geocoder, the geo-address cache and the
 * composed watermark text all describe the spoofed place, exactly
 * as if the photo had been taken there. The watermark's location
 * text follows the coordinates through the app's own place
 * resolution; there is no separate text funnel to pin (the composed
 * strings live in LocationInfoData fields read at many sites).
 *
 * Configuration: set BOTH options, or neither.
 *   - Both empty (the default): the patch inserts nothing and the
 *     app uses the real GPS fix. That is why this patch can ship
 *     enabled by default — an unconfigured install changes nothing
 *     and can never mislabel a photo.
 *   - Exactly one set, an unparseable value, or an out-of-range
 *     value: the patch run fails loudly with a PatchException
 *     instead of silently stamping a half-spoofed location.
 *
 * Register math (verified against the exact v10.0.316 dex):
 * refreshPlace has 36 registers and 4 ins (this + 3 parameters),
 * so the fix parameter sits in v33; v0/v1 are entry scratch.
 *
 * Scope note: the mutation happens on the fix object handed to
 * refreshPlace — the watermark/place-resolution path. Code that
 * reads a raw Location from the platform location APIs directly
 * (outside this funnel) is not affected.
 */
@Suppress("unused")
val spoofWatermarkLocationPatch = bytecodePatch(
    name = "Spoof watermark location",
    description = "Stamps photos as if taken at the latitude/longitude set in the patch options: the app resolves the place name and address for those coordinates, so the watermark shows that location. With both options empty (the default) nothing changes and the real GPS location is used.",
    default = true
) {
    compatibleWith(COMPATIBILITY_TIMEMARK)

    val latitude = stringOption(
        key = "latitude",
        default = "",
        title = "Spoofed latitude",
        description = "Decimal latitude, e.g. 33.4484. Must be set together with the longitude; leave both empty to keep the real GPS location."
    )

    val longitude = stringOption(
        key = "longitude",
        default = "",
        title = "Spoofed longitude",
        description = "Decimal longitude, e.g. -112.0740. Must be set together with the latitude; leave both empty to keep the real GPS location."
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val latText = latitude.value?.trim().orEmpty()
        val lngText = longitude.value?.trim().orEmpty()

        if (latText.isEmpty() && lngText.isEmpty()) {
            // Unconfigured: insert nothing. The app keeps using the
            // real GPS fix, so an install that never set the options
            // behaves exactly like stock.
            logger.info("Timemark spoof: no coordinates configured; location left unchanged.")
        } else {
            if (latText.isEmpty() || lngText.isEmpty()) {
                throw PatchException(
                    "Timemark spoof: latitude and longitude must be set together " +
                        "(latitude='$latText', longitude='$lngText'). Set both, or clear both " +
                        "to keep the real GPS location."
                )
            }
            val lat = latText.toDoubleOrNull()
                ?: throw PatchException("Timemark spoof: latitude '$latText' is not a number.")
            val lng = lngText.toDoubleOrNull()
                ?: throw PatchException("Timemark spoof: longitude '$lngText' is not a number.")
            if (lat < -90.0 || lat > 90.0) {
                throw PatchException("Timemark spoof: latitude $lat is out of range (-90 to 90).")
            }
            if (lng < -180.0 || lng > 180.0) {
                throw PatchException("Timemark spoof: longitude $lng is out of range (-180 to 180).")
            }

            val latBits = java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(lat))
            val lngBits = java.lang.Long.toHexString(java.lang.Double.doubleToRawLongBits(lng))

            RefreshPlaceFingerprint.method.addInstructions(
                0,
                """
                    const-wide v0, 0x$latBits
                    iput-wide v0, v33, Lry/c;->a:D
                    const-wide v0, 0x$lngBits
                    iput-wide v0, v33, Lry/c;->b:D
                """.trimIndent()
            )
            logger.info("Timemark spoof: watermark location set to $lat, $lng.")
        }
    }
}
