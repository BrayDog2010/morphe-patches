package app.braydog2010.patches.spotify.misc.privacy

import app.morphe.patcher.Fingerprint

/** Copying a link to the clipboard. The method is part of an obfuscated coroutine. */
internal object ShareCopyUrlFingerprint : Fingerprint(
    strings = listOf("Spotify Link"),
    name = "invokeSuspend",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
)

/**
 * Every method that builds a SEND intent and puts text in it. The share sheet code is inlined
 * into several large obfuscated methods, so each one has to be found and patched.
 */
internal object ShareSheetTextFingerprint : Fingerprint(
    strings = listOf("android.intent.action.SEND", "android.intent.extra.TEXT"),
)
