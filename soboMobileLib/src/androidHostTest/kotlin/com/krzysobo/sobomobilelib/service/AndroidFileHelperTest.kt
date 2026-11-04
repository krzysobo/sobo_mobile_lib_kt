package com.krzysobo.sobomobilelib.service

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AndroidFileHelperTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun copyUriFileToCache_copiesTheContentIntoTheCacheDir() {
        val source = File(context.filesDir, "phrases.csv").apply {
            writeText("handle,pl,en\nwater,Woda,Water\n")
        }

        val copy = AndroidFileHelper().copyUriFileToCache(context, Uri.fromFile(source), "tmp_phrases.csv")

        assertEquals(File(context.cacheDir, "tmp_phrases.csv"), copy)
        assertTrue(copy.exists())
        assertEquals(source.readText(), copy.readText())
    }

    @Test
    fun copyUriFileToCache_overwritesAnExistingTempFile() {
        File(context.cacheDir, "tmp.bin").writeText("old, much longer content")
        val source = File(context.filesDir, "new.bin").apply { writeText("new") }

        val copy = AndroidFileHelper().copyUriFileToCache(context, Uri.fromFile(source), "tmp.bin")

        assertEquals("new", copy.readText())
    }
}
