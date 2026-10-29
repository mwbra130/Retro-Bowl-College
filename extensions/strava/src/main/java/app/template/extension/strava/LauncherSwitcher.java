package app.template.extension.strava;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;

/**
 * Switches the launcher name/icon at runtime.
 *
 * Strava's launcher entry is SplashActivity plus a set of activity-alias
 * components (its own built-in alternate icons). The "Custom app name and
 * icon" patch prepares the full combination table in the manifest: every
 * (name, icon) pair below exists as exactly one launcher component, and
 * precisely one of them is enabled at any time. This class flips which
 * one is enabled through PackageManager, which is how Android supports
 * runtime icon changes; no re-patching is needed.
 *
 * Names: "original" (@string/app_name) and "custom"
 * (@string/app_name_custom, written by the branding patch).
 * Icons: "Original" (SplashActivity / ic_launcher_default), "Accent" (an
 * alias whose icon is the Strava glyph recolored to the theme accent),
 * plus Strava's 15 built-in alternate icons.
 */
public final class LauncherSwitcher {
    public static final String NAME_ORIGINAL = "original";
    public static final String NAME_CUSTOM = "custom";

    public static final String[] ICON_KEYS = {
            "Original", "Accent", "LocalLegend", "Xom", "Trophy", "Medal", "Hiking",
            "Cycling", "Running", "Kudos", "Sticker", "Pride", "Road", "Gravel",
            "Teal", "Red", "Black",
    };

    private static final String PACKAGE = "com.strava";
    private static final String SPLASH_ACTIVITY = "com.strava.SplashActivity";

    private LauncherSwitcher() {
    }

    /** The launcher component that shows the given name/icon combination. */
    public static String componentFor(String nameChoice, String iconKey) {
        boolean custom = NAME_CUSTOM.equals(nameChoice);
        if ("Original".equals(iconKey)) {
            return custom ? SPLASH_ACTIVITY + "DefaultIconCustomName" : SPLASH_ACTIVITY;
        }
        if ("Accent".equals(iconKey)) {
            return SPLASH_ACTIVITY + "AccentIcon" + (custom ? "CustomName" : "");
        }
        return SPLASH_ACTIVITY + iconKey + "Icon" + (custom ? "CustomName" : "");
    }

    /** Every launcher component prepared by the branding patch. */
    public static String[] allComponents() {
        String[] result = new String[ICON_KEYS.length * 2];
        int i = 0;
        for (String name : new String[]{NAME_ORIGINAL, NAME_CUSTOM}) {
            for (String icon : ICON_KEYS) {
                result[i++] = componentFor(name, icon);
            }
        }
        return result;
    }

    /**
     * The currently effective selection, read back from PackageManager
     * (the ground truth, independent of what this settings store last
     * wrote). Returns {nameChoice, iconKey}, defaulting to the original
     * name/icon if nothing conclusive is found.
     */
    public static String[] currentSelection(Context context) {
        PackageManager pm = context.getPackageManager();
        for (String component : allComponents()) {
            ComponentName cn = new ComponentName(PACKAGE, component);
            try {
                int state = pm.getComponentEnabledSetting(cn);
                boolean enabled;
                if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                    enabled = true;
                } else if (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                    ActivityInfo info = pm.getActivityInfo(cn, 0);
                    enabled = info.enabled;
                } else {
                    enabled = false;
                }
                if (enabled) {
                    for (String name : new String[]{NAME_ORIGINAL, NAME_CUSTOM}) {
                        for (String icon : ICON_KEYS) {
                            if (componentFor(name, icon).equals(component)) {
                                return new String[]{name, icon};
                            }
                        }
                    }
                    return new String[]{NAME_ORIGINAL, "Original"};
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                // Component not present (branding patch not installed): skip.
            }
        }
        return new String[]{NAME_ORIGINAL, "Original"};
    }

    /** Enables exactly the chosen component and disables all the others. */
    public static void apply(Context context, String nameChoice, String iconKey) {
        PackageManager pm = context.getPackageManager();
        String target = componentFor(nameChoice, iconKey);
        for (String component : allComponents()) {
            ComponentName cn = new ComponentName(PACKAGE, component);
            try {
                pm.setComponentEnabledSetting(
                        cn,
                        component.equals(target)
                                ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                                : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP);
            } catch (IllegalArgumentException ignored) {
                // Component not present (branding patch not installed): skip.
            }
        }
        StravaSettings.putString(StravaSettings.KEY_LAUNCHER_NAME, nameChoice);
        StravaSettings.putString(StravaSettings.KEY_LAUNCHER_ICON, iconKey);
    }
}
