package app.braydog2010.patches.spotify.misc.check

import app.braydog2010.patches.shared.misc.extension.ExtensionHook
import app.braydog2010.patches.spotify.shared.SpiritualMainActivityOnCreateFingerprint

internal val checkEnvironmentPatch = ExtensionHook(
    fingerprint = SpiritualMainActivityOnCreateFingerprint,
)
