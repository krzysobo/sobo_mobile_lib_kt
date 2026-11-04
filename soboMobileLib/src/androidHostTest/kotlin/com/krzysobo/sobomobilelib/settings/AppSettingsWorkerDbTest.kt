package com.krzysobo.sobomobilelib.settings

import android.content.Context
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

/**
 * Integration test of the settings storage: AppSettingsWorkerDb -> AppSettingsDBHelper -> a real
 * SQLite database file in the public Documents directory (as on the TV set), run by Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSettingsWorkerDbTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dbName: String
    private lateinit var docsDir: File

    @Before
    fun setUp() {
        dbName = "TEST_SETTINGS_${UUID.randomUUID()}"
        docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        docsDir.mkdirs()    // exists on a real device
    }

    @After
    fun tearDown() {
        File(docsDir, dbName).delete()
        context.deleteDatabase(dbName)
    }

    private fun newWorker() = AppSettingsWorkerDb(context, null, dbName, 1)

    @Test
    fun freshDatabase_returnsDefaults() {
        val worker = newWorker()

        assertEquals("fallback", worker.getString("missing", "fallback"))
        assertTrue(worker.getBoolean("missing_bool", true))
        assertFalse(worker.getBoolean("missing_bool", false))
    }

    @Test
    fun putString_thenGetString() {
        val worker = newWorker()
        worker.putString("host", "http://192.168.1.20")

        assertEquals("http://192.168.1.20", worker.getString("host", ""))
        assertTrue("the DB file is created in Documents", File(docsDir, dbName).exists())
    }

    @Test
    fun putString_overwritesExistingValue() {
        val worker = newWorker()
        worker.putString("lang", "en")
        worker.putString("lang", "pl")
        worker.putString("lang", "tr")

        assertEquals("tr", worker.getString("lang", ""))
    }

    @Test
    fun booleans_roundTrip() {
        val worker = newWorker()
        worker.putBoolean("intro_displayed", true)
        assertTrue(worker.getBoolean("intro_displayed", false))

        worker.putBoolean("intro_displayed", false)
        assertFalse(worker.getBoolean("intro_displayed", true))
    }

    @Test
    fun booleans_areStoredAsOneAndZero() {
        val worker = newWorker()
        worker.putBoolean("flag", true)
        assertEquals("1", worker.getString("flag", ""))

        worker.putString("flag", "yes")   // anything but "1" reads as false
        assertFalse(worker.getBoolean("flag", true))
    }

    @Test
    fun keysAreIndependent() {
        val worker = newWorker()
        worker.putString("a", "1")
        worker.putString("b", "2")

        assertEquals("1", worker.getString("a", ""))
        assertEquals("2", worker.getString("b", ""))
        assertEquals("-", worker.getString("c", "-"))
    }

    @Test
    fun specialCharactersAreStoredVerbatim() {
        val worker = newWorker()
        val value = "Zażółć gęślą jaźń; 'quoted' \"double\" -- DROP TABLE x; 🦷"
        worker.putString("weird'key", value)

        assertEquals(value, worker.getString("weird'key", ""))
    }

    @Test
    fun valuesSurviveANewWorkerInstance() {
        newWorker().apply {
            putString("theme", "dark_ocean")
            putBoolean("was_intro_displayed", true)
        }

        val reopened = newWorker()
        assertEquals("dark_ocean", reopened.getString("theme", ""))
        assertTrue(reopened.getBoolean("was_intro_displayed", false))
    }

    @Test
    fun readsSeeWritesOfAnotherInstance() {
        val reader = newWorker()
        val writer = newWorker()
        writer.putString("x", "1")
        assertEquals("1", reader.getString("x", ""))   // reader opens its (cached) connection now

        writer.putString("x", "2")
        assertEquals("2", reader.getString("x", ""))   // ...and still sees later writes
    }

    @Test
    fun manyReadsAndWrites() {
        val worker = newWorker()
        repeat(50) { worker.putString("key_$it", "value_$it") }
        repeat(50) { assertEquals("value_$it", worker.getString("key_$it", "")) }
    }
}
