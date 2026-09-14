package app.braydog2010.patches.tiktok.misc.suggestedaccounts

import app.morphe.patcher.Fingerprint

internal object ProfileHeaderRecommendComponentFingerprint : Fingerprint(
    definingClass =
        "Lcom/ss/android/ugc/profile/platform/business/header/business/recommend/assemble/" +
            "ProfileHeaderRecommendComponent;",
    returnType = "V",
    parameters = emptyList(),
    strings = listOf("recommend_user_card"),
)
