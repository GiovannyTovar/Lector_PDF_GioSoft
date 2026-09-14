package com.giosoft.lectorpdf.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "Thumbnails"

/**
 * Miniatura de la primera pagina de un PDF.
 *
 * Esta apagado por defecto (ver [SettingsRepository.showThumbnails]) porque
 * obliga a abrir cada documento del historial. Se usa el renderizador del
 * sistema directamente y no el visor completo: aqui solo hace falta un mapa de
 * bits pequeno, no interaccion.
 *
 * Los documentos protegidos con contrasena no producen miniatura, y es lo
 * correcto: mostrar su contenido en la lista filtraria informacion que el
 * usuario quiso proteger.
 */
object Thumbnails {

    private const val WIDTH_PX = 220

    // Cache en memoria: al desplazar la lista se vuelve a pedir la misma
    // miniatura constantemente, y regenerarla cada vez seria muy costoso.
    private val cache = LruCache<String, Bitmap>(60)

    suspend fun firstPage(context: Context, uri: Uri): Bitmap? {
        cache.get(uri.toString())?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        if (renderer.pageCount == 0) return@use null
                        renderer.openPage(0).use { page ->
                            val height = (WIDTH_PX.toFloat() / page.width * page.height).toInt()
                            val bitmap = Bitmap.createBitmap(
                                WIDTH_PX,
                                height.coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            )
                            // Fondo blanco: un PDF sin fondo propio saldria
                            // transparente y en tema oscuro no se veria nada.
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            cache.put(uri.toString(), bitmap)
                            bitmap
                        }
                    }
                }
            } catch (e: SecurityException) {
                // Protegido con contrasena, o sin acceso ya.
                Log.d(TAG, "Sin miniatura para $uri: ${e.message}")
                null
            } catch (e: Exception) {
                Log.d(TAG, "No se pudo generar la miniatura de $uri: ${e.message}")
                null
            }
        }
    }

    fun clear() = cache.evictAll()
}
