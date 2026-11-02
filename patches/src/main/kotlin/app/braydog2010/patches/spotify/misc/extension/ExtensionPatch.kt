package app.braydog2010.patches.spotify.misc.extension

import app.braydog2010.patches.shared.misc.extension.sharedExtensionPatch

val sharedExtensionPatch = sharedExtensionPatch(
    extensionName = "spotify",
    isYouTubeOrYouTubeMusic = false,
    mainActivityOnCreateHook,
)
