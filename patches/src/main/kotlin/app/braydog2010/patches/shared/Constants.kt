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

    /** SHA-256 of TikTok's signing certificate (CN=musical.ly). */
    const val TIKTOK_SIGNER_SHA256 = "9041803e91bcb814b4b4399fb5c85a91640b755e5e8ba76813814bf4cf2ab5ba"

    /** TikTok 47.4.3's version code. Only the newest supported build is declared. */
    const val TIKTOK_4743_VERSION_CODE = 2024704030

    val TIKTOK_VERSION_CODES = setOf(TIKTOK_4743_VERSION_CODE)

    val COMPATIBILITY_TIKTOK = Compatibility(
        name = "TikTok",
        packageName = "com.zhiliaoapp.musically",
        appIconColor = 0xFE2C55,
        signatures = setOf(TIKTOK_SIGNER_SHA256),
        targets = listOf(
            AppTarget(version = "47.4.3", versionCode = TIKTOK_4743_VERSION_CODE),
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
