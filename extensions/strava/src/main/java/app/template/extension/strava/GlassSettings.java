package app.template.extension.strava;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Liquid Glass settings for Strava — in-app tweaks without a
 * separate settings activity: a long press on the Home tab of the
 * bottom bar opens a dialog with the glass controls (the same
 * gesture this bundle's old Strava settings used).
 *
 * Controls write the morphe.glass.* keys in Strava's own
 * com.strava_preferences file; GlassNav reads them when the main
 * activity starts, so changes apply on the next app start.
 */
public final class GlassSettings {

    private static final String TAG_HOME_LONG_PRESS = "morphe_glass_settings";

    private GlassSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("com.strava_preferences", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------
    // Long-press Home tab -> settings dialog
    // ------------------------------------------------------------------

    /** Called from the patched BottomNavigationActivity.onCreate. */
    public static void attachHomeLongPress(final Activity activity) {
        try {
            String pkg = activity.getPackageName();
            int navId = activity.getResources().getIdentifier("bottom_navigation", "id", pkg);
            int homeId = activity.getResources().getIdentifier("navigation_home", "id", pkg);
            if (navId == 0 || homeId == 0) {
                return;
            }
            final View nav = activity.findViewById(navId);
            if (nav == null) {
                return;
            }
            if (!attachToHomeItem(activity, nav, homeId)) {
                // Item views are created with the menu; retry after layout.
                nav.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            attachToHomeItem(activity, nav, homeId);
                        } catch (Throwable ignored) {
                        }
                    }
                });
            }
        } catch (Throwable ignored) {
            // Never let a settings hook failure break the activity.
        }
    }

    private static boolean attachToHomeItem(final Activity activity, View nav, int homeId) {
        View home = findViewByIdRecursive(nav, homeId);
        if (home == null) {
            return false;
        }
        if (!TAG_HOME_LONG_PRESS.equals(home.getTag())) {
            home.setTag(TAG_HOME_LONG_PRESS);
            home.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    show(activity);
                    return true;
                }
            });
        }
        return true;
    }

    private static View findViewByIdRecursive(View root, int id) {
        if (root.getId() == id) {
            return root;
        }
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findViewByIdRecursive(group.getChildAt(i), id);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Dialog
    // ------------------------------------------------------------------

    public static void show(final Activity activity) {
        try {
            final SharedPreferences prefs = prefs(activity);
            float density = activity.getResources().getDisplayMetrics().density;
            int pad = Math.round(20 * density);

            LinearLayout root = new LinearLayout(activity);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setPadding(pad, Math.round(8 * density), pad, 0);

            addSwitch(root, activity, prefs, "Liquid Glass navigation",
                    GlassNav.KEY_ENABLED, true);
            addSwitch(root, activity, prefs, "Floating pill (off = docked bar)",
                    GlassNav.KEY_FLOATING, true);
            addSwitch(root, activity, prefs, "Blur backdrop",
                    GlassNav.KEY_BLUR, true);

            // Tint opacity: SeekBar progress 0..75 maps to 20..95%.
            final TextView label = new TextView(activity);
            label.setTextSize(16);
            label.setPadding(0, Math.round(14 * density), 0, Math.round(4 * density));
            int current = prefs.getInt(GlassNav.KEY_OPACITY, 72);
            current = Math.max(20, Math.min(95, current));
            label.setText("Tint opacity — " + current + "%");
            root.addView(label);

            SeekBar seek = new SeekBar(activity);
            seek.setMax(75);
            seek.setProgress(current - 20);
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                    label.setText("Tint opacity — " + (progress + 20) + "%");
                    if (fromUser) {
                        prefs.edit().putInt(GlassNav.KEY_OPACITY, progress + 20).apply();
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar bar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar bar) {
                }
            });
            root.addView(seek);

            TextView note = new TextView(activity);
            note.setText("Changes apply on next app start.");
            note.setTextSize(13);
            note.setPadding(0, Math.round(12 * density), 0, 0);
            root.addView(note);

            new AlertDialog.Builder(activity)
                    .setTitle("Liquid Glass")
                    .setView(root)
                    .setPositiveButton("Done", null)
                    .show();
        } catch (Throwable ignored) {
        }
    }

    private static void addSwitch(LinearLayout root, Activity activity,
                                  final SharedPreferences prefs, String title,
                                  final String key, boolean def) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, Math.round(10 * activity.getResources()
                .getDisplayMetrics().density), 0, Math.round(10 * activity
                .getResources().getDisplayMetrics().density));

        TextView tv = new TextView(activity);
        tv.setText(title);
        tv.setTextSize(16);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        Switch sw = new Switch(activity);
        sw.setChecked(prefs.getBoolean(key, def));
        sw.setOnCheckedChangeListener((button, checked) ->
                prefs.edit().putBoolean(key, checked).apply());
        row.addView(sw);

        root.addView(row);
    }
}
