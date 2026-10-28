# Strava Custom Morphe Bundle v0.1

Status: **implementation scaffold / APK inspection required**.

This work is being developed in the existing Morphe patches repository. It does not replace the repository's existing patches.

## Target

- Android package: `com.strava`
- Patch framework: Morphe
- Build task: `./gradlew buildAndroid` (Windows: `./gradlew.bat buildAndroid`)
- Expected bundle output: `patches/build/libs/patches-*.mpp`

## v0.1 goals

The initial scope is limited to client-side customization that does not unlock paid/server-side entitlements:

1. Theme or appearance customization, if the target app exposes a safe local hook.
2. Hide selected promotional UI elements, where they can be identified reliably.
3. Hide selected feed cards or sections through local UI filtering.
4. Local branding options only if the app's resource setup supports them.
5. Optional privacy-oriented display changes that do not falsify shared activity data.
6. A local pace/distance helper only if it can be added without interfering with recording or uploaded activity data.

These are candidate features, not claims that they are already implemented or confirmed to work on a specific Strava release.

## Required before implementation

The exact Strava APK and version must be inspected before writing bytecode patches. Class names, method signatures, resources, and obfuscation can change between releases; guessed targets would be unreliable.

Once the APK is available, record:

- Strava version and APK source
- Package and DEX/resource inventory
- Candidate classes/methods/resources for each feature
- Supported-version constraints for each patch
- Any feature that cannot be implemented safely

## Explicit exclusions

- No bypassing subscriptions, paid features, or server-side access controls.
- No spoofing GPS, activity metrics, or data uploaded to Strava.
- No claiming a patch works until it compiles and has been tested against the target APK.

## Validation checklist

- [ ] Confirm target APK/version and inspect DEX/resources.
- [ ] Implement one isolated patch at a time using the repository's existing Morphe conventions.
- [ ] Compile the patch bundle with `buildAndroid`.
- [ ] Verify patch metadata and supported-version constraints.
- [ ] Test app launch, login, feed, profile, and activity recording.
- [ ] Test each patch independently and verify disabling it restores default behavior.
- [ ] Document tested Strava version(s) and known limitations.

## Build

From the repository root on Windows:

```powershell
.\\gradlew.bat buildAndroid
```

The generated `.mpp` should appear under `patches/build/libs/`.

## Next input needed

Attach the Strava APK you want to target (or provide its exact version and a locally accessible APK file). The APK is required to identify real patch targets rather than inventing class or method names.
