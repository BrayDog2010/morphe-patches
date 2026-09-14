package app.braydog2010.patches.tiktok.interaction.resume

import app.morphe.patcher.Fingerprint
import app.braydog2010.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

// TikTok 46.8.3: the progress-cache holder X/0Lze was replaced by X/08SD (lazy providers +
// LruCache). The "continue caching progress" gate lambda is now X/08SF;->invoke()Object, which
// reads FeedPlayProgressContinueConfig.enable and returns Boolean.
internal object FeedProgressContinueGateFingerprint : Fingerprint(
    definingClass = "LX/08SF;",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    custom = custom@{ method, _ ->
        if (method.name != "invoke") return@custom false
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<FieldReference>()?.let { ref ->
                ref.definingClass == "Lcom/ss/android/ugc/aweme/feed/experiment/FeedPlayProgressContinueConfig;" &&
                    ref.name == "enable"
            } == true
        } == true
    },
)

internal object FeedPlayCompletedFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
    custom = custom@{ method, classDef ->
        classDef.type == "Lcom/ss/android/ugc/aweme/feed/controller/PlayerController;" &&
            method.name == "onPlayCompleted"
    },
)
