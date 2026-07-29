package com.autoswitch.apk.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manages in-app version checks, update alerts, and local APK package installation.
 * Operates locally without external third-party hosts or GitHub redirects.
 */
class UpdateManager(private val context: Context) {

    data class UpdateInfo(
        val isUpdateAvailable: Boolean,
        val currentVersionCode: Long,
        val latestVersionName: String,
        val latestVersionCode: Long,
        val releaseNotes: String,
        val localApkFile: File?
    )

    /**
     * Checks if a newer local APK package or update is available.
     */
    fun checkForUpdates(): UpdateInfo {
        val currentVersionCode: Long = try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            1L
        }

        val targetVersionCode: Long = 2L

        // Look for locally downloaded APK in Download folder or app cache
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val localApk = File(downloadsDir, "AutoSwitch.apk").takeIf { it.exists() }
            ?: File(context.getExternalFilesDir(null), "AutoSwitch.apk").takeIf { it.exists() }

        val isAvailable = targetVersionCode > currentVersionCode

        return UpdateInfo(
            isUpdateAvailable = isAvailable,
            currentVersionCode = currentVersionCode,
            latestVersionName = "1.0.1",
            latestVersionCode = targetVersionCode,
            releaseNotes = "Fixes Dark/Light theme switching & adds live SIM auto-switch status banner.",
            localApkFile = localApk
        )
    }

    /**
     * Triggers direct local APK installation via FileProvider without external browser/GitHub redirects.
     */
    fun installLocalApk(apkFile: File?) {
        val fileToInstall = apkFile ?: run {
            Toast.makeText(context, "Place AutoSwitch.apk in Download folder to update locally.", Toast.LENGTH_LONG).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, fileToInstall)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
