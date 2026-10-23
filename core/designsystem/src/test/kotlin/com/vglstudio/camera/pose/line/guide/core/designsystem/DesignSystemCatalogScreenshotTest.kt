package com.vglstudio.camera.pose.line.guide.core.designsystem

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.AppColorCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.BrandCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.CameraComponentCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.ColorCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.PoseCardCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.PoseComponentCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.SettingsComponentCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.ShapeCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.SpacingAndComponentCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.catalog.TypographyCatalog
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Reference pages of the design system (ADR-0020). Designers rebuild the system in Claude Design or Figma from
 * these images and docs/design-system/tokens.json. The frame is the standard design frame, 411 x 891 dp.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class DesignSystemCatalogScreenshotTest {
    @Test
    fun colors_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_colors_light.png") {
            AppTheme(darkTheme = false) { ColorCatalog() }
        }
    }

    @Test
    fun colors_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_colors_dark.png") {
            AppTheme(darkTheme = true) { ColorCatalog() }
        }
    }

    @Test
    fun typography_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_typography_light.png") {
            AppTheme(darkTheme = false) { TypographyCatalog() }
        }
    }

    @Test
    fun spacingAndComponents_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_spacing_components_light.png") {
            AppTheme(darkTheme = false) { SpacingAndComponentCatalog() }
        }
    }

    @Test
    fun brand_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_brand_light.png") {
            AppTheme(darkTheme = false) { BrandCatalog() }
        }
    }

    @Test
    fun brand_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_brand_dark.png") {
            AppTheme(darkTheme = true) { BrandCatalog() }
        }
    }

    @Test
    fun appColors_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_app_colors_light.png") {
            AppTheme(darkTheme = false) { AppColorCatalog() }
        }
    }

    @Test
    fun appColors_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_app_colors_dark.png") {
            AppTheme(darkTheme = true) { AppColorCatalog() }
        }
    }

    @Test
    fun shapes_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_shapes_light.png") {
            AppTheme(darkTheme = false) { ShapeCatalog() }
        }
    }

    @Test
    fun spacingAndComponents_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_spacing_components_dark.png") {
            AppTheme(darkTheme = true) { SpacingAndComponentCatalog() }
        }
    }

    @Test
    fun cameraComponents_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_camera_components_dark.png") {
            AppTheme(darkTheme = true) { CameraComponentCatalog() }
        }
    }

    @Test
    fun poseComponents_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_pose_components_light.png") {
            AppTheme(darkTheme = false) { PoseComponentCatalog() }
        }
    }

    @Test
    fun poseComponents_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_pose_components_dark.png") {
            AppTheme(darkTheme = true) { PoseComponentCatalog() }
        }
    }

    @Test
    fun poseCards_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_pose_cards_light.png") {
            AppTheme(darkTheme = false) { PoseCardCatalog() }
        }
    }

    @Test
    fun settingsComponents_light() {
        captureRoboImage("src/test/screenshots/DesignSystem_settings_components_light.png") {
            AppTheme(darkTheme = false) { SettingsComponentCatalog() }
        }
    }

    @Test
    fun settingsComponents_dark() {
        captureRoboImage("src/test/screenshots/DesignSystem_settings_components_dark.png") {
            AppTheme(darkTheme = true) { SettingsComponentCatalog() }
        }
    }
}
