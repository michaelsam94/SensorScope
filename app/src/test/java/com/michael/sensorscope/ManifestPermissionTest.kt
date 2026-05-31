package com.michael.sensorscope

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ManifestPermissionTest {

    @Test
    fun manifestDeclaresNotificationPermissionForForegroundService() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue(
            "Foreground service notifications require POST_NOTIFICATIONS on Android 13+.",
            manifest.contains("android.permission.POST_NOTIFICATIONS")
        )
    }
}
