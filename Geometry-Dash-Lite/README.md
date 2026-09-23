# Geometry Dash Lite

Patches for **Geometry Dash Lite** (`com.robtopx.geometryjumplite`, v2.2.147 XAPK).

## 🩹 Patches

### Complete all levels
Marks official levels 1–22 as 100% complete with all secret coins, stars, and demon completions by updating the game's save file when it launches. No gameplay code is modified.

## 📲 Use

1. Add `https://github.com/mwbra130/Retro-Bowl-College` as a source in Morphe Manager. If the patch doesn't show up, remove the source and add it again — Manager caches the bundle.
2. Select the full Geometry Dash Lite XAPK (not just the base APK) and the **Complete all levels** patch, then patch.
3. Uninstall the original game first — patched builds are signed with a different key — then install the patched APK.
4. Open the game **twice**: the first launch creates the save file, the second launch applies the completions.

⚠️ The fake completions sync to RobTop's servers, which carries a ban risk.

## 🔍 Troubleshooting

Each launch (until it succeeds once) shows a short toast: "no save yet — reopen the game once", "could not read save, will retry", or "marked N levels + M coins complete — reopen the game". The patch also writes `gdl_patch_before.xml`, `gdl_patch_after.xml`, and `gdl_patch_log.txt` into the app's external files dir (`/Android/data/com.robtopx.geometryjumplite/files/`, readable over USB) — send those files if the completions still don't appear and the exact save contents can be checked.

Patch source: [`../patches/src/main/kotlin/app/zdrgon/patches/geometrydashlite/`](../patches/src/main/kotlin/app/zdrgon/patches/geometrydashlite/)
· save-editing helper: [`../extensions/extension/src/main/java/app/template/extension/geometrydashlite/SaveCompleter.java`](../extensions/extension/src/main/java/app/template/extension/geometrydashlite/SaveCompleter.java)
