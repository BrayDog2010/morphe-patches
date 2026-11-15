package app.braydog2010.patches.tiktok.interaction.quality

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.settings.settingsPatch
import app.braydog2010.util.addInstruction
import app.braydog2010.util.addInstructions
import app.braydog2010.util.addInstructionsAtControlFlowLabel
import app.braydog2010.util.findInstructionIndicesReversedOrThrow
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val SDR = "Lapp/morphe/extension/tiktok/playback/SdrPlayback;"
private const val PLAYER_MODELS = "Lcom/ss/android/ugc/playerkit/simapicommon/model/"

/**
 * TikTok's own answer to "turn HDR off": the accessibility switch keva_is_hdr_off, read only while
 * its experiment is on. The player keeps the SDR gears when this says yes.
 */
internal object ForceHdrOffFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/video/simplayer/PlayerConfigImpl;",
    name = "isForceHdrOff",
    parameters = emptyList(),
    returnType = "Z",
)

/** The one door into the gear list the player kit's SimVideo carries. */
internal object SimVideoSetBitRateFingerprint : Fingerprint(
    definingClass = "${PLAYER_MODELS}SimVideo;",
    name = "setBitRate",
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
)

/** The one door into the gear list the player's bitrate selectors choose from. */
internal object SimVideoUrlModelSetBitRateFingerprint : Fingerprint(
    definingClass = "${PLAYER_MODELS}SimVideoUrlModel;",
    name = "setBitRate",
    parameters = listOf("Ljava/util/List;"),
    returnType = "V",
)

@Suppress("unused")
val sdrPlaybackPatch = bytecodePatch(
    name = "Play SDR instead of HDR",
    description = "Plays the normal version of HDR videos, so your screen doesn't suddenly " +
        "jump to full brightness. Starts off. Turn it on in the patch settings > App behavior.",
    default = true,
) {
    category("Playback")
    compatibleWith(COMPATIBILITY_TIKTOK)
    dependsOn(settingsPatch, sharedExtensionPatch)
    execute {
        ForceHdrOffFingerprint.method.apply {
            findInstructionIndicesReversedOrThrow { opcode == Opcode.RETURN }.forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructionsAtControlFlowLabel(index, """
                    invoke-static/range { v$register .. v$register }, $SDR->forceHdrOff(Z)Z
                    move-result v$register
                """)
            }
        }
        // TikTok's own SDR filter only runs while its experiment is on, so the gear lists are
        // filtered here too. Playback quality's hooks on the same setters drop HDR gears first
        // when the switch is on, so the two agree whichever runs first.
        mapOf(
            SimVideoSetBitRateFingerprint to "filterPlayerVideoGears",
            SimVideoUrlModelSetBitRateFingerprint to "filterPlayerUrlModelGears",
        ).forEach { (fingerprint, callback) ->
            fingerprint.method.addInstructions(0, """
                invoke-static/range { p1 .. p1 }, $SDR->$callback(Ljava/util/List;)Ljava/util/List;
                move-result-object p1
            """)
        }
        SettingsStatusLoadFingerprint.method.addInstruction(0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableSdrPlayback()V")
    }
}
