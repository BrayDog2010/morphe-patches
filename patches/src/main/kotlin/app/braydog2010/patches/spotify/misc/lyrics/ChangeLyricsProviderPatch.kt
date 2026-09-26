package app.braydog2010.patches.spotify.misc.lyrics

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch

@Suppress("unused")
val changeLyricsProviderPatch = bytecodePatch(
    name = "Change lyrics provider",
    description = "Changes the lyrics provider to a custom one.",
    default = false,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    val lyricsProviderHost by stringOption(
        key = "lyricsProviderHost",
        default = "lyrics.natanchiodi.fr",
        title = "Lyrics provider host",
        description = "The domain name or IP address of a custom lyrics provider.",
        required = false,
    )

    execute {
        // Stub: Method cloning and replacement requires fingerprint updates per Spotify version.
        // The original ReVanced implementation used cloneMutable which is not available in Morphe.
        // TODO: Update fingerprints for target Spotify version and implement with Morphe API.
    }
}
