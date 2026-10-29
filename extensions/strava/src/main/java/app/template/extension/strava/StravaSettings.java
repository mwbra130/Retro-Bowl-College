package app.template.extension.strava;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.lang.ref.WeakReference;

/**
 * Runtime settings store for the Strava patches.
 *
 * Values live in a private SharedPreferences file so they can be changed
 * from the in-app settings screen (see StravaSettingsActivity) without
 * re-patching. Every read takes the patch-time value as its default, so
 * behavior is unchanged until the user actually flips a switch.
 *
 * The application context is captured by a hook the "Add settings" patch
 * installs in StravaApplication.onCreate. Until that hook has run (or if
 * the settings patch is not installed), reads simply return the provided
 * defaults.
 */
public final class StravaSettings {
    private static final String PREFS_NAME = "strava_patch_settings";

    // Feed cleanup toggles (defaults come from HideDistractionsPatch fields).
    public static final String KEY_HIDE_UPSELLING = "hide_upselling";
    public static final String KEY_HIDE_PROMO = "hide_promo";
    public static final String KEY_HIDE_FOLLOW_SUGGESTIONS = "hide_follow_suggestions";
    public static final String KEY_HIDE_CHALLENGE_SUGGESTIONS = "hide_challenge_suggestions";
    public static final String KEY_HIDE_JOIN_CHALLENGE = "hide_join_challenge";
    public static final String KEY_HIDE_JOIN_CLUB = "hide_join_club";
    public static final String KEY_HIDE_ACTIVITY_LOOKBACK = "hide_activity_lookback";
    public static final String KEY_HIDE_LOCAL_EVENTS = "hide_local_events";

    // Route trim.
    public static final String KEY_TRIM_ENABLED = "trim_enabled";
    public static final String KEY_TRIM_METERS = "trim_meters";

    // Bottom navigation tab visibility (true = tab shown).
    public static final String KEY_TAB_HOME = "tab_home";
    public static final String KEY_TAB_MAPS = "tab_maps";
    public static final String KEY_TAB_RECORD = "tab_record";
    public static final String KEY_TAB_GROUPS = "tab_groups";
    public static final String KEY_TAB_YOU = "tab_you";

    // Launcher name/icon selection (see LauncherSwitcher).
    public static final String KEY_LAUNCHER_NAME = "launcher_name";
    public static final String KEY_LAUNCHER_ICON = "launcher_icon";

    private static volatile Context appContext;
    private static volatile SharedPreferences preferences;

    private StravaSettings() {
    }

    // The currently resumed activity, tracked hxreborn-PatchContext-style
    // through lifecycle callbacks registered in attach(). The settings
    // screen is opened from this activity, which is always a valid,
    // foreground window context.
    private static WeakReference<Activity> resumedActivity = new WeakReference<>(null);

    /** Called from the patched StravaApplication.onCreate. */
    public static void attach(Context context) {
        if (context == null) {
            return;
        }
        appContext = context.getApplicationContext();
        if (context instanceof Application) {
            ((Application) context).registerActivityLifecycleCallbacks(
                    new Application.ActivityLifecycleCallbacks() {
                        @Override
                        public void onActivityResumed(Activity activity) {
                            resumedActivity = new WeakReference<>(activity);
                        }

                        @Override
                        public void onActivityCreated(Activity activity, Bundle state) {
                        }

                        @Override
                        public void onActivityStarted(Activity activity) {
                        }

                        @Override
                        public void onActivityPaused(Activity activity) {
                        }

                        @Override
                        public void onActivityStopped(Activity activity) {
                        }

                        @Override
                        public void onActivitySaveInstanceState(Activity activity, Bundle state) {
                        }

                        @Override
                        public void onActivityDestroyed(Activity activity) {
                        }
                    });
        }
    }

    /**
     * Opens the patch settings screen, hxreborn PatchContext.open() style:
     * from the currently resumed activity when there is one, otherwise
     * from the application context with FLAG_ACTIVITY_NEW_TASK.
     */
    public static void openSettings() {
        Activity activity = resumedActivity.get();
        Context context = activity != null ? activity : appContext;
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, StravaSettingsActivity.class);
        if (activity == null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private static SharedPreferences prefs() {
        SharedPreferences local = preferences;
        if (local == null) {
            Context context = appContext;
            if (context == null) {
                return null;
            }
            synchronized (StravaSettings.class) {
                local = preferences;
                if (local == null) {
                    local = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    preferences = local;
                }
            }
        }
        return local;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences prefs = prefs();
        return prefs == null ? defaultValue : prefs.getBoolean(key, defaultValue);
    }

    public static void putBoolean(String key, boolean value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) {
            prefs.edit().putBoolean(key, value).apply();
        }
    }

    public static int getInt(String key, int defaultValue) {
        SharedPreferences prefs = prefs();
        return prefs == null ? defaultValue : prefs.getInt(key, defaultValue);
    }

    public static void putInt(String key, int value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) {
            prefs.edit().putInt(key, value).apply();
        }
    }

    public static String getString(String key, String defaultValue) {
        SharedPreferences prefs = prefs();
        return prefs == null ? defaultValue : prefs.getString(key, defaultValue);
    }

    public static void putString(String key, String value) {
        SharedPreferences prefs = prefs();
        if (prefs != null) {
            prefs.edit().putString(key, value).apply();
        }
    }
}
