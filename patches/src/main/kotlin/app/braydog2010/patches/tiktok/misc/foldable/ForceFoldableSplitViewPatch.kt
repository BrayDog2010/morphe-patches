package app.braydog2010.patches.tiktok.misc.foldable

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint

private const val EXTENSION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/foldable/FoldableSplitView;"

@Suppress("unused")
val foldableSplitViewPatch = bytecodePatch(
    name = "Foldable split comment view",
    description = "Forces TikTok's tablet-style split layout (video beside comments instead of a bottom sheet) " +
        "once the screen is at least as wide as a configurable threshold, for foldables TikTok doesn't " +
        "already recognize as tablet-class.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableFoldableSplitView()V",
        )

        CommentSplitLiveCheckFingerprint.method.addInstructions(
            0,
            """
                invoke-static {p0, p1}, $EXTENSION_CLASS_DESCRIPTOR->shouldForceCommentSplit(Landroid/app/Activity;Landroid/content/res/Configuration;)Z
                move-result v0
                if-eqz v0, :morphe_stock_comment_split_check
                const/4 v0, 0x1
                return v0
                :morphe_stock_comment_split_check
                nop
            """,
        )

        CommentSplitContainerCheckFingerprint.method.addInstructions(
            0,
            """
                invoke-static {}, $EXTENSION_CLASS_DESCRIPTOR->shouldForceCommentSplitContainer()Z
                move-result v0
                if-eqz v0, :morphe_stock_split_container_check
                const/4 v0, 0x1
                return v0
                :morphe_stock_split_container_check
                nop
            """,
        )
    }
}
