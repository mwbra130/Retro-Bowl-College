# Strava Custom Morphe v0.1 — Test Plan

This plan applies to Strava Android 486.9 (versionCode 12400193) and must be revised if the target APK changes.

## Important current status

No Strava patch has been implemented or compiled yet. Do not attempt to install a nonexistent or unverified `.mpp` bundle. The current PR contains planning and APK inventory only.

## Stage 0 — Prepare a safe test setup

- [ ] Confirm you can sign in to the official Strava app and that important activities are synced.
- [ ] Keep the official APK/XAPK available so you can reinstall it if needed.
- [ ] Prefer a spare Android device or emulator for the first install test. A patched app is signed differently from the official app and usually cannot update over the official install.
- [ ] Do not uninstall the daily-use Strava app until the patch has built and you are ready to test installation.
- [ ] Keep the original XAPK and note its version: 486.9 / 12400193.

## Stage 1 — Static validation on PC

- [ ] Decompile the exact base APK with JADX and inspect relevant methods/call sites.
- [ ] Implement one isolated patch only after a real method target and signature are confirmed.
- [ ] Run the repository's Windows build command from the repository root:

  ```powershell
  .\\gradlew.bat buildAndroid
  ```

- [ ] Confirm Gradle exits successfully and a new `.mpp` exists in `patches/build/libs/`.
- [ ] Record the generated bundle name, build output, target version, and patch fingerprint.
- [ ] Inspect the bundle metadata and confirm the patch only targets the intended package/version.

A successful build proves only that the patch bundle compiles. It does not prove that the patch applies or that Strava works.

## Stage 2 — Apply the patch

- [ ] Open the exact Strava 486.9 XAPK in the compatible Morphe Manager/Desktop workflow.
- [ ] Select only the one new test patch; leave all unrelated patches disabled.
- [ ] Build the patched app and save the complete build/apply log.
- [ ] If patch matching fails, stop and inspect the log; do not guess alternate class names or disable validation checks.
- [ ] Install only on the test device first. Keep the official app on the main device until the patched app is confirmed installable and usable.

## Stage 3 — Basic smoke test

With only the test patch enabled:

- [ ] App launches without crashing.
- [ ] Sign-in and account loading work.
- [ ] Home/feed opens and scrolls.
- [ ] Profile and activity detail screens open.
- [ ] Settings open and the new behavior is limited to the intended UI.
- [ ] Force-stop and reopen the app; behavior remains stable.
- [ ] No unexpected battery drain, repeated crash, or persistent error notification appears.

For a feed/promotion UI patch, do not start an outdoor recording as the first test. Confirm basic UI behavior first.

## Stage 4 — Feature-specific test

- **Theme/UI:** check light/dark mode, system font scaling, portrait/landscape if supported, and major screens.
- **Hide promotional UI:** confirm only the selected local promotional element disappears; subscription/account access should behave exactly as before.
- **Hide feed sections:** confirm normal activity cards remain visible and scrolling/pagination still work.
- **Local branding:** verify launcher icon/name and app launch; no server/account behavior should change.
- **Privacy display:** test with non-sensitive sample activity data; verify existing private-zone and route-privacy behavior remains intact.
- **Pace helper:** use a non-critical test only after recording behavior is separately validated; do not change or fabricate uploaded metrics.

## Stage 5 — Regression and rollback

- [ ] Disable the patch and confirm the original UI behavior returns.
- [ ] Re-enable only the patch and repeat the smoke test.
- [ ] Check login, feed, profile, activity details, and settings again.
- [ ] Do not publish a patch as supported until tests pass on the exact Strava version and are documented.
- [ ] If install/app launch fails, uninstall the test build and reinstall the official Strava app. Sign in and confirm server-synced activities are present.

## Report back with

1. The exact Strava version shown in app settings.
2. The Morphe version used.
3. The patch bundle name.
4. The full build/apply error text or log if it fails.
5. What happened during the smoke test.

Never send account passwords, session cookies, or API tokens in logs.
