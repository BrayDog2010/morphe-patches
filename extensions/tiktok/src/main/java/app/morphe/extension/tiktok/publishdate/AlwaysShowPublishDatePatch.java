package app.morphe.extension.tiktok.publishdate;

import app.morphe.extension.tiktok.settings.Settings;

public final class AlwaysShowPublishDatePatch {
    private AlwaysShowPublishDatePatch() {
    }

    public static boolean showPostTimeForMainFeeds(boolean original) {
        return Settings.ALWAYS_SHOW_PUBLISH_DATE.get() ? false : original;
    }
}
