package app.zdrgon.patches.berealcleanup.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_BEREAL_CLEANUP = Compatibility(
        name = "BeReal",
        packageName = "com.bereal.ft",
        // XAPK: Morphe merges the split bundle (including config.arm64_v8a.apk,
        // which holds the native libraries) into a single APK before patching.
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x111111,
        targets = listOf(
            AppTarget(version = "3.96.0"),
        ),
    )
}
