package app.braydog2010.patches.tiktok.interaction.feedmute

import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.interaction.blockauthor.PlayerPlayFingerprint
import app.braydog2010.patches.tiktok.interaction.blockauthor.awemeParameterRegister
import app.braydog2010.patches.tiktok.interaction.blockauthor.blockAuthorPatch
import app.braydog2010.patches.tiktok.interaction.blockauthor.resolveNativeFocus
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.inbox.MainActivityOnCreateFingerprint
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.braydog2010.patches.tiktok.misc.settings.settingsPatch
import app.braydog2010.patches.tiktok.shared.guardAtEntry
import app.braydog2010.util.addInstruction
import app.braydog2010.util.addInstructions
import app.braydog2010.util.cloneMutable
import app.braydog2010.util.singleOrPatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass as MutableClassDef
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.TypeReference

private const val PATCH = "Mute feed videos"
private const val FEED_MUTE = "Lapp/morphe/extension/tiktok/playback/FeedMute;"
private const val CONTEXT = "Landroid/content/Context;"
private const val SYNTHETIC_ABANDON = "hushfeedAbandonSessionFocus"

/**
 * TikTok's members the extension's bridges call, all renamed by each build and read here off
 * TikTok's own code:
 * - the feed engine's player field and the player type that declares `getSourceId()`, from the
 *   first thing the engine's play() reads off itself and casts;
 * - TTVideoEngine's public setIsMute forwarder, the `(Z)V` that calls the impl logging it;
 * - the player session's focus request, the `()V` beside its give-up that calls the manager's
 *   request.
 */
internal class FeedMuteMembers(
    val engine: String,
    val playerField: FieldReference,
    val player: String,
    val setMute: MethodReference,
    val session: String,
    val sessionAbandon: String,
    val sessionRequest: String,
    // True on builds that inlined the session's give-up into the player's release(), so the
    // patch adds that method to the session itself (see addSessionAbandon).
    val synthesizedAbandon: Boolean = false,
)

private fun Method.invokes(target: Method): Boolean =
    implementation?.instructions?.any { instruction ->
        val called = (instruction as? ReferenceInstruction)?.reference as? MethodReference
        called != null && called.definingClass == target.definingClass && called.name == target.name &&
            called.parameterTypes.map(CharSequence::toString) == target.parameterTypes.map(CharSequence::toString)
    } == true

private fun isInstance(method: Method) = !AccessFlags.STATIC.isSet(method.accessFlags)

private fun requirePublic(what: String, flags: Int) {
    if (!AccessFlags.PUBLIC.isSet(flags)) {
        throw PatchException("$PATCH: $what isn't public in this build, and the extension calls it from outside.")
    }
}

/**
 * Reads the members off the four fingerprinted methods. [classOf] gives a class or null; the
 * patch reads the app with it and the fixture test the APKs, so both run this code.
 */
internal fun resolveFeedMuteMembers(
    enginePlay: Method,
    setIsMuteImpl: Method,
    sessionAbandon: Method,
    focusRequest: Method,
    requestOwners: List<ClassDef> = emptyList(),
    classOf: (String) -> ClassDef?,
): FeedMuteMembers {
    val body = enginePlay.implementation ?: throw PatchException("$PATCH: the engine's play() has no body.")
    val instructions = body.instructions.toList()
    val self = body.registerCount - 1
    val read = instructions.indexOfFirst {
        it.opcode == Opcode.IGET_OBJECT && (it as TwoRegisterInstruction).registerB == self
    }
    if (read < 0) throw PatchException("$PATCH: ${enginePlay.definingClass}.play() reads no field of its own.")
    val field = (instructions[read] as ReferenceInstruction).reference as FieldReference
    val into = (instructions[read] as TwoRegisterInstruction).registerA
    val player = instructions.drop(read + 1).firstOrNull {
        it.opcode == Opcode.CHECK_CAST && (it as OneRegisterInstruction).registerA == into
    }?.let { ((it as ReferenceInstruction).reference as TypeReference).type }
        ?: throw PatchException("$PATCH: ${enginePlay.definingClass}.play() doesn't cast its ${field.name}.")
    val playerClass = classOf(player) ?: throw PatchException("$PATCH: $player isn't in this build.")
    val sourceId = playerClass.methods.singleOrNull {
        it.name == "getSourceId" && it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;"
    } ?: throw PatchException("$PATCH: $player, the engine's player, declares no getSourceId().")

    val engineBase = classOf(TT_VIDEO_ENGINE) ?: throw PatchException("$PATCH: $TT_VIDEO_ENGINE isn't in this build.")
    val forwarder = engineBase.methods.filter {
        isInstance(it) && it.returnType == "V" &&
            it.parameterTypes.map(CharSequence::toString) == listOf("Z") && it.invokes(setIsMuteImpl)
    }.singleOrPatchException("$PATCH: TTVideoEngine's forwarder to ${setIsMuteImpl.definingClass}->${setIsMuteImpl.name}")
    val isMute = engineBase.methods.singleOrNull {
        it.name == "isMute" && it.parameterTypes.isEmpty() && it.returnType == "Z" && isInstance(it)
    } ?: throw PatchException("$PATCH: TTVideoEngine has no isMute().")

    fun requestsIn(owner: ClassDef) = owner.methods.filter {
        isInstance(it) && it.returnType == "V" && it.parameterTypes.isEmpty() && it.invokes(focusRequest)
    }
    val abandonOwner = classOf(sessionAbandon.definingClass)
        ?: throw PatchException("$PATCH: ${sessionAbandon.definingClass} isn't in this build.")
    // 47.4.3 keeps the give-up and the request together. 47.6.2 inlined the give-up into the
    // player's release(), so the session is the one class whose ()V method makes the request.
    val together = requestsIn(abandonOwner).isNotEmpty()
    val session = if (together) abandonOwner else requestOwners.filter { requestsIn(it).isNotEmpty() }
        .singleOrPatchException("$PATCH: the class holding the player session's focus request")
    val request = requestsIn(session)
        .singleOrPatchException("$PATCH: the player session's focus request in ${session.type}")

    val engine = classOf(enginePlay.definingClass)!!
    requirePublic("the engine ${engine.type}", engine.accessFlags)
    requirePublic("the player ${playerClass.type}", playerClass.accessFlags)
    requirePublic("${playerClass.type}->getSourceId()", sourceId.accessFlags)
    requirePublic("TTVideoEngine", engineBase.accessFlags)
    requirePublic("TTVideoEngine->${forwarder.name}(Z)", forwarder.accessFlags)
    requirePublic("TTVideoEngine->isMute()", isMute.accessFlags)
    requirePublic("the player session ${session.type}", session.accessFlags)
    if (together) requirePublic("${session.type}->${sessionAbandon.name}()", sessionAbandon.accessFlags)
    requirePublic("${session.type}->${request.name}()", request.accessFlags)
    val declared = generateSequence(engine) { classOf(it.superclass ?: return@generateSequence null) }
        .firstNotNullOfOrNull { owner -> owner.fields.singleOrNull { it.name == field.name && it.type == field.type } }
        ?: throw PatchException("$PATCH: nothing above ${engine.type} declares ${field.name}.")
    requirePublic("${field.definingClass}->${field.name}", declared.accessFlags)

    return FeedMuteMembers(
        engine = engine.type,
        playerField = field,
        player = player,
        setMute = forwarder,
        session = session.type,
        sessionAbandon = if (together) sessionAbandon.name else SYNTHETIC_ABANDON,
        sessionRequest = request.name,
        synthesizedAbandon = !together,
    )
}

@Suppress("unused")
val feedMutePatch = bytecodePatch(
    name = "Mute feed videos",
    description = "Mutes feed videos without changing your phone's volume, so music from " +
        "another app keeps playing. You can also add a mute button on videos. Starts off. Turn it " +
        "on in the patch settings > App behavior.",
) {
    category("Playback")
    dependsOn(settingsPatch, sharedExtensionPatch, blockAuthorPatch)
    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableFeedMute()V",
        )
        MainActivityOnCreateFingerprint.method.addInstruction(
            0,
            "invoke-static/range { p0 .. p0 }, $FEED_MUTE->install(Landroid/app/Activity;)V",
        )

        // Which video a PlayerController asks for, before the engine for it plays. The Aweme is
        // p1 on both declared builds; a build that moves it gets a patch error, not a guess.
        PlayerPlayFingerprint.method.apply {
            val aweme = awemeParameterRegister()
            if (aweme != 1) throw PatchException("$PATCH: the play method's Aweme is p$aweme, not p1.")
            addInstruction(
                0,
                "invoke-static/range { p0 .. p1 }, $FEED_MUTE->onControllerPlay(Ljava/lang/Object;Ljava/lang/Object;)V",
            )
        }

        val focusRequest = SimAudioFocusRequestFingerprint.method
        val requestOwners = mutableListOf<ClassDef>()
        classDefForEach { classDef ->
            if (classDef.methods.any { isInstance(it) && it.returnType == "V" && it.parameterTypes.isEmpty() && it.invokes(focusRequest) }) {
                requestOwners += classDef
            }
        }
        val members = resolveFeedMuteMembers(
            FeedEnginePlayFingerprint.method,
            EngineSetIsMuteFingerprint.method,
            SimAudioSessionAbandonFingerprint.method,
            focusRequest,
            requestOwners,
        ) { classDefByOrNull(it) }

        // Every start of a feed video goes through the engine's play(); start() is hooked as
        // well, since it costs one line and nothing on these engines calls it today.
        val engine = mutableClassDefBy(members.engine)
        engine.methods.filter {
            (it.name == "play" || it.name == "start") && it.parameterTypes.isEmpty() && it.returnType == "V"
        }.also { if (it.none { method -> method.name == "play" }) throw PatchException("$PATCH: the engine lost play().") }
            .forEach {
                it.addInstruction(0, "invoke-static/range { p0 .. p0 }, $FEED_MUTE->onEnginePlay(Ljava/lang/Object;)V")
            }

        // The player session's lasting grant and the page helper's transient one, turned down
        // while muted with a feed video in front.
        val session = mutableClassDefBy(members.session)
        if (members.synthesizedAbandon) {
            addSessionAbandon(session, members.sessionAbandon, SimAudioSessionAbandonFingerprint.method)
        }
        session.methods.single { it.name == members.sessionRequest && it.parameterTypes.isEmpty() }
            .guardAtEntry(PATCH, "invoke-static/range { p0 .. p0 }, $FEED_MUTE->holdSessionFocus(Ljava/lang/Object;)Z", "return-void")
        val page = resolveNativeFocus()
        requirePublic("the page focus helper ${page.helper}", classDefBy(page.helper).accessFlags)
        requirePublic("${page.helper}->${page.request.name}(Context)", page.request.accessFlags)
        requirePublic("${page.helper}->${page.abandon.name}(Context)", page.abandon.accessFlags)
        page.request.guardAtEntry(PATCH, "invoke-static/range { p0 .. p0 }, $FEED_MUTE->holdPageFocus(Ljava/lang/Object;)Z", "return-void")

        installFeedMuteBridges(members, page.helper, page.request.name, page.abandon.name)
    }
}

/** Writes TikTok's members into FeedMute's bridge methods, whose own bodies serve the tests. */
private fun BytecodePatchContext.installFeedMuteBridges(
    members: FeedMuteMembers,
    pageHelper: String,
    pageRequest: String,
    pageAbandon: String,
) {
    val feedMute = mutableClassDefBy(FEED_MUTE)
    fun rewrite(name: String, locals: Int, body: String) {
        val original = feedMute.methods.filter { it.name == name }
            .singleOrPatchException("$PATCH: FeedMute's $name bridge")
        val bridge = original.cloneMutable(additionalRegisters = locals)
        feedMute.methods.remove(original)
        feedMute.methods.add(bridge)
        bridge.addInstructions(0, body)
    }
    val field = members.playerField
    rewrite("engineSourceId", 2, """
        check-cast p0, ${members.engine}
        iget-object v0, p0, ${field.definingClass}->${field.name}:${field.type}
        instance-of v1, v0, ${members.player}
        if-eqz v1, :none
        check-cast v0, ${members.player}
        invoke-virtual { v0 }, ${members.player}->getSourceId()Ljava/lang/String;
        move-result-object v0
        return-object v0
        :none
        const/4 v0, 0x0
        return-object v0
    """)
    rewrite("engineIsMute", 1, """
        check-cast p0, $TT_VIDEO_ENGINE
        invoke-virtual { p0 }, $TT_VIDEO_ENGINE->isMute()Z
        move-result v0
        return v0
    """)
    rewrite("setEngineMute", 1, """
        check-cast p0, $TT_VIDEO_ENGINE
        invoke-virtual { p0, p1 }, $TT_VIDEO_ENGINE->${members.setMute.name}(Z)V
        return-void
    """)
    for ((name, member) in listOf("abandonSessionFocus" to members.sessionAbandon, "requestSessionFocus" to members.sessionRequest)) {
        rewrite(name, 1, """
            check-cast p0, ${members.session}
            invoke-virtual { p0 }, ${members.session}->$member()V
            return-void
        """)
    }
    for ((name, member) in listOf("abandonPageFocus" to pageAbandon, "requestPageFocus" to pageRequest)) {
        rewrite(name, 1, """
            check-cast p0, $pageHelper
            invoke-virtual { p0, p1 }, $pageHelper->$member($CONTEXT)V
            return-void
        """)
    }
}

/**
 * 47.6.x inlined the session's give-up of audio focus into the player's release(), so the
 * session has no method to call. This adds one with that same code: it posts the focus
 * manager's abandon message (what 2) through the manager's own handler, then clears the
 * session's pending flags, exactly as release() does for its session. Every type and helper it
 * touches is read off release() and the session's own fields, so a rename between builds moves
 * with them, and a shape that changed gets a patch error and not a broken class.
 */
private fun BytecodePatchContext.addSessionAbandon(session: MutableClassDef, name: String, release: Method) {
    val type = session.type
    fun field(name: String, fieldType: String? = null, static: Boolean = false) = session.fields.singleOrNull {
        it.name == name && AccessFlags.STATIC.isSet(it.accessFlags) == static && (fieldType == null || it.type == fieldType)
    } ?: throw PatchException("$PATCH: $type->$name isn't in this build, and the session's give-up needs it.")
    field("LJIILIIL", "Z", static = true)
    field("LIZJ", "Z")
    field("LJI", "Z")
    field("LIZ", "F")
    field("LIZIZ", "F")
    val manager = field("LJFF").type
    val clock = field("LJIIL").type
    val handlerField = field("LJIIJ")
    if (handlerField.type != "Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;") {
        throw PatchException("$PATCH: $type->LJIIJ is no longer the focus handler.")
    }
    val managerClass = classDefByOrNull(manager)
        ?: throw PatchException("$PATCH: the focus manager $manager isn't in this build.")
    val checkStartField = managerClass.fields.singleOrNull { it.type == SIM_AUDIO_CHECK_START }
        ?: throw PatchException("$PATCH: $manager has no single checkStart field.")
    val checkStart = classDefByOrNull(SIM_AUDIO_CHECK_START)
        ?: throw PatchException("$PATCH: $SIM_AUDIO_CHECK_START isn't in this build.")
    if (checkStart.methods.none { it.name == "<init>" && it.parameterTypes.map(CharSequence::toString) == listOf(manager, "Landroid/os/Looper;") }) {
        throw PatchException("$PATCH: $SIM_AUDIO_CHECK_START no longer takes (manager, Looper).")
    }

    val calls = release.implementation?.instructions?.mapNotNull { (it as? ReferenceInstruction)?.reference as? MethodReference }.orEmpty()
    fun staticCall(parameters: List<String>, result: String) = calls.filter {
        it.returnType == result && it.parameterTypes.map(CharSequence::toString) == parameters &&
            classDefByOrNull(it.definingClass)?.methods?.any { method ->
                method.name == it.name && AccessFlags.STATIC.isSet(method.accessFlags) &&
                    method.parameterTypes.map(CharSequence::toString) == parameters
            } == true
    }.distinctBy { "${it.definingClass}->${it.name}" }.singleOrNull()
        ?: throw PatchException("$PATCH: release() has no single static ${parameters.joinToString("")}$result call.")
    val looper = staticCall(listOf("Landroid/os/HandlerThread;"), "Landroid/os/Looper;")
    val post = staticCall(listOf("Landroid/os/Handler;", "Landroid/os/Message;"), "Z")
    val messageInit = calls.filter { it.name == "<init>" && it.parameterTypes.size == 4 &&
        it.parameterTypes[0].toString() == "I" && it.parameterTypes[1].toString() == "Ljava/lang/Object;" &&
        it.parameterTypes[3].toString() == "Landroid/os/Handler;" }
        .distinctBy { it.definingClass }.singleOrNull()
        ?: throw PatchException("$PATCH: release() builds no single abandon message.")
    val message = messageInit.definingClass
    val callback = messageInit.parameterTypes[2].toString()
    fun descriptor(call: MethodReference) = "${call.definingClass}->${call.name}(${call.parameterTypes.joinToString("")})${call.returnType}"
    if (session.methods.any { it.name == name }) throw PatchException("$PATCH: $type already has $name.")

    val method = MutableMethod(
        ImmutableMethod(
            type, name, emptyList(), "V",
            AccessFlags.PUBLIC.value or AccessFlags.FINAL.value, null, null,
            ImmutableMethodImplementation(8, emptyList(), null, null),
        ),
    )
    method.addInstructions(
        0,
        """
            sget-boolean v0, $type->LJIILIIL:Z
            if-eqz v0, :reset
            iget-boolean v0, p0, $type->LIZJ:Z
            if-nez v0, :reset
            iget-object v1, p0, $type->LJFF:$manager
            iget-object v2, p0, $type->LJIIL:$clock
            iget-object v3, p0, $type->LJIIJ:Lcom/bytedance/bpea/transmit/delegate/BPEAHandler;
            iget-object v0, v1, $manager->${checkStartField.name}:$SIM_AUDIO_CHECK_START
            if-nez v0, :have
            new-instance v4, Landroid/os/HandlerThread;
            const-string v0, "SimAudioFocusManager"
            invoke-direct { v4, v0 }, Landroid/os/HandlerThread;-><init>(Ljava/lang/String;)V
            invoke-virtual { v4 }, Ljava/lang/Thread;->start()V
            invoke-static { v4 }, ${descriptor(looper)}
            move-result-object v4
            new-instance v0, $SIM_AUDIO_CHECK_START
            invoke-direct { v0, v1, v4 }, $SIM_AUDIO_CHECK_START-><init>(${manager}Landroid/os/Looper;)V
            iput-object v0, v1, $manager->${checkStartField.name}:$SIM_AUDIO_CHECK_START
            :have
            invoke-static {}, Landroid/os/Message;->obtain()Landroid/os/Message;
            move-result-object v4
            const/4 v5, 0x2
            iput v5, v4, Landroid/os/Message;->what:I
            new-instance v6, $message
            invoke-direct { v6, v5, p0, v2, v3 }, $message-><init>(ILjava/lang/Object;${callback}Landroid/os/Handler;)V
            iput-object v6, v4, Landroid/os/Message;->obj:Ljava/lang/Object;
            iget-object v0, v1, $manager->${checkStartField.name}:$SIM_AUDIO_CHECK_START
            invoke-static { v0, v4 }, ${descriptor(post)}
            :reset
            const/4 v0, 0x0
            iput-boolean v0, p0, $type->LJI:Z
            const/high16 v0, -0x40800000
            iput v0, p0, $type->LIZ:F
            iput v0, p0, $type->LIZIZ:F
            return-void
        """.trimIndent(),
    )
    session.methods.add(method)
}
