package app.braydog2010.patches.spotify.misc.fix.login

import app.morphe.patcher.Fingerprint

/**
 * The Facebook SDK's "katana proxy" login handler, which signs in through the installed Facebook
 * app. The class is obfuscated, so it is found by the name it logs ("katana_proxy_auth"); its
 * tryAuthorize method is then found in the same class.
 */
internal object KatanaProxyLoginMethodHandlerFingerprint : Fingerprint(
    strings = listOf("katana_proxy_auth"),
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)
