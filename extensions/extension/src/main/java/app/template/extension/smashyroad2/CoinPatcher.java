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
 * <p>Slot machine upgrades: the ownership flags {@code hasMachine1},
 * {@code hasMachine2} (blue, 50/spin), {@code hasMachine3} (red, 25/spin,
 * always 3 items) are set to 1 — the same flags the game sets in
 * {@code purchaseMachine1/2/3} after a real purchase. A leftover
 * red-slot-machine name heuristic is kept as a fallback.
 *
 * <p>Missions: every quest completion flag ({@code questDone*} /
 * {@code questDoneMain*}, from the game's string table) is set to 1, and
 * missing flags are seeded for numeric ids 0-50 plus known mission names,
 * so main and side missions show as complete.
 *
 * <p>Durability: the per-rarity health upgrade levels ({@code commonHealth},
 * {@code rareHealth}, {@code epicHealth}, {@code legendaryHealth},
 * {@code mysteryHealth}) are set to 999 so vehicles are at max durability.
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
     * Slot machine ownership flags (exact key names from the game's string
     * table). machine1 = default, machine2 = blue (50/spin), machine3 = red
     * (25/spin, always 3 items). Set by purchaseMachine1/2/3 after a real
     * purchase.
     */
    private static final String[] MACHINE_KEYS =
            {"hasMachine1", "hasMachine2", "hasMachine3"};

    /**
     * Red Slot Machine ownership (best effort): no definitive key name was
     * found in the game's string table, so match any int pref that looks like
     * a red-slot-machine flag. Most likely a no-op; the sr2_debug/ dump lets
     * us identify the real key from a live save afterwards.
     */
    private static final Pattern RED_SLOT_KEY = Pattern.compile(
            "^(.*red.*slot.*|.*slot.*red.*|.*red.*machine.*|.*machine.*red.*)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Mission/quest completion flags (from the game's string table:
     * questDone, questDoneMain, questCompleted, questID). The game stores one
     * flag per quest as questDone&lt;id&gt; / questDoneMain&lt;id&gt;; the id
     * may be numeric or the mission name. Match any int pref that looks like
     * a quest completion flag.
     */
    private static final Pattern QUEST_DONE_KEY = Pattern.compile(
            "^(.*quest.*(done|complete|finish).*|.*(done|complete|finish).*quest.*)$",
            Pattern.CASE_INSENSITIVE);

    /**
     * Known mission names (Smashy Road: Wanted 2 wiki). Inserted as
     * questDone&lt;name&gt; / questDoneMain&lt;name&gt; so missions the player
     * never started are marked complete too.
     */
    private static final String[] MISSION_NAMES = {
            "BusDriver", "CowboyStandoff", "CollectSRLetters", "Number1",
            "ZombieSmasher", "ZombieApocalypse", "BankRobbery", "KeytoSuccess",
            "FireFighter", "PerformAStunt", "LivingontheEdge", "AlienInvasion",
            "SwimSwimSwim", "ThatsATank", "Pilot", "BigAirtime", "Zombies"
    };

    /** Highest numeric quest id to pre-seed (covers questDone0..N pattern). */
    private static final int MAX_QUEST_ID = 50;

    /**
     * Vehicle durability/health upgrade levels (exact key names from the
     * game's string table). Set to max so all vehicles are at 100% durability.
     */
    private static final String[] HEALTH_KEYS = {
            "commonHealth", "rareHealth", "epicHealth", "legendaryHealth", "mysteryHealth"
    };

    /** Durability level that effectively maxes vehicle health. */
    private static final long DURABILITY_LEVEL = 999L;

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
        int questTotal = 0;
        int durabilityTotal = 0;
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
                questTotal += r.questChanges;
                durabilityTotal += r.durabilityChanges;
                changedKeys.addAll(r.keys);
            }
        }

        appendLog(extDir, "run: patched " + prefsFiles.size() + " prefs file(s), "
                + changedTotal + " value(s) changed "
                + "(cash=" + cashTotal + ", cards=" + cardTotal
                + ", unlocks=" + unlockTotal + ", quests=" + questTotal
                + ", durability=" + durabilityTotal + "): " + changedKeys);
        if (changedTotal == 0) {
            toast(context, "SR2 patch: no coin/card keys found \u2014 recon saved, send me the log");
        } else {
            toast(context, "SR2 patch: cash 9,999,999 + " + cardTotal
                    + " cards + " + unlockTotal + " unlocks + "
                    + questTotal + " missions + durability maxed");
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
        int questChanges;
        int durabilityChanges;
        final List<String> keys = new ArrayList<>();
    }

    private static boolean isProPassKey(String name) {
        for (String k : PRO_PASS_KEYS) {
            if (k.equals(name)) return true;
        }
        return false;
    }

    private static boolean isMachineKey(String name) {
        for (String k : MACHINE_KEYS) {
            if (k.equals(name)) return true;
        }
        return false;
    }

    private static boolean isHealthKey(String name) {
        for (String k : HEALTH_KEYS) {
            if (k.equals(name)) return true;
        }
        return false;
    }

    private static boolean isUnlockKey(String name) {
        return isProPassKey(name) || isMachineKey(name);
    }

    /**
     * Rewrites matching int/long/string values in a PlayerPrefs XML string.
     * Only the value attributes/text of matched keys change; everything else
     * in the file is preserved byte-for-byte.
     *
     * <p>Keys that don't exist yet (e.g. a rarity the player never earned, or
     * the Pro Pass entitlement) are inserted before {@code </map>} so
     * first-time grants work too.
     */
    private static PatchResult patchPrefsXml(String xml) {
        PatchResult r = new PatchResult();
        String current = patchIntTags(xml, r);
        current = patchStringTags(current, r);
        current = insertMissingKeys(current, r);
        r.xml = current;
        return r;
    }

    /**
     * Inserts target keys that are absent from the prefs file. Without this,
     * rarities/entitlements the player never earned can't be granted because
     * there is no existing value to rewrite.
     */
    private static String insertMissingKeys(String xml, PatchResult r) {
        int mapEnd = xml.lastIndexOf("</map>");
        if (mapEnd < 0) return xml;
        StringBuilder missing = new StringBuilder();
        for (String rarity : new String[]{"common", "rare", "epic", "legendary"}) {
            for (String suffix : new String[]{"Upgrade", "UpgradeValue", "UpgradeValue2", "UpgradeValuePerson"}) {
                String key = rarity + suffix;
                if (!xml.contains("name=\"" + key + "\"")) {
                    missing.append("    <int name=\"").append(key)
                            .append("\" value=\"").append(CARD_AMOUNT).append("\" />\n");
                    r.changes++;
                    r.cardChanges++;
                    r.keys.add(key + "=NEW->" + CARD_AMOUNT);
                }
            }
        }
        for (String key : PRO_PASS_KEYS) {
            if (!xml.contains("name=\"" + key + "\"")) {
                missing.append("    <int name=\"").append(key)
                        .append("\" value=\"1\" />\n");
                r.changes++;
                r.unlockChanges++;
                r.keys.add(key + "=NEW->1");
            }
        }
        for (String key : MACHINE_KEYS) {
            if (!xml.contains("name=\"" + key + "\"")) {
                missing.append("    <int name=\"").append(key)
                        .append("\" value=\"1\" />\n");
                r.changes++;
                r.unlockChanges++;
                r.keys.add(key + "=NEW->1");
            }
        }
        // Mission/quest completion: seed questDone<id> and questDoneMain<id>
        // for numeric ids and known mission names so unstarted missions count
        // as complete too.
        for (String prefix : new String[]{"questDone", "questDoneMain"}) {
            for (int i = 0; i <= MAX_QUEST_ID; i++) {
                String key = prefix + i;
                if (!xml.contains("name=\"" + key + "\"")) {
                    missing.append("    <int name=\"").append(key)
                            .append("\" value=\"1\" />\n");
                    r.changes++;
                    r.questChanges++;
                }
            }
            for (String name : MISSION_NAMES) {
                String key = prefix + name;
                if (!xml.contains("name=\"" + key + "\"")) {
                    missing.append("    <int name=\"").append(key)
                            .append("\" value=\"1\" />\n");
                    r.changes++;
                    r.questChanges++;
                }
            }
        }
        if (r.questChanges > 0) {
            r.keys.add("quests=NEW->" + r.questChanges + " flags");
        }
        // Vehicle durability: seed missing health upgrade levels at max.
        for (String key : HEALTH_KEYS) {
            if (!xml.contains("name=\"" + key + "\"")) {
                missing.append("    <int name=\"").append(key)
                        .append("\" value=\"").append(DURABILITY_LEVEL).append("\" />\n");
                r.changes++;
                r.durabilityChanges++;
                r.keys.add(key + "=NEW->" + DURABILITY_LEVEL);
            }
        }
        if (missing.length() == 0) return xml;
        return xml.substring(0, mapEnd) + missing + xml.substring(mapEnd);
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
            } else if (isHealthKey(name)) {
                newValue = DURABILITY_LEVEL;
                kind = "durability";
            } else if (isUnlockKey(name) || RED_SLOT_KEY.matcher(name).matches()) {
                newValue = 1L;
                kind = "unlock";
            } else if (QUEST_DONE_KEY.matcher(name).matches()) {
                newValue = 1L;
                kind = "quest";
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
            else if ("quest".equals(kind)) r.questChanges++;
            else if ("durability".equals(kind)) r.durabilityChanges++;
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
            if (!isUnlockKey(name)) continue;
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
