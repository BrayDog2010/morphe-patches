package app.braydog2010.patches.spotify.shared

import app.morphe.patcher.Fingerprint

private const val SPOTIFY_MAIN_ACTIVITY = "Lcom/spotify/music/SpotifyMainActivity;"

object SpiritualMainActivityOnCreateFingerprint : Fingerprint(
    definingClass = SPOTIFY_MAIN_ACTIVITY,
    name = "onCreate",
    strings = listOf("main_activity_on_create"),
    returnType = "V",
    parameters = listOf("Landroid/os/Bundle;"),
)
