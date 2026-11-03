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
            AppTarget(version = "1.4.3"),
            AppTarget(version = "1.4.1"),
            AppTarget(version = "1.4.0"),
            AppTarget(version = "1.3.9"),
            AppTarget(version = "1.3.8"),
            AppTarget(version = "1.3.7"),
            AppTarget(version = "1.3.6"),
            AppTarget(version = "1.3.5"),
            AppTarget(version = "1.3.4")
        )
    )

    /** Version codes of the TikTok builds **/
    val TIKTOK_VERSION_CODES = listOf(
        2024704030, // 47.4.3
        2024706020, // 47.6.2
        2024706030, // 47.6.3
    )

    val COMPATIBILITY_TIKTOK = Compatibility(
        name = "TikTok",
        packageName = "com.zhiliaoapp.musically",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFE2C55,
        targets = listOf(
            AppTarget(version = "47.6.3", isExperimental = true),
            AppTarget(version = "47.6.2", isExperimental = true),
            AppTarget(version = "47.4.3"),
        ),
    )

    val COMPATIBILITY_SPOTIFY = Compatibility(
        name = "Spotify",
        packageName = "com.spotify.music",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1DB954,
        targets = listOf(
            AppTarget(version = "9.1.94.1929", isExperimental = true)
        )
    )
}
