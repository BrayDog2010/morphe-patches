package app.braydog2010.patches.spotify.misc.privacy

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.getReference
import app.braydog2010.util.indexOfFirstInstructionOrThrow
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/braydog2010/extension/spotify/misc/privacy/SanitizeSharingLinksPatch;"

private const val EXTRA_TEXT = "android.intent.extra.TEXT"

@Suppress("unused")
val sanitizeSharingLinksPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes the tracking query parameters from shared links.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val sanitize = "$EXTENSION_CLASS_DESCRIPTOR->sanitizeSharingLink(Ljava/lang/String;)Ljava/lang/String;"

        // Copy link to the clipboard.
        ShareCopyUrlFingerprint.method.apply {
            val newPlainTextInvokeIndex = indexOfFirstInstructionOrThrow {
                getReference<MethodReference>()?.name == "newPlainText"
            }
            val urlRegister = getInstruction<FiveRegisterInstruction>(newPlainTextInvokeIndex).registerD

            addInstructions(
                newPlainTextInvokeIndex,
                """
                    invoke-static { v$urlRegister }, $sanitize
                    move-result-object v$urlRegister
                """,
            )
        }

        // Android share sheet: Intent.putExtra("android.intent.extra.TEXT", text).
        var patchedSites = 0
        ShareSheetTextFingerprint.matchAll().forEach { match ->
            val method = match.method
            val instructions = method.implementation!!.instructions.toList()

            // Back to front, so inserting does not move the sites still to be patched.
            for (index in instructions.indices.reversed()) {
                val instruction = instructions[index] as? FiveRegisterInstruction ?: continue
                val reference = (instruction as ReferenceInstruction).reference as? MethodReference ?: continue
                if (reference.definingClass != "Landroid/content/Intent;" || reference.name != "putExtra" ||
                    reference.parameterTypes.map(CharSequence::toString) != listOf("Ljava/lang/String;", "Ljava/lang/String;")
                ) continue

                // The key must be the string constant loaded just before.
                val keyRegister = instruction.registerD
                val key = (index - 1 downTo maxOf(0, index - 4)).firstNotNullOfOrNull { before ->
                    val loaded = instructions[before]
                    if (loaded is OneRegisterInstruction && loaded.registerA == keyRegister) {
                        ((loaded as? ReferenceInstruction)?.reference as? StringReference)?.string
                    } else {
                        null
                    }
                }
                if (key != EXTRA_TEXT) continue

                val textRegister = instruction.registerE
                method.addInstructions(
                    index,
                    """
                        invoke-static { v$textRegister }, $sanitize
                        move-result-object v$textRegister
                    """,
                )
                patchedSites++
            }
        }

        if (patchedSites == 0) throw PatchException("Sanitize sharing links: no share sheet text found")
    }
}
