package com.darmix.darmixfit.data.update

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.net.toUri

data class DownloadProgress(val status: Int, val downloaded: Long, val total: Long) {
    val fraction: Float? get() = if (total > 0) (downloaded.toFloat() / total).coerceIn(0f, 1f) else null
}

object ApkUpdater {
    private const val MIME = "application/vnd.android.package-archive"
    private const val PREFIX = "darmixfit-"

    fun enqueue(context: Context, info: ReleaseInfo): Long {
        val url = requireNotNull(info.apkUrl)
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?.listFiles { file -> file.name.startsWith(PREFIX) && file.name.endsWith(".apk") }
            ?.forEach { it.delete() }
        val request = DownloadManager.Request(url.toUri())
            .setTitle("DarmixFit ${info.version}")
            .setDescription("Descargando actualización")
            .setMimeType(MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "$PREFIX${info.version}.apk")
        return manager(context).enqueue(request)
    }

    fun progress(context: Context, id: Long): DownloadProgress? {
        manager(context).query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            return DownloadProgress(status, downloaded, total)
        }
        return null
    }

    fun uriFor(context: Context, id: Long): Uri? = manager(context).getUriForDownloadedFile(id)

    fun cancel(context: Context, id: Long) {
        manager(context).remove(id)
    }

    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
        }
    }

    fun install(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun manager(context: Context): DownloadManager =
        context.getSystemService(DownloadManager::class.java)
}
