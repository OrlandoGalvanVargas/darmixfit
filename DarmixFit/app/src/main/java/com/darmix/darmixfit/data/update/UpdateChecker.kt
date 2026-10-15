package com.darmix.darmixfit.data.update

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Immutable
data class ReleaseInfo(
    val tag: String,
    val title: String,
    val notes: String,
    val apkUrl: String?,
    val apkSize: Long,
    val pageUrl: String
) {
    val version: String get() = tag.trimStart('v', 'V')
}

object UpdateChecker {
    const val OFFICIAL_PAGE = "https://darmixista.dev/darmixfit/"
    private const val API = "https://api.github.com/repos/OrlandoGalvanVargas/darmixfit/releases/latest"
    private const val INTERVAL_MS = 30 * 60 * 1000L
    private const val TIMEOUT_MS = 8_000

    private const val PREFS = "darmixfit_updates"
    private const val K_LAST_CHECK = "last_check"
    private const val K_ETAG = "etag"
    private const val K_TAG = "tag"
    private const val K_TITLE = "title"
    private const val K_NOTES = "notes"
    private const val K_APK = "apk_url"
    private const val K_SIZE = "apk_size"
    private const val K_PAGE = "page_url"

    suspend fun latest(context: Context): ReleaseInfo? = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        val now = System.currentTimeMillis()
        val last = prefs.getLong(K_LAST_CHECK, 0L)
        if (now - last in 0 until INTERVAL_MS) return@withContext cached(prefs)

        try {
            val connection = (URL(API).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "DarmixFit-Android")
                setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                if (prefs.contains(K_TAG)) prefs.getString(K_ETAG, null)?.let { setRequestProperty("If-None-Match", it) }
            }
            try {
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val info = parse(body)
                        store(prefs, info, connection.getHeaderField("ETag"), now)
                        info
                    }

                    HttpURLConnection.HTTP_NOT_MODIFIED -> {
                        prefs.edit { putLong(K_LAST_CHECK, now) }
                        cached(prefs)
                    }

                    HttpURLConnection.HTTP_NOT_FOUND -> {
                        store(prefs, null, null, now)
                        null
                    }

                    else -> {
                        prefs.edit { putLong(K_LAST_CHECK, now) }
                        cached(prefs)
                    }
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: IOException) {
            cached(prefs)
        } catch (_: RuntimeException) {
            cached(prefs)
        }
    }

    fun currentVersion(context: Context): String =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0"

    fun isNewer(candidate: String, current: String): Boolean {
        val a = numbers(candidate)
        val b = numbers(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    fun shouldPrompt(context: Context, info: ReleaseInfo): Boolean =
        isNewer(info.version, currentVersion(context))

    private fun numbers(version: String): List<Int> =
        Regex("\\d+").findAll(version).mapNotNull { it.value.toIntOrNull() }.toList()

    private fun parse(body: String): ReleaseInfo {
        val json = JSONObject(body)
        val assets = json.optJSONArray("assets")
        var apkUrl: String? = null
        var apkSize = 0L
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").takeIf { it.isNotBlank() }
                    apkSize = asset.optLong("size")
                    break
                }
            }
        }
        val tag = json.optString("tag_name")
        return ReleaseInfo(
            tag = tag,
            title = json.optString("name").ifBlank { tag },
            notes = json.optString("body"),
            apkUrl = apkUrl,
            apkSize = apkSize,
            pageUrl = json.optString("html_url").ifBlank { OFFICIAL_PAGE }
        )
    }

    private fun store(prefs: SharedPreferences, info: ReleaseInfo?, etag: String?, now: Long) {
        prefs.edit {
            putLong(K_LAST_CHECK, now)
            if (info == null) {
                remove(K_ETAG)
                remove(K_TAG)
                remove(K_TITLE)
                remove(K_NOTES)
                remove(K_APK)
                remove(K_SIZE)
                remove(K_PAGE)
            } else {
                if (etag != null) putString(K_ETAG, etag) else remove(K_ETAG)
                putString(K_TAG, info.tag)
                putString(K_TITLE, info.title)
                putString(K_NOTES, info.notes)
                putString(K_APK, info.apkUrl)
                putLong(K_SIZE, info.apkSize)
                putString(K_PAGE, info.pageUrl)
            }
        }
    }

    private fun cached(prefs: SharedPreferences): ReleaseInfo? {
        val tag = prefs.getString(K_TAG, null) ?: return null
        return ReleaseInfo(
            tag = tag,
            title = prefs.getString(K_TITLE, null) ?: tag,
            notes = prefs.getString(K_NOTES, null).orEmpty(),
            apkUrl = prefs.getString(K_APK, null),
            apkSize = prefs.getLong(K_SIZE, 0L),
            pageUrl = prefs.getString(K_PAGE, null) ?: OFFICIAL_PAGE
        )
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
