package app.braydog2010.patches.spotify.misc.premium

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val EXTENSION_CLASS_DESCRIPTOR =
    "Lapp/braydog2010/extension/spotify/misc/premium/PremiumAccountOverride;"

@Suppress("unused")
val premiumAccountOverride = bytecodePatch(
    name = "Premium Account Override",
    description = "(DONT NOT USE THIS IF YOU ARE PAYING FOR SPOTIFY PREMIUM) Rewrites the account attributes the server sends so the app sees a Premium account " +
        "with no ads. Features the server itself restricts, such as very high audio quality, " +
        "stay unavailable.",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        ProductStateAttributesFingerprint.method.apply {
            val returns = implementation!!.instructions.withIndex()
                .filter { it.value.opcode == Opcode.RETURN_OBJECT }
                .map { it.index }
                .asReversed()
            check(returns.isNotEmpty()) { "Product state getter has no return" }

            returns.forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstructions(
                    index,
                    "invoke-static { v$register }, $EXTENSION_CLASS_DESCRIPTOR->override(Ljava/util/Map;)V",
                )
            }
        }
    }
}
