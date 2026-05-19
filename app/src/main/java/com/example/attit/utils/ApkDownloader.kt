package com.example.attit.utils

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class ApkDownloader(private val context: Context) {

    private var downloadId: Long = -1

    fun downloadAndInstall(apkUrl: String, fileName: String = "ATT_it_update.apk") {
        Toast.makeText(context, "Downloading update in background...", Toast.LENGTH_SHORT).show()

        // 1. Setup the DownloadManager
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("ATT it Update")
            .setDescription("Downloading the latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        // 2. Register a receiver to know when the download finishes
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    // Download finished! Trigger the installation.
                    installApk(fileName)
                    context.unregisterReceiver(this) // Clean up
                }
            }
        }

        // Register receiver securely for modern Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        }
    }

    private fun installApk(fileName: String) {
        try {
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)

            if (!file.exists()) {
                Toast.makeText(context, "Update file not found", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // --- Check for "Unknown Sources" Permission (Android 8.0+) ---
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    // The user hasn't granted permission yet.
                    Toast.makeText(context, "Please allow ATT it to install updates", Toast.LENGTH_LONG).show()

                    // Auto-route them to the exact settings screen for your app
                    val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    return // Stop the installation crash.
                }
            }

            // If we have permission, trigger the install!
            context.startActivity(installIntent)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to start installation", Toast.LENGTH_SHORT).show()
        }
    }
}