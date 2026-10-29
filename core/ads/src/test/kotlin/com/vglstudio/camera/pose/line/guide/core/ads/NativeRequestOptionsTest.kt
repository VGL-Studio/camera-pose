package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.nativead.NativeAdOptions
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The request options of every native ad (O-06): muted video, no custom controls, one image, landscape media; the
 * full-screen native keeps any aspect (plan T-027, question 1). One function builds the SDK options for every path
 * (L-072).
 */
@RunWith(AndroidJUnit4::class)
class NativeRequestOptionsTest {
    @Test
    fun everySize_startMuted_noCustomControls_singleImage() {
        NativeAdSize.entries.forEach { size ->
            val options = nativeRequestOptions(size)
            assertTrue("$size muted", options.startMuted)
            assertFalse("$size custom controls", options.customControls)
            assertFalse("$size multiple images", options.multipleImages)
        }
    }

    @Test
    fun mediumAndFull_landscapeMedia() {
        assertEquals(MediaAspectRatio.LANDSCAPE, nativeRequestOptions(NativeAdSize.MEDIUM).mediaAspect)
        assertEquals(MediaAspectRatio.LANDSCAPE, nativeRequestOptions(NativeAdSize.FULL).mediaAspect)
    }

    @Test
    fun card_portraitMedia() {
        // O-06: a card of a grid asks for portrait media; the other options are those of every size.
        val card = nativeRequestOptions(NativeAdSize.CARD)
        assertEquals(MediaAspectRatio.PORTRAIT, card.mediaAspect)
        assertEquals(nativeRequestOptions(NativeAdSize.MEDIUM).copy(mediaAspect = MediaAspectRatio.PORTRAIT), card)
    }

    @Test
    fun step_landscapeMedia_adChoicesTopRight() {
        // O-06: the 250 dp card of a flow step draws its media wide, like medium; O-08: AdChoices top right.
        val step = nativeRequestOptions(NativeAdSize.STEP)
        assertEquals(MediaAspectRatio.LANDSCAPE, step.mediaAspect)
        assertEquals(NativeAdOptions.ADCHOICES_TOP_RIGHT, step.adChoicesCorner)
        assertEquals(nativeRequestOptions(NativeAdSize.MEDIUM), step)
        assertEquals(NativeAdOptions.ADCHOICES_TOP_RIGHT, step.toNativeAdOptions().adChoicesPlacement)
    }

    @Test
    fun small_anyAspect() {
        // Small draws no media, so it does not narrow the ads it can get.
        assertEquals(MediaAspectRatio.ANY, nativeRequestOptions(NativeAdSize.SMALL).mediaAspect)
        // Control: the sizes with media ask for another aspect, so ANY above is not the value of every size.
        assertTrue(nativeRequestOptions(NativeAdSize.MEDIUM).mediaAspect != MediaAspectRatio.ANY)
    }

    @Test
    fun fullScreen_startMuted_noCustomControls_singleImage_anyAspect() {
        val options = fullScreenNativeRequestOptions()
        assertTrue(options.startMuted)
        assertFalse(options.customControls)
        assertFalse(options.multipleImages)
        assertEquals(MediaAspectRatio.ANY, options.mediaAspect)
    }

    @Test
    fun toNativeAdOptions_copiesEveryField() {
        val landscape = NativeRequestOptions(
            startMuted = true,
            customControls = false,
            multipleImages = false,
            mediaAspect = MediaAspectRatio.LANDSCAPE,
        ).toNativeAdOptions()
        assertEquals(NativeAdOptions.ADCHOICES_TOP_RIGHT, landscape.adChoicesPlacement)
        assertEquals(MediaAspectRatio.LANDSCAPE, landscape.mediaAspectRatio)
        assertFalse(landscape.shouldRequestMultipleImages())
        assertTrue(landscape.videoOptions!!.startMuted)
        assertFalse(landscape.videoOptions!!.customControlsRequested)

        // Control: the opposite values come through too, so the values above are copied, not defaults.
        val opposite = NativeRequestOptions(
            startMuted = false,
            customControls = true,
            multipleImages = true,
            mediaAspect = MediaAspectRatio.PORTRAIT,
        ).toNativeAdOptions()
        assertEquals(NativeAdOptions.ADCHOICES_TOP_RIGHT, opposite.adChoicesPlacement)
        assertEquals(MediaAspectRatio.PORTRAIT, opposite.mediaAspectRatio)
        assertTrue(opposite.shouldRequestMultipleImages())
        assertFalse(opposite.videoOptions!!.startMuted)
        assertTrue(opposite.videoOptions!!.customControlsRequested)
    }

    @Test
    fun collapsible_adChoicesTopLeft_otherwiseLikeMedium() {
        // The collapse button takes the top right corner of the layer, so the SDK pins AdChoices top left (plan T-030).
        val collapsible = collapsibleNativeRequestOptions()
        assertEquals(NativeAdOptions.ADCHOICES_TOP_LEFT, collapsible.adChoicesCorner)
        val medium = nativeRequestOptions(NativeAdSize.MEDIUM)
        assertEquals(medium.copy(adChoicesCorner = NativeAdOptions.ADCHOICES_TOP_LEFT), collapsible)
        // Control: every other native ad keeps the top right corner.
        NativeAdSize.entries.forEach { size ->
            assertEquals("$size", NativeAdOptions.ADCHOICES_TOP_RIGHT, nativeRequestOptions(size).adChoicesCorner)
        }
        assertEquals(NativeAdOptions.ADCHOICES_TOP_RIGHT, fullScreenNativeRequestOptions().adChoicesCorner)
    }

    @Test
    fun toNativeAdOptions_copiesAdChoicesCorner() {
        val collapsible = collapsibleNativeRequestOptions().toNativeAdOptions()
        assertEquals(NativeAdOptions.ADCHOICES_TOP_LEFT, collapsible.adChoicesPlacement)
        // Control: the default corner comes through as top right.
        assertEquals(
            NativeAdOptions.ADCHOICES_TOP_RIGHT,
            nativeRequestOptions(NativeAdSize.MEDIUM).toNativeAdOptions().adChoicesPlacement,
        )
    }

    @Test
    fun onlyNativeRequestOptions_buildsNativeAdOptions() {
        val sources = File(SOURCE_DIR).walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        // Control: the scan reads the module's sources, so an empty result below is not an empty folder.
        assertTrue("found ${sources.size} sources in $SOURCE_DIR", sources.any { it.name == "SdkNativeAdLoader.kt" })

        val builders = sources.filter { file ->
            file.readLines()
                .map { it.trim() }
                .filterNot { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }
                .any { it.contains(NATIVE_AD_OPTIONS_IMPORT) || it.contains("NativeAdOptions.Builder(") }
        }.map { it.name }
        assertEquals(listOf("NativeRequestOptions.kt"), builders)
    }

    private companion object {
        const val SOURCE_DIR = "src/main/kotlin"
        const val NATIVE_AD_OPTIONS_IMPORT = "import com.google.android.gms.ads.nativead.NativeAdOptions"
    }
}
