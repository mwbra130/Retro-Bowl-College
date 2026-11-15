package app.template.extension.strava;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import java.lang.reflect.Field;

/**
 * Liquid Glass styling for Strava's bottom navigation bar.
 *
 * Called from the patched BottomNavigationActivity.onCreate (see
 * LiquidGlassNavPatch), after the original onCreate has run, so the bar
 * exists and the classic BottomNavigationView is the visible variant
 * (the patch forces the classic path). Turns the full-width bar into a
 * floating pill in the style of the current YouTube / YouTube Music /
 * Proton VPN bars:
 *
 *   - detached from the screen edges (side + bottom margins, lifted
 *     above the system navigation / gesture bar)
 *   - fully rounded corners, clipped item ripples
 *   - translucent surface taken from Strava's own navbar_background
 *     color, with a hairline border and a soft shadow (elevation)
 *   - page content re-constrained to the bottom of the screen so it
 *     scrolls underneath the pill instead of stopping above it
 *
 * The extension compiles against the stock Android SDK only, so the
 * ConstraintLayout constraint fields of the content container are set
 * reflectively (the class is present at runtime inside Strava).
 * Everything is wrapped so a styling failure can never break the
 * activity: worst case the bar keeps its stock look.
 */
public final class GlassNav {

    private static final String TAG_GLASS = "morphe_glass_nav";

    private GlassNav() {
    }

    /** Entry point called from the patched BottomNavigationActivity.onCreate. */
    public static void apply(final Activity activity) {
        try {
            String pkg = activity.getPackageName();
            int navId = activity.getResources().getIdentifier("bottom_navigation", "id", pkg);
            int composeId = activity.getResources()
                    .getIdentifier("bottom_navigation_compose_view", "id", pkg);
            if (navId == 0) {
                return;
            }
            final View nav = activity.findViewById(navId);
            View compose = composeId != 0 ? activity.findViewById(composeId) : null;

            // The patch forces the classic bar, but style whichever
            // variant is actually visible so a flag change can never
            // leave a stock full-width bar behind.
            View target = null;
            if (nav != null && nav.getVisibility() == View.VISIBLE) {
                target = nav;
            } else if (compose != null && compose.getVisibility() == View.VISIBLE) {
                target = compose;
            } else if (nav != null) {
                target = nav;
            }
            if (target == null) {
                return;
            }

            final View pill = target;
            stylePill(activity, pill);
            // Re-run after the first layout pass: margins and the
            // content re-constraint only take full effect once the
            // hierarchy has been measured with the stock constraints.
            pill.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        stylePill(activity, pill);
                        scrollContentUnder(activity);
                    } catch (Throwable ignored) {
                    }
                }
            });
            scrollContentUnder(activity);
        } catch (Throwable ignored) {
            // Never let a styling failure break the activity.
        }
    }

    private static void stylePill(Activity activity, View pill) {
        if (TAG_GLASS.equals(pill.getTag())) {
            return;
        }
        pill.setTag(TAG_GLASS);

        float density = activity.getResources().getDisplayMetrics().density;
        int sideMargin = Math.round(18 * density);
        int bottomMargin = Math.round(10 * density) + navigationBarHeight(activity);
        int radius = Math.round(30 * density);

        ViewGroup.LayoutParams raw = pill.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
            lp.leftMargin = sideMargin;
            lp.rightMargin = sideMargin;
            lp.bottomMargin = bottomMargin;
            lp.topMargin = 0;
            pill.setLayoutParams(lp);
        }

        int surface = surfaceColor(activity);
        int fill = Color.argb(184, Color.red(surface), Color.green(surface), Color.blue(surface));
        double luminance = (0.299 * Color.red(surface) + 0.587 * Color.green(surface)
                + 0.114 * Color.blue(surface)) / 255.0;
        int stroke = luminance > 0.5
                ? Color.argb(31, 0, 0, 0)
                : Color.argb(51, 255, 255, 255);

        GradientDrawable glass = new GradientDrawable();
        glass.setShape(GradientDrawable.RECTANGLE);
        glass.setCornerRadius(radius);
        glass.setColor(fill);
        glass.setStroke(Math.max(1, Math.round(1 * density)), stroke);
        pill.setBackground(glass);

        pill.setElevation(10 * density);
        final int outlineRadius = radius;
        pill.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), outlineRadius);
            }
        });
        pill.setClipToOutline(true);
    }

    /**
     * The content container (toolbar_container) is constrained to the
     * top of the bottom-navigation barrier, so content stops exactly
     * where the bar begins. Re-constrain its bottom to the parent so
     * feeds and maps scroll underneath the floating pill.
     */
    private static void scrollContentUnder(Activity activity) {
        try {
            String pkg = activity.getPackageName();
            int containerId = activity.getResources()
                    .getIdentifier("toolbar_container", "id", pkg);
            if (containerId == 0) {
                return;
            }
            View container = activity.findViewById(containerId);
            if (container == null) {
                return;
            }
            ViewGroup.LayoutParams raw = container.getLayoutParams();
            if (raw == null) {
                return;
            }
            // ConstraintLayout.LayoutParams fields, set reflectively:
            // bottomToBottom = 0 (PARENT_ID), bottomToTop = -1 (UNSET).
            setIntField(raw, "bottomToBottom", 0);
            setIntField(raw, "bottomToTop", -1);
            if (raw instanceof ViewGroup.MarginLayoutParams) {
                ((ViewGroup.MarginLayoutParams) raw).bottomMargin = 0;
            }
            container.setLayoutParams(raw);
        } catch (Throwable ignored) {
        }
    }

    private static void setIntField(Object target, String name, int value) {
        try {
            Field field = target.getClass().getField(name);
            field.setInt(target, value);
        } catch (Throwable ignored) {
        }
    }

    /** Strava's own navbar surface color, so the pill matches light/dark mode. */
    private static int surfaceColor(Activity activity) {
        try {
            String pkg = activity.getPackageName();
            int colorId = activity.getResources()
                    .getIdentifier("navbar_background", "color", pkg);
            if (colorId != 0) {
                return activity.getResources().getColor(colorId, activity.getTheme());
            }
        } catch (Throwable ignored) {
        }
        boolean night = (activity.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        return night ? Color.rgb(20, 20, 19) : Color.WHITE;
    }

    private static int navigationBarHeight(Activity activity) {
        try {
            int dimenId = activity.getResources()
                    .getIdentifier("navigation_bar_height", "dimen", "android");
            if (dimenId != 0) {
                int height = activity.getResources().getDimensionPixelSize(dimenId);
                float density = activity.getResources().getDisplayMetrics().density;
                // Cap it: on gesture devices the reported height can
                // include more than the bar the pill must clear.
                return Math.min(height, Math.round(48 * density));
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }
}
