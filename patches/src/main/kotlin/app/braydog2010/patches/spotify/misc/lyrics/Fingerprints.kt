package app.braydog2010.patches.spotify.misc.lyrics

import app.morphe.patcher.Fingerprint

internal object HttpClientBuilderFingerprint : Fingerprint(
    strings = listOf("client == null", "scheduler == null"),
    definingClass = "com/spotify/",
)
