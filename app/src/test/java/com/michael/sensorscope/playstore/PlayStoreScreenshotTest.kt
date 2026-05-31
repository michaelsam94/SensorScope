package com.michael.sensorscope.playstore

import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PHONE = "w360dp-h640dp-xxhdpi"
private const val TABLET = "w800dp-h1280dp-xhdpi"

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreScreenshotTest {
    @Test
    @Config(qualifiers = PHONE)
    fun phone_01_dashboard() {
        capturePlayStoreImage("phone/01_dashboard.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Dashboard)
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun phone_02_filters() {
        capturePlayStoreImage("phone/02_filters.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Filters)
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun phone_03_battery() {
        capturePlayStoreImage("phone/03_battery.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Battery)
        }
    }

    @Test
    @Config(qualifiers = PHONE)
    fun phone_04_sensor_detail() {
        capturePlayStoreImage("phone/04_sensor_detail.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Detail)
        }
    }

    @Test
    @Config(qualifiers = TABLET)
    fun tablet_01_dashboard() {
        capturePlayStoreImage("tablet/01_dashboard.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Dashboard)
        }
    }

    @Test
    @Config(qualifiers = TABLET)
    fun tablet_02_sensor_detail() {
        capturePlayStoreImage("tablet/02_sensor_detail.png") {
            PlayStoreScreenshotFrame(PlayStoreScene.Detail)
        }
    }
}
