package app.braydog2010.patches.tiktok.interaction.quickactions

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.Opcode

private const val FEATURE_CONTROLS_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/featurecontrols/FeatureControls;"

@Suppress("unused")
val disableLongPressQuickSharePatch = bytecodePatch(
    name = "Disable long-press quick share",
    description = "Keeps long-pressing Share from opening TikTok's quick-share interaction.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableDisableLongPressQuickShare()V",
        )

        // Obfuscated gate class shifts between TikTok releases; skip instead of failing the run.
        LongPressQuickShareGateFingerprint.methodOrNull?.apply {
            val returnIndex = indexOfFirstInstructionOrThrow {
                opcode == Opcode.RETURN
            }
            addInstructions(
                returnIndex,
                """
                    invoke-static {v0}, $FEATURE_CONTROLS_DESCRIPTOR->overrideLongPressQuickShare(I)I
                    move-result v0
                """,
            )
        }
    }
}
