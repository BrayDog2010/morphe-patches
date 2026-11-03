package app.braydog2010.patches.spotify.misc.lyrics

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/braydog2010/extension/spotify/misc/lyrics/CustomLyricsPatch;"

@Suppress("unused")
val customLyricsProviderPatch = bytecodePatch(
    name = "Custom Lyrics Providers",
    description = "Replaces the lyrics Spotify returns with synced lyrics from LRCLIB, " +
        "falling back to Musixmatch when LRCLIB has none.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        // Remember the track that is playing, since the lyrics response does not name it.
        PlayerStateTrackFingerprint.method.apply {
            val returns = implementation!!.instructions.withIndex()
                .filter { it.value.opcode == Opcode.RETURN_OBJECT }
                .map { it.index }
                .asReversed()
            check(returns.isNotEmpty()) { "Player state track getter has no return" }

            returns.forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructions(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->captureTrack(Ljava/lang/Object;)V",
                )
            }
        }

        ColorLyricsMapperFingerprint.method.apply {
            addInstructions(
                0,
                """
                    invoke-static/range { p0 .. p0 }, $EXTENSION_CLASS_DESCRIPTOR->replaceLyrics(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object p0
                    check-cast p0, Lcom/spotify/lyrics/serviceretrofit/proto/ColorLyricsResponse;
                """,
            )
        }
    }
}
