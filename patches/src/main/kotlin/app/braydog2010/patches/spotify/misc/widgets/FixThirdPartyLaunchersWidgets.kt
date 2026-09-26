package app.braydog2010.patches.spotify.misc.widgets

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch

@Suppress("unused")
val fixThirdPartyLaunchersWidgetsPatch = bytecodePatch(
    name = "Fix third party launchers widgets",
    description = "Fixes Spotify widgets not working in third party launchers, like Nova Launcher.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        CanBindAppWidgetPermissionFingerprint.method.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }
    }
}
