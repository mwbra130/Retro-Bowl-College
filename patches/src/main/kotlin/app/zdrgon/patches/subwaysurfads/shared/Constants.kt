package app.zdrgon.patches.subwaysurfads.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_SUBWAYSURF_ADS = Compatibility(
        name = "Subway Surfers",
        packageName = "com.kiloo.subwaysurfers",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x111111,
        targets = listOf(
            AppTarget(version = "3.70.0"),
        ),
    )
}
