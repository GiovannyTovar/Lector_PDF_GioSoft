package com.giosoft.pdf.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "MediaStoreDocuments"

/**
 * Renombrado alternativo para los archivos que el selector entrega a traves de
 * un proveedor que no admite renombrar.
 *
 * El caso es muy comun y no tiene nada de excepcional: cuando el usuario elige
 * un PDF desde "Recientes" o desde la lista por tipo de archivo, el selector lo
 * entrega por `com.android.providers.media.documents`, cuyo proveedor **no
 * implementa** `renameDocument`; el mismo archivo abierto navegando por
 * "Almacenamiento interno" llega por otro proveedor que si lo admite. Es decir,
 * que se pueda renombrar dependia de por donde hubiera entrado el documento.
 *
 * Esos documentos, sin embargo, son archivos indexados por MediaStore, y su
 * identificador va dentro de la propia URI. Renombrandolos por MediaStore se
 * consigue lo mismo que el proveedor negaba. Como el archivo es del usuario y
 * no de la app, Android exige su consentimiento explicito: [rename] devuelve
 * [RenameOutcome.PermisoRequerido] con el diálogo del sistema que hay que
 * mostrar, y tras aceptarlo el renombrado se reintenta y ya funciona.
 *
 * Ver `SafDocuments.canRename`, que es la via preferida cuando esta disponible.
 */
object MediaStoreDocuments {

    sealed interface RenameOutcome {
        /** Renombrado hecho. */
        data object Renombrado : RenameOutcome

        /** Falta el permiso del usuario; hay que lanzar este dialogo del sistema. */
        data class PermisoRequerido(val intentSender: IntentSender) : RenameOutcome

        data class Fallo(val error: Throwable) : RenameOutcome
    }

    /**
     * URI de MediaStore equivalente, o `null` si el documento no esta indexado
     * ahi (Drive, otra app, un proveedor propio...).
     */
    fun mediaUri(context: Context, uri: Uri): Uri? {
        if (uri.authority == MediaStore.AUTHORITY) return uri
        if (!DocumentsContract.isDocumentUri(context, uri)) return null

        // Via oficial: traduce la URI del proveedor a la de MediaStore. Exige
        // que la app tenga permiso sobre la URI, que es justo nuestro caso.
        runCatching { MediaStore.getMediaUri(context, uri) }
            .onSuccess { return it }
            .onFailure { Log.i(TAG, "getMediaUri no pudo traducir ${uri.authority}: ${it.message}") }

        // Reserva: el identificador de MediaStore viaja dentro del identificador
        // del documento ("document:1000105447", "msf:1000108700").
        return runCatching {
            val id = DocumentsContract.getDocumentId(uri).substringAfterLast(':').toLong()
            ContentUris.withAppendedId(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                id,
            )
        }.getOrNull()
    }

    /** `true` si este documento se puede renombrar por esta via. */
    fun canRename(context: Context, uri: Uri): Boolean = mediaUri(context, uri) != null

    /**
     * Cambia el nombre del archivo en MediaStore.
     *
     * No cambia la URI: el identificador del archivo es el mismo antes y
     * despues, de modo que la fila del historial sigue siendo valida.
     */
    suspend fun rename(context: Context, uri: Uri, newName: String): RenameOutcome =
        withContext(Dispatchers.IO) {
            val media = mediaUri(context, uri)
                ?: return@withContext RenameOutcome.Fallo(
                    IllegalStateException("El documento no esta en MediaStore"),
                )
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
            }

            try {
                // Si el archivo lo creo la app (escaneos, copias en "Mis PDF"),
                // esto funciona directamente y no se molesta al usuario.
                if (resolver.update(media, values, null, null) > 0) {
                    RenameOutcome.Renombrado
                } else {
                    RenameOutcome.Fallo(IllegalStateException("MediaStore no actualizo ninguna fila"))
                }
            } catch (e: SecurityException) {
                // El archivo es del usuario: Android pide su consentimiento.
                Log.i(TAG, "Hace falta el permiso del usuario para renombrar: ${e.message}")
                runCatching {
                    MediaStore.createWriteRequest(resolver, listOf(media)).intentSender
                }.fold(
                    onSuccess = { RenameOutcome.PermisoRequerido(it) },
                    onFailure = { RenameOutcome.Fallo(it) },
                )
            } catch (e: Exception) {
                // Tipicamente: ya existe otro archivo con ese nombre.
                Log.w(TAG, "Fallo al renombrar por MediaStore", e)
                RenameOutcome.Fallo(e)
            }
        }
}
