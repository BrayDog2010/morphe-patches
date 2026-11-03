package app.braydog2010.patches.spotify.misc.widgets

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.getReference
import app.braydog2010.util.indexOfFirstInstructionOrThrow
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val fixThirdPartyLaunchersWidgetsPatch = bytecodePatch(
    name = "Fix third party launchers widgets",
    description = "Fixes Spotify widgets not working in third party launchers, like Nova Launcher.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        CanBindAppWidgetPermissionFingerprint.method.apply {
            // The provider asks the package manager whether the caller holds BIND_APPWIDGET and
            // only opens the file when the answer is PERMISSION_GRANTED (0). Report granted.
            val checkIndex = indexOfFirstInstructionOrThrow {
                val reference = getReference<MethodReference>()
                reference?.definingClass == "Landroid/content/pm/PackageManager;" &&
                    reference.name == "checkPermission"
            }
            val resultRegister = getInstruction<OneRegisterInstruction>(checkIndex + 1).registerA

            replaceInstruction(checkIndex + 1, "const/4 v$resultRegister, 0x0")
        }
    }
}
