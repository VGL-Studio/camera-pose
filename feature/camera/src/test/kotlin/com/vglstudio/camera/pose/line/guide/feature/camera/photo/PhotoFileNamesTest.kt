package com.vglstudio.camera.pose.line.guide.feature.camera.photo

import java.io.File
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PhotoFileNamesTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun displayName_formatsInGivenZone() {
        assertEquals("CameraPose_19700101_000000.jpg", PhotoFileNames.displayName(0L, TimeZone.getTimeZone("UTC")))
        // 2026-10-23 01:02:03 UTC is 08:02:03 in Ho Chi Minh City.
        assertEquals(
            "CameraPose_20261023_080203.jpg",
            PhotoFileNames.displayName(1_792_717_323_000L, TimeZone.getTimeZone("Asia/Ho_Chi_Minh")),
        )
    }

    @Test
    fun uniqueFile_free_keepsName() {
        val dir = folder.newFolder("album")
        File(dir, "CameraPose_19700101_000001.jpg").writeText("other")
        assertEquals(
            File(dir, "CameraPose_19700101_000000.jpg"),
            PhotoFileNames.uniqueFile(dir, "CameraPose_19700101_000000.jpg"),
        )
        // Control: the name that is taken in the same folder gets a counter.
        assertEquals(
            File(dir, "CameraPose_19700101_000001_1.jpg"),
            PhotoFileNames.uniqueFile(dir, "CameraPose_19700101_000001.jpg"),
        )
    }

    @Test
    fun uniqueFile_taken_addsCounter() {
        val dir = folder.newFolder("album")
        File(dir, "CameraPose_19700101_000000.jpg").writeText("first")
        File(dir, "CameraPose_19700101_000000_1.jpg").writeText("second")
        assertEquals(
            File(dir, "CameraPose_19700101_000000_2.jpg"),
            PhotoFileNames.uniqueFile(dir, "CameraPose_19700101_000000.jpg"),
        )
    }
}
