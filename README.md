# 🎮 Matsen's Morphe Patches

Custom Morphe patches for Android apps. Add this repo as a source in Morphe Manager or Morphe Desktop, pick an app, and patch.

## 📲 How to use

1. Open **Morphe Manager** on your Android device.
2. Go to **Settings** > **Patch Sources** and add:
   ```
   https://github.com/mwbra130/Retro-Bowl-College
   ```
   Or tap: [Add to Morphe](https://morphe.software/add-source?github=mwbra130/Retro-Bowl-College)
3. Pick the app and the patch, then patch. **Uninstall the original app first** — patched builds are signed with a different key and won't install over the original.

## 🩹 What the patches do

### Retro Bowl College
- **Unlock premium** — Unlocks the full version of Retro Bowl College (`com.newstargames.retrobowlcollege`, v1.1.2) by reporting a synthetic purchased entitlement to the game's purchase check. No real purchase is made.

### Geometry Dash Lite
- **Complete all levels** — Marks official levels 1–22 as 100% complete with all secret coins, stars, and demon completions (`com.robtopx.geometryjumplite`, v2.2.147 XAPK) by updating the game's save file on launch. No gameplay code is modified. Open the game **twice**: the first launch creates the save file, the second applies the completions. ⚠️ Fake completions sync to RobTop's servers, which carries a ban risk.

Details per app: [Retro-Bowl-College](Retro-Bowl-College/) · [Geometry-Dash-Lite](Geometry-Dash-Lite/)

---

## 🛠️ Building Locally

To build the patch bundle (`.mpp`) on your machine:

```powershell
.\gradlew.bat buildAndroid
```

The compiled patch package will be generated at:
`patches/build/libs/patches-*.mpp`

Load this `.mpp` file into [Morphe Desktop](https://github.com/MorpheApp/morphe-desktop) or Morphe Manager to patch your APKs.

---

<!-- PATCHES_START EXPANDED -->
> **[v1.1.3](https://github.com/mwbra130/Retro-Bowl-College/releases/tag/v1.1.3)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;3 patches total
<details open>
<summary>📦 Geometry Dash Lite&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 2.2.147 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Complete all levels](#complete-all-levels) | Marks every official level 100% complete with all secret coins by updating the game's save file on launch. No gameplay code is modified. |  |

</details>

<details open>
<summary>📦 Retro Bowl College&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

**🎯 Supported versions:**

| 1.1.2 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Unlock premium](#unlock-premium) | Unlocks the full version of Retro Bowl College by reporting a synthetic purchased entitlement to the game's purchase check. No real purchase is made. |  |

</details>

<details open>
<summary>🌐 Universal&nbsp;&nbsp;•&nbsp;&nbsp;1 patch</summary>
<br>

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Satellite Data Optimization](#satellite-data-optimization) | Injects PROPERTY_SATELLITE_DATA_OPTIMIZED meta-data tag into AndroidManifest.xml to enable Direct-To-Cell (DTC) satellite data on T-Mobile / SpaceX network. |  |

</details>

<!-- PATCHES_END -->

## 📜 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).
