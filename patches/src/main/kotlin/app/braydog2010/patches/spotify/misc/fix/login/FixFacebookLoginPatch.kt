package app.braydog2010.patches.spotify.misc.fix.login

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.returnEarly

@Suppress("unused")
val fixFacebookLoginPatch = bytecodePatch(
    name = "Fix Facebook login",
    description = "Fix logging in with Facebook when the app is patched by always opening the login in a web browser window.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        KatanaProxyLoginMethodTryAuthorizeFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        }
    }
}
