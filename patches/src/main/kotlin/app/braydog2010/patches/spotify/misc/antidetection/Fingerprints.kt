package app.braydog2010.patches.spotify.misc.antidetection

import app.morphe.patcher.Fingerprint

/*
 Ravelin SDK ("com.ravelin.*") is Spotify's third-party fraud / device-integrity
 vendor. Its class names are NOT obfuscated by Spotify (they ship as a normal
 library), so anchoring on the literal descriptor is stable across app updates.
*/

// Lcom/ravelin/core/util/security/RootCheckerNative;->b(Object[])Z
// wraps the native checkForRoot() call and returns whether the device looks rooted.
internal object RavelinRootCheckWrapperFingerprint : Fingerprint(
    definingClass = "Lcom/ravelin/core/util/security/RootCheckerNative;",
    name = "b",
    returnType = "Z",
)

// Shared "enqueue work request" entry point used by every Ravelin background worker
// (RavelinWorker / MobileReportWorker / RavelinFingerprintWorker), including device
// fingerprinting and mobile tamper/integrity reports. The class itself is obfuscated
// (renamed every Spotify build), so anchor on the stable literal string it emits when
// a duplicate enqueue is detected.
internal object RavelinEnqueueWorkFingerprint : Fingerprint(
    strings = listOf("This request is already enqueued"),
    returnType = "V",
)
