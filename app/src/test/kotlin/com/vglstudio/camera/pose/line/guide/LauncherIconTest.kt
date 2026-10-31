package com.vglstudio.camera.pose.line.guide

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.PI
import kotlin.math.hypot
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.w3c.dom.Element

/**
 * The launcher icon (spec loading, KB-6; plan T-087): an adaptive icon whose foreground artwork stays inside the
 * 66 dp safe zone of the 108 dp canvas, so no launcher mask cuts the camera or the photo card.
 * The working directory of a unit test is the module directory. NATIVE graphics, so BitmapFactory decodes real pixels.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LauncherIconTest {
    private val context: Application get() = ApplicationProvider.getApplicationContext()

    @Test
    fun mergedManifest_usesLauncherIcons() {
        val file = File(MERGED_MANIFEST)
        assertTrue("Missing ${file.absolutePath}; run :app:processStagingDebugMainManifest", file.isFile)
        val manifest = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(file)
        val application = manifest.getElementsByTagName("application").item(0) as Element
        assertEquals("@mipmap/ic_launcher", application.getAttributeNS(ANDROID_NS, "icon"))
        assertEquals("@mipmap/ic_launcher_round", application.getAttributeNS(ANDROID_NS, "roundIcon"))
    }

    @Test
    fun launcherIcon_isAdaptiveIcon() {
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val name = context.resources.getResourceEntryName(id)
            val icon = context.getDrawable(id)
            assertTrue("$name is ${icon?.javaClass}", icon is AdaptiveIconDrawable)
            icon as AdaptiveIconDrawable
            assertTrue("$name background is ${icon.background?.javaClass}", icon.background is GradientDrawable)
            // The layer the icon draws, not only the drawable resource: a wrong reference fails here.
            val background = icon.background as GradientDrawable
            assertArrayEquals("$name background colors", BACKGROUND_COLORS, background.colors)
            assertEquals("$name background direction", GradientDrawable.Orientation.TL_BR, background.orientation)
            assertTrue("$name foreground is ${icon.foreground?.javaClass}", icon.foreground is BitmapDrawable)
        }
    }

    @Test
    fun background_isOrangeGradientTopLeftToBottomRight() {
        val background = context.getDrawable(R.drawable.ic_launcher_background) as GradientDrawable
        assertArrayEquals(BACKGROUND_COLORS, background.colors)
        assertEquals(GradientDrawable.Orientation.TL_BR, background.orientation)
    }

    @Test
    fun foreground_hasLauncherSizeAtEveryDensity() {
        for ((density, scale) in DENSITIES) {
            val image = readForeground(density)
            val expected = (CANVAS_DP * scale).toInt()
            assertEquals("$density width", expected, image.width)
            assertEquals("$density height", expected, image.height)
        }
    }

    @Test
    fun foreground_staysInsideSafeZone() {
        for ((density, scale) in DENSITIES) {
            val image = readForeground(density)
            val center = image.width / 2.0
            // One pixel of slack for resampling at the edge of the artwork.
            val safeRadius = SAFE_ZONE_DP / 2 * scale + 1
            var opaque = 0
            for (y in 0 until image.height) {
                for (x in 0 until image.width) {
                    val alpha = image.getPixel(x, y) ushr 24
                    if (alpha == 0) continue
                    if (alpha == 255) opaque++
                    val distance = hypot(x + 0.5 - center, y + 0.5 - center)
                    assertTrue(
                        "$density: pixel ($x, $y) alpha $alpha is ${"%.1f".format(distance)} px from the center, " +
                            "outside the safe zone of ${"%.1f".format(safeRadius)} px",
                        distance <= safeRadius,
                    )
                }
            }
            // Control: an empty or faint image would pass the loop above.
            val safeArea = PI * (SAFE_ZONE_DP / 2 * scale) * (SAFE_ZONE_DP / 2 * scale)
            assertTrue("$density: only $opaque opaque pixels", opaque > safeArea * 0.25)
        }
    }

    @Test
    fun foreground_artworkIsOpaque() {
        val image = readForeground("xxxhdpi")
        assertEquals(255, image.getPixel(image.width / 2, image.height / 2) ushr 24)
    }

    private fun readForeground(density: String): Bitmap {
        val file = File("src/main/res/mipmap-$density/ic_launcher_foreground.png")
        assertTrue("Missing ${file.path}", file.isFile)
        // Unscaled and not premultiplied: the pixels exactly as stored in the file.
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPremultiplied = false
        }
        return BitmapFactory.decodeFile(file.path, options)
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val MERGED_MANIFEST =
            "build/intermediates/merged_manifest/stagingDebug/processStagingDebugMainManifest/AndroidManifest.xml"
        const val CANVAS_DP = 108.0
        const val SAFE_ZONE_DP = 66.0
        val BACKGROUND_COLORS = intArrayOf(0xFFFDBE0C.toInt(), 0xFFFD5E02.toInt())
        val DENSITIES = listOf(
            "mdpi" to 1.0,
            "hdpi" to 1.5,
            "xhdpi" to 2.0,
            "xxhdpi" to 3.0,
            "xxxhdpi" to 4.0,
        )
    }
}
