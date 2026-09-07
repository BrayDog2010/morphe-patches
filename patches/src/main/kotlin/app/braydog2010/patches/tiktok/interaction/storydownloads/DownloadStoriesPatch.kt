package app.braydog2010.patches.tiktok.interaction.storydownloads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.interaction.downloads.downloadsPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/download/StoryDownloadsPatch;"
private const val AWEME_DESCRIPTOR = "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
private const val AWEME_EXT_DESCRIPTOR = "Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;"

@Suppress("unused")
val downloadStoriesPatch = bytecodePatch(
    name = "Download stories",
    description =
        "Adds TikTok's native save action to each story's share menu. " +
            "The current story item is preserved so profiles with several stories download the intended media.",
    default = true,
) {
    dependsOn(downloadsPatch)
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->" +
                "enableStoryDownloads()V",
        )

        ModernStoryActionBuilderFingerprint.method.apply {
            val currentAwemeField = implementation!!.instructions.mapNotNull { instruction ->
                instruction.getReference<FieldReference>()
            }.firstOrNull { reference ->
                reference.definingClass == definingClass && reference.type == AWEME_DESCRIPTOR
            } ?: throw PatchException(
                "Download stories: current Aweme field was not found in the Story action builder.",
            )

            val panelDownloadStringIndex = implementation!!.instructions.indexOfFirst { instruction ->
                instruction.getReference<StringReference>()?.string == "panel_download_bar"
            }
            if (panelDownloadStringIndex < 1) {
                throw PatchException(
                    "Download stories: Story download-panel gate was not found.",
                )
            }

            val saveTelemetryCallIndex = implementation!!.instructions.indexOfLast { instruction ->
                instruction.getReference<MethodReference>()?.let {
                    it.definingClass == "LX/0GuV;" &&
                        it.name == "LIZ" &&
                        it.parameterTypes == listOf(AWEME_DESCRIPTOR, "Ljava/lang/String;", "Z")
                } == true
            }
            if (saveTelemetryCallIndex < panelDownloadStringIndex || saveTelemetryCallIndex < 2) {
                throw PatchException(
                    "Download stories: native Story save-action boundary was not found.",
                )
            }
            val nativeSaveActionStart = getInstruction(saveTelemetryCallIndex - 2)

            // At this boundary TikTok has initialized every register used by its native save
            // constructor. v7 is scratch here and is overwritten on both the native and forced
            // paths before it is consumed.
            addInstructionsWithLabels(
                panelDownloadStringIndex - 1,
                """
                    move-object/from16 v7, p0
                    iget-object v7, v7, $currentAwemeField
                    invoke-static {v7}, $EXTENSION_CLASS_DESCRIPTOR->shouldShowForAweme($AWEME_DESCRIPTOR)Z
                    move-result v7
                    if-nez v7, :story_native_save_action
                """,
                ExternalLabel("story_native_save_action", nativeSaveActionStart),
            )
        }

        StoryVideoActionBuilderFingerprint.method.apply {
            val currentAwemeField = implementation!!.instructions.mapNotNull { instruction ->
                instruction.getReference<FieldReference>()
            }.firstOrNull { reference ->
                reference.definingClass == definingClass && reference.type == AWEME_DESCRIPTOR
            } ?: throw PatchException(
                "Download stories: current Aweme field was not found in the story share action builder.",
            )

            val sharedStoryCallIndex = implementation!!.instructions.indexOfFirst { instruction ->
                instruction.getReference<MethodReference>()?.let {
                    it.definingClass == AWEME_EXT_DESCRIPTOR && it.name == "isSharedStoryVisible"
                } == true
            }
            if (sharedStoryCallIndex < 1 || sharedStoryCallIndex + 4 >= implementation!!.instructions.size) {
                throw PatchException(
                    "Download stories: shared-story visibility boundary was not found in the action builder.",
                )
            }
            val sharePanelStringIndex = implementation!!.instructions.indexOfFirst { instruction ->
                instruction.getReference<StringReference>()?.string == "share_panel"
            }
            if (sharePanelStringIndex < sharedStoryCallIndex || sharePanelStringIndex < 1) {
                throw PatchException(
                    "Download stories: native save-action construction boundary was not found.",
                )
            }
            val nativeSaveActionStart = getInstruction(sharePanelStringIndex - 1)

            addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, $currentAwemeField
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->shouldShowForAweme($AWEME_DESCRIPTOR)Z
                    move-result v0
                    if-nez v0, :story_native_save_action
                """,
                ExternalLabel("story_native_save_action", nativeSaveActionStart),
            )
        }

        StoryVideoSaveActionEnableFingerprint.method.apply {
            val awemeField = implementation!!.instructions.mapNotNull { instruction ->
                instruction.getReference<FieldReference>()
            }.firstOrNull { reference ->
                reference.definingClass == definingClass && reference.type == AWEME_DESCRIPTOR
            } ?: throw PatchException(
                "Download stories: Aweme field was not found in the native save action gate.",
            )

            addInstructionsWithLabels(
                0,
                """
                    iget-object v0, p0, $awemeField
                    invoke-static {v0}, $EXTENSION_CLASS_DESCRIPTOR->shouldShowForAweme($AWEME_DESCRIPTOR)Z
                    move-result v0
                    if-eqz v0, :tiktok_native_enable_checks
                    const/4 v0, 0x1
                    return v0
                """,
                ExternalLabel("tiktok_native_enable_checks", getInstruction(0)),
            )
        }
    }
}
