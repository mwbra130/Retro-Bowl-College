package app.template.extension.strava;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import java.lang.reflect.Method;

/**
 * In-app settings screen for the Strava patches, modelled on hxreborn's
 * BeReal settings panel: a dedicated Activity from the extension, opened
 * from a bar the "Add settings" patch adds to Strava's own settings
 * screen.
 *
 * Every control writes straight to StravaSettings (SharedPreferences).
 * The feed filters and the route trim read that store at call time, so
 * changes take effect on the next content load without re-patching. Tab
 * visibility is applied when the main activity is created, and launcher
 * name/icon changes are applied immediately through LauncherSwitcher.
 */
public class StravaSettingsActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(18, 18, 18);
    private static final int BAR_BACKGROUND = Color.rgb(30, 30, 30);
    private static final int TEXT_PRIMARY = Color.WHITE;
    private static final int TEXT_SECONDARY = Color.rgb(170, 170, 170);

    // ------------------------------------------------------------------
    // Hooks called from patched Strava code (see StravaSettingsPatch).
    // ------------------------------------------------------------------

    /**
     * Wraps the settings screen's content view with a bottom bar whose
     * button opens this activity. Strava's settings screen is a Compose
     * view; wrapping it in a FrameLayout and shrinking it by the bar
     * height leaves the Compose content fully usable above the bar.
     * Any failure returns the original view untouched.
     */
    public static View wrapSettingsView(Context context, View content) {
        if (context == null || content == null) {
            return content;
        }
        try {
            int barHeight = dp(context, 56);
            FrameLayout root = new FrameLayout(context);

            FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            contentParams.bottomMargin = barHeight;
            root.addView(content, contentParams);

            Button bar = new Button(context);
            bar.setText("Morphe patch settings");
            bar.setTextColor(TEXT_PRIMARY);
            bar.setBackgroundColor(BAR_BACKGROUND);
            bar.setOnClickListener(view -> {
                Intent intent = new Intent(context, StravaSettingsActivity.class);
                if (!(context instanceof Activity)) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            });
            FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, barHeight);
            barParams.gravity = Gravity.BOTTOM;
            root.addView(bar, barParams);
            return root;
        } catch (Throwable ignored) {
            return content;
        }
    }

    /**
     * Hides bottom navigation tabs whose settings toggle is off. Called
     * at the end of BottomNavigationActivity.onCreate, when the menu is
     * already inflated. Resolved reflectively because the extension
     * compiles against the stock Android SDK, not Material. If every tab
     * would end up hidden the bar is left untouched.
     */
    public static void applyTabVisibility(Activity activity) {
        try {
            String pkg = activity.getPackageName();
            int navId = activity.getResources().getIdentifier("bottom_navigation", "id", pkg);
            if (navId == 0) {
                return;
            }
            View nav = activity.findViewById(navId);
            if (nav == null) {
                return;
            }
            Method getMenu = nav.getClass().getMethod("getMenu");
            Menu menu = (Menu) getMenu.invoke(nav);
            if (menu == null) {
                return;
            }
            String[][] tabs = {
                    {"navigation_home", StravaSettings.KEY_TAB_HOME},
                    {"navigation_maps", StravaSettings.KEY_TAB_MAPS},
                    {"navigation_record", StravaSettings.KEY_TAB_RECORD},
                    {"navigation_groups", StravaSettings.KEY_TAB_GROUPS},
                    {"navigation_you", StravaSettings.KEY_TAB_YOU},
            };
            int visibleCount = 0;
            for (String[] tab : tabs) {
                if (StravaSettings.getBoolean(tab[1], true)) {
                    visibleCount++;
                }
            }
            if (visibleCount == 0) {
                return;
            }
            for (String[] tab : tabs) {
                int itemId = activity.getResources().getIdentifier(tab[0], "id", pkg);
                if (itemId == 0) {
                    continue;
                }
                MenuItem item = menu.findItem(itemId);
                if (item != null) {
                    item.setVisible(StravaSettings.getBoolean(tab[1], true));
                }
            }
        } catch (Throwable ignored) {
            // Never let a settings application failure break the activity.
        }
    }

    // ------------------------------------------------------------------
    // Settings UI (built programmatically; the extension ships no XML).
    // ------------------------------------------------------------------

    private LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(BACKGROUND);
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setPadding(dp(16), dp(16), dp(16), dp(24));
        scrollView.addView(rootLayout);
        setContentView(scrollView);

        TextView title = text("Strava patch settings", 20, TEXT_PRIMARY);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        rootLayout.addView(title);

        addSection("Feed cleanup");
        addSwitch("Upsell prompts", StravaSettings.KEY_HIDE_UPSELLING, HideDistractionsPatch.upselling);
        addSwitch("Promotions", StravaSettings.KEY_HIDE_PROMO, HideDistractionsPatch.promo);
        addSwitch("Who to follow suggestions", StravaSettings.KEY_HIDE_FOLLOW_SUGGESTIONS,
                HideDistractionsPatch.followSuggestions);
        addSwitch("Suggested challenges", StravaSettings.KEY_HIDE_CHALLENGE_SUGGESTIONS,
                HideDistractionsPatch.challengeSuggestions);
        addSwitch("Join challenge cards", StravaSettings.KEY_HIDE_JOIN_CHALLENGE,
                HideDistractionsPatch.joinChallenge);
        addSwitch("Joined a club cards", StravaSettings.KEY_HIDE_JOIN_CLUB, HideDistractionsPatch.joinClub);
        addSwitch("Activity lookback", StravaSettings.KEY_HIDE_ACTIVITY_LOOKBACK,
                HideDistractionsPatch.activityLookback);
        addSwitch("Local events carousel", StravaSettings.KEY_HIDE_LOCAL_EVENTS,
                HideDistractionsPatch.localEvents);

        addSection("Route trim");
        addSwitch("Trim route ends on the activity map", StravaSettings.KEY_TRIM_ENABLED, true);
        addTrimStepper();

        addSection("Bottom bar tabs");
        addSwitch("Home tab", StravaSettings.KEY_TAB_HOME, true);
        addSwitch("Maps tab", StravaSettings.KEY_TAB_MAPS, true);
        addSwitch("Record tab", StravaSettings.KEY_TAB_RECORD, true);
        addSwitch("Groups tab", StravaSettings.KEY_TAB_GROUPS, true);
        addSwitch("You tab", StravaSettings.KEY_TAB_YOU, true);

        addSection("Launcher");
        addNamePicker();
        addIconPicker();

        TextView footer = text("Feed changes apply to newly loaded content. "
                + "Bottom bar changes apply the next time the app is opened.", 13, TEXT_SECONDARY);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerParams.topMargin = dp(20);
        rootLayout.addView(footer, footerParams);
    }

    private void addSection(String title) {
        TextView header = text(title, 15, TEXT_SECONDARY);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(22);
        params.bottomMargin = dp(4);
        rootLayout.addView(header, params);
    }

    private void addSwitch(String label, String key, boolean defaultValue) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView textView = text(label, 16, TEXT_PRIMARY);
        row.addView(textView, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Switch toggle = new Switch(this);
        toggle.setChecked(StravaSettings.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener((button, checked) ->
                StravaSettings.putBoolean(key, checked));
        row.addView(toggle);
        rootLayout.addView(row);
    }

    private void addTrimStepper() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(6), 0, dp(6));

        TextView label = text("Extra meters trimmed from each end", 16, TEXT_PRIMARY);
        row.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView value = text(metersLabel(), 16, TEXT_PRIMARY);
        value.setMinWidth(dp(64));
        value.setGravity(Gravity.CENTER);

        Button minus = new Button(this);
        minus.setText("−");
        minus.setOnClickListener(view -> {
            int next = Math.max(0, currentMeters() - 25);
            StravaSettings.putInt(StravaSettings.KEY_TRIM_METERS, next);
            value.setText(metersLabel());
        });
        Button plus = new Button(this);
        plus.setText("+");
        plus.setOnClickListener(view -> {
            int next = Math.min(1000, currentMeters() + 25);
            StravaSettings.putInt(StravaSettings.KEY_TRIM_METERS, next);
            value.setText(metersLabel());
        });

        row.addView(minus);
        row.addView(value);
        row.addView(plus);
        rootLayout.addView(row);
    }

    private int currentMeters() {
        return StravaSettings.getInt(
                StravaSettings.KEY_TRIM_METERS, RoutePrivacyTrimPatch.patchDefaultMeters);
    }

    private String metersLabel() {
        return currentMeters() + " m";
    }

    private void addNamePicker() {
        TextView label = text("App name", 16, TEXT_PRIMARY);
        rootLayout.addView(label);

        String[] current = LauncherSwitcher.currentSelection(this);
        String customName = stringResource("app_name_custom", "Strava Custom");
        String[][] options = {
                {LauncherSwitcher.NAME_ORIGINAL, stringResource("app_name", "Strava")},
                {LauncherSwitcher.NAME_CUSTOM, customName},
        };
        final RadioButton[] radios = new RadioButton[options.length];
        for (int i = 0; i < options.length; i++) {
            final String nameChoice = options[i][0];
            RadioButton radio = radio(options[i][1],
                    nameChoice.equals(current[0]), view -> {
                        for (RadioButton other : radios) {
                            other.setChecked(other == view);
                        }
                        String[] selection = LauncherSwitcher.currentSelection(this);
                        LauncherSwitcher.apply(this, nameChoice, selection[1]);
                    });
            radios[i] = radio;
            rootLayout.addView(radio);
        }
    }

    private void addIconPicker() {
        TextView label = text("App icon", 16, TEXT_PRIMARY);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(12);
        rootLayout.addView(label, labelParams);

        String[] current = LauncherSwitcher.currentSelection(this);
        final RadioButton[] radios = new RadioButton[LauncherSwitcher.ICON_KEYS.length];
        for (int i = 0; i < LauncherSwitcher.ICON_KEYS.length; i++) {
            final String iconKey = LauncherSwitcher.ICON_KEYS[i];
            RadioButton radio = radio(iconLabel(iconKey), iconKey.equals(current[1]), view -> {
                for (RadioButton other : radios) {
                    other.setChecked(other == view);
                }
                String[] selection = LauncherSwitcher.currentSelection(this);
                LauncherSwitcher.apply(this, selection[0], iconKey);
            });
            radios[i] = radio;
            rootLayout.addView(radio);
        }
    }

    private static String iconLabel(String iconKey) {
        switch (iconKey) {
            case "Original":
                return "Original";
            case "Accent":
                return "Accent color";
            case "LocalLegend":
                return "Local Legend";
            case "Xom":
                return "XOM";
            default:
                return iconKey;
        }
    }

    private RadioButton radio(String label, boolean checked, View.OnClickListener listener) {
        RadioButton radio = new RadioButton(this);
        radio.setText(label);
        radio.setTextSize(16);
        radio.setTextColor(TEXT_PRIMARY);
        radio.setChecked(checked);
        radio.setPadding(dp(4), dp(6), 0, dp(6));
        radio.setOnClickListener(listener);
        return radio;
    }

    private String stringResource(String name, String fallback) {
        try {
            int id = getResources().getIdentifier(name, "string", getPackageName());
            if (id != 0) {
                return getString(id);
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    private TextView text(String value, int sp, int color) {
        TextView textView = new TextView(this);
        textView.setText(value);
        textView.setTextSize(sp);
        textView.setTextColor(color);
        return textView;
    }

    private int dp(int value) {
        return dp(this, value);
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }
}
