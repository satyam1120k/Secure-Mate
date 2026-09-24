package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.securemate.ui.theme.SecureMateTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk =[34])
class SecureMateScreenshotTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun app_light_theme_screenshot() {
    composeTestRule.setContent {
      SecureMateTheme(darkTheme = false) {
        // TODO: Put the specific Composable screen you want to test here.
        // Example: LoginScreen() or DashboardScreen()
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/kp.png")
  }
}
