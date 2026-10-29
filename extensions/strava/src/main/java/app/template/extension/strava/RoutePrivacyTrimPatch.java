package app.template.extension.strava;

import com.strava.geomodels.data.GeoPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Local display privacy helper for activity maps.
 *
 * Strava already filters the activity-detail polyline to the range the
 * activity's per-point privacy settings allow (points marked "Only me"
 * at the start and end are dropped). This helper only ever shrinks that
 * visible range further, on this device, by trimming up to trimMeters
 * of route from both ends of the already-filtered polyline.
 *
 * It never widens the visible range, never touches recorded or uploaded
 * data, and has no effect on what anyone else sees. If trimming would
 * leave fewer than two points, the input is returned unchanged so the
 * map always has a drawable line.
 */
public class RoutePrivacyTrimPatch {
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /**
     * The patch-time trim distance: the "Local route privacy trim" patch
     * writes its trimMeters option into this field when the APK is built.
     * The in-app settings override it at runtime; this is only the
     * fallback for users who never open the settings screen.
     */
    public static int patchDefaultMeters = 200;

    /**
     * Runtime entry point called by the patched polyline filter. Honors
     * the in-app settings: the trim can be switched off, and the distance
     * changed, without re-patching.
     */
    public static List<GeoPoint> trimFurtherRuntime(List<GeoPoint> points) {
        if (!StravaSettings.getBoolean(StravaSettings.KEY_TRIM_ENABLED, true)) {
            return points;
        }
        int meters = StravaSettings.getInt(StravaSettings.KEY_TRIM_METERS, patchDefaultMeters);
        return trimFurther(points, meters);
    }

    public static List<GeoPoint> trimFurther(List<GeoPoint> points, int trimMeters) {
        if (points == null || trimMeters <= 0 || points.size() < 3) {
            return points;
        }

        int start = 0;
        double accumulated = 0.0;
        while (start < points.size() - 2 && accumulated < trimMeters) {
            accumulated += distanceMeters(points.get(start), points.get(start + 1));
            start++;
        }

        int end = points.size() - 1;
        accumulated = 0.0;
        while (end > start + 1 && accumulated < trimMeters) {
            accumulated += distanceMeters(points.get(end), points.get(end - 1));
            end--;
        }

        if (end - start < 1) {
            return points;
        }
        return new ArrayList<>(points.subList(start, end + 1));
    }

    private static double distanceMeters(GeoPoint a, GeoPoint b) {
        double lat1 = Math.toRadians(a.getLatitude());
        double lat2 = Math.toRadians(b.getLatitude());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1.0, Math.sqrt(h)));
    }
}
