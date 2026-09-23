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

Patch source: [`../patches/src/main/kotlin/app/zdrgon/patches/geometrydashlite/`](../patches/src/main/kotlin/app/zdrgon/patches/geometrydashlite/)
· save-editing helper: [`../extensions/extension/src/main/java/app/template/extension/geometrydashlite/SaveCompleter.java`](../extensions/extension/src/main/java/app/template/extension/geometrydashlite/SaveCompleter.java)
