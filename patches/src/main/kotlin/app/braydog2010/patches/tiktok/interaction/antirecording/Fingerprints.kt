package app.braydog2010.patches.tiktok.interaction.antirecording

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.string

private fun antiRecordingFingerprint(marker: String) = Fingerprint(
    definingClass = "/ClearModePanelComponent;",
    returnType = "V",
    parameters = listOf("I"),
    strings = listOf(marker).map { string(it, StringComparisonType.CONTAINS) },
)

internal val antiRecordingAddedFingerprint = antiRecordingFingerprint("[onDisplayAdded]")
internal val antiRecordingRemovedFingerprint = antiRecordingFingerprint("[onDisplayRemoved]")
