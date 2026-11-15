package app.template.extension.strava;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

/**
 * Liquid Glass bottom navigation for Strava — runtime half.
 *
 * Called from the patched BottomNavigationActivity.onCreate (see
 * LiquidGlassNavPatch). The patch forces Strava's classic
 * BottomNavigationView (the Compose bar paints its own opaque
 * background and cannot be glassed from outside); this class then
 * restyles that bar as a floating glass pill like the current
 * YouTube / YouTube Music / Proton VPN bars:
 *
 *   - FLOATING: detached from the screen edges with side margins,
 *     lifted above the gesture bar, page content re-constrained to
 *     the bottom of the screen so it scrolls underneath the pill.
 *   - GLASS: the pill background is a live backdrop sample — the
 *     content behind the bar is captured, downscaled and blurred,
 *     then drawn inside the pill under a translucent tint, a top
 *     light highlight and a hairline border. The sample refreshes
 *     when the content scrolls or the layout changes, so the blur
 *     tracks what is actually behind the bar.
 *
 * All tweaks live in Strava's own preferences file, edited from the
 * Liquid Glass settings dialog (long-press the Home tab, see
 * GlassSettings):
 *   morphe.glass.enabled   master switch            (default true)
 *   morphe.glass.floating  floating pill vs docked  (default true)
 *   morphe.glass.blur      backdrop blur vs tint    (default true)
 *   morphe.glass.opacity   tint opacity, percent    (default 72)
 * Changes apply on the next app start.
 *
 * Everything is failure-swallowing: worst case the bar keeps a
 * plain translucent look, and the activity never breaks.
 */
public final class GlassNav {

    private static final String TAG_GLASS = "morphe_glass_nav";

    public static final String KEY_ENABLED = "morphe.glass.enabled";
    public static final String KEY_FLOATING = "morphe.glass.floating";
    public static final String KEY_BLUR = "morphe.glass.blur";
    public static final String KEY_OPACITY = "morphe.glass.opacity";

    private GlassNav() {
    }

    public static void apply(final Activity activity) {
        try {
            SharedPreferences prefs = activity.getSharedPreferences("com.strava_preferences", Context.MODE_PRIVATE);
            boolean enabled = prefs.getBoolean(KEY_ENABLED, true);
            String pkg = activity.getPackageName();
            int navId = activity.getResources().getIdentifier("bottom_navigation", "id", pkg);
            if (navId == 0) {
                return;
            }
            final View nav = activity.findViewById(navId);
            if (nav == null) {
                return;
            }
            if (!enabled) {
                restoreStock(activity, nav);
                return;
            }
            boolean floating = prefs.getBoolean(KEY_FLOATING, true);
            boolean blur = prefs.getBoolean(KEY_BLUR, true);
            int opacity = prefs.getInt(KEY_OPACITY, 72);
            stylePill(activity, nav, floating, blur, opacity);
            if (floating) {
                scrollContentUnder(activity);
            } else {
                dockContent(activity);
            }
        } catch (Throwable ignored) {
            // Never let a styling failure break the activity.
        }
    }

    // ------------------------------------------------------------------
    // Pill styling
    // ------------------------------------------------------------------

    private static void stylePill(final Activity activity, final View pill,
                                  boolean floating, boolean blur, int opacityPct) {
        float density = activity.getResources().getDisplayMetrics().density;
        final int radius = floating ? Math.round(30 * density) : 0;

        ViewGroup.LayoutParams raw = pill.getLayoutParams();
        if (raw instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
            if (floating) {
                lp.leftMargin = Math.round(18 * density);
                lp.rightMargin = Math.round(18 * density);
                lp.bottomMargin = Math.round(10 * density) + navigationBarHeight(activity);
            } else {
                lp.leftMargin = 0;
                lp.rightMargin = 0;
                lp.bottomMargin = 0;
            }
            lp.topMargin = 0;
            pill.setLayoutParams(lp);
        }

        int surface = surfaceColor(activity);
        int opacity = Math.max(10, Math.min(95, opacityPct));
        View root = (View) pill.getParent();
        GlassDrawable glass = new GlassDrawable(
                activity, pill, root, surface, opacity, radius, blur);
        pill.setBackground(glass);
        glass.refreshSoon();

        pill.setElevation(floating ? 10 * density : 4 * density);
        pill.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                if (radius > 0) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                } else {
                    outline.setRect(0, 0, view.getWidth(), view.getHeight());
                }
            }
        });
        pill.setClipToOutline(true);
        pill.setTag(TAG_GLASS);

        // Keep the backdrop sample fresh: recapture shortly after
        // layout settles, and (throttled) while anything scrolls.
        pill.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        glass.refreshSoon();
                    }
                });
        pill.getViewTreeObserver().addOnScrollChangedListener(
                new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        glass.refreshSoon();
                    }
                });
        pill.postDelayed(new Runnable() {
            @Override
            public void run() {
                glass.refresh();
            }
        }, 600);
    }

    private static void restoreStock(Activity activity, View nav) {
        try {
            ViewGroup.LayoutParams raw = nav.getLayoutParams();
            if (raw instanceof ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) raw;
                lp.leftMargin = 0;
                lp.rightMargin = 0;
                lp.bottomMargin = 0;
                nav.setLayoutParams(lp);
            }
            nav.setBackground(null);
            nav.setElevation(0);
            nav.setClipToOutline(false);
            dockContent(activity);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------
    // Content constraints (ConstraintLayout fields set reflectively —
    // the extension compiles against the stock Android SDK only)
    // ------------------------------------------------------------------

    /** Let page content scroll underneath the floating pill. */
    private static void scrollContentUnder(Activity activity) {
        setContentConstraint(activity, true);
    }

    /** Put page content back above the bar (docked / stock layout). */
    private static void dockContent(Activity activity) {
        setContentConstraint(activity, false);
    }

    private static void setContentConstraint(Activity activity, boolean under) {
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
            if (under) {
                // bottomToBottom = 0 (PARENT_ID), bottomToTop = -1 (UNSET)
                setIntField(raw, "bottomToBottom", 0);
                setIntField(raw, "bottomToTop", -1);
            } else {
                int barrierId = activity.getResources()
                        .getIdentifier("bottom_navigation_barrier", "id", pkg);
                if (barrierId != 0) {
                    setIntField(raw, "bottomToBottom", -1);
                    setIntField(raw, "bottomToTop", barrierId);
                }
            }
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

    // ------------------------------------------------------------------
    // Colors / insets
    // ------------------------------------------------------------------

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
                return Math.min(height, Math.round(48 * density));
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Glass background drawable: live blurred backdrop + tint + light
    // ------------------------------------------------------------------

    static final class GlassDrawable extends Drawable {
        private static final float DOWNSCALE = 0.18f;
        private static final long REFRESH_DELAY_MS = 120;

        private final WeakReference<View> pillRef;
        private final WeakReference<View> rootRef;
        private final int surface;
        private final int opacityPct;
        private final int radiusPx;
        private final boolean blurEnabled;
        private final Handler handler = new Handler(Looper.getMainLooper());

        private Bitmap blurred;
        private boolean refreshPosted;
        private final Runnable refreshRunnable = new Runnable() {
            @Override
            public void run() {
                refreshPosted = false;
                refresh();
            }
        };

        GlassDrawable(Activity activity, View pill, View root, int surface,
                      int opacityPct, int radiusPx, boolean blurEnabled) {
            this.pillRef = new WeakReference<>(pill);
            this.rootRef = new WeakReference<>(root);
            this.surface = surface;
            this.opacityPct = opacityPct;
            this.radiusPx = radiusPx;
            this.blurEnabled = blurEnabled;
        }

        void refreshSoon() {
            if (!blurEnabled || refreshPosted) {
                return;
            }
            refreshPosted = true;
            handler.postDelayed(refreshRunnable, REFRESH_DELAY_MS);
        }

        /** Capture the content behind the pill, blur it, redraw. */
        void refresh() {
            if (!blurEnabled) {
                return;
            }
            try {
                View pill = pillRef.get();
                View root = rootRef.get();
                if (pill == null || root == null
                        || root.getWidth() == 0 || root.getHeight() == 0) {
                    return;
                }
                int w = Math.max(1, Math.round(root.getWidth() * DOWNSCALE));
                int h = Math.max(1, Math.round(root.getHeight() * DOWNSCALE));
                Bitmap shot = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(shot);
                canvas.scale(DOWNSCALE, DOWNSCALE);
                int vis = pill.getVisibility();
                pill.setVisibility(View.INVISIBLE);
                try {
                    root.draw(canvas);
                } finally {
                    pill.setVisibility(vis);
                }
                boxBlur(shot, 10);
                boxBlur(shot, 10);
                Bitmap old = blurred;
                blurred = shot;
                if (old != null && old != shot) {
                    old.recycle();
                }
                invalidateSelf();
            } catch (Throwable ignored) {
                // Keep the previous sample (or plain tint) — never crash.
            }
        }

        @Override
        public void draw(Canvas canvas) {
            Rect bounds = getBounds();
            if (bounds.isEmpty()) {
                return;
            }
            View pill = pillRef.get();
            View root = rootRef.get();

            Path clip = new Path();
            RectF rect = new RectF(bounds);
            if (radiusPx > 0) {
                clip.addRoundRect(rect, radiusPx, radiusPx, Path.Direction.CW);
            } else {
                clip.addRect(rect, Path.Direction.CW);
            }
            canvas.save();
            canvas.clipPath(clip);

            // 1. Blurred backdrop, aligned to the pill's real position.
            Bitmap sample = blurred;
            if (sample != null && pill != null && root != null) {
                int[] pillLoc = new int[2];
                int[] rootLoc = new int[2];
                pill.getLocationOnScreen(pillLoc);
                root.getLocationOnScreen(rootLoc);
                float scale = 1f / DOWNSCALE;
                canvas.save();
                canvas.translate(bounds.left, bounds.top);
                canvas.scale(scale, scale);
                canvas.translate(-(pillLoc[0] - rootLoc[0]), -(pillLoc[1] - rootLoc[1]));
                Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
                canvas.drawBitmap(sample, 0, 0, paint);
                canvas.restore();
            } else {
                // No sample yet: solid surface as the base.
                Paint base = new Paint();
                base.setColor(surface);
                canvas.drawRect(rect, base);
            }

            // 2. Translucent tint (the glass body).
            int alpha = Math.round(255f * opacityPct / 100f);
            Paint tint = new Paint();
            tint.setColor(Color.argb(alpha, Color.red(surface),
                    Color.green(surface), Color.blue(surface)));
            canvas.drawRect(rect, tint);

            // 3. Top light highlight (light catching the top edge).
            Paint highlight = new Paint();
            highlight.setShader(new LinearGradient(
                    0, bounds.top, 0, bounds.top + bounds.height() * 0.5f,
                    Color.argb(46, 255, 255, 255), Color.argb(0, 255, 255, 255),
                    Shader.TileMode.CLAMP));
            canvas.drawRect(rect, highlight);

            canvas.restore();

            // 4. Hairline border.
            double luminance = (0.299 * Color.red(surface) + 0.587 * Color.green(surface)
                    + 0.114 * Color.blue(surface)) / 255.0;
            Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeWidth(Math.max(1f, bounds.height() * 0.012f));
            stroke.setColor(luminance > 0.5
                    ? Color.argb(31, 0, 0, 0)
                    : Color.argb(56, 255, 255, 255));
            if (radiusPx > 0) {
                canvas.drawRoundRect(rect, radiusPx, radiusPx, stroke);
            } else {
                canvas.drawRect(rect, stroke);
            }
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }

        /** Simple separable box blur, in place (downscaled bitmap). */
        private static void boxBlur(Bitmap bitmap, int radius) {
            int w = bitmap.getWidth();
            int h = bitmap.getHeight();
            if (w <= 2 || h <= 2 || radius < 1) {
                return;
            }
            int[] pixels = new int[w * h];
            bitmap.getPixels(pixels, 0, w, 0, 0, w, h);
            int[] out = new int[w * h];
            int diameter = radius * 2 + 1;

            // Horizontal pass.
            for (int y = 0; y < h; y++) {
                long sr = 0, sg = 0, sb = 0, sa = 0;
                for (int x = -radius; x <= radius; x++) {
                    int px = pixels[y * w + clamp(x, 0, w - 1)];
                    sa += Color.alpha(px);
                    sr += Color.red(px);
                    sg += Color.green(px);
                    sb += Color.blue(px);
                }
                for (int x = 0; x < w; x++) {
                    out[y * w + x] = Color.argb(
                            (int) (sa / diameter), (int) (sr / diameter),
                            (int) (sg / diameter), (int) (sb / diameter));
                    int leave = pixels[y * w + clamp(x - radius, 0, w - 1)];
                    int enter = pixels[y * w + clamp(x + radius + 1, 0, w - 1)];
                    sa += Color.alpha(enter) - Color.alpha(leave);
                    sr += Color.red(enter) - Color.red(leave);
                    sg += Color.green(enter) - Color.green(leave);
                    sb += Color.blue(enter) - Color.blue(leave);
                }
            }
            // Vertical pass.
            for (int x = 0; x < w; x++) {
                long sr = 0, sg = 0, sb = 0, sa = 0;
                for (int y = -radius; y <= radius; y++) {
                    int px = out[clamp(y, 0, h - 1) * w + x];
                    sa += Color.alpha(px);
                    sr += Color.red(px);
                    sg += Color.green(px);
                    sb += Color.blue(px);
                }
                for (int y = 0; y < h; y++) {
                    pixels[y * w + x] = Color.argb(
                            (int) (sa / diameter), (int) (sr / diameter),
                            (int) (sg / diameter), (int) (sb / diameter));
                    int leave = out[clamp(y - radius, 0, h - 1) * w + x];
                    int enter = out[clamp(y + radius + 1, 0, h - 1) * w + x];
                    sa += Color.alpha(enter) - Color.alpha(leave);
                    sr += Color.red(enter) - Color.red(leave);
                    sg += Color.green(enter) - Color.green(leave);
                    sb += Color.blue(enter) - Color.blue(leave);
                }
            }
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        }

        private static int clamp(int v, int min, int max) {
            return v < min ? min : (v > max ? max : v);
        }
    }
}
