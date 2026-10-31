package app.braydog2010.patches.tiktok.misc.featuregatelab

import app.braydog2010.util.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.compat.AppCompatibilities
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.settings.settingsPatch

@Suppress("unused")
val featureGateRecorderPatch = bytecodePatch(
    name = "Feature Gate Recorder",
    description = "Records which hidden TikTok settings the app reads while you use it, and " +
        "which ones changed. It's a research tool and changes nothing in TikTok. Start a " +
        "recording in Metra settings > Diagnostics.",
    default = false,
) {
    category("Settings")
    dependsOn(settingsPatch, featureGateLabPatch)
    compatibleWith(*AppCompatibilities.tiktokAndPineDrama())
    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableFeatureGateRecorder()V")
    }
}
