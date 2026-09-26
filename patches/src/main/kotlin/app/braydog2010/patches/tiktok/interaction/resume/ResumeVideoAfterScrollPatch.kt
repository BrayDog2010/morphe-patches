/*
 Copyright 2026 icysymmetra/tiktok-patches-for-morphe contributors
https://github.com/icysymmetra/tiktok-patches-for-morphe
*/
package app.braydog2010.patches.tiktok.interaction.resume

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.smali.ExternalLabel
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_TIKTOK
import app.braydog2010.patches.tiktok.misc.extension.sharedExtensionPatch
import app.braydog2010.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint

private const val EXTENSION_DESCRIPTOR =
    "Lapp/morphe/extension/tiktok/interaction/ResumeVideoAfterScrollPatch;"

@Suppress("unused")
val resumeVideoAfterScrollPatch = bytecodePatch(
    name = "Resume videos after scrolling",
    description = "Continues supported videos from where playback stopped when returning after a scroll.",
    default = true,
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_TIKTOK)

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, " +
                "Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableResumeVideoAfterScroll()V",
        )

        // 46.8.3: the progress-cache gate is the lazy lambda X/08SF;->invoke()Object which reads
        // FeedPlayProgressContinueConfig.enable. Force it to continue when the extension wants resume.
        FeedProgressContinueGateFingerprint.method.let { method ->
            method.addInstructionsWithLabels(
                0,
                """
                invoke-static {},$EXTENSION_DESCRIPTOR->shouldResumeVideoAfterScroll()Z
                move-result v0
                if-eqz v0, :continue_gate
                const/4 v0, 0x1
                invoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;
                move-result-object v0
                return-object v0
                """,
                ExternalLabel("continue_gate", method.getInstruction(0)),
            )
        }

        // When a video completes, drop its saved position (mirrors the old X/0Lze cache-clear hook):
        // read the LruCache from the X/08SD;->LIZLLL lazy provider, remove the aid, reset the memos.
        FeedPlayCompletedFingerprint.method.let { method ->
            method.addInstructionsWithLabels(
                0,
                """
                invoke-static {},$EXTENSION_DESCRIPTOR->shouldResumeVideoAfterScroll()Z
                move-result v0
                if-eqz v0, :continue_completion
                sget-object v0, LX/08SD;->LIZLLL:LX/02ou;
                invoke-interface {v0}, LX/02ou;->getValue()Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Landroid/util/LruCache;
                move-object/from16 v1, p1
                invoke-virtual {v0, v1}, Landroid/util/LruCache;->remove(Ljava/lang/Object;)Ljava/lang/Object;
                const/4 v0, 0x0
                sput-object v0, LX/08SD;->LJ:LX/0GkD;
                sput-object v0, LX/08SD;->LJFF:Ljava/lang/String;
                """,
                ExternalLabel("continue_completion", method.getInstruction(0)),
            )
        }
    }
}
