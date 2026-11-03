package app.braydog2010.patches.spotify.misc.extension

import app.braydog2010.patches.shared.misc.extension.activityOnCreateExtensionHook

internal val mainActivityOnCreateHook = activityOnCreateExtensionHook(
    "Lcom/spotify/music/SpotifyMainActivity;"
)
