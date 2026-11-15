package app.braydog2010.patches.spotify.layout.theme

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal object ColorSpaceUtilsClassFingerprint : Fingerprint(
    strings = listOf("The specified color must be encoded in an RGB color space. The supplied color space is "),
    custom = { method, _ ->
        AccessFlags.STATIC.isSet(method.accessFlags) &&
            method.returnType == "F"
    },
)

internal object ParseLottieJsonFingerprint : Fingerprint(
    strings = listOf("Unsupported matte type: "),
)

/**
 * The Lottie color parser: it reads the four color channels, builds the color with Color.argb and
 * boxes it. The gradient parser also calls Color.argb but fills a list first, so it is excluded.
 * The class and method names are obfuscated and change on every release.
 */
internal object ParseAnimatedColorFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    custom = { method, _ ->
        val parameters = method.parameterTypes.map(CharSequence::toString)
        val calls = method.implementation?.instructions?.mapNotNull {
            (it as? ReferenceInstruction)?.reference as? MethodReference
        }.orEmpty().map { "${it.definingClass}->${it.name}" }
        parameters.size == 2 && parameters[1] == "F" && parameters[0].startsWith("Lp/") &&
            "Landroid/graphics/Color;->argb" in calls && "Ljava/lang/Integer;->valueOf" in calls &&
            "Ljava/util/ArrayList;-><init>" !in calls
    },
)
