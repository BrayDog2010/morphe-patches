package app.braydog2010.patches.tiktok.interaction.feedtoolbar

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint

private const val LIVE_ICON_GENERATOR_DESCRIPTOR =
    "Lcom/bytedance/tiktok/homepage/mainfragment/toolbar/LiveIconGenerator;"

private object LiveIconEnabledFingerprint : Fingerprint(
    definingClass = LIVE_ICON_GENERATOR_DESCRIPTOR,
    name = "enabled",
    returnType = "Z",
    parameters = emptyList(),
)

@Suppress("unused")
val hideFeedLiveButtonPatch = bytecodePatch(
    name = "Hide feed LIVE button",
    description = "Adds an option to hide the LIVE button at the top left of video feeds.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableHideFeedLiveButton()V",
        )
        LiveIconEnabledFingerprint.method.overrideToolbarButtonEnabled(
            "hideFeedLiveButtonEnabled",
        )
    }
}
