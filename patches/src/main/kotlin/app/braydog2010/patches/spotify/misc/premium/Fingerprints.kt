package app.braydog2010.patches.spotify.misc.premium

import app.morphe.patcher.Fingerprint

/** The product state's account attributes, a read-only view of the map the server sent. */
internal object ProductStateAttributesFingerprint : Fingerprint(
    definingClass = "Lcom/spotify/remoteconfig/internal/ProductStateProto;",
    returnType = "Ljava/util/Map;",
    parameters = emptyList(),
)
