package app.braydog2010.patches.tiktok.misc.inbox

import app.braydog2010.util.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.settings.settingsPatch

/** Shares the live `hide_inbox_stories` row filter with `Hide inbox items`. */
@Suppress("unused")
val hideInboxStoriesPatch = bytecodePatch(
    name = "Hide inbox stories",
    description = "Hides the row of stories at the top of the Inbox. It uses the same switch " +
        "as Hide inbox items. Starts off. Turn it on in Metra settings > Interface.",
) {
    category("Inbox")
    dependsOn(settingsPatch, sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableHideInboxStories()V",
        )

        MainActivityOnCreateFingerprint.method.installInboxLayoutFilter()
    }
}
