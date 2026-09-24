package app.template.extension.smashyroad2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Smashy Road 2 (com.rkgames.basisgame) save patcher.
 *
 * <p>The game is Unity-based and keeps progress in Unity PlayerPrefs:
 * {@code /data/data/com.rkgames.basisgame/shared_prefs/com.rkgames.basisgame.v2.playerprefs.xml}
 *
 * <p>Runs at the very start of UnityPlayerActivity.onCreate, before the Unity
 * player loads PlayerPrefs from disk, so the edited values win the race.
 *
 * <p>Every launch:
 * <ol>
 *   <li>If {@code <ext>/SR2_RESTORE.txt} exists, the pristine backup is copied
 *       back over the live save, the trigger is deleted, a Toast confirms,
 *       and no patching happens.
 *   <li>If no backup exists yet, {@code shared_prefs/} (plus small files from
 *       {@code files/}) is copied to {@code <ext>/morphe_backup/} with a
 *       {@code BACKUP_DONE} marker. The pristine backup is never overwritten.
 *   <li>Cash keys are set to 9,999,999 and every upgrade-card key to 10,000 in
 *       each shared_prefs XML. Pre-patch copies go to {@code <ext>/sr2_debug/}
 *       and a change log is appended, so a missed key can be diagnosed from
 *       real data.
 * </ol>
 *
 * <p>Cash keys (heuristic): any int/long pref whose name starts with "cash" or
 * "money" (case-insensitive) — e.g. cashAmount, moneyAmount.
 *
 * <p>Upgrade-card keys (exact, taken from the game's own string table):
 * {@code {common,rare,epic,legendary}Upgrade{,Value,Value2,ValuePerson}}.
 *
 * <p>Pro Pass: sets the local entitlement flags {@code proPassEnabled},
 * {@code hasProPass} (and the lowercase variant) to 1 / "true" — the same
 * flags the game sets in {@code purchaseProPassComplete}.
 *
 * <p>Red Slot Machine: no definitive save key was found in the game's string
 * table, so any int pref that looks like a red-slot-machine flag is set to 1
 * as a best effort. If the upgrade is bought with in-game cash, the
 * 9,999,999 cash covers it directly; the sr2_debug/ dump identifies the real
 * key from a live save for a follow-up if needed.
 *
 * <p>Never crashes the game: every failure is swallowed after logging.
 */
public final class CoinPatcher {

    private static boolean sRan = false;

    private static final long CASH_AMOUNT = 9_999_999L;
    private static final long CARD_AMOUNT = 10_000L;

    private static final Pattern UPGRADE_KEY = Pattern.compile(
            "^(common|rare|epic|legendary)Upgrade(Value|Value2|ValuePerson)?$");
    private static final Pattern CASH_KEY = Pattern.compile(
            "^(cash|money).*$", Pattern.CASE_INSENSITIVE);

    /**
     * Pro Pass entitlement flags (exact key names from the game's string table;
     * set by purchaseProPassComplete after a real purchase).
     */
    private static final String[] PRO_PASS_KEYS =
            {"proPassEnabled", "hasProPass", "propassenabled"};

    /**
     * Red Slot Machine ownership (best effort): no definitive key name was
     * found in the game's string table, so match any int pref that looks like
     * a red-slot-machine flag. Most likely a no-op; the sr2_debug/ dump lets
     * us identify the real key from a live save afterwards.
     */
    private static final Pattern RED_SLOT_KEY = Pattern.compile(
            "^(.*red.*slot.*|.*slot.*red.*|.*red.*machine.*|.*machine.*red.*)$",
            Pattern.CASE_INSENSITIVE);

    /** Unity's PlayerPrefs emission: <int name="..." value="..." /> */
    private static final Pattern INT_TAG = Pattern.compile(
            "<(int|long)\\s+name=\"([^\"]+)\"\\s+value=\"(-?\\d+)\"\\s*/>");

    /** Unity's PlayerPrefs emission: <string name="...">...</string> */
    private static final Pattern STRING_TAG = Pattern.compile(
            "<string\\s+name=\"([^\"]+)\">([^<]*)</string>");

    private static final String BACKUP_DIR = "morphe_backup";
    private static final String BACKUP_MARKER = "BACKUP_DONE";
    private static final String RESTORE_TRIGGER = "SR2_RESTORE.txt";
    private static final String DEBUG_DIR = "sr2_debug";
    private static final String LOG_FILE = "sr2_patch_log.txt";

    /** Skip huge downloads/caches when backing up files/. */
    private static final long BACKUP_FILE_SIZE_CAP = 10L * 1024 * 1024;

    private CoinPatcher() {
    }

    public static void run(Context context) {
        if (sRan) return;
        sRan = true;
        try {
            doRun(context);
        } catch (Throwable ignored) {
            // Never crash the game.
        }
    }

    private static void doRun(Context context) throws Exception {
        File dataDir = getDataDir(context);
        File extDir = context.getExternalFilesDir(null);
        if (extDir == null) extDir = context.getFilesDir();
        File backupDir = new File(extDir, BACKUP_DIR);
        File debugDir = new File(extDir, DEBUG_DIR);

        // 1. Restore path: trigger file wins over everything.
        File trigger = new File(extDir, RESTORE_TRIGGER);
        if (trigger.exists()) {
            boolean ok = restoreBackup(dataDir, backupDir);
            appendLog(extDir, "restore: trigger found, restore " + (ok ? "OK" : "FAILED (no backup?)"));
            try {
                trigger.delete();
            } catch (Throwable ignored) {
            }
            toast(context, ok ? "SR2 patch: save restored from backup"
                    : "SR2 patch: restore failed \u2014 no backup found");
            return;
        }

        File prefsDir = new File(dataDir, "shared_prefs");
        List<File> prefsFiles = listXml(prefsDir);
        if (prefsFiles.isEmpty()) {
            appendLog(extDir, "run: no shared_prefs yet (fresh install or unexpected layout).");
            toast(context, "SR2 patch: no save yet \u2014 play first, then reopen");
            return;
        }

        // 2. Pristine backup, once ever.
        File marker = new File(backupDir, BACKUP_MARKER);
        if (!marker.exists()) {
            boolean ok = backupSave(dataDir, backupDir);
            appendLog(extDir, "backup: " + (ok ? "OK -> " + backupDir.getAbsolutePath() : "FAILED"));
            toast(context, ok ? "SR2 patch: save backed up"
                    : "SR2 patch: backup failed, patch NOT applied");
            if (!ok) return;
        }

        // 3. Patch every prefs XML.
        int changedTotal = 0;
        int cashTotal = 0;
        int cardTotal = 0;
        int unlockTotal = 0;
        List<String> changedKeys = new ArrayList<>();
        debugDir.mkdirs();
        for (File prefs : prefsFiles) {
            String xml = readText(prefs);
            // Keep a pre-patch copy for forensics.
            try {
                writeAll(new File(debugDir, "before_" + prefs.getName()),
                        xml.getBytes(StandardCharsets.UTF_8));
            } catch (Throwable ignored) {
            }
            PatchResult r = patchPrefsXml(xml);
            if (r.changes > 0) {
                writeAll(prefs, r.xml.getBytes(StandardCharsets.UTF_8));
                changedTotal += r.changes;
                cashTotal += r.cashChanges;
                cardTotal += r.cardChanges;
                unlockTotal += r.unlockChanges;
                changedKeys.addAll(r.keys);
            }
        }

        appendLog(extDir, "run: patched " + prefsFiles.size() + " prefs file(s), "
                + changedTotal + " value(s) changed "
                + "(cash=" + cashTotal + ", cards=" + cardTotal
                + ", unlocks=" + unlockTotal + "): " + changedKeys);
        if (changedTotal == 0) {
            toast(context, "SR2 patch: no coin/card keys found \u2014 recon saved, send me the log");
        } else {
            toast(context, "SR2 patch: cash 9,999,999 + " + cardTotal
                    + " cards + " + unlockTotal + " unlocks set");
        }
    }

    // ------------------------------------------------------------------ //
    // Backup / restore                                                    //
    // ------------------------------------------------------------------ //

    /** Copies shared_prefs/ and small files/ files into the backup dir. */
    private static boolean backupSave(File dataDir, File backupDir) {
        try {
            if (!backupDir.mkdirs() && !backupDir.isDirectory()) return false;
            File prefs = new File(dataDir, "shared_prefs");
            if (prefs.isDirectory()) {
                copyRecursive(prefs, new File(backupDir, "shared_prefs"), true);
            }
            File files = new File(dataDir, "files");
            if (files.isDirectory()) {
                copyRecursive(files, new File(backupDir, "files"), false);
            }
            File databases = new File(dataDir, "databases");
            if (databases.isDirectory()) {
                copyRecursive(databases, new File(backupDir, "databases"), true);
            }
            writeAll(new File(backupDir, BACKUP_MARKER),
                    ("backed up " + System.currentTimeMillis()).getBytes(StandardCharsets.UTF_8));
            return new File(backupDir, BACKUP_MARKER).exists();
        } catch (Throwable t) {
            return false;
        }
    }

    /** Copies the pristine backup back over the live save. */
    private static boolean restoreBackup(File dataDir, File backupDir) {
        try {
            if (!new File(backupDir, BACKUP_MARKER).exists()) return false;
            for (String sub : new String[]{"shared_prefs", "files", "databases"}) {
                File src = new File(backupDir, sub);
                if (src.isDirectory()) {
                    copyRecursive(src, new File(dataDir, sub), true);
                }
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Copies src -> dst recursively. When respectCap is false, files larger
     * than BACKUP_FILE_SIZE_CAP are skipped (asset bundles, caches).
     */
    private static void copyRecursive(File src, File dst, boolean respectCap) throws Exception {
        if (src.isDirectory()) {
            if (!dst.mkdirs() && !dst.isDirectory()) {
                throw new IllegalStateException("cannot mkdir " + dst);
            }
            File[] kids = src.listFiles();
            if (kids != null) {
                for (File kid : kids) {
                    copyRecursive(kid, new File(dst, kid.getName()), respectCap);
                }
            }
            return;
        }
        if (!respectCap && src.length() > BACKUP_FILE_SIZE_CAP) return;
        copyFile(src, dst);
    }

    // ------------------------------------------------------------------ //
    // Prefs patching                                                      //
    // ------------------------------------------------------------------ //

    private static final class PatchResult {
        String xml;
        int changes;
        int cashChanges;
        int cardChanges;
        int unlockChanges;
        final List<String> keys = new ArrayList<>();
    }

    private static boolean isProPassKey(String name) {
        for (String k : PRO_PASS_KEYS) {
            if (k.equals(name)) return true;
        }
        return false;
    }

    /**
     * Rewrites matching int/long/string values in a PlayerPrefs XML string.
     * Only the value attributes/text of matched keys change; everything else
     * in the file is preserved byte-for-byte.
     */
    private static PatchResult patchPrefsXml(String xml) {
        PatchResult r = new PatchResult();
        String current = patchIntTags(xml, r);
        current = patchStringTags(current, r);
        r.xml = current;
        return r;
    }

    private static String patchIntTags(String xml, PatchResult r) {
        Matcher m = INT_TAG.matcher(xml);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String name = m.group(2);
            long newValue;
            String kind;
            if (UPGRADE_KEY.matcher(name).matches()) {
                newValue = CARD_AMOUNT;
                kind = "card";
            } else if (isProPassKey(name) || RED_SLOT_KEY.matcher(name).matches()) {
                newValue = 1L;
                kind = "unlock";
            } else if (CASH_KEY.matcher(name).matches()) {
                newValue = CASH_AMOUNT;
                kind = "cash";
            } else {
                continue;
            }
            long oldValue;
            try {
                oldValue = Long.parseLong(m.group(3));
            } catch (NumberFormatException e) {
                continue;
            }
            if (oldValue == newValue) continue;
            String replacement = "<" + m.group(1) + " name=\"" + name
                    + "\" value=\"" + newValue + "\" />";
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
            r.changes++;
            if ("card".equals(kind)) r.cardChanges++;
            else if ("cash".equals(kind)) r.cashChanges++;
            else r.unlockChanges++;
            r.keys.add(name + "=" + oldValue + "->" + newValue);
        }
        m.appendTail(out);
        return out.toString();
    }

    /**
     * Flips exact Pro Pass keys stored as strings ("false"->"true",
     * "0"->"1"). Only touches the known entitlement key names.
     */
    private static String patchStringTags(String xml, PatchResult r) {
        Matcher m = STRING_TAG.matcher(xml);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if (!isProPassKey(name)) continue;
            String val = m.group(2);
            String newVal = null;
            if ("false".equalsIgnoreCase(val)) newVal = "true";
            else if ("0".equals(val)) newVal = "1";
            if (newVal == null || newVal.equals(val)) continue;
            String replacement = "<string name=\"" + name + "\">"
                    + newVal + "</string>";
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
            r.changes++;
            r.unlockChanges++;
            r.keys.add(name + "=" + val + "->" + newVal);
        }
        m.appendTail(out);
        return out.toString();
    }

    private static List<File> listXml(File dir) {
        List<File> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] files = dir.listFiles();
        if (files == null) return out;
        for (File f : files) {
            if (f.isFile() && f.getName().endsWith(".xml")) out.add(f);
        }
        return out;
    }

    // ------------------------------------------------------------------ //
    // Small IO + UI helpers                                               //
    // ------------------------------------------------------------------ //

    private static File getDataDir(Context context) {
        try {
            return context.getDataDir();
        } catch (Throwable t) {
            return new File(context.getApplicationInfo().dataDir);
        }
    }

    private static void copyFile(File src, File dst) throws Exception {
        File parent = dst.getParentFile();
        if (parent != null) parent.mkdirs();
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    private static String readText(File f) throws Exception {
        InputStream in = new FileInputStream(f);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            in.close();
        }
    }

    private static void writeAll(File f, byte[] bytes) throws Exception {
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        OutputStream out = new FileOutputStream(f);
        try {
            out.write(bytes);
        } finally {
            out.close();
        }
    }

    private static void appendLog(File extDir, String line) {
        try {
            File log = new File(extDir, LOG_FILE);
            String prev = "";
            if (log.exists()) {
                try {
                    prev = readText(log);
                } catch (Throwable ignored) {
                }
                if (prev.length() > 20000) prev = prev.substring(prev.length() - 20000);
            }
            String entry = "[" + System.currentTimeMillis() + "] " + line + "\n";
            writeAll(log, (prev + entry).getBytes(StandardCharsets.UTF_8));
        } catch (Throwable ignored) {
        }
    }

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
}
