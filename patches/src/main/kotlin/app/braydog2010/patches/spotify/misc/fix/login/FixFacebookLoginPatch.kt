package app.braydog2010.patches.spotify.misc.fix.login

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.returnEarly
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.findMutableMethodOf

@Suppress("unused")
val fixFacebookLoginPatch = bytecodePatch(
    name = "Fix Facebook login",
    description = "Fix logging in with Facebook when the app is patched by always opening the login in a web browser window.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        val handler = KatanaProxyLoginMethodHandlerFingerprint.classDef

        // tryAuthorize(request): Int. Returning 0 reports that the Facebook app login did not
        // start, so the SDK falls back to the web login.
        val tryAuthorize = handler.methods.singleOrNull {
            it.returnType == "I" && it.parameterTypes.size == 1 && it.implementation != null
        } ?: throw PatchException("Facebook login: no single tryAuthorize method in ${handler.type}")

        mutableClassDefBy(handler.type).findMutableMethodOf(tryAuthorize).returnEarly(0)
    }
}
