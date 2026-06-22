package app.braydog2010.patches.venaboxhub

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.AccessFlags

// Matches admopub.a.t(ViewGroup, Activity) — the method that creates LevelPlayBannerAdView
// and calls loadAd(). Identified by its calls to adBrand.a.H() and ViewGroup.removeAllViews().
object AdBannerLoaderFingerprint : Fingerprint(
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Landroid/view/ViewGroup;", "Landroid/app/Activity;"),
    filters = listOf(
        methodCall(
            definingClass = "Lcom/dubani/dub/mvc/apptools/adBrand/a;",
            name = "H",
            returnType = "Z",
        ),
        methodCall(
            definingClass = "Landroid/view/ViewGroup;",
            name = "removeAllViews",
        ),
    ),
    custom = { _, classDef ->
        classDef.type == "Lcom/dubani/dub/mvc/apptools/admopub/a;"
    }
)
