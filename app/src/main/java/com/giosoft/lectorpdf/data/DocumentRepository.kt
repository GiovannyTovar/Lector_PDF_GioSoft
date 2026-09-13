package com.giosoft.lectorpdf.data

import android.content.Context
import android.net.Uri
import com.giosoft.lectorpdf.data.db.DocumentDao
import com.giosoft.lectorpdf.data.db.DocumentEntity
import kotlinx.coroutines.flow.Flow

/**
 * Unico punto de acceso al historial.
 *
 * Regla que esta clase garantiza: **quitar un documento del historial nunca
 * borra el archivo del celular.** En la version 4.x el dialogo prometia eso
 * mismo mientras el codigo llamaba a File.delete(); aqui simplemente no existe
 * ninguna operacion de borrado sobre el almacenamiento del usuario.
 */
class DocumentRepository(
    private val context: Context,
    private val dao: DocumentDao,
) {

    fun observeAll(): Flow<List<DocumentEntity>> = dao.observeAll()

    suspend fun find(uri: Uri): DocumentEntity? = dao.findByUri(uri.toString())

    /**
     * Registra (o actualiza) un documento recien abierto y devuelve su ficha.
     * Si ya existia, conserva favorito y ultima pagina leida.
     */
    suspend fun registerOpened(uri: Uri, persistable: Boolean): DocumentEntity {
        val key = uri.toString()
        val existing = dao.findByUri(key)
        val name = SafDocuments.displayName(context, uri)
            ?: existing?.name
            ?: uri.lastPathSegment
            ?: "documento.pdf"

        val entity = DocumentEntity(
            uri = key,
            name = name,
            lastOpened = System.currentTimeMillis(),
            lastPage = existing?.lastPage ?: 0,
            pageCount = existing?.pageCount ?: 0,
            sizeBytes = SafDocuments.sizeBytes(context, uri),
            isFavorite = existing?.isFavorite ?: false,
            persistable = persistable || (existing?.persistable ?: false),
            location = SafDocuments.locationLabel(context, uri) ?: existing?.location,
        )
        dao.upsert(entity)
        return entity
    }

    /** Quita del historial y suelta el permiso. NO toca el archivo. */
    suspend fun removeFromHistory(entity: DocumentEntity) {
        dao.deleteByUri(entity.uri)
        if (entity.persistable) {
            SafDocuments.releasePersistablePermission(context, Uri.parse(entity.uri))
        }
    }

    /** Reinserta una ficha; se usa para el "Deshacer" del historial. */
    suspend fun restore(entity: DocumentEntity) = dao.upsert(entity)

    suspend fun setFavorite(entity: DocumentEntity, favorite: Boolean) =
        dao.updateFavorite(entity.uri, favorite)

    suspend fun setLastPage(uri: Uri, page: Int) = dao.updateLastPage(uri.toString(), page)

    suspend fun setPageCount(uri: Uri, pageCount: Int) =
        dao.updatePageCount(uri.toString(), pageCount)

    /**
     * Renombra el archivo original y sincroniza el historial.
     *
     * Si el proveedor emite una URI distinta, migra la fila a la clave nueva:
     * de lo contrario el historial apuntaria a una URI que ya no resuelve.
     */
    suspend fun rename(entity: DocumentEntity, newName: String): Result<DocumentEntity> {
        val finalName = ensurePdfExtension(newName.trim())
        val oldUri = Uri.parse(entity.uri)

        return SafDocuments.rename(context, oldUri, finalName).map { newUri ->
            if (newUri.toString() == entity.uri) {
                dao.updateName(entity.uri, finalName)
                entity.copy(name = finalName)
            } else {
                val migrated = entity.copy(uri = newUri.toString(), name = finalName)
                dao.upsert(migrated)
                dao.deleteByUri(entity.uri)
                migrated
            }
        }
    }

    suspend fun canRename(entity: DocumentEntity): Boolean =
        SafDocuments.canRename(context, Uri.parse(entity.uri))

    suspend fun isAvailable(entity: DocumentEntity): Boolean =
        SafDocuments.isAvailable(context, Uri.parse(entity.uri))

    private fun ensurePdfExtension(name: String): String =
        if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
}
