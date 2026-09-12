/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/interaction/cleardisplay/Fingerprints.kt
 */
package app.braydog2010.patches.tiktok.interaction.cleardisplay

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object OnClearDisplayEventFingerprint : Fingerprint(
    custom = { method, classDef ->
        classDef.endsWith("/ClearModePanelComponent;") && method.name == "onClearModeEvent"
    },
)

/** The extracted body of PlayerController.onRenderFirstFrame in TikTok 46.8.3. */
internal object OnRenderFirstFrameBodyFingerprint : Fingerprint(
    definingClass = "/feed/controller/PlayerController;",
    // No accessFlags here: Morphe compares accessFlags by EXACT equality and 46.8.3 added
    // an extra modifier bit (0x1000) to LLILZIL, breaking the previous exact match.
    returnType = "V",
    parameters = listOf(
        "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;",
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
    ),
    custom = { method, _ -> method.name == "LLILZIL" },
)

internal object ClearModeLogCoreFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Z",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        "Ljava/lang/String;",
        "J",
        "I",
    ),
)

internal object ClearModeLogStateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC, AccessFlags.FINAL),
    returnType = "V",
    parameters = listOf(
        "Lcom/bytedance/common/utility/collection/WeakHandler;",
        "Z",
        "Ljava/lang/String;",
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        "J",
        "I",
        "I",
    ),
)

internal object ClearModeLogPlaytimeFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    returnType = "V",
    parameters = listOf(
        "F",
        "I",
        "J",
        "J",
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
        "Z",
        "Z",
    ),
)

