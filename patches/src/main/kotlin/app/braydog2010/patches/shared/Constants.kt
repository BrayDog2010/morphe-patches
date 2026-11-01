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
            AppTarget(version = "1.4.0"),
            AppTarget(version = "1.3.9"),
            AppTarget(version = "1.3.8"),
            AppTarget(version = "1.3.7"),
            AppTarget(version = "1.3.6"),
            AppTarget(version = "1.3.5"),
            AppTarget(version = "1.3.4")
        )
    )

    /** TikTok 47.4.3's version code. */
    const val TIKTOK_4743_VERSION_CODE = 2024704030

    /** TikTok 47.6.2's version code. */
    const val TIKTOK_4762_VERSION_CODE = 2024706020

    val TIKTOK_VERSION_CODES = setOf(TIKTOK_4743_VERSION_CODE, TIKTOK_4762_VERSION_CODE)

    val COMPATIBILITY_TIKTOK = Compatibility(
        name = "TikTok",
        packageName = "com.zhiliaoapp.musically",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFE2C55,
        targets = listOf(
            AppTarget(version = "47.6.2", isExperimental = true),
            AppTarget(version = "47.4.3"),
        ),
    )

    val COMPATIBILITY_SPOTIFY = Compatibility(
        name = "Spotify",
        packageName = "com.spotify.music",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x1DB954,
        /*
        targets = listOf(
            AppTarget(version = "9.0.66")
        )
        */
    )
}
