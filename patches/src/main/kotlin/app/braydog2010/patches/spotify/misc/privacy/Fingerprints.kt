package app.braydog2010.patches.spotify.misc.privacy

import app.morphe.patcher.Fingerprint

internal object ShareCopyUrlFingerprint : Fingerprint(
    strings = listOf("Spotify Link"),
    definingClass = "com/spotify/music",
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)

internal object FormatAndroidShareSheetUrlFingerprint : Fingerprint(
    strings = listOf("android.intent.action.SEND"),
    definingClass = "com/spotify/music",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/lang/String;"),
)
