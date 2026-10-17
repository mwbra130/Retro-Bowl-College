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
> **[v1.27.0-dev.1](https://github.com/mwbra130/Retro-Bowl-College/releases/tag/v1.27.0-dev.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`dev`&nbsp;&nbsp;•&nbsp;&nbsp;16 patches total
<details open>
<summary>Subway Surfers&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 3.70.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Block Ads](#block-ads) | Blocks all Unity Ads (interstitial and rewarded) by no-opping the show() methods. No ads will display. |  |
| [Skip Unity Ads](#skip-unity-ads) | Blocks forced interstitial ads — no fullscreen ad between runs. Rewarded ads still work normally and grant their reward. |  |

</details>

<details open>
<summary>Subway Surfers&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 3.70.0 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Block Ads](#block-ads) | Blocks all Unity Ads (interstitial and rewarded) by no-opping the show() methods. No ads will display. |  |
| [Skip Unity Ads](#skip-unity-ads) | Blocks forced interstitial ads — no fullscreen ad between runs. Rewarded ads still work normally and grant their reward. |  |

</details>

<details open>
<summary>BeReal&nbsp;&nbsp;•&nbsp;&nbsp;8 patches</summary>
<br>

**Supported versions:**

| 3.97.1 |
| :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Block ads](#block-ads) | Blocks fullscreen interstitial ads and removes sponsored posts from feeds. |  |
| [Change location](#change-location) | Reads /sdcard/bereal_location.txt and substitutes its coordinates for BeReal's GPS location. Only active when the file exists. |  |
| [Disable video autoplay](#disable-video-autoplay) | Videos stay paused until you tap them. Works in all feeds and profiles. |  |
| [Hide screenshot notification](#hide-screenshot-notification) | Blocks BeReal's screenshot reports from reaching the server. Screenshots work normally locally, but the poster is not notified. |  |
| [Remove late flag](#remove-late-flag) | Reads /sdcard/bereal_late.txt and substitutes its value for BeReal's late flag. Only active when the file exists. |  |
| [Remove suggested-people card](#remove-suggested-people-card) | Removes the suggested-people card from the feed. |  |
| [Set retake count](#set-retake-count) | Reads /sdcard/bereal_retakes.txt and substitutes its integer value for BeReal's retake counter. Only active when the file exists. |  |
| [Use gallery photo](#use-gallery-photo) | Replaces camera-captured images with gallery files from /sdcard/bereal_gallery_primary.jpg and /sdcard/bereal_gallery_secondary.jpg. Only active when the files exist. |  |

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
<summary>Smashy Road 2&nbsp;&nbsp;•&nbsp;&nbsp;2 patches</summary>
<br>

**Supported versions:**

| 1.54 | 1.45 |
| :---: | :---: |

| Patch | Description | Options |
|----------|----------------|-----------|
| [Unlimited coins & unlocks](#unlimited-coins-unlocks) | Sets cash to 9,999,999, every upgrade card to 10,000, unlocks ALL vehicles and characters (no spinning needed), unlocks the Pro Pass, slot/lottery machines, completes all main + side missions, and maxes vehicle durability. Backs up your save before touching it. To revert: place an empty file named SR2_RESTORE.txt in Android/data/com.rkgames.basisgame/files/ and open the game. |  |
| [Unlock All Slot Machines](#unlock-all-slot-machines) | Unlocks all slot machines (1/2/3) without Google Play purchase. Patches the native ownership checks to always grant the machines. |  |

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
