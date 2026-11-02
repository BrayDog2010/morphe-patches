package app.braydog2010.patches.spotify.misc.antidetection

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.braydog2010.patches.shared.Constants.COMPATIBILITY_SPOTIFY
import app.braydog2010.patches.spotify.misc.extension.sharedExtensionPatch
import app.braydog2010.util.returnEarly

@Suppress("unused")
val spoofDeviceIntegrityPatch = bytecodePatch(
    name = "Spoof device integrity",
    description = "Prevents Spotify's third-party fraud/device-integrity from detecting root/tamper signals or reporting device fingerprint and mobile integrity data back to Spotify's servers",
) {
    dependsOn(sharedExtensionPatch)

    compatibleWith(COMPATIBILITY_SPOTIFY)

    execute {
        // Force the root-check wrapper to always report "not rooted".
        RavelinRootCheckWrapperFingerprint.methodOrNull?.returnEarly(false)

        // Neuter every Ravelin background worker (device fingerprinting, mobile
        // tamper/integrity reports) by making their shared enqueue entry point a no-op,
        // so no report ever leaves the device.
        RavelinEnqueueWorkFingerprint.methodOrNull?.apply {
            addInstructions(
                0,
                """
                    return-void
                """,
            )
        }
    }
}
