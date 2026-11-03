package app.braydog2010.patches.spotify.misc.ads

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/braydog2010/extension/spotify/misc/ads/BlockAdRequestsPatch;"

@Suppress("unused")
val blockAdRequestsPatch = bytecodePatch(
    name = "Block ad requests",
    description = "Blocks Spotify's ad, ad-logic, upsell and marketing message requests, and " +
        "requests to ad and tracking hosts.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        CoreHttpRequestFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range { p1 .. p1 }, $EXTENSION_CLASS_DESCRIPTOR->filterUrl(Ljava/lang/String;)Ljava/lang/String;
                move-result-object p1
            """,
        )
    }
}
