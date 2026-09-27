package app.braydog2010.patches.tiktok.feedfilter

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.TypeReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import app.morphe.patcher.patch.PatchException
import com.android.tools.smali.dexlib2.AccessFlags

private object MainEffectiveListFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/model/FeedItemList;",
    name = "getItems", parameters = emptyList(), returnType = "Ljava/util/List;",
)
private object WhyThisPostPanelFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythisvideo/AdjustableWhyThisVideoManager;",
    parameters = listOf("Landroid/content/Context;", "Lcom/ss/android/ugc/aweme/feed/model/Aweme;",
        "Lcom/ss/android/ugc/aweme/share/base/model/BaseSharePackage;",
        "Lcom/ss/android/ugc/aweme/feed/model/RecReasonsStruct;"),
    returnType = "V", strings = listOf("panel_source", "recReasons must be set before calling validateAndShow()"),
)
private object WhyThisPostResponseConsumerFingerprint : Fingerprint(
    name = "onResponse", returnType = "V", strings = listOf("why_this_post_api_error", "reasons_missing"),
)
private object WhyThisPostLayoutFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythispost/ui/ContainerFragment;",
    name = "onGlobalLayout", parameters = emptyList(), returnType = "V",
)
private object WhyThisPostValidationRequestCtorFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythisvideo/models/WTVValidationRequest;",
    name = "<init>",
    parameters = listOf("Lcom/ss/android/ugc/aweme/feed/model/RecReasonsStruct;"),
    returnType = "V",
)
private object WhyThisPostValidationResponseCtorFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythisvideo/WTVValidationResponse;",
    name = "<init>",
    parameters = listOf(
        "Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythisvideo/Extra;",
        "Lcom/ss/android/ugc/aweme/feed/model/LogPbBean;",
        "Lcom/ss/android/ugc/aweme/feed/model/RecReasonsStruct;",
        "I", "Ljava/lang/String;",
    ),
    returnType = "V",
)
internal val feedObservationProbeHooksPatch = bytecodePatch {
    dependsOn(sharedExtensionPatch)
    compatibleWith(COMPATIBILITY_TIKTOK)
    execute {
    val layout = WhyThisPostLayoutFingerprint.method
    // 46.8.3 no longer reads Aweme.getRecReasonsStruct inside onGlobalLayout;
    // the layout work moved into the private helper it calls. Re-anchor: observe
    // the shown item from the fragment's Aweme field at the top of onGlobalLayout.
    layout.addInstructionsAtControlFlowLabel(0,
        "iget-object v0, p0, Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythispost/ui/ContainerFragment;->LL:Lcom/ss/android/ugc/aweme/feed/model/Aweme;\n" +
        "invoke-static {v0}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->whyThisPostLayout(Ljava/lang/Object;)V")
        WhyThisPostPanelFingerprint.method.addInstruction(0,
            "invoke-static/range {p2 .. p4}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->whyThisPostPanel(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)V")
        val responseConsumer = WhyThisPostResponseConsumerFingerprint.method
        val responseCast = responseConsumer.implementation!!.instructions.withIndex().firstOrNull {
            it.value.opcode == Opcode.CHECK_CAST &&
                ((it.value as? ReferenceInstruction)?.reference as? TypeReference)?.type ==
                "Lcom/ss/android/ugc/tiktok/pns/feedsafety/whythisvideo/WTVValidationResponse;"
        } ?: throw PatchException("Why-this-post response consumer no longer casts the validation response")
        val responseRegister = (responseCast.value as OneRegisterInstruction).registerA
        responseConsumer.addInstruction(responseCast.index + 1,
            "invoke-static/range {v$responseRegister .. v$responseRegister}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->whyThisPostResponseConsumed(Ljava/lang/Object;)V")
        val profileTransform = ProfileNativeListTransformFingerprint.method
        val profileItemsRegister = if (AccessFlags.STATIC.isSet(profileTransform.accessFlags)) "p0" else "p1"
        profileTransform.addInstruction(0,
            "invoke-static {$profileItemsRegister}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->profileNativeTransform(Ljava/lang/Object;)V")
        val finalInsertion = FinalFeedInsertionFingerprint.method
        val finalInsertionReturns = finalInsertion.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }
        if (finalInsertionReturns.isEmpty()) {
            throw PatchException("Final feed insertion has no return to observe")
        }
        finalInsertionReturns.asReversed().forEach { index ->
            finalInsertion.addInstructionsAtControlFlowLabel(index,
                "invoke-static/range {p1 .. p1}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->finalInsert(Ljava/lang/Object;)V")
        }
    CacheChainDeliveryFingerprint.methodOrNull?.let { method ->
        method.addInstruction(0,
            "invoke-static/range {p1 .. p1}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->cacheChain(Ljava/lang/Object;)V")
    }
    listOf(
        FollowFeedPresenterPostProcessFingerprint.method to "followPost",
        ProfileDetailAdEventFingerprint.method to "profileDetail",
    ).forEach { (method, callback) ->
        method.addInstruction(0,
            "invoke-static/range {p1 .. p1}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->$callback(Ljava/lang/Object;)V")
    }
        SettingsStatusLoadFingerprint.method.addInstruction(0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->enable()V")
        listOf(MainEffectiveListFingerprint.method to "mainGetter",
            FollowFeedListGetItemsFingerprint.method to "followGetter").forEach { (method, callback) ->
            val returns = method.implementation!!.instructions.withIndex()
                .filter { it.value.opcode == Opcode.RETURN_OBJECT }.map { it.index }
            returns.asReversed().forEach { index ->
                val register = (method.implementation!!.instructions[index] as OneRegisterInstruction).registerA
                method.addInstructionsAtControlFlowLabel(index,
                    "invoke-static/range {v$register .. v$register}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->$callback(Ljava/lang/Object;)V")
            }
        }
        listOf(
            WhyThisPostValidationRequestCtorFingerprint.method to "whyThisPostRequest",
            WhyThisPostValidationResponseCtorFingerprint.method to "whyThisPostResponse",
        ).forEach { (method, callback) ->
            method.implementation!!.instructions.withIndex()
                .filter { it.value.opcode == Opcode.RETURN_VOID }.map { it.index }.asReversed()
                .forEach { index ->
                    method.addInstructionsAtControlFlowLabel(index,
                        "invoke-static/range {p0 .. p0}, Lapp/morphe/extension/tiktok/diagnostics/FeedObservationProbe;->$callback(Ljava/lang/Object;)V")
                }
        }
    }
}
