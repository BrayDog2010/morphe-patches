package app.braydog2010.patches.venaboxhub

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_VENABOX_HUB
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags

// adBrand.a.H() is the ad gate (returns true when ads should be shown). It is the
// only public, no-argument boolean method on adBrand.a that calls helper.b.h(),
// which makes it uniquely identifiable. Forced to false for an ad-free experience.
internal object AdGateFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "Z",
    parameters = emptyList(),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/dubani/dub/mvc/helper/b;",
            name = "h",
            returnType = "Z",
        ),
    ),
    custom = { _, classDef ->
        classDef.type == "Lcom/dubani/dub/mvc/apptools/adBrand/a;"
    },
)

@Suppress("unused")
val premiumPatch = bytecodePatch(
    name = "Premium",
    description = "Unlocks premium. Forces the app's account/entitlement checks to report an " +
        "active subscription so locked content and premium-only features become available, and " +
        "removes ads. Note: media that the server streams and authorizes per-account may still " +
        "depend on the backend honouring the account.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_VENABOX_HUB)

    execute {
        // The real premium gates live on the session helper, com.dubani.dub.mvc.helper.b:
        //
        //   h()Z  – the master entitlement check, called in ~90 places to decide whether
        //           content/features are unlocked. At every gate the app does
        //           `if-eqz <h()>, :locked`, i.e. true = entitled. Force it true.
        //   g()Z  – reports an active subscription (returns CxgrBean.val == 1, the value the
        //           server sets after verifying a purchase in sub.a.k()). Force it true.
        //
        // Both are public, static, no-argument boolean methods. .locals leaves v0 free.
        val sessionHelper = mutableClassDefBy("Lcom/dubani/dub/mvc/helper/b;")
        sequenceOf("h", "g").forEach { methodName ->
            sessionHelper.methods.first {
                it.name == methodName && it.returnType == "Z" && it.parameterTypes.isEmpty()
            }.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }

        // Ad-free: force the ad gate to report "do not show ads".
        AdGateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
