# BeReal Cleanup

Patches for **BeReal** (`com.bereal.ft`, v3.96.0 XAPK) that remove sponsored content, ads, trackers, and feed annoyances. No login, posting, or messaging functionality is touched.

## 🩹 Patches

### Remove sponsored posts
Filters sponsored posts out of the feed at the data layer by adding `AND isSponsored = 0` to BeReal's Room feed queries (`FeedItemEntity` cache). Applies to the Friends and Discovery feeds.

### Disable AppLovin ads
Prevents the AppLovin MAX SDK from initializing by returning early from BeReal's ad-setup routine. Stops SDK-served ads. (Sponsored feed posts are first-party items — those are handled by "Remove sponsored posts".)

### Disable Adjust tracking
Prevents the Adjust analytics/attribution SDK from initializing by returning early from BeReal's Adjust setup routine. No Adjust session or events are started.

### Remove ad/tracker auto-init providers
Removes the manifest `<provider>` entries that auto-initialize ad and tracker SDKs at startup: Google Mobile Ads, InMobi, AppLovin, Datadog RUM, Vungle, and Adjust. Push notifications (FCM) and AndroidX Startup are left untouched.

### Remove suggested-people card
Hides the "suggested people / suggested friends" card from the feed by making its renderer a no-op.

### Disable video autoplay
Stops feed videos from auto-playing by removing the play-when-ready trigger from the video player's visibility effect. Videos load paused on their thumbnail; tap-to-play still works. Applies everywhere the video player is used: the Discovery feed, the friends feed, and profiles.

## 📲 Use

1. Add `https://github.com/mwbra130/Retro-Bowl-College` as a source in Morphe Manager. If the patches don't show up, remove the source and add it again — Manager caches the bundle.
2. Select the full BeReal XAPK (not just the base APK) and the cleanup patches, then patch.
3. Uninstall the original BeReal first — patched builds are signed with a different key and won't install over it — then install the patched APK. **Back up anything you care about first; you'll have to log back in.**

Patch source: [`../patches/src/main/kotlin/app/zdrgon/patches/berealcleanup/`](../patches/src/main/kotlin/app/zdrgon/patches/berealcleanup/)

⚠️ These fingerprints target BeReal **3.96.0** specifically (the app is obfuscated, so method/class names change every release). If BeReal updates, the patches may fail to apply until the fingerprints are re-derived.
