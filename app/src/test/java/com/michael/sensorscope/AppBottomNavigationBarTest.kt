package com.michael.sensorscope

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.michael.sensorscope.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [35])
class AppBottomNavigationBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun showsOnlyTheSelectedItemLabel() {
        composeTestRule.setContent {
            MyApplicationTheme {
                AppBottomNavigationBar(
                    navItems = listOf(
                        BottomNavItem("Sensors", Icons.Default.Sensors, Screen.Dashboard),
                        BottomNavItem("Power", Icons.Default.BatteryChargingFull, Screen.BatteryTelemetry)
                    ),
                    currentRoute = Screen.Dashboard,
                    onNavigate = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sensors").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("Power").assertCountEquals(0)
    }
}
