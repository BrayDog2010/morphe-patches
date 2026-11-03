package app.braydog2010.patches.spotify.misc.lyrics

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object HttpClientBuilderFingerprint : Fingerprint(
    strings = listOf("client == null", "scheduler == null"),
    definingClass = "com/spotify/",
)

private const val COLOR_LYRICS_RESPONSE = "Lcom/spotify/lyrics/serviceretrofit/proto/ColorLyricsResponse;"

/** Turns the color lyrics response into the lyrics screen's state. The method name is obfuscated. */
internal object ColorLyricsMapperFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    parameters = listOf(COLOR_LYRICS_RESPONSE),
)

/** The current track of the player state. */
internal object PlayerStateTrackFingerprint : Fingerprint(
    definingClass = "Lcom/spotify/player/model/AutoValue_PlayerState;",
    name = "track",
    parameters = emptyList(),
)
