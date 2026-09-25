# Matsen's Morphe Patches

Custom Morphe patches for Android apps. Add this repo as a source in Morphe Manager or Morphe Desktop, pick an app, and patch.

## How to use

1. Open **Morphe Manager** on your Android device.
2. Go to **Settings** > **Patch Sources** and add:
   ```
   https://github.com/mwbra130/Retro-Bowl-College
   ```
   Or tap: [Add to Morphe](https://morphe.software/add-source?github=mwbra130/Retro-Bowl-College)
3. Pick the app and the patch, then patch. **Uninstall the original app first** — patched builds are signed with a different key and won't install over the original.

   Tip: if the patches don't show up or look outdated, remove this source and add it again. Morphe Manager caches the patch bundle.

## Building locally

To build the patch bundle (`.mpp`) on your machine:

```powershell
.\gradlew.bat buildAndroid
```

The compiled patch package will be generated at:
`patches/build/libs/patches-*.mpp`

Load this `.mpp` file into [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or Morphe Manager to patch your APKs.

---

<!-- PATCHES_START EXPANDED -->
> **[v1.7.0](https://github.com/mwbra130/Retro-Bowl-College/releases/tag/v1.7.0)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;14 patches total
<details open>
<summary>BeReal&nbsp;&nbsp;•&nbsp;&nbsp;11 patches</summary>
<br>

**Supported versions:**

| 3.96.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Bypass license check](#bypass-license-check) | Lets the patched app open without the 'Get this app from Play' block. Required for the app to launch. |  |
| [Disable Adjust tracking](#disable-adjust-tracking) | Stops Adjust analytics tracking. |  |
| [Disable AppLovin ads](#disable-applovin-ads) | Blocks AppLovin ads from loading. |  |
| [Disable video autoplay](#disable-video-autoplay) | Videos stay paused until you tap them. Works in all feeds and profiles. |  |
| [Kill PairIP native library](#kill-pairip-native-library) | Stops the PairIP tamper-check native library from loading, so the app can't be killed by its native code. Required for the app to launch. |  |
| [Kill license blocking activity](#kill-license-blocking-activity) | Backup: instantly closes the 'Get this app from Play' screen if it ever appears. |  |
| [Kill license delayed shutdown](#kill-license-delayed-shutdown) | Backup: stops the app from force-closing itself after the license check. |  |
| [Kill license verdict handler](#kill-license-verdict-handler) | Backup: prevents the license check from triggering the block screen. |  |
| [Remove ad/tracker auto-init providers](#remove-ad-tracker-auto-init-providers) | Stops ad and tracker SDKs from starting with the app (Mobile Ads, InMobi, AppLovin, Datadog, Vungle, Adjust). Notifications keep working. |  |
| [Remove sponsored posts](#remove-sponsored-posts) | Removes sponsored posts from the Friends and Discovery feeds. |  |
| [Remove suggested-people card](#remove-suggested-people-card) | Removes the suggested-people card from the feed. |  |

</details>

<details open>
<summary>Geometry Dash Lite&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 2.2.147 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Complete all levels](#complete-all-levels) | Marks every official level 100% complete with all secret coins. |  |

</details>

<details open>
<summary>Smashy Road 2&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 1.54 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlimited coins & upgrade cards](#unlimited-coins-upgrade-cards) | Sets cash to 9,999,999, every upgrade card (common/rare/epic/legendary) to 10,000, unlocks the Pro Pass and slot machines, completes all main + side missions, and maxes vehicle durability. Backs up your save before touching it. To revert: place an empty file named SR2_RESTORE.txt in Android/data/com.rkgames.basisgame/files/ and open the game. |  |

</details>

<details open>
<summary>Retro Bowl College&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**Supported versions:**

| 1.1.2 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlock premium](#unlock-premium) | Unlocks the full version. No real purchase is made. |  |

</details>

<!-- PATCHES_END -->

## App notes

**BeReal** (`com.bereal.ft`, XAPK) — the fingerprints target **3.96.0** specifically (the app is obfuscated, so class/method names change every release). If BeReal updates, patches may fail to apply until the fingerprints are re-derived. Uninstall the original first and log back in after installing the patched build — back up anything you care about first. No login, posting, or messaging functionality is touched.

**Geometry Dash Lite** (`com.robtopx.geometryjumplite`, XAPK) — open the game **twice**: the first launch creates the save file, the second applies the completions. Each launch (until it succeeds once) shows a short toast: "no save yet — reopen the game once", "could not read save, will retry", or "marked N levels + M coins complete — reopen the game". Diagnostics land in the app's external files dir (`/Android/data/com.robtopx.geometryjumplite/files/`, readable over USB): `gdl_patch_before.xml`, `gdl_patch_after.xml`, `gdl_patch_log.txt`. Fake completions sync to RobTop's servers, which carries a ban risk.

**Retro Bowl College** (`com.newstargames.retrobowlcollege`, v1.1.2) — uninstall the original game first (patched builds are signed with a different key and won't install over it), then install the patched APK.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
