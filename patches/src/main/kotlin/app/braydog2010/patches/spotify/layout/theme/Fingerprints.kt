package app.braydog2010.patches.spotify.layout.theme

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags

internal object ColorSpaceUtilsClassFingerprint : Fingerprint(
    strings = listOf("The specified color must be encoded in an RGB color space."),
    custom = { method, _ ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.returnType == "F"
    },
)

internal object ParseLottieJsonFingerprint : Fingerprint(
    strings = listOf("Unsupported matte type: "),
)

internal object ParseAnimatedColorFingerprint : Fingerprint(
    strings = listOf("parse", "color"),
    definingClass = "/com/spotify/",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("F", "F"),
)
