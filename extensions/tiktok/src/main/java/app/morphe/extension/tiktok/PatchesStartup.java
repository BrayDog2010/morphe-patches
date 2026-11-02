package app.morphe.extension.tiktok;

import android.app.Activity;
import android.content.Context;
import app.morphe.extension.tiktok.download.MediaCache;
import app.morphe.extension.tiktok.navigation.HomeTabSettingsShortcut;
import app.morphe.extension.tiktok.privacy.BenchmarkRuns;
import app.morphe.extension.tiktok.settings.LegacySettings;

/** Shared startup keeps upgrade migrations and owned Android state independent of optional patches. */
public final class PatchesStartup {
    private PatchesStartup() {}
    public static void onContextReady(Context context) {
        LegacySettings.migrate(context);
        MediaCache.reconcileAsync(context);
    }
    public static void onActivity(Context context) {
        LegacySettings.migrate(context);
        HomeTabSettingsShortcut.install(context);
        if (context instanceof Activity) BenchmarkRuns.onAppOpened((Activity) context);
    }
}
