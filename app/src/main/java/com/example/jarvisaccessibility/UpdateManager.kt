package com.example.jarvisaccessibility

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(
    private val context: Context
) {

    data class UpdateInfo(
        val hasUpdate: Boolean,
        val currentVersionCode: Int,
        val latestVersionCode: Int,
        val latestVersionName: String,
        val notes: String,
        val releaseUrl: String,
        val apkUrl: String
    )

    fun checkForUpdates(callback: (UpdateInfo?, String?) -> Unit) {
        Thread {
            try {
                val jsonUrl =
                    "https://raw.githubusercontent.com/andrei99989/JarvisAccessibility/main/version.json"

                val connection = URL(jsonUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                val latestVersionCode = json.getInt("versionCode")
                val latestVersionName = json.getString("versionName")
                val notes = json.optString("notes", "")
                val releaseUrl = json.getString("releaseUrl")
                val apkUrl = json.getString("apkUrl")

                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersionCode = packageInfo.versionCode

                val info = UpdateInfo(
                    hasUpdate = latestVersionCode > currentVersionCode,
                    currentVersionCode = currentVersionCode,
                    latestVersionCode = latestVersionCode,
                    latestVersionName = latestVersionName,
                    notes = notes,
                    releaseUrl = releaseUrl,
                    apkUrl = apkUrl
                )

                callback(info, null)
            } catch (e: Exception) {
                callback(null, e.message ?: "Eroare necunoscută la verificarea update-ului.")
            }
        }.start()
    }

    fun downloadAndInstallApk(apkUrl: String, callback: (String?) -> Unit) {
        Thread {
            try {
                val updatesDir = File(context.cacheDir, "updates")
                if (!updatesDir.exists()) {
                    updatesDir.mkdirs()
                }

                val apkFile = File(updatesDir, "JarvisAccessibility-update.apk")

                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"
                connection.instanceFollowRedirects = true

                connection.inputStream.use { input ->
                    apkFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                installApk(apkFile)
                callback(null)
            } catch (e: Exception) {
                callback(e.message ?: "Eroare necunoscută la download update.")
            }
        }.start()
    }

    private fun installApk(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }

    fun openReleasePage(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
