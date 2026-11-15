package app.braydog2010.patches.tiktok.misc.settings

import app.braydog2010.util.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
import app.braydog2010.util.returnEarly
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.diagnostics.resolveShareTimingWrites
import app.braydog2010.util.addInstructionsAtControlFlowLabel
import app.morphe.patcher.util.numberOfParameterRegisters
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode

private const val CRASH_CAPTURE_INITIALIZER =
    "Lapp/morphe/extension/tiktok/diagnostics/JavaCrashCapture;->initialize(Landroid/content/Context;)V"

@Suppress("unused")
val enableOpenDebugPatch = bytecodePatch(
    name = "Diagnostic tools",
    description = "Adds logs and reports you can save or share when something goes wrong, so " +
        "a problem is easier to report and fix. Starts off. Turn it on in the patch settings > " +
        "Diagnostics.",
) {
    category("Settings")
    dependsOn(sharedExtensionPatch, settingsPatch)

    compatibleWith(COMPATIBILITY_TIKTOK)

    val recoveryTrial by booleanOption(
        key = "recoveryTrial",
        default = false,
        title = "Automatic recovery report trial",
        description = "Requested trials on Android 11 or later only: captures failures from up to " +
            "three process starts in 30 minutes. Reopen after a freeze to save a report in Downloads/Morphe. " +
            "Re-patching starts a fresh trial. No uploads or other settings changes. Leave off for normal use.",
    )

    execute {
        if (recoveryTrial == true) {
            mutableClassDefBy("Lapp/morphe/extension/shared/diagnostics/RecoveryCapture;").methods
                .single { it.name == "trialId" && it.parameterTypes.isEmpty() }
                .returnEarly("issue215-v1-${java.util.UUID.randomUUID()}")
        }
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableDiagnostics()V",
        )

        val npthExtent = NpthExtentTaskInitFingerprint.method
        // The fingerprint holds the context to the first and only parameter, so it is p0 on a
        // static method and p1 behind `this` on an instance one. Both are named here rather
        // than assumed: a frame past v15 cannot be reached by the plain invoke below.
        val contextRegister =
            if (npthExtent.accessFlags and AccessFlags.STATIC.value != 0) "p0" else "p1"
        val contextIndex = npthExtent.implementation!!.registerCount -
            npthExtent.numberOfParameterRegisters +
            (if (npthExtent.accessFlags and AccessFlags.STATIC.value != 0) 0 else 1)
        check(contextIndex <= 15) {
            "Diagnostics: NpthExtentTask.LIZ keeps its context in v$contextIndex, past what " +
                "the plain invoke can name."
        }
        val returnIndices = npthExtent.implementation!!.instructions.withIndex()
            .filter { it.value.opcode == Opcode.RETURN_VOID }
            .map { it.index }

        returnIndices.asReversed().forEach { returnIndex ->
            npthExtent.addInstructionsAtControlFlowLabel(
                returnIndex,
                "invoke-static {$contextRegister}, $CRASH_CAPTURE_INITIALIZER",
            )
        }
    }

    finalize {
        // Other selected patches can prepend share interception and add normal returns.
        // Time the completed native methods, including those optional extension paths.
        resolveShareTimingWrites()()
    }
}
