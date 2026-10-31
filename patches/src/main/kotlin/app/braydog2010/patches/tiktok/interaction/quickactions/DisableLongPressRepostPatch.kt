package app.braydog2010.patches.tiktok.interaction.quickactions

import app.braydog2010.util.addInstruction
import app.braydog2010.util.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.settings.settingsPatch
import app.braydog2010.patches.tiktok.shared.requireLocals

private const val FEATURE_CONTROLS_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/featurecontrols/FeatureControls;"

@Suppress("unused")
val disableLongPressRepostPatch = bytecodePatch(
    name = "Disable the long press repost",
    description = "Stops a long press on Like from opening TikTok's repost option, so you " +
        "don't repost by accident. Starts off. Turn it on in Metra settings > Interface.",
    default = true,
) {
    category("Interaction")
    dependsOn(settingsPatch, sharedExtensionPatch)
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableDisableLongPressRepost()V",
        )

        // The method is the Like button's long-click callback (an OnLongClickListener hands it
        // the view). Answering false used to say "not handled", so the platform turned the
        // release into a click and a hold liked the video: on the S22 the heart went red and
        // the count rose by one. Answering true consumes the hold, and holding Like does
        // nothing, which is what the switch promises.
        LongPressRepostGateFingerprint.method.apply {
            requireLocals("Disable the long press repost", 1)
            addInstructionsWithLabels(
                0,
                """
                    invoke-static {}, $FEATURE_CONTROLS_DESCRIPTOR->disableLongPressRepost()Z
                    move-result v0
                    if-eqz v0, :continue_long_press_repost
                    const/4 v0, 0x1
                    return v0
                """,
                ExternalLabel("continue_long_press_repost", getInstruction(0)),
            )
        }
    }
}
