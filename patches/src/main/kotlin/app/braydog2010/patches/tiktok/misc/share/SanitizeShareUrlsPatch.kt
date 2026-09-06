/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/share/SanitizeShareUrlsPatch.kt
 */
package app.braydog2010.patches.tiktok.misc.share

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/share/ShareUrlSanitizer;"

@Suppress("unused")
val sanitizeShareUrlsPatch = bytecodePatch(
    name = "Sanitize sharing links",
    description = "Removes tracking parameters from TikTok links before they are shared.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        ShareUrlTrackerFingerprint.method.apply {
            val urlRegister = implementation!!.registerCount - parameterTypes.size +
                if (parameterTypes[0] in arrayOf("J", "D")) 2 else 1

            addInstructions(
                0,
                """
                    invoke-static {v$urlRegister}, $EXTENSION_CLASS_DESCRIPTOR->stripAllQueryParams(Ljava/lang/String;)Ljava/lang/String;
                    move-result-object v0
                    return-object v0
                """,
            )
        }
    }
}
