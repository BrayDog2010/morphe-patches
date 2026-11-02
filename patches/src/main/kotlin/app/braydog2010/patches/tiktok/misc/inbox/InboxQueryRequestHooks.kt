package app.braydog2010.patches.tiktok.misc.inbox

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import app.braydog2010.patches.tiktok.shared.requireLocals
import app.braydog2010.util.addInstruction
import app.braydog2010.util.addInstructions
import app.braydog2010.util.addInstructionsWithLabels
import app.morphe.patcher.util.getReference
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

internal const val INBOX_REGULAR_VM = "Lcom/ss/android/ugc/aweme/notification/v2/viewmodel/NotificationResultViewModel;"
internal const val INBOX_CREATOR_VM = "Lcom/ss/android/ugc/aweme/notification/v2/viewmodel/NotificationCreatorResultViewModel;"
internal const val INBOX_WIDGET = "Lcom/ss/android/ugc/aweme/notification/v2/widget/NotificationWidget;"
internal const val INBOX_PAGE_LOADER = "Lcom/bytedance/ies/powerlist/page/PowerPageLoader;"

// Parameter type `LX/0NZp;` on TikTok 47.4.3 is `LX/0LZh;` on 47.6.2.
private val INBOX_PAGE_REQUEST_TYPES = setOf("LX/0NZp;", "LX/0LZh;")

/** One reviewed request seam: each variant is an owner and method name from one TikTok build. */
internal data class InboxRequestSpec(val variants: List<Pair<String, String>>, val parameters: List<String>,
    val tracked: Boolean = false, val closure: Boolean = false) {
    constructor(owner: String, name: String, parameters: List<String>, tracked: Boolean = false, closure: Boolean = false) :
        this(listOf(owner to name), parameters, tracked, closure)

    val counted = tracked || variants.any { it.first in setOf(INBOX_REGULAR_VM, INBOX_CREATOR_VM,
        "Lcom/ss/android/ugc/aweme/notification/v2/viewmodel/NotificationAllTabResultViewModel;") }

    fun matches(owner: String, name: String, parameterTypes: List<String>) =
        variants.any { it.first == owner && it.second == name } && parameterTypes.size == parameters.size &&
            parameterTypes.zip(parameters).all { (actual, wanted) ->
                actual == wanted || (wanted in INBOX_PAGE_REQUEST_TYPES && actual in INBOX_PAGE_REQUEST_TYPES) ||
                    (closure && actual == owner)
            }

    val fingerprint = Fingerprint(returnType = if (closure) "Ljava/lang/Object;" else "V",
        custom = { method, classDef ->
            matches(classDef.type, method.name, method.parameterTypes.map(CharSequence::toString))
        })
}

private const val INBOX_WIDGET_CONTAINER = "Lcom/ss/android/ugc/aweme/inbox/widget/v2/InboxWidgetContainer;"
private const val INBOX_ALL_TAB_VM = "Lcom/ss/android/ugc/aweme/notification/v2/viewmodel/NotificationAllTabResultViewModel;"
private const val INBOX_CLOSURE_47_4 = "Lkotlin/jvm/internal/AwS439S0200000_37_I1;"
private const val INBOX_CLOSURE_47_6 = "Lkotlin/jvm/internal/AwS432S0200000_31_I1;"

internal val loadedInboxRequestSpecs = listOf(
    InboxRequestSpec(listOf(INBOX_WIDGET_CONTAINER to "LJJIJIL", INBOX_WIDGET_CONTAINER to "LJJIJIIJIL"), emptyList()),
    InboxRequestSpec(listOf(INBOX_WIDGET to "LJJIJIL", INBOX_WIDGET to "LJJIJIIJIL"), emptyList()),
    InboxRequestSpec(listOf(INBOX_WIDGET to "Rp", INBOX_WIDGET to "Vp"), listOf("LX/0NZp;")),
    InboxRequestSpec(listOf(INBOX_REGULAR_VM to "m33", INBOX_REGULAR_VM to "H23"), emptyList(), tracked = true),
    InboxRequestSpec(listOf(INBOX_CREATOR_VM to "m33", INBOX_CREATOR_VM to "H23"), emptyList(), tracked = true),
    InboxRequestSpec(listOf(INBOX_REGULAR_VM to "n33", INBOX_REGULAR_VM to "I23"), listOf("LX/0NZp;")),
    InboxRequestSpec(listOf(INBOX_CREATOR_VM to "n33", INBOX_CREATOR_VM to "I23"), listOf("LX/0NZp;")),
    InboxRequestSpec(listOf(INBOX_ALL_TAB_VM to "n33", INBOX_ALL_TAB_VM to "I23"), listOf("LX/0NZp;")),
    InboxRequestSpec(listOf(INBOX_CLOSURE_47_4 to "invoke\$45", INBOX_CLOSURE_47_6 to "invoke\$48"),
        listOf(INBOX_CLOSURE_47_4), tracked = true, closure = true),
    InboxRequestSpec(listOf(INBOX_CLOSURE_47_4 to "invoke\$46", INBOX_CLOSURE_47_6 to "invoke\$49"),
        listOf(INBOX_CLOSURE_47_4), tracked = true, closure = true),
    InboxRequestSpec(INBOX_PAGE_LOADER, "LJII", listOf("Ljava/lang/Object;"), tracked = true),
    InboxRequestSpec(INBOX_PAGE_LOADER, "LJIIIIZZ", listOf("Ljava/lang/Object;"), tracked = true),
    InboxRequestSpec(INBOX_PAGE_LOADER, "LJIIIZ", emptyList(), tracked = true),
)

context(patchContext: BytecodePatchContext)
internal fun loadedInboxRequestSeams() = InboxRequestSeams(loadedInboxRequestSpecs.map { it to it.fingerprint.method })

internal class InboxRequestSeams(private val methods: List<Pair<InboxRequestSpec, MutableMethod>>) {
    init { methods.forEach { (spec, method) -> method.loadedRequestHook(spec, true) } }
    fun install() { methods.forEach { (spec, method) -> method.loadedRequestHook(spec) } }
}

internal fun MutableMethod.loadedRequestHook(spec: InboxRequestSpec, checkOnly: Boolean = false) {
    check(spec.matches(definingClass, name, parameterTypes.map(CharSequence::toString)))
    requireLocals("Loaded Inbox search", 1)
    val native = implementation!!.instructions.toList()
    val returns = native.withIndex().filter { it.value.opcode == if (spec.closure) Opcode.RETURN_OBJECT else Opcode.RETURN_VOID }
    check(returns.isNotEmpty())
    val launches = native.withIndex().filter { it.value.getReference<MethodReference>()?.let { reference ->
        reference.name == "LIZLLL" && ((reference.definingClass == "LX/03WS;" && reference.returnType == "LX/03Wz;") ||
            (reference.definingClass == "LX/03Sz;" && reference.returnType == "LX/03Tu;")) } == true }
    if (spec.tracked) check(launches.size == 1) { "Loaded Inbox request must launch exactly one reviewed native job" }
    if (spec.tracked) check(implementation!!.registerCount <= 16)
    if (checkOnly) return

    if (spec.counted) {
        // Return labels must land on the counter cleanup, including branches in the refresh
        // closure. This marks only synchronous method exit, never asynchronous job completion.
        returns.asReversed().forEach { returned ->
            val function = if (spec.closure) "returnedLoadedClosureRequest" else "returnedLoadedRequest"
            replaceInstruction(returned.index, "invoke-static/range {p0 .. p0}, $INBOX_QUERY_EXTENSION->$function(Ljava/lang/Object;)V")
            implementation!!.addInstruction(returned.index + 1, returned.value)
        }
    }
    if (spec.tracked) {
        // The returned Job was previously discarded at VM/loader entries. Capture that genuine
        // result. Existing refresh closures already move it into a native job field.
        val launch = launches.single().value
        val launchIndex = implementation!!.instructions.indexOf(launch)
        val resultInstruction = implementation!!.instructions[launchIndex + 1]
        val existingResult = resultInstruction.opcode == Opcode.MOVE_RESULT_OBJECT
        val result = if (existingResult) (resultInstruction as OneRegisterInstruction).registerA else 0
        val function = if (spec.closure) "onLoadedClosureRequestJob" else "onLoadedRequestJob"
        if (existingResult) addInstruction(launchIndex + 2,
            "invoke-static {p0, v$result}, $INBOX_QUERY_EXTENSION->$function(Ljava/lang/Object;Ljava/lang/Object;)V")
        else addInstructions(launchIndex + 1, """
            move-result-object v0
            invoke-static {p0, v0}, $INBOX_QUERY_EXTENSION->$function(Ljava/lang/Object;Ljava/lang/Object;)V
        """.trimIndent())
    }

    val guard = if (spec.closure) "beginLoadedClosureRequest" else if (spec.counted) "beginLoadedRequest" else "allowLoadedRequest"
    val refusal = if (spec.closure) "sget-object v0, Lkotlin/Unit;->LIZ:Lkotlin/Unit;\nreturn-object v0" else "return-void"
    addInstructionsWithLabels(0, """
        invoke-static/range {p0 .. p0}, $INBOX_QUERY_EXTENSION->$guard(Ljava/lang/Object;)Z
        move-result v0
        if-nez v0, :native_request
        $refusal
    """.trimIndent(), ExternalLabel("native_request", getInstruction(0)))
}
