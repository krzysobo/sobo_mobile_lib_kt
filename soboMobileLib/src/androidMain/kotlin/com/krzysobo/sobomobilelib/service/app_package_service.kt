package com.krzysobo.sobomobilelib.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.runtime.Composable
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.krzysobo.soboapptpl.service.AnyRes
import com.krzysobo.sobomobilelib.viewmodel.AppItem
import com.krzysobo.soboapptpl.viewmodel.AnyImage


/**
 * service for handling applications (filtering, querying, launching etc)
 */
class AppPackageService {
    val FILTER_MODE_GET_ALL = 5
    val FILTER_MODE_INCLUDE_SELECTED = 10
    val FILTER_MODE_EXCLUDE_SELECTED = 20

    @Composable
    fun getAllPackagesListAppItems(
        context: Context,
        appOwnPackageNameToOmit: String,
    ): List<AppItem> {
        return getFilteredPackagesListAppItems(
            context,
            listOf(),
            FILTER_MODE_GET_ALL,
            appOwnPackageNameToOmit
        )
    }

    @Composable
    fun getSelectedPackagesListAppItems(
        context: Context,
        pkgNames: List<String>,
        appOwnPackageNameToOmit: String,
    ): List<AppItem> {
        return getFilteredPackagesListAppItems(
            context,
            pkgNames,
            FILTER_MODE_INCLUDE_SELECTED,
            appOwnPackageNameToOmit
        )
    }

    @Composable
    fun getUnselectedPackagesListAppItems(
        context: Context,
        pkgNames: List<String>,
        appOwnPackageNameToOmit: String,
    ): List<AppItem> {
        return getFilteredPackagesListAppItems(
            context,
            pkgNames,
            FILTER_MODE_EXCLUDE_SELECTED,
            appOwnPackageNameToOmit
        )
    }

    @Composable
    private fun getFilteredPackagesListAppItems(
        context: Context,
        filteredPackageNames: List<String>,
        filterMode: Int,
        appOwnPackageNameToOmit: String = "",
    ): List<AppItem> {
        var resultPackages: MutableList<AppItem> = mutableListOf()
        val allAppItems = getPackagesListAsAppItems(context, appOwnPackageNameToOmit)
        for (appItem in allAppItems) {
            if (
                (filterMode == FILTER_MODE_GET_ALL) ||
                ((filterMode == FILTER_MODE_INCLUDE_SELECTED) &&
                        (appItem.packageName in filteredPackageNames)) ||
                ((filterMode == FILTER_MODE_EXCLUDE_SELECTED) &&
                        (appItem.packageName !in filteredPackageNames))
            ) {
                resultPackages.add(appItem)
            }
        }

        return resultPackages
    }

    @Composable
    fun getPackagesListAsAppItems(
        context: Context,
        appOwnPackageNameToOmit: String,
    ): List<AppItem> {
        var appsOut: MutableList<AppItem> = mutableListOf()
        val pkgs = getPackagesList(context)

//    val apps = context.packageManager.getInstalledApplications(PackageManager.MATCH_ALL)

        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val leanbackIntent =
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val resolveInfos =
            (context.packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.MATCH_ALL
            ) +
                    context.packageManager.queryIntentActivities(
                        leanbackIntent,
                        PackageManager.MATCH_ALL
                    ))
                .distinctBy { it.activityInfo.packageName }

        val defaultIcon = AnyImage(Icons.Default.Tv)
        // for ((index, app) in apps.withIndex()) {
        for ((index, resolveInfo) in resolveInfos.withIndex()) {
            val packageInfo = context.packageManager.getPackageInfo(
                resolveInfo.activityInfo.packageName,
                0
            ) // , PackageManager.GET_ACTIVITIES)
            val appInfo = packageInfo.applicationInfo ?: continue
            if ((appOwnPackageNameToOmit != "") && (appInfo.packageName == appOwnPackageNameToOmit)) {
                continue
            }

            val appName = appInfo.name
            val appLabel = context.packageManager.getApplicationLabel(appInfo)
            val appIcon: Drawable = context.packageManager.getApplicationIcon(appInfo)
            val appIconPainter = rememberDrawablePainter(appIcon)
            println("RESOLVEINTENT - PACKAGE: ${resolveInfo.activityInfo.packageName} ACTIV: ${resolveInfo.activityInfo.name}")
            val launchIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setComponent(
                    ComponentName(
                        resolveInfo.activityInfo.packageName,
                        resolveInfo.activityInfo.name
                    )
                )
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

//        val iconOut = defaultIcon
            val appItem = AppItem(
//            title = AnyRes("$index:: $appName:: $appLabel"),
                title = AnyRes("$appLabel"),
                image = AnyImage(appIconPainter),
                packageName = appInfo.packageName,
                launchIntent = launchIntent
            )

            appsOut.add(appItem)
        }

        return appsOut
    }

    fun getPackagesList(context: Context): List<PackageInfo> {
        try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
            return context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
                .mapNotNull { resolveInfo ->
                    val packageInfos: List<PackageInfo> =
                        context.packageManager.getInstalledPackages(
                            PackageManager.GET_ACTIVITIES
                        )
                    return packageInfos
                }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        return listOf()
    }
}
