package com.darmix.darmixfit.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Guarda las imágenes de los ejercicios en el almacenamiento interno de la app. */
object ExerciseImageStore {
    private const val DIR = "exercise_images"

    fun fileFor(context: Context, name: String): File =
        File(File(context.filesDir, DIR), name)

    /** Copia la imagen tal cual (sin recomprimir, para que los GIF sigan animados). */
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
        if (name == null) return
        withContext(Dispatchers.IO) { fileFor(context, name).delete() }
    }
}