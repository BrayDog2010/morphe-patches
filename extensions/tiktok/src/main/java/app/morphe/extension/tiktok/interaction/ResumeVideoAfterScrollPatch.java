package app.morphe.extension.tiktok.interaction;

import app.morphe.extension.tiktok.settings.Settings;

public final class ResumeVideoAfterScrollPatch {
    private ResumeVideoAfterScrollPatch() {
    }

    public static boolean shouldResumeVideoAfterScroll() {
        return Settings.RESUME_VIDEO_AFTER_SCROLL.get();
    }

    public static boolean shouldClearCompletedProgress(long position, long duration) {
        return Settings.RESUME_VIDEO_AFTER_SCROLL.get() && duration > 0 && position >= duration;
    }
}
