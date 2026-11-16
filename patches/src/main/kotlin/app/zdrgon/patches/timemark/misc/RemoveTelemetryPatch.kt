package app.zdrgon.patches.timemark.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.zdrgon.patches.timemark.shared.Constants.COMPATIBILITY_TIMEMARK
import java.util.logging.Logger
import org.w3c.dom.Element

/**
 * Timemark — Remove telemetry.
 *
 * What telemetry this app actually has (full census in
 * Fingerprints.kt and in the workdir RECON.md, verified 2026-11-16
 * against the exact v10.0.316 XAPK):
 *
 * The analytics platform is SensorsData (694 SDK class references,
 * plus its AB-test and Sensors Focus campaign SDKs). The app's own
 * product analytics — clicks, watermark edits, purchase funnel
 * steps, and location-page events whose properties include the
 * latitude/longitude and the chosen address — are sent through two
 * funnels, and both end in SensorsDataAPI.track:
 *
 *   1. SensorAnalyzeUtil.wrapSensorsTrack (both overloads): the
 *      analytics facade's sinks; every one of its dozens of event
 *      methods (camera clicks, watermark statistics, weather-item
 *      settings, logo-edit pages, take-photo properties with
 *      location detail registered as super properties) ends here.
 *   2. LogService.track (both overloads): the backing service for
 *      the framework logger's track() (Xlog / Lty/h;), called from
 *      all over the app (photo-memory events, team-upload monitors,
 *      auto-save results, attendance events); LogService.monitor()
 *      delegates to it too.
 *
 * This patch neutralizes the telemetry per component:
 *
 * 1. Both funnels — PINNED AT THE SINKS. All four sink methods
 *    return immediately, so no app event reaches SensorsData. The
 *    SDK itself stays initialized (the app also reads experiment
 *    configuration through it), and the framework logger's local
 *    on-device log — which only leaves the phone when the user
 *    explicitly sends a feedback report — is untouched.
 *
 * 2. Upload/campaign components — DISABLED IN THE MANIFEST
 *    (inline resource patch in dependsOn below, so the Manager
 *    lists exactly one "Remove telemetry" entry):
 *    - SensorsFocusContentProvider: the Sensors Focus marketing
 *      campaign channel (vendor in-app campaigns/push targeting).
 *      The app's own push is its FCM service, not this.
 *    - The three Google datatransport components
 *      (TransportBackendDiscovery, JobInfoSchedulerService,
 *      AlarmManagerSchedulerBroadcastReceiver): the upload
 *      plumbing Firebase Analytics/Crashlytics/Sessions data
 *      leaves through. The app code never calls FirebaseAnalytics
 *      itself; with the scheduler disabled nothing uploads.
 *    A missing component is a hard failure: a build that changed
 *    the manifest must not silently ship with a channel enabled.
 *
 * Deliberately left alone, and why:
 * - SensorsDataContentProvider: the analytics SDK's storage
 *   provider. With both event funnels pinned, no app event ever
 *   enters it; disabling it risks breaking the SDK initialization
 *   the app's experiment/config reads rely on.
 * - The first-party API reporters on the feature client
 *   (reportDeviceFeature, reportActiveVersion, addEventCount,
 *   saveUserAttribution): suspend calls returning response beans
 *   that feature code consumes — the event counters feed
 *   server-side feature accounting — so they are dual-use feature
 *   traffic, not a clean telemetry funnel (same call made for the
 *   /inf exchange in the App Finder set).
 * - reportWatermarkSaveUsage: the save-quota metering tied to the
 *   photo-save feature itself.
 * - Firebase Messaging (the app's push notifications — a feature),
 *   the place/weather/backup/verify API traffic (the app's
 *   function), and the ad SDKs (ads, not telemetry; out of scope).
 */
@Suppress("unused")
val removeTelemetryPatch = bytecodePatch(
    name = "Remove telemetry",
    description = "Stops the app reporting usage to SensorsData: both analytics event funnels are cut, the Sensors Focus campaign provider is disabled, and the Firebase datatransport upload services are disabled. Camera, backup, weather and all other features are unaffected.",
    default = true
) {
    compatibleWith(COMPATIBILITY_TIMEMARK)

    // The manifest half of this patch, defined INLINE inside
    // dependsOn() so PatchLoader never discovers it as a separate
    // top-level entry and the Manager shows exactly one
    // "Remove telemetry" patch (same one-entry pattern as the
    // App Finder and ContentCam sets).
    dependsOn(
        resourcePatch(
            name = "Remove telemetry (manifest)",
            description = "Disables the Sensors Focus campaign provider and the Google datatransport upload components in the manifest. Part of 'Remove telemetry'; the two are applied together.",
            default = true,
        ) {
            compatibleWith(COMPATIBILITY_TIMEMARK)

            execute {
                val logger = Logger.getLogger(this::class.java.name)

                val targets = setOf(
                    "com.sensorsdata.sf.core.data.SensorsFocusContentProvider",
                    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
                    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
                    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver"
                )

                document("AndroidManifest.xml").use { doc ->
                    var disabled = 0
                    for (tag in listOf("service", "receiver", "activity", "provider")) {
                        val elements = doc.getElementsByTagName(tag)
                        for (i in 0 until elements.length) {
                            val element = elements.item(i) as Element
                            if (element.getAttribute("android:name") in targets) {
                                element.setAttribute("android:enabled", "false")
                                disabled++
                            }
                        }
                    }
                    if (disabled != targets.size) {
                        throw PatchException(
                            "Timemark telemetry: disabled $disabled of ${targets.size} upload/campaign " +
                                "components in the manifest; refusing to ship with a channel " +
                                "partly enabled."
                        )
                    }
                    logger.info("Timemark telemetry: all ${targets.size} upload/campaign components disabled in the manifest.")
                }
            }
        }
    )

    execute {
        // 1a. The analytics facade's sinks.
        WrapSensorsTrackPropsFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )

        WrapSensorsTrackFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )

        // 1b. The framework logger's backing sinks.
        LogServiceTrackPropsFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )

        LogServiceTrackFingerprint.method.addInstructions(
            0,
            """
                return-void
            """.trimIndent()
        )
    }
}
