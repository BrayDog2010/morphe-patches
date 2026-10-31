package app.braydog2010.patches.tiktok.interaction.looping

import app.braydog2010.util.addInstruction
import app.braydog2010.util.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.settings.settingsPatch

private const val EXTENSION_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/interaction/StopVideoLoopingPatch;"

@Suppress("unused")
val stopVideoLoopingPatch = bytecodePatch(
    name = "Stop video looping",
    description = "Stops a video at its end instead of playing it again. Starts off. Turn it " +
        "on in Metra settings > App behavior.",
    default = true,
) {
    category("Playback")
    dependsOn(settingsPatch, sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableStopVideoLooping()V",
        )

        VideoEngineSetLoopingFingerprint.method.addInstructions(
            0,
            """
                invoke-static/range {p1 .. p1}, $EXTENSION_DESCRIPTOR->overrideLooping(Z)Z
                move-result p1
            """,
        )
    }
}
