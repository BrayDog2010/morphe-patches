package app.braydog2010.patches.venaboxhub

import app.braydog2010.patches.shared.Constants.COMPATIBILITY_VENABOX_HUB
import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.AccessFlags

// Matches admopub's master ad/feature gate: adBrand.a.H().
//
// H() reads several SharedPreferences flags and decides whether ads (and the
// related upsell UI) should be shown. It is the *only* public, no-argument
// boolean method on Lcom/dubani/dub/mvc/apptools/adBrand/a; that calls
// helper.b.h(), which makes it uniquely identifiable. The rest of the app reads
// this decision indirectly through adBrand.a.G() (used in 20+ call sites), so
// flipping H() flips the whole app into its ad-free / premium state.
internal object PremiumGateFingerprint : Fingerprint(
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
    description = "Forces the app into its ad-free premium state: removes banner and " +
        "interstitial ads and unlocks features that are gated locally. Note: content " +
        "that the server validates against a paid account may still require a subscription.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_VENABOX_HUB)

    execute {
        // Return false from the gate so the app behaves as a premium (ad-free) user.
        // v0 is a free local register (the method declares .locals 7).
        PremiumGateFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
    }
}
