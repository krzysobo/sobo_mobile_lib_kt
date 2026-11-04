package com.krzysobo.sobomobilelib.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.krzysobo.sobomobilelib.viewmodel.AppItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Integration test: AppPackageService queries the real (Robolectric) PackageManager for launcher
 * and leanback-launcher apps inside a composition, then filters them for the app grid.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppPackageServiceTest {
    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setUp() {
        // the service keeps the launchable apps for 60 s in a static cache - start every test fresh
        AppPackageService::class.java.getDeclaredField("launchableAppsCache").apply {
            isAccessible = true
            set(null, null)
        }
        installApp("com.vforb.dentalglass", "Dental Glass")             // the app itself
        installApp("com.vforb.dentalglasstalk", "Dental Glass Talk")
        installApp("com.google.android.youtube.tv", "YouTube", leanback = true)
        installApp("com.netflix.ninja", "Netflix", leanback = true)
        installApp("org.videolan.vlc", "VLC")
        installService("com.android.providers.settings")               // not launchable
    }

    private fun installApp(pkg: String, label: String, leanback: Boolean = false) {
        val pm = shadowOf(context.packageManager)
        val appInfo = ApplicationInfo().apply {
            packageName = pkg
            nonLocalizedLabel = label
        }
        pm.installPackage(PackageInfo().apply {
            packageName = pkg
            applicationInfo = appInfo
        })
        val component = ComponentName(pkg, "$pkg.MainActivity")
        pm.addActivityIfNotPresent(component)
        pm.addIntentFilterForActivity(component, IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(if (leanback) Intent.CATEGORY_LEANBACK_LAUNCHER else Intent.CATEGORY_LAUNCHER)
        })
    }

    private fun installService(pkg: String) {
        shadowOf(context.packageManager).installPackage(PackageInfo().apply {
            packageName = pkg
            applicationInfo = ApplicationInfo().apply { packageName = pkg }
        })
    }

    private fun appItems(query: @androidx.compose.runtime.Composable (Context) -> List<AppItem>): List<AppItem> {
        var items: List<AppItem> = emptyList()
        compose.setContent { items = query(LocalContext.current) }
        compose.waitForIdle()
        return items
    }

    @Test
    fun unselected_listsLauncherAndLeanbackApps_withoutOwnPackage() {
        val items = appItems {
            AppPackageService().getUnselectedPackagesListAppItems(it, listOf(), "com.vforb.dentalglass")
        }

        assertEquals(
            setOf("com.vforb.dentalglasstalk", "com.google.android.youtube.tv", "com.netflix.ninja", "org.videolan.vlc"),
            items.map { it.packageName }.toSet(),
        )
    }

    @Test
    fun unselected_excludesSelectedAndOmittedPackages() {
        val items = appItems {
            AppPackageService().getUnselectedPackagesListAppItems(
                it,
                pkgNames = listOf("org.videolan.vlc"),
                appOwnPackageNameToOmit = "com.vforb.dentalglass",
                appOtherPackageNamesToOmit = listOf("com.vforb.dentalglasstalk"),
            )
        }

        assertEquals(
            setOf("com.google.android.youtube.tv", "com.netflix.ninja"),
            items.map { it.packageName }.toSet(),
        )
    }

    @Test
    fun selected_returnsOnlySelectedPackages_privilegedFirst() {
        val items = appItems {
            AppPackageService().getSelectedPackagesListAppItems(
                it,
                pkgNames = listOf("org.videolan.vlc", "com.netflix.ninja", "com.google.android.youtube.tv"),
                appOwnPackageNameToOmit = "com.vforb.dentalglass",
                appPackagesToBePrivileged = listOf("com.google.android.youtube.tv"),
            )
        }

        assertEquals(3, items.size)
        assertEquals("com.google.android.youtube.tv", items.first().packageName)
    }

    @Test
    fun appItems_carryLabelAndLaunchIntent() {
        val items = appItems {
            AppPackageService().getSelectedPackagesListAppItems(it, listOf("org.videolan.vlc"), "")
        }

        val vlc = items.single()
        assertNotNull(vlc.launchIntent)
        val intent = vlc.launchIntent!!
        assertEquals(ComponentName("org.videolan.vlc", "org.videolan.vlc.MainActivity"), intent.component)
        assertEquals(Intent.ACTION_MAIN, intent.action)
        assertNotNull(vlc.title)
        assertNotNull(vlc.image)
    }
}
