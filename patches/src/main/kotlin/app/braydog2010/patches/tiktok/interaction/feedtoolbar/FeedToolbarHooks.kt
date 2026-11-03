package app.braydog2010.patches.tiktok.interaction.feedtoolbar

import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.braydog2010.util.addInstructionsAtControlFlowLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private const val FEATURE_CONTROLS_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/featurecontrols/FeatureControls;"

internal fun MutableMethod.overrideToolbarButtonEnabled(extensionMethodName: String) {
    val returnIndices = implementation!!.instructions.withIndex()
        .filter { (_, instruction) -> instruction.opcode == Opcode.RETURN }
        .map { (index, _) -> index }

    if (returnIndices.isEmpty()) {
        throw PatchException(
            "Found no return to wrap in $this, so $extensionMethodName would never run.",
        )
    }

    returnIndices
        .asReversed()
        .forEach { returnIndex ->
            val returnRegister = getInstruction<OneRegisterInstruction>(returnIndex).registerA
            addInstructionsAtControlFlowLabel(
                returnIndex,
                """
                    invoke-static/range { v$returnRegister .. v$returnRegister }, $FEATURE_CONTROLS_DESCRIPTOR->$extensionMethodName(Z)Z
                    move-result v$returnRegister
                """,
            )
        }
}
