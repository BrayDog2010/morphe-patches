package app.braydog2010.patches.tiktok.interaction.autoscroll

import app.morphe.patcher.Fingerprint

internal object AutoScrollFeatureGateFingerprint : Fingerprint(
    definingClass = "Load/n1;",
    name = "LIZ",
    returnType = "Z",
    parameters = emptyList(),
    strings = listOf("fyp_auto_scroll"),
)

internal object AutoScrollActionFactoryFingerprint : Fingerprint(
    definingClass = "LX/1JTr;",
    name = "LJI",
    returnType = "LX/165Y;",
    parameters = listOf("LX/165i;"),
    strings = listOf("panel_auto_scroll", "auto_scroll"),
)