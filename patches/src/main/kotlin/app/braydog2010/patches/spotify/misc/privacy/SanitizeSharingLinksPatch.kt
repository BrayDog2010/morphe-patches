package app.braydog2010.patches.spotify.misc.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.getReference
import app.braydog2010.util.indexOfFirstInstructionOrThrow
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/braydog2010/extension/spotify/misc/privacy/SanitizeSharingLinksPatch;"

@Suppress("unused")
val sanitizeSharingLinksPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val extensionMethodDescriptor = "$EXTENSION_CLASS_DESCRIPTOR->" +
            "sanitizeSharingLink(Ljava/lang/String;)Ljava/lang/String;"

        // Copy URL method
        ShareCopyUrlFingerprint.methodOrNull?.apply {
            val newPlainTextInvokeIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "newPlainText"
            }
            val urlRegister = getInstruction<FiveRegisterInstruction>(newPlainTextInvokeIndex).registerD

            addInstructions(
                newPlainTextInvokeIndex,
                """
                    invoke-static { v$urlRegister }, $extensionMethodDescriptor
                    move-result-object v$urlRegister
                """,
            )
        }

        // Android native share sheet
        FormatAndroidShareSheetUrlFingerprint.methodOrNull?.apply {
            val shareUrlParameter = if (AccessFlags.STATIC.isSet(accessFlags)) {
                "p0"
            } else {
                "p1"
            }

            addInstructions(
                0,
                """
                    invoke-static { $shareUrlParameter }, $extensionMethodDescriptor
                    move-result-object $shareUrlParameter
                """,
            )
        }
    }
}
