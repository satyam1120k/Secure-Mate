package com.example.securemate.security.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.example.securemate.domain.model.AppSecurityInfo
import com.example.securemate.security.risk.SecurityRiskEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppSecurityAnalyzer(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun getInstalledAppsSecurity(): List<AppSecurityInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppSecurityInfo>()

        try {
            val flags = PackageManager.GET_PERMISSIONS

            val installedPackages: List<PackageInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledPackages(flags)
            }

            for (pkg in installedPackages) {
                // Ignore self
                if (pkg.packageName == context.packageName) continue

                val appInfo: ApplicationInfo? = pkg.applicationInfo
                val appName = if (appInfo != null) {
                    try {
                        packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkg.packageName
                    }
                } else {
                    pkg.packageName
                }

                val isSystem = appInfo?.let {
                    (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                            (it.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                } ?: false

                val isDebuggable = appInfo?.let {
                    (it.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                } ?: false

                val targetSdk = appInfo?.targetSdkVersion
                val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    appInfo?.minSdkVersion
                } else null

                val requestedPerms: List<String> = pkg.requestedPermissions?.toList() ?: emptyList()

                val versionCode: Long? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                // Installation source where available
                val installer = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        packageManager.getInstallSourceInfo(pkg.packageName).installingPackageName
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getInstallerPackageName(pkg.packageName)
                    }
                } catch (e: Exception) {
                    null
                }

                val evaluation = SecurityRiskEngine.evaluateAppRisk(
                    packageName = pkg.packageName,
                    appName = appName,
                    targetSdk = targetSdk,
                    isSystemApp = isSystem,
                    isDebuggable = isDebuggable,
                    requestedPermissions = requestedPerms
                )

                result.add(
                    AppSecurityInfo(
                        packageName = pkg.packageName,
                        appName = appName,
                        versionName = pkg.versionName,
                        versionCode = versionCode,
                        targetSdk = targetSdk,
                        minSdk = minSdk,
                        isSystemApp = isSystem,
                        isDebuggable = isDebuggable,
                        requestedPermissions = requestedPerms,
                        dangerousPermissions = evaluation.dangerousPermissions,
                        riskScore = evaluation.riskScore,
                        riskLevel = evaluation.riskLevel,
                        riskReasons = evaluation.riskReasons,
                        installSource = installer
                    )
                )
            }
        } catch (e: Exception) {
            // Fallback gracefully on security or platform error
        }

        // Sort by risk score descending so high risk apps are prominently placed
        result.sortedByDescending { it.riskScore }
    }

    fun createLaunchIntent(packageName: String): Intent? {
        return packageManager.getLaunchIntentForPackage(packageName)
    }

    fun createAppDetailsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createUninstallIntent(packageName: String): Intent {
        return Intent(Intent.ACTION_DELETE).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
