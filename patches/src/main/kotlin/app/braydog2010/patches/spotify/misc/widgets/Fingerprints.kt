package app.braydog2010.patches.spotify.misc.widgets

import app.morphe.patcher.Fingerprint

/**
 * The media provider that serves album art to the home screen widgets. It only opens files for
 * callers that hold the BIND_APPWIDGET permission, which third party launchers do not.
 */
internal object CanBindAppWidgetPermissionFingerprint : Fingerprint(
    definingClass = "Lcom/spotify/externalintegration/service/provider/MediaProvider;",
    name = "openFile",
    strings = listOf("android.permission.BIND_APPWIDGET"),
)
