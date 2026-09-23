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
> **[v1.3.1](https://github.com/mwbra130/Retro-Bowl-College/releases/tag/v1.3.1)**&nbsp;&nbsp;•&nbsp;&nbsp;`main`&nbsp;&nbsp;•&nbsp;&nbsp;13 patches total
<details open>
<summary>📦 BeReal&nbsp;&nbsp;•&nbsp;&nbsp;10 patches</summary>
<br>

**🎯 Supported versions:**

| 3.96.0 |
| :---: |

| 💊&nbsp;Patch | 📜&nbsp;Description | ⚙️&nbsp;Options |
|----------|----------------|-----------|
| [Bypass license check](#bypass-license-check) | Skips BeReal's PairIP license check so the re-signed app isn't blocked by the 'Get this app from Play' screen. Required for the patched app to launch at all. |  |
| [Disable Adjust tracking](#disable-adjust-tracking) | Prevents the Adjust analytics/attribution SDK from initializing by returning early from BeReal's Adjust setup routine. No Adjust session or events are started. |  |
| [Disable AppLovin ads](#disable-applovin-ads) | Prevents the AppLovin MAX SDK from initializing by returning early from BeReal's ad-setup routine. Stops SDK-served ads; use with 'Remove sponsored posts' to also clear sponsored feed items. |  |
| [Disable video autoplay](#disable-video-autoplay) | Stops feed videos from auto-playing by removing the play-when-ready trigger from the video player's visibility effect. Videos load paused; tap-to-play still works. Applies to the Discovery feed, friends feed, and profiles. |  |
| [Kill license blocking activity](#kill-license-blocking-activity) | Makes BeReal's 'Get this app from Play' blocking activity finish itself immediately on start, so it can never be displayed. Backup layer behind the license-check bypass. |  |
| [Kill license delayed shutdown](#kill-license-delayed-shutdown) | Disables BeReal's PairIP delayed process kill (System.exit) that fires after an unlicensed verdict and shows up as an app crash. Backup layer behind the license-check bypass. |  |
| [Kill license verdict handler](#kill-license-verdict-handler) | Makes BeReal's PairIP license-verdict handler a no-op so a NOT_LICENSED verdict can never trigger the 'Get this app from Play' screen. Backup layer behind the license-check bypass. |  |
| [Remove ad/tracker auto-init providers](#remove-ad-tracker-auto-init-providers) | Removes the manifest <provider> entries that auto-initialize ad and tracker SDKs at startup (Mobile Ads, InMobi, AppLovin, Datadog RUM, Vungle, Adjust). Push notifications and AndroidX Startup are left untouched. |  |
| [Remove sponsored posts](#remove-sponsored-posts) | Filters sponsored posts out of the feed at the data layer by adding 'AND isSponsored = 0' to the Room feed queries. Applies to the Friends and Discovery feeds. |  |
| [Remove suggested-people card](#remove-suggested-people-card) | Hides the 'suggested people / suggested friends' card from the feed by making its renderer a no-op. |  |

</details>

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
