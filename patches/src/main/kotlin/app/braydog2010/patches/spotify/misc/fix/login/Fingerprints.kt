package app.braydog2010.patches.spotify.misc.fix.login

import app.morphe.patcher.Fingerprint

internal object KatanaProxyLoginMethodHandlerFingerprint : Fingerprint(
    strings = listOf("katana_proxy_auth"),
    definingClass = "com/spotify/",
    name = "a",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/lang/String;"),
)

internal object KatanaProxyLoginMethodTryAuthorizeFingerprint : Fingerprint(
    strings = listOf("katana_proxy_auth"),
    definingClass = "com/spotify/",
)
