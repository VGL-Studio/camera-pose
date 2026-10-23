package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CaptureFileStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun newFile_deletesPreviousCaptures() {
        val dir = folder.newFolder("captures")
        val old1 = File(dir, "capture_1.jpg").apply { writeText("old") }
        val old2 = File(dir, "capture_2.jpg").apply { writeText("old") }

        val file = CaptureFileStore(dir).newFile(5)

        assertEquals(File(dir, "capture_5.jpg"), file)
        assertFalse(file.exists())
        assertFalse(old1.exists())
        assertFalse(old2.exists())
        assertEquals(0, dir.listFiles()!!.size)
    }

    @Test
    fun newFile_createsMissingDirectory() {
        val dir = File(folder.root, "cache/captures")

        val file = CaptureFileStore(dir).newFile(7)

        assertTrue(dir.isDirectory)
        assertEquals(File(dir, "capture_7.jpg"), file)
    }

    @Test
    fun delete_removesFileInDir() {
        val dir = folder.newFolder("captures")
        val file = File(dir, "capture_3.jpg").apply { writeText("photo") }

        CaptureFileStore(dir).delete(file)

        assertFalse(file.exists())
    }

    @Test
    fun delete_ignoresFileOutsideDir() {
        val dir = folder.newFolder("captures")
        val other = File(folder.newFolder("pictures"), "capture_3.jpg").apply { writeText("photo") }

        val inside = File(dir, "capture_4.jpg").apply { writeText("photo") }
        val store = CaptureFileStore(dir)

        store.delete(other)
        store.delete(inside)

        assertTrue(other.exists())
        // Control: the file in the capture folder is deleted by the same store.
        assertFalse(inside.exists())
    }
}
