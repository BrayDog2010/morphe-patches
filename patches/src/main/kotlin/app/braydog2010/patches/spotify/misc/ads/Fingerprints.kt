package app.braydog2010.patches.spotify.misc.ads

import app.morphe.patcher.Fingerprint

/**
 * The native core's HTTP transport. It implements HttpConnectionDelegate and turns each request
 * into a Cronet UrlRequest, so it sees the address of everything the core fetches.
 */
internal object CoreHttpRequestFingerprint : Fingerprint(
    returnType = "Lorg/chromium/net/UrlRequest;",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/spotify/core/http/HttpRequest;",
        "Lcom/spotify/core/http/HttpConnection;",
        "Lcom/spotify/core/http/HttpOptions;",
    ),
)
