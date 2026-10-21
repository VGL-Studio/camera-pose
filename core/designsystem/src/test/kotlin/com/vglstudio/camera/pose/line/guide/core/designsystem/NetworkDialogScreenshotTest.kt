package com.vglstudio.camera.pose.line.guide.core.designsystem

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.NetworkDialogScreen
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The network dialog, named after the design images in docs/features/network/design/screens (ADR-0020). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NetworkDialogScreenshotTest {
    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/NetworkDialogScreen_default_light.png") {
            Dialog(darkTheme = false, stillOffline = false)
        }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/NetworkDialogScreen_default_dark.png") {
            Dialog(darkTheme = true, stillOffline = false)
        }
    }

    /** Not on the design images: the line under the button after a retry that found no network. */
    @Test
    fun stillOffline_light() {
        captureRoboImage("src/test/screenshots/NetworkDialogScreen_stillOffline_light.png") {
            Dialog(darkTheme = false, stillOffline = true)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun stillOffline_small() {
        captureRoboImage("src/test/screenshots/NetworkDialogScreen_stillOffline_small.png") {
            Dialog(darkTheme = false, stillOffline = true)
        }
    }

    @Composable
    private fun Dialog(darkTheme: Boolean, stillOffline: Boolean) {
        AppTheme(darkTheme = darkTheme) {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NetworkDialogScreen(checking = false, stillOffline = stillOffline, onRetry = {})
            }
        }
    }
}
