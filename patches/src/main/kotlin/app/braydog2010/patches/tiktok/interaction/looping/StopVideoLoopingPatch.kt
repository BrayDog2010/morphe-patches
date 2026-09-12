package app.braydog2010.patches.tiktok.interaction.looping

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint

private const val EXTENSION_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/interaction/StopVideoLoopingPatch;"

@Suppress("unused")
val stopVideoLoopingPatch = bytecodePatch(
    name = "Stop video looping",
    description = "Stops videos at the end instead of replaying them.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

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
                invoke-static {p1}, $EXTENSION_DESCRIPTOR->overrideLooping(Z)Z
                move-result p1
            """,
        )
    }
}
