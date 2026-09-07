package app.braydog2010.patches.tiktok.interaction.storydownloads

import app.morphe.patcher.Fingerprint
import app.braydog2010.util.getReference
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

/**
 * TikTok 46.2.3 action-list builder for the ordinary video "save" item. The method already
 * owns the exact Aweme being shared, but its native gates return before constructing the action
 * for TikTok stories.
 */
internal object StoryVideoActionBuilderFingerprint : Fingerprint(
    definingClass = "LX/0GpI;",
    name = "LJJI",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = emptyList(),
    strings = listOf(
        "save",
        "panel_download_bar",
        "homepage_podcast",
        "share_panel",
        "click_download_icon",
        "long_press_download",
    ),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions
        if (instructions == null) {
            false
        } else {
            var readsCurrentAweme = false
            var checksSharedStory = false
            var constructsVideoSaveAction = false
            instructions.forEach { instruction ->
                instruction.getReference<FieldReference>()?.let { reference ->
                    if (reference.definingClass == "LX/0GpI;" &&
                        reference.type == "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
                    ) {
                        readsCurrentAweme = true
                    }
                }
                instruction.getReference<MethodReference>()?.let { reference ->
                    if (reference.definingClass == "Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;" &&
                        reference.name == "isSharedStoryVisible"
                    ) {
                        checksSharedStory = true
                    }
                }
                instruction.getReference<TypeReference>()?.let { reference ->
                    if (reference.type == "LX/0GtV;") {
                        constructsVideoSaveAction = true
                    }
                }
            }
            readsCurrentAweme && checksSharedStory && constructsVideoSaveAction
        }
    },
)
/**
 * TikTok 46.2.3 action-list builder used by the Story share sheet. Its native save and
 * save-photo action is constructed late in the method, after Story-specific gates normally
 * skip over it.
 */
internal object ModernStoryActionBuilderFingerprint : Fingerprint(
    definingClass = "LX/1JTr;",
    name = "LIZIZ",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Ljava/util/List;",
    parameters = listOf("LX/165i;"),
    strings = listOf(
        "save",
        "save_photo",
        "panel_download_bar",
        "offline_mode",
        "homepage_podcast",
    ),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions
        if (instructions == null) {
            false
        } else {
            var readsCurrentAweme = false
            var checksSharedStory = false
            var constructsSaveAction = false
            instructions.forEach { instruction ->
                instruction.getReference<FieldReference>()?.let { reference ->
                    if (reference.definingClass == "LX/1JTr;" &&
                        reference.type == "Lcom/ss/android/ugc/aweme/feed/model/Aweme;"
                    ) {
                        readsCurrentAweme = true
                    }
                }
                instruction.getReference<MethodReference>()?.let { reference ->
                    if (reference.definingClass == "Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;" &&
                        reference.name == "isSharedStoryVisible"
                    ) {
                        checksSharedStory = true
                    }
                }
                instruction.getReference<TypeReference>()?.let { reference ->
                    if (reference.type == "LX/1JTi;") {
                        constructsSaveAction = true
                    }
                }
            }
            readsCurrentAweme && checksSharedStory && constructsSaveAction
        }
    },
)

/** Native visibility gate on the 46.2.3 video save action constructed above. */
internal object StoryVideoSaveActionEnableFingerprint : Fingerprint(
    definingClass = "LX/0GtV;",
    name = "enable",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL),
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ ->
        val methodReferences = method.implementation?.instructions?.mapNotNull { instruction ->
            instruction.getReference<MethodReference>()
        } ?: emptyList()
        methodReferences.any {
            it.definingClass == "Lcom/ss/android/ugc/aweme/feed/model/AwemeACLShare;" &&
                it.name == "getDownloadGeneral"
        } && methodReferences.any {
            it.definingClass == "Lcom/ss/android/ugc/aweme/feed/model/AwemeExtKt;" &&
                it.name == "isSharedStoryVisible"
        } && methodReferences.any {
            it.definingClass == "Lcom/ss/android/ugc/aweme/feed/model/Aweme;" &&
                it.name == "getIsCommentPostVideo"
        }
    },
)
