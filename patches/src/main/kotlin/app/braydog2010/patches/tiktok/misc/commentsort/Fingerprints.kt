package app.braydog2010.patches.tiktok.misc.commentsort

import app.morphe.patcher.Fingerprint

internal object CommentSortOptionStyleFingerprint : Fingerprint(
    definingClass = "LX/0EZq;",
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    strings = listOf("comment_sort_opt_style"),
)

internal object CommentSortEligibilityFingerprint : Fingerprint(
    definingClass = "LX/0qEG;",
    name = "LIZ",
    returnType = "Z",
    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/Aweme;"),
)
