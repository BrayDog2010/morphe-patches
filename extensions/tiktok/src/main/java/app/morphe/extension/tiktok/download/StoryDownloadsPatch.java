package app.morphe.extension.tiktok.download;

import com.ss.android.ugc.aweme.feed.model.Aweme;

import app.morphe.extension.tiktok.settings.Settings;

/** Runtime gate used by the exact-version story share-menu hooks. */
public final class StoryDownloadsPatch {
    private StoryDownloadsPatch() {
    }

    public static boolean shouldShowForAweme(Aweme aweme) {
        if (!Settings.DOWNLOAD_STORIES.get() || aweme == null) {
            return false;
        }

        try {
            return aweme.getIsTikTokStory();
        } catch (Throwable throwable) {
            return false;
        }
    }
}
