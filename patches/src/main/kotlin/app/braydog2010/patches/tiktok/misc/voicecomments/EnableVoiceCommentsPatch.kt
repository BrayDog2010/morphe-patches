package app.braydog2010.patches.tiktok.misc.voicecomments

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val enableVoiceCommentsPatch = bytecodePatch(
    name = "Enable voice comments",
    description = "Enables TikTok's native voice-comment recording and publishing entry points.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        VoiceCommentPublishGateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
