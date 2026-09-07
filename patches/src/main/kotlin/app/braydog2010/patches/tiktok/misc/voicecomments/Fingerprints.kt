package app.braydog2010.patches.tiktok.misc.voicecomments

import app.morphe.patcher.Fingerprint
import app.braydog2010.util.getReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

// Voice-comment publish entry gate. TikTok 46.8.3: the old X/0AkX;->LIZ()Z lambda gate was
// removed; the equivalent gate is X/09PD;->LJFF(CommentContextSource)Z which checks the
// "comment_audio_publish_entry_forbidden" AB flag plus keyboard/landscape/photo-page conditions.
internal object VoiceCommentPublishGateFingerprint : Fingerprint(
    definingClass = "LX/09PD;",
    name = "LJFF",
    returnType = "Z",
    parameters = listOf("Lcom/ss/android/ugc/aweme/comment/model/CommentContextSource;"),
    custom = custom@{ method, _ ->
        method.implementation?.instructions?.any { instruction ->
            instruction.getReference<StringReference>()?.let { ref ->
                ref.string == "comment_audio_publish_entry_forbidden"
            } == true
        } == true
    },
)
