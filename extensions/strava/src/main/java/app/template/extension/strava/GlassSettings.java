package app.template.extension.strava;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Liquid Glass settings for Strava — an "Extra Settings" row at the
 * top of Strava's own Settings screen (the Liquid Glass patch wraps
 * SettingsFragment.onCreateView and calls wrapSettingsView), which
 * opens a dialog with the glass controls.
 *
 * Controls write the morphe.glass.* keys in Strava's own
 * com.strava_preferences file; GlassNav reads them when the main
 * activity starts, so changes apply on the next app start.
 */
public final class GlassSettings {

    private static final String TAG_ROW = "morphe_glass_settings_row";

    private GlassSettings() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("com.strava_preferences", Context.MODE_PRIVATE);
    }

    // ------------------------------------------------------------------
    // Settings-screen row
    // ------------------------------------------------------------------

    /**
     * Returns a new root containing an "Extra Settings" header row
     * above the original Settings view. Called from the patched
     * SettingsFragment.onCreateView.
     */
    public static View wrapSettingsView(View original) {
        try {
            if (original == null || TAG_ROW.equals(original.getTag())) {
                return original;
            }
            Context context = original.getContext();
            float density = context.getResources().getDisplayMetrics().density;

            LinearLayout root = new LinearLayout(context);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setTag(TAG_ROW);
            root.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, Math.round(56 * density)));
            row.setPadding(Math.round(20 * density), 0, Math.round(20 * density), 0);
            row.setClickable(true);
            row.setFocusable(true);

            TextView label = new TextView(context);
            label.setText("Extra Settings");
            label.setTextSize(16);
            label.setTextColor(themeColor(context, android.R.attr.textColorPrimary, Color.WHITE));
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            row.addView(label);

            TextView chevron = new TextView(context);
            chevron.setText("›");
            chevron.setTextSize(22);
            chevron.setTextColor(themeColor(context, android.R.attr.textColorSecondary, Color.LTGRAY));
            row.addView(chevron);

            final View originalRef = original;
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = activityFrom(originalRef.getContext());
                    if (activity != null) {
                        show(activity);
                    }
                }
            });
            root.addView(row);

            ViewGroup.LayoutParams lp = original.getLayoutParams();
            root.addView(original, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            if (lp != null) {
                root.setLayoutParams(lp);
            }
            return root;
        } catch (Throwable ignored) {
            return original;
        }
    }

    private static Activity activityFrom(Context context) {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                return (Activity) ctx;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        return context instanceof Activity ? (Activity) context : null;
    }

    private static int themeColor(Context context, int attr, int fallback) {
        try {
            TypedArray ta = context.getTheme().obtainStyledAttributes(new int[]{attr});
            int color = ta.getColor(0, fallback);
            ta.recycle();
            return color;
        } catch (Throwable ignored) {
            return fallback;
        }
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

            TextView section = new TextView(activity);
            section.setText("LIQUID GLASS");
            section.setTextSize(13);
            section.setPadding(0, 0, 0, Math.round(4 * density));
            root.addView(section);

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
                    .setTitle("Extra Settings")
                    .setView(root)
                    .setPositiveButton("Done", null)
                    .show();
        } catch (Throwable ignored) {
        }
    }

    private static void addSwitch(LinearLayout root, Activity activity,
                                  final SharedPreferences prefs, String title,
                                  final String key, boolean def) {
        float density = activity.getResources().getDisplayMetrics().density;
        int vPad = Math.round(10 * density);
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, vPad, 0, vPad);

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
