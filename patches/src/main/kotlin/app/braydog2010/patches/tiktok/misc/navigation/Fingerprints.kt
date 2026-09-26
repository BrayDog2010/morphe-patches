package app.braydog2010.patches.tiktok.misc.navigation

import app.morphe.patcher.Fingerprint

internal object TopTabModelListFingerprint : Fingerprint(
    definingClass = "/TabAbilityAssem;",
    name = "O9",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)

internal object BottomTabModelListFingerprint : Fingerprint(
    definingClass = "/TabAbilityAssem;",
    name = "kC",
    returnType = "Ljava/util/List;",
    parameters = emptyList(),
)
