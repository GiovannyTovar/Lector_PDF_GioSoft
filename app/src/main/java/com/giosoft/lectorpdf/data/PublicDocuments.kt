package com.giosoft.lectorpdf.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val TAG = "PublicDocuments"

/**
 * Carpeta publica "Documentos/Mis PDF", donde la app guarda los escaneos y las
 * copias que el usuario pide explicitamente.
 *
 * Se usa MediaStore y no rutas de archivo: desde Android 10 una app puede
 * crear archivos en las carpetas publicas de documentos **sin ningun permiso**.
 * Eso mantiene la app libre de permisos peligrosos y, a la vez, deja los PDF
 * visibles para el explorador de archivos y para WhatsApp.
 */
object PublicDocuments {

    const val FOLDER_NAME = "Mis PDF"

    /** Ruta legible para mostrarsela al usuario. */
    val displayPath: String get() = "Documentos/$FOLDER_NAME"

    private val relativePath: String
        get() = "${Environment.DIRECTORY_DOCUMENTS}/$FOLDER_NAME"

    /** Nombre por defecto de un escaneo: "Documento 2026-09-13 14-30". */
    fun defaultScanName(): String {
        val stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm"))
        return "Documento $stamp"
    }

    /**
     * Copia [source] a "Documentos/Mis PDF" con el nombre indicado.
     *
     * Si ya existe un archivo con ese nombre, el sistema anade un sufijo
     * automaticamente en vez de sobrescribir.
     *
     * Devuelve la URI del archivo creado, que la app puede volver a abrir
     * mientras siga instalada, sin depender de permisos temporales.
     */
    suspend fun save(context: Context, source: Uri, name: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

                val pending = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, ensurePdfExtension(name))
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                    // Mientras esta "pendiente", otras apps no lo ven a medio escribir.
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val destination = resolver.insert(collection, pending)
                    ?: error("No se pudo crear el archivo en $displayPath")

                try {
                    resolver.openInputStream(source).use { input ->
                        requireNotNull(input) { "No se pudo leer el documento de origen" }
                        resolver.openOutputStream(destination).use { output ->
                            requireNotNull(output) { "No se pudo escribir en $displayPath" }
                            input.copyTo(output)
                        }
                    }
                } catch (e: Exception) {
                    // No dejar un archivo a medias en la carpeta del usuario.
                    runCatching { resolver.delete(destination, null, null) }
                    throw e
                }

                resolver.update(
                    destination,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null,
                )
                destination
            }.onFailure { Log.e(TAG, "Fallo al guardar en $displayPath", it) }
        }

    /**
     * Huella SHA-256 del contenido del archivo.
     *
     * Responde a "¿es el mismo documento?" sin depender del nombre ni de la
     * URI: dos archivos llamados igual pero distintos dan huellas distintas, y
     * el mismo documento reenviado por WhatsApp da siempre la misma. Es lo que
     * evita acumular "factura(1).pdf", "factura(2).pdf" al reabrirlo.
     */
    suspend fun contentHash(context: Context, uri: Uri): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val digest = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri).use { input ->
                    requireNotNull(input)
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }.onFailure { Log.d(TAG, "No se pudo calcular la huella de $uri: ${it.message}") }
                .getOrNull()
        }

    private fun ensurePdfExtension(name: String): String =
        if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
}
