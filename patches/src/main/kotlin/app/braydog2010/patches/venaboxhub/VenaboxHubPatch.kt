package app.braydog2010.patches.venaboxhub

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_VENABOX_HUB

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove Ads",
    description = "Removes ads from Venabox Hub.",
    default = true
) {
    compatibleWith(COMPATIBILITY_VENABOX_HUB)

    execute {
        // Insert return-void at instruction 0 of the banner loader.
        // The method body never executes — no LevelPlayBannerAdView is created,
        // no loadAd() is called, and no ad SDK network request is made.
        AdBannerLoaderFingerprint.method.addInstructions(0, "return-void")
    }
}
