package com.darmix.darmixfit.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

object ExerciseImageStore {
    private const val DIR = "exercise_images"
    private const val ASSET_DIR = "exercises"
    private const val ASSET_PREFIX = "asset:"
    private val EXTENSION_PRIORITY = listOf("webp", "gif", "png", "jpg", "jpeg")

    @Volatile
    private var assetIndex: Map<String, String>? = null

    fun isBundled(name: String?): Boolean = name?.startsWith(ASSET_PREFIX) == true

    fun bundledRef(slug: String): String = "$ASSET_PREFIX$slug"

    fun fileFor(context: Context, name: String): File =
        File(File(context.filesDir, DIR), name)

    fun model(context: Context, name: String): Any? {
        if (!isBundled(name)) return fileFor(context, name)
        val slug = name.removePrefix(ASSET_PREFIX)
        val file = assets(context)[slug] ?: return null
        return "file:///android_asset/$ASSET_DIR/$file"
    }

    private fun assets(context: Context): Map<String, String> = assetIndex ?: synchronized(this) {
        assetIndex ?: buildIndex(context).also { assetIndex = it }
    }

    private fun buildIndex(context: Context): Map<String, String> {
        val files = runCatching { context.assets.list(ASSET_DIR)?.toList() }.getOrNull().orEmpty()
        return files
            .filter { it.substringAfterLast('.', "").lowercase() in EXTENSION_PRIORITY }
            .groupBy { it.substringBeforeLast('.') }
            .mapValues { (_, candidates) ->
                candidates.minBy { EXTENSION_PRIORITY.indexOf(it.substringAfterLast('.').lowercase()) }
            }
    }

    suspend fun import(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val extension = when (resolver.getType(source)) {
            "image/gif" -> "gif"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        val target = File(
            dir,
            "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$extension"
        )
        val input = resolver.openInputStream(source) ?: return@withContext null
        try {
            input.use { src -> target.outputStream().use { dst -> src.copyTo(dst) } }
            target.name
        } catch (e: Exception) {
            target.delete()
            null
        }
    }

    suspend fun delete(context: Context, name: String?) {
        if (name == null || isBundled(name)) return
        withContext(Dispatchers.IO) { fileFor(context, name).delete() }
    }
}
