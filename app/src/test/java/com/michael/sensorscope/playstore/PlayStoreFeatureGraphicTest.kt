package com.michael.sensorscope.playstore

import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Category(PlayStoreScreenshotTests::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class PlayStoreFeatureGraphicTest {
    @Test
    @Config(qualifiers = "w1024dp-h500dp-mdpi")
    fun feature_graphic() {
        capturePlayStoreImage("feature-graphic.png") {
            FeatureGraphicContent()
        }
    }
}
