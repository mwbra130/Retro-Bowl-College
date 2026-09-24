package app.template.extension.geometrydashlite;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Marks every official Geometry Dash Lite level 100% complete with all secret
 * coins by rewriting the game's own save files before the game loads them.
 *
 * Save pipeline (both files): file bytes -> XOR 0x0B -> URL-safe Base64 ->
 * gzip -> custom XML plist. There is no checksum, so the files can be freely
 * rewritten.
 *
 * Two files are patched, because the game keeps the two halves of progress
 * in different places:
 *  - CCGameManager.dat: stats and completion flags (GS_value, GS_completed)
 *    and a copy of the level objects (GLM_01).
 *  - CCLocalLevels.dat: the authoritative in-memory level objects (GLM_01).
 *
 * What is written (all additive and idempotent):
 *  - GS_completed: n_&lt;id&gt;, c_&lt;id&gt;, star_&lt;id&gt; for levels 1-22,
 *    plus demon_&lt;id&gt; for the three demons (14, 18, 20).
 *  - GS_value: unique_&lt;id&gt;_&lt;coin&gt; per collected coin, and the
 *    aggregate counters "3" (completed official levels = 22), "5" (completed
 *    demons = 3), "6" (total stars), "8" (secret coins collected = 61).
 *  - GLM_01 (in both files): well-formed GJGameLevel records with kCEK=4,
 *    k1=level id, k2=official name, k19=100 (normal %), k20=100 (practice %),
 *    k21=1 (official level type). Existing game-written records are merged,
 *    never wiped.
 *
 * Runs until it succeeds once (SharedPreferences marker).
 *
 * Diagnostics: shows a short Toast describing the outcome of every run until
 * it succeeds, and dumps the decoded saves before/after the edit plus a log
 * file into the app's external files dir
 * (/Android/data/com.robtopx.geometryjumplite/files/), which is readable
 * over USB file transfer, so a failed run can be diagnosed from real data.
 */
public final class SaveCompleter {

    private static final String PREFS = "gdl_complete_all";
    private static final String DONE_KEY = "done_v5";
    private static final String SAVE_GM = "CCGameManager.dat";
    private static final String SAVE_LL = "CCLocalLevels.dat";

    /** Official demon levels: Clubstep (14), Theory of Everything 2 (18), Deadlocked (20). */
    private static final int[] DEMONS = {14, 18, 20};

    /**
     * Official level id -> which of the 3 secret coins exist. Most levels have
     * all three; the exceptions are documented per level.
     */
    private static final int[][] COINS = new int[23][];

    /** Official level id -> star rating (2.2 values; used for the star aggregate). */
    private static final int[] STARS = new int[23];

    /** Official level id -> official level name (used for k2 in new records). */
    private static final String[] NAMES = new String[23];

    static {
        for (int id = 1; id <= 22; id++) {
            COINS[id] = new int[]{1, 2, 3};
        }
        COINS[11] = new int[]{1, 3}; // Clutterfunk
        COINS[14] = new int[]{1, 2}; // Clubstep
        COINS[15] = new int[]{2};    // Electrodynamix
        COINS[18] = new int[]{1, 3}; // Theory of Everything 2

        STARS[1] = 1;   // Stereo Madness
        STARS[2] = 2;   // Back on Track
        STARS[3] = 3;   // Polargeist
        STARS[4] = 4;   // Dry Out
        STARS[5] = 5;   // Base After Base
        STARS[6] = 6;   // Can't Let Go
        STARS[7] = 7;   // Jumper
        STARS[8] = 8;   // Time Machine
        STARS[9] = 9;   // Cycles
        STARS[10] = 10; // xStep
        STARS[11] = 11; // Clutterfunk
        STARS[12] = 12; // Theory of Everything
        STARS[13] = 13; // Electroman Adventures
        STARS[14] = 14; // Clubstep
        STARS[15] = 15; // Electrodynamix
        STARS[16] = 12; // Hexagon Force
        STARS[17] = 10; // Blast Processing
        STARS[18] = 14; // Theory of Everything 2
        STARS[19] = 10; // Geometrical Dominator
        STARS[20] = 15; // Deadlocked
        STARS[21] = 12; // Fingerdash
        STARS[22] = 12; // Dash

        NAMES[1] = "Stereo Madness";
        NAMES[2] = "Back on Track";
        NAMES[3] = "Polargeist";
        NAMES[4] = "Dry Out";
        NAMES[5] = "Base After Base";
        NAMES[6] = "Can't Let Go";
        NAMES[7] = "Jumper";
        NAMES[8] = "Time Machine";
        NAMES[9] = "Cycles";
        NAMES[10] = "xStep";
        NAMES[11] = "Clutterfunk";
        NAMES[12] = "Theory of Everything";
        NAMES[13] = "Electroman Adventures";
        NAMES[14] = "Clubstep";
        NAMES[15] = "Electrodynamix";
        NAMES[16] = "Hexagon Force";
        NAMES[17] = "Blast Processing";
        NAMES[18] = "Theory of Everything 2";
        NAMES[19] = "Geometrical Dominator";
        NAMES[20] = "Deadlocked";
        NAMES[21] = "Fingerdash";
        NAMES[22] = "Dash";
    }

    private SaveCompleter() {
    }

    public static void run(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (prefs.getBoolean(DONE_KEY, false)) return;

            File dataDir = getDataDirSafe(context);
            File dbgDir = context.getExternalFilesDir(null);

            File gmSave = findSave(dataDir, context, SAVE_GM);
            if (gmSave == null) {
                String report = probeSaveLocations(context, dbgDir);
                writeLog(dbgDir, "run: save file not present yet (fresh install).");
                toast(context, "GD patch: no save yet \u2014 screenshot the popup");
                // Delayed: our hook runs before the Activity finishes onCreate,
                // so wait a few seconds until it can safely show a dialog.
                showReportDialog(context, report, 4000);
                return;
            }

            Dict gmRoot;
            try {
                gmRoot = decodeSave(readAll(gmSave));
            } catch (Exception e) {
                writeLog(dbgDir, "run: FAILED to decode " + SAVE_GM + ": " + e);
                toast(context, "GD patch: could not read save, will retry");
                return;
            }

            writeDebugXml(dbgDir, "gdl_patch_before_gm.xml", gmRoot);
            String summary = processGameManager(gmRoot);
            writeDebugXml(dbgDir, "gdl_patch_after_gm.xml", gmRoot);
            rewriteSave(dataDir, gmSave, gmRoot);
            writeLog(dbgDir, "run: " + SAVE_GM + " OK. " + summary);

            // Level objects: patch CCLocalLevels.dat too, since GLM_01 is the
            // authoritative level store there. If the file is absent there is
            // nothing more we can do for it, so it does not block completion.
            File llSave = findSave(dataDir, context, SAVE_LL);
            boolean llOk = true;
            String llNote = "";
            if (llSave != null) {
                try {
                    Dict llRoot = decodeSave(readAll(llSave));
                    writeDebugXml(dbgDir, "gdl_patch_before_ll.xml", llRoot);
                    processLocalLevels(llRoot);
                    writeDebugXml(dbgDir, "gdl_patch_after_ll.xml", llRoot);
                    rewriteSave(dataDir, llSave, llRoot);
                    writeLog(dbgDir, "run: " + SAVE_LL + " OK.");
                } catch (Exception e) {
                    llOk = false;
                    writeLog(dbgDir, "run: FAILED to process " + SAVE_LL + ": " + e);
                }
            } else {
                llNote = " (no " + SAVE_LL + " found)";
                writeLog(dbgDir, "run: " + SAVE_LL + " not present; skipped.");
            }

            writeLog(dbgDir, "run: finished. " + summary + llNote);
            if (llOk) {
                prefs.edit().putBoolean(DONE_KEY, true).apply();
                toast(context, "GD patch: " + summary + " \u2014 reopen the game" + llNote);
            } else {
                toast(context, "GD patch: level file unreadable, will retry");
            }
        } catch (Throwable ignored) {
            // Never crash the game: a failed edit just means no completions.
        }
    }

    /** Backs up the original, self-checks our encoding, then overwrites. */
    private static void rewriteSave(File dataDir, File save, Dict root) throws Exception {
        try {
            writeAll(new File(dataDir, save.getName() + ".patchbak"), readAll(save));
        } catch (Throwable ignored) {
        }
        byte[] encoded = encodeSave(root);
        decodeSave(encoded); // throws if our output is unreadable; aborts before write
        writeAll(save, encoded);
    }

    private static File findSave(File dataDir, Context context, String name) {
        File f = new File(dataDir, name);
        if (f.exists()) return f;
        // Fallback: some builds keep saves under files/.
        f = new File(context.getFilesDir(), name);
        return f.exists() ? f : null;
    }

    // ------------------------------------------------------------------ //
    // Completion logic                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Stats and completion flags in CCGameManager.dat. Returns a short
     * human-readable summary for the Toast/log.
     */
    private static String processGameManager(Dict root) {
        Dict gsValue = dict(root, "GS_value");
        Dict gsCompleted = dict(root, "GS_completed");
        Dict glm01 = dict(root, "GLM_01");

        int newlyCompleted = 0;
        int newCoins = 0;

        for (int id = 1; id <= 22; id++) {
            if (!(gsCompleted.map.get("n_" + id) instanceof TrueNode)) {
                newlyCompleted++;
            }
            gsCompleted.map.put("n_" + id, TrueNode.INSTANCE);
            gsCompleted.map.put("c_" + id, TrueNode.INSTANCE);
            gsCompleted.map.put("star_" + id, TrueNode.INSTANCE);
            if (isDemon(id)) {
                gsCompleted.map.put("demon_" + id, TrueNode.INSTANCE);
            }

            // Also keep this file's GLM_01 copy well-formed, in case the
            // game reads level objects from here instead of CCLocalLevels.dat.
            mergeLevelRecord(glm01, id);

            for (int coin : COINS[id]) {
                String uniqueKey = "unique_" + id + "_" + coin;
                if (!gsValue.map.containsKey(uniqueKey)) {
                    newCoins++;
                }
                gsValue.map.put(uniqueKey, new IntNum(1));
            }
        }

        // Aggregate counters (integers): "3" = completed official levels,
        // "5" = completed demons, "6" = total stars, "8" = secret coins.
        gsValue.map.put("3", new IntNum(22));
        gsValue.map.put("5", new IntNum(DEMONS.length));
        gsValue.map.put("6", new IntNum(totalStars()));
        gsValue.map.put("8", new IntNum(totalCoins()));

        if (newlyCompleted == 0 && newCoins == 0) {
            return "levels already complete";
        }
        return "marked " + newlyCompleted + " levels + " + newCoins + " coins complete";
    }

    /** Level objects in CCLocalLevels.dat. */
    private static void processLocalLevels(Dict root) {
        Dict glm01 = dict(root, "GLM_01");
        for (int id = 1; id <= 22; id++) {
            mergeLevelRecord(glm01, id);
        }
    }

    /**
     * Ensures GLM_01 holds a well-formed GJGameLevel record for the level:
     * kCEK=4 (serialized GJGameLevel), k1=level id, k2=official name,
     * k19=100 (normal %), k20=100 (practice %), k21=1 (official type).
     * Existing game-written records are merged in place: their own values
     * are kept and only missing identity keys are filled, while the
     * percentages are always set to 100.
     */
    private static void mergeLevelRecord(Dict glm01, int id) {
        String key = Integer.toString(id);
        Node existing = glm01.map.get(key);
        Dict entry;
        if (existing instanceof Dict) {
            entry = (Dict) existing;
        } else {
            entry = new Dict();
            glm01.map.put(key, entry);
        }
        if (!(entry.map.get("kCEK") instanceof IntNum)) {
            entry.map.put("kCEK", new IntNum(4));
        }
        if (!(entry.map.get("k1") instanceof IntNum)) {
            entry.map.put("k1", new IntNum(id));
        }
        if (!(entry.map.get("k2") instanceof Str)) {
            entry.map.put("k2", new Str(NAMES[id]));
        }
        entry.map.put("k19", new IntNum(100));
        entry.map.put("k20", new IntNum(100));
        if (!(entry.map.get("k21") instanceof IntNum)) {
            entry.map.put("k21", new IntNum(1));
        }
    }

    private static int totalStars() {
        int total = 0;
        for (int id = 1; id <= 22; id++) total += STARS[id];
        return total;
    }

    private static int totalCoins() {
        int total = 0;
        for (int id = 1; id <= 22; id++) total += COINS[id].length;
        return total;
    }

    private static boolean isDemon(int id) {
        for (int demon : DEMONS) {
            if (demon == id) return true;
        }
        return false;
    }

    private static Dict dict(Dict parent, String key) {
        Node existing = parent.map.get(key);
        if (existing instanceof Dict) return (Dict) existing;
        Dict created = new Dict();
        parent.map.put(key, created);
        return created;
    }

    /** getDataDir() needs API 24+; fall back to applicationInfo on older. */
    private static File getDataDirSafe(Context context) {
        try {
            return context.getDataDir();
        } catch (Throwable t) {
            return new File(context.getApplicationInfo().dataDir);
        }
    }

    // ------------------------------------------------------------------ //
    // Save-location probe: when CCGameManager.dat is missing, scan the    //
    // WHOLE app data dir (shared_prefs, databases, files, cache, ...)    //
    // and dump shared_prefs contents, so the real progress store shows.   //
    // ------------------------------------------------------------------ //

    /**
     * Writes a probe file into getFilesDir() (proves we can write there and
     * that the path is right), then recursively scans the entire app data
     * directory — shared_prefs, databases, files, cache, no_backup and all —
     * listing the most recently modified files, and dumps the contents of
     * every shared_prefs XML file. The game must have written SOMETHING while
     * being played; whatever it is, and wherever it is, this finds it.
     * Returns the full human-readable report.
     */
    private static String probeSaveLocations(Context context, File dbgDir) {
        StringBuilder report = new StringBuilder();
        try {
            File dataDir;
            try {
                dataDir = context.getDataDir();
            } catch (Throwable t) {
                dataDir = new File(context.getApplicationInfo().dataDir);
            }
            final String base = dataDir.getAbsolutePath();
            report.append("dataDir: ").append(base).append('\n');

            File filesDir = context.getFilesDir();
            try {
                writeAll(new File(filesDir, "gdl_patch_probe.txt"),
                        "patch can write here".getBytes(StandardCharsets.UTF_8));
                report.append("probe write: OK\n");
            } catch (Throwable t) {
                report.append("probe write FAILED: ").append(t).append('\n');
            }

            java.util.ArrayList<File> all = new java.util.ArrayList<File>();
            collectFiles(dataDir, all, 0);
            collectFiles(dbgDir, all, 0);
            // Most recently modified first.
            java.util.Collections.sort(all, new java.util.Comparator<File>() {
                @Override
                public int compare(File a, File b) {
                    return Long.compare(b.lastModified(), a.lastModified());
                }
            });
            long now = System.currentTimeMillis();
            report.append("\nRecently modified files (newest first, top 80):\n");
            int shown = 0;
            int saveHits = 0;
            StringBuilder interesting = new StringBuilder();
            for (File f : all) {
                String abs = f.getAbsolutePath();
                String rel = abs.startsWith(base)
                        ? abs.substring(base.length()) : abs;
                long ageMin = (now - f.lastModified()) / 60000;
                String low = f.getName().toLowerCase();
                boolean looksLikeSave = low.contains(".dat") || low.contains("save")
                        || low.contains("game") || low.contains("manager")
                        || low.contains("profile") || low.contains("local")
                        || low.contains("level") || low.contains("stat")
                        || low.contains("pref") || low.contains("progress")
                        || low.contains("user") || low.contains("shared")
                        || low.endsWith(".xml") || low.endsWith(".db")
                        || low.endsWith(".bin");
                if (looksLikeSave) {
                    saveHits++;
                    interesting.append("  [SAVE?] ").append(rel)
                            .append(" (").append(f.length()).append("b, ")
                            .append(ageMin).append(" min ago)\n");
                }
                if (shown >= 80) continue;
                if (isNoise(abs)) continue;
                report.append("  ").append(rel)
                        .append(" (").append(f.length()).append("b, ")
                        .append(ageMin).append(" min ago)\n");
                shown++;
            }
            report.append("\nFiles with save-like names: ").append(saveHits).append('\n');
            report.append(interesting);
            report.append("\nTotal files scanned: ").append(all.size()).append('\n');

            // Dump every shared_prefs XML: game progress often lives here.
            report.append("\n--- shared_prefs contents ---\n");
            File spDir = new File(dataDir, "shared_prefs");
            File[] spFiles = null;
            try {
                spFiles = spDir.listFiles();
            } catch (Throwable ignored) {
            }
            if (spFiles == null) {
                report.append("(no shared_prefs dir)\n");
            } else {
                for (File x : spFiles) {
                    if (!x.isFile() || !x.getName().endsWith(".xml")) continue;
                    report.append("\n== ").append(x.getName()).append(" ==\n");
                    try {
                        String text = new String(readAll(x), StandardCharsets.UTF_8);
                        if (text.length() > 2000) {
                            text = text.substring(0, 2000) + "\n...[truncated]";
                        }
                        report.append(text).append('\n');
                    } catch (Throwable t) {
                        report.append("(unreadable: ").append(t).append(")\n");
                    }
                }
            }

            // List databases/ in case progress is SQLite.
            report.append("\n--- databases ---\n");
            File dbDir = new File(dataDir, "databases");
            File[] dbFiles = null;
            try {
                dbFiles = dbDir.listFiles();
            } catch (Throwable ignored) {
            }
            if (dbFiles == null) {
                report.append("(no databases dir)\n");
            } else {
                for (File d : dbFiles) {
                    long ageMin = (now - d.lastModified()) / 60000;
                    report.append("  ").append(d.getName())
                            .append(" (").append(d.length()).append("b, ")
                            .append(ageMin).append(" min ago)\n");
                }
            }

            try {
                writeAll(new File(dbgDir == null ? filesDir : dbgDir,
                        "gdl_patch_filelist.txt"),
                        report.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ignored) {
            }
        } catch (Throwable t) {
            report.append("probe crashed: ").append(t).append('\n');
        }
        return report.toString();
    }

    /** Ad-SDK / build-artifact noise that never holds game progress. */
    private static boolean isNoise(String absPath) {
        return absPath.contains("gdl_patch_") || absPath.contains("unityAds")
                || absPath.contains("vungle") || absPath.contains("inmobi")
                || absPath.contains("inneractive") || absPath.contains("ia-")
                || absPath.contains("supersonic") || absPath.contains("webview")
                || absPath.contains("WebView") || absPath.contains("oat")
                || absPath.contains("appmetrica") || absPath.contains("volley")
                || absPath.contains("Crash Reports") || absPath.contains("safedk");
    }

    /** Recursively collects files up to depth 8, skipping unreadable dirs. */
    private static void collectFiles(File dir, java.util.ArrayList<File> out, int depth) {
        if (dir == null || depth > 8) return;
        File[] files;
        try {
            files = dir.listFiles();
        } catch (Throwable t) {
            return;
        }
        if (files == null) return;
        for (File f : files) {
            try {
                if (f.isDirectory()) {
                    collectFiles(f, out, depth + 1);
                } else {
                    out.add(f);
                }
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Shows the probe report in a scrollable popup so it can be screenshotted
     * and sent back for diagnosis. Delayed because this hook runs before the
     * Activity finishes onCreate.
     */
    private static void showReportDialog(final Context context, final String report,
                                         long delayMs) {
        try {
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        android.widget.ScrollView sv =
                                new android.widget.ScrollView(context);
                        android.widget.TextView tv =
                                new android.widget.TextView(context);
                        tv.setText(report);
                        tv.setTextIsSelectable(true);
                        int pad = (int) (16 * context.getResources()
                                .getDisplayMetrics().density);
                        tv.setPadding(pad, pad, pad, pad);
                        sv.addView(tv);
                        new android.app.AlertDialog.Builder(context)
                                .setTitle("GD patch: save not found")
                                .setView(sv)
                                .setPositiveButton("OK", null)
                                .show();
                    } catch (Throwable ignored) {
                    }
                }
            }, delayMs);
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ //
    // Diagnostics: Toast + debug files                                    //
    // ------------------------------------------------------------------ //

    private static void toast(final Context context, final String text) {
        try {
            final Context app = context.getApplicationContext();
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    try {
                        Toast.makeText(app, text, Toast.LENGTH_LONG).show();
                    } catch (Throwable ignored) {
                    }
                }
            });
        } catch (Throwable ignored) {
        }
    }

    private static void writeDebugXml(File dir, String name, Dict root) {
        if (dir == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            serialize(root, sb);
            writeAll(new File(dir, name),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    private static void writeLog(File dir, String line) {
        if (dir == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(line).append('\n');
            writeAll(new File(dir, "gdl_patch_log.txt"),
                    sb.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

    // ------------------------------------------------------------------ //
    // Save encode / decode                                                //
    // ------------------------------------------------------------------ //

    private static Dict decodeSave(byte[] bytes) throws Exception {
        byte[] xored = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            xored[i] = (byte) (bytes[i] ^ 0x0B);
        }
        // Normalize both Base64 alphabets before decoding.
        String b64 = new String(xored, StandardCharsets.US_ASCII).trim()
                .replace('-', '+')
                .replace('_', '/');
        byte[] gzipped = Base64.decode(b64, Base64.DEFAULT);
        byte[] xml = gunzip(gzipped);
        return parsePlist(new String(xml, StandardCharsets.UTF_8));
    }

    private static byte[] encodeSave(Dict root) throws Exception {
        StringBuilder sb = new StringBuilder();
        serialize(root, sb);
        byte[] deflated = gzip(sb.toString().getBytes(StandardCharsets.UTF_8));
        String b64 = Base64.encodeToString(deflated, Base64.URL_SAFE | Base64.NO_WRAP);
        byte[] ascii = b64.getBytes(StandardCharsets.US_ASCII);
        byte[] xored = new byte[ascii.length];
        for (int i = 0; i < ascii.length; i++) {
            xored[i] = (byte) (ascii[i] ^ 0x0B);
        }
        return xored;
    }

    private static byte[] gunzip(byte[] gzipped) throws Exception {
        InputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static byte[] gzip(byte[] raw) throws Exception {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputStream gzip = new GZIPOutputStream(buf);
        try {
            gzip.write(raw);
        } finally {
            gzip.close();
        }
        return buf.toByteArray();
    }

    private static byte[] readAll(File file) throws Exception {
        InputStream in = new FileInputStream(file);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

    private static void writeAll(File file, byte[] bytes) throws Exception {
        OutputStream out = new FileOutputStream(file);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    // ------------------------------------------------------------------ //
    // Save model: <d>, <k>, <s>, <i>, <t/>, <f/> plus passthrough of any   //
    // other single-letter value type so real saves always round-trip.      //
    // ------------------------------------------------------------------ //

    private interface Node {
    }

    private static final class Dict implements Node {
        final LinkedHashMap<String, Node> map = new LinkedHashMap<>();
    }

    private static final class Str implements Node {
        final String value;

        Str(String value) {
            this.value = value;
        }
    }

    private static final class IntNum implements Node {
        final long value;

        IntNum(long value) {
            this.value = value;
        }
    }

    private static final class TrueNode implements Node {
        static final TrueNode INSTANCE = new TrueNode();

        private TrueNode() {
        }
    }

    private static final class FalseNode implements Node {
        static final FalseNode INSTANCE = new FalseNode();

        private FalseNode() {
        }
    }

    /** Any other single-letter value type, preserved verbatim. */
    private static final class Raw implements Node {
        final String type;
        final String text;

        Raw(String type, String text) {
            this.type = type;
            this.text = text;
        }
    }

    private static Dict parsePlist(String xml) throws Exception {
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(new java.io.StringReader(xml));
        int event = parser.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.getName().equals("d")) {
                return parseDict(parser);
            }
            event = parser.next();
        }
        throw new IllegalArgumentException("plist has no root <d>");
    }

    /** Entered with the parser on START_TAG "d"; leaves it on END_TAG "d". */
    private static Dict parseDict(XmlPullParser parser) throws Exception {
        LinkedHashMap<String, Node> map = new LinkedHashMap<>();
        int event = parser.nextTag();
        while (!(event == XmlPullParser.END_TAG && parser.getName().equals("d"))) {
            if (!(event == XmlPullParser.START_TAG && parser.getName().equals("k"))) {
                throw new IllegalArgumentException("expected <k> in dict");
            }
            String key = parser.nextText(); // consumes </k>
            event = parser.nextTag(); // START_TAG of the value
            String name = parser.getName();
            Node value;
            if (name.equals("d")) {
                value = parseDict(parser);
            } else if (name.equals("s")) {
                value = new Str(parser.nextText());
            } else if (name.equals("i")) {
                String numText = parser.nextText().trim();
                value = new IntNum(numText.isEmpty() ? 0 : Long.parseLong(numText));
            } else if (name.equals("t")) {
                parser.nextTag(); // consume <t/>
                value = TrueNode.INSTANCE;
            } else if (name.equals("f")) {
                parser.nextTag(); // consume <f/>
                value = FalseNode.INSTANCE;
            } else if (name.length() == 1) {
                // Unknown value type (e.g. <r> reals): keep the raw text so
                // the save round-trips byte-identically for these nodes.
                String text = parser.nextText();
                value = new Raw(name, text);
            } else {
                throw new IllegalArgumentException("unexpected <" + name + ">");
            }
            map.put(key, value);
            event = parser.nextTag();
        }
        Dict dict = new Dict();
        dict.map.putAll(map);
        return dict;
    }

    private static void serialize(Node node, StringBuilder sb) {
        if (node instanceof Dict) {
            sb.append("<d>");
            for (Map.Entry<String, Node> e : ((Dict) node).map.entrySet()) {
                sb.append("<k>").append(escape(e.getKey())).append("</k>");
                serialize(e.getValue(), sb);
            }
            sb.append("</d>");
        } else if (node instanceof Str) {
            sb.append("<s>").append(escape(((Str) node).value)).append("</s>");
        } else if (node instanceof IntNum) {
            sb.append("<i>").append(((IntNum) node).value).append("</i>");
        } else if (node instanceof TrueNode) {
            sb.append("<t/>");
        } else if (node instanceof FalseNode) {
            sb.append("<f/>");
        } else if (node instanceof Raw) {
            Raw raw = (Raw) node;
            sb.append('<').append(raw.type).append('>')
                    .append(escape(raw.text))
                    .append("</").append(raw.type).append('>');
        } else {
            throw new IllegalArgumentException("unknown node");
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
