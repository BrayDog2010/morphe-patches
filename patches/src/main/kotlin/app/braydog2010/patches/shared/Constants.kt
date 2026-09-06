package app.braydog2010.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    val COMPATIBILITY_VENABOX_HUB = Compatibility(
        name = "Venabox Hub",
        packageName = "com.dubani.dub",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x1A237E,
        targets = listOf(
            AppTarget(
                version = "1.4.0"
            ),
            AppTarget(
                version = "1.3.9"
            ),
            AppTarget(
                version = "1.3.8"
            ),
            AppTarget(
                version = "1.3.7"
            ),
            AppTarget(
                version = "1.3.6"
            ),
            AppTarget(
                version = "1.3.5"
            ),
            AppTarget(
                version = "1.3.4"
            )
        )
    )

    val COMPATIBILITY_TIKTOK = Compatibility(
        name = "TikTok",
        packageName = "com.zhiliaoapp.musically",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x000000,
        targets = listOf(
            AppTarget(
                version = "46.2.3"
            )
        )
    )
}
