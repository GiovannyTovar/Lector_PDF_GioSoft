package com.giosoft.pdf.data

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import com.giosoft.pdf.data.db.DocumentDao
import com.giosoft.pdf.data.db.DocumentEntity
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
        // Los documentos que llegan compartidos (WhatsApp, correo) traen una URI
        // temporal que caduca. En vez de dejar una entrada que dejara de abrir,
        // se conservan en "Documentos/Mis PDF" y el historial apunta ya a la
        // copia estable. Devuelve null si no hizo falta o no se pudo.
        if (!persistable) {
            preserveTemporary(uri)?.let { return it }
        }
        return register(uri, persistable, contentHash = null)
    }

    /**
     * Conserva un documento de acceso temporal.
     *
     * Antes de copiar comprueba la huella del contenido: si ya guardamos ese
     * mismo documento (aunque el usuario lo reabra una y otra vez desde
     * WhatsApp), se reutiliza la copia existente en lugar de acumular
     * "factura(1).pdf", "factura(2).pdf"...
     */
    private suspend fun preserveTemporary(uri: Uri): DocumentEntity? {
        val hash = PublicDocuments.contentHash(context, uri) ?: return null

        dao.findByHash(hash)?.let { existing ->
            // Solo sirve si la copia sigue existiendo; el usuario pudo borrarla.
            if (SafDocuments.isAvailable(context, Uri.parse(existing.uri))) {
                val now = System.currentTimeMillis()
                dao.touch(existing.uri, now)
                return existing.copy(lastOpened = now)
            }
        }

        val name = SafDocuments.displayName(context, uri) ?: "documento.pdf"
        val saved = PublicDocuments.save(context, uri, name).getOrNull() ?: return null
        return register(saved, persistable = true, contentHash = hash)
    }

    private suspend fun register(
        uri: Uri,
        persistable: Boolean,
        contentHash: String?,
    ): DocumentEntity {
        val key = uri.toString()
        val existing = dao.findByUri(key)
        val name = SafDocuments.displayName(context, uri)
            ?: existing?.name
            ?: uri.lastPathSegment
            ?: "documento.pdf"

        // Se parte de la ficha que ya existia y solo se pisan los campos que
        // el hecho de abrir cambia. Enumerar aqui los que hay que conservar
        // (favorito, categoria, bloqueo...) hacia que cualquier campo nuevo se
        // perdiera en silencio al reabrir: asi se perdia la categoria.
        val base = existing ?: DocumentEntity(uri = key, name = name, lastOpened = 0L)
        val entity = base.copy(
            uri = key,
            name = name,
            lastOpened = System.currentTimeMillis(),
            sizeBytes = SafDocuments.sizeBytes(context, uri),
            persistable = persistable || existing?.persistable == true,
            location = SafDocuments.locationLabel(context, uri) ?: base.location,
            contentHash = contentHash ?: base.contentHash,
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

    /**
     * Borra el archivo del celular Y lo quita del historial. **Irreversible.**
     *
     * Es la UNICA operacion de la app que destruye un archivo del usuario, y
     * esta deliberadamente separada de [removeFromHistory] para que no puedan
     * confundirse. Solo debe invocarse tras confirmacion explicita.
     *
     * Si el borrado falla, el historial se deja intacto: no tiene sentido
     * perder la entrada de un archivo que sigue existiendo.
     */
    suspend fun deleteFromDevice(entity: DocumentEntity): Result<Unit> {
        val uri = Uri.parse(entity.uri)
        return SafDocuments.delete(context, uri).onSuccess {
            dao.deleteByUri(entity.uri)
            if (entity.persistable) SafDocuments.releasePersistablePermission(context, uri)
        }
    }

    suspend fun canDelete(entity: DocumentEntity): Boolean =
        SafDocuments.canDelete(context, Uri.parse(entity.uri))

    /** Reinserta una ficha; se usa para el "Deshacer" del historial. */
    suspend fun restore(entity: DocumentEntity) = dao.upsert(entity)

    suspend fun setHasPassword(uri: Uri, hasPassword: Boolean) =
        dao.updateHasPassword(uri.toString(), hasPassword)

    suspend fun setLocked(entity: DocumentEntity, locked: Boolean) =
        dao.updateLocked(entity.uri, locked)

    suspend fun setFavorite(entity: DocumentEntity, favorite: Boolean) =
        dao.updateFavorite(entity.uri, favorite)

    suspend fun setLastPage(uri: Uri, page: Int) = dao.updateLastPage(uri.toString(), page)

    suspend fun setPageCount(uri: Uri, pageCount: Int) =
        dao.updatePageCount(uri.toString(), pageCount)

    /**
     * Renombra el archivo original y sincroniza el historial.
     *
     * Hay dos caminos y se prefiere siempre el primero:
     *
     * 1. El proveedor del documento admite renombrar. Es lo normal cuando el
     *    PDF se abrio navegando por el almacenamiento.
     * 2. El proveedor no lo admite, pero el archivo esta indexado en MediaStore
     *    (el caso de los PDF elegidos desde "Recientes"). Entonces se renombra
     *    por MediaStore, que exige el permiso del usuario la primera vez; ver
     *    [MediaStoreDocuments].
     *
     * Si el proveedor emite una URI distinta, migra la fila a la clave nueva:
     * de lo contrario el historial apuntaria a una URI que ya no resuelve.
     */
    suspend fun rename(entity: DocumentEntity, newName: String): RenameResult {
        val finalName = ensurePdfExtension(newName.trim())
        val oldUri = Uri.parse(entity.uri)

        if (SafDocuments.canRename(context, oldUri)) {
            return SafDocuments.rename(context, oldUri, finalName).fold(
                onSuccess = { RenameResult.Renombrado(applyName(entity, it, finalName)) },
                onFailure = { RenameResult.Fallo(it) },
            )
        }

        return when (val outcome = MediaStoreDocuments.rename(context, oldUri, finalName)) {
            // MediaStore conserva el identificador del archivo, asi que la URI
            // del historial sigue siendo valida tal cual.
            is MediaStoreDocuments.RenameOutcome.Renombrado ->
                RenameResult.Renombrado(applyName(entity, oldUri, finalName))

            is MediaStoreDocuments.RenameOutcome.PermisoRequerido ->
                RenameResult.PermisoRequerido(outcome.intentSender)

            is MediaStoreDocuments.RenameOutcome.Fallo ->
                RenameResult.Fallo(outcome.error)
        }
    }

    /** Deja el historial acorde con el archivo ya renombrado. */
    private suspend fun applyName(
        entity: DocumentEntity,
        newUri: Uri,
        finalName: String,
    ): DocumentEntity =
        if (newUri.toString() == entity.uri) {
            dao.updateName(entity.uri, finalName)
            entity.copy(name = finalName)
        } else {
            val migrated = entity.copy(uri = newUri.toString(), name = finalName)
            dao.upsert(migrated)
            dao.deleteByUri(entity.uri)
            migrated
        }

    /**
     * Como se puede renombrar este documento. La interfaz lo consulta antes de
     * abrir el dialogo para avisar de que Android pedira confirmacion, en vez
     * de soltar la peticion de permiso sin contexto.
     */
    suspend fun renameSupport(entity: DocumentEntity): RenameSupport {
        val uri = Uri.parse(entity.uri)
        return when {
            SafDocuments.canRename(context, uri) -> RenameSupport.DIRECTO
            MediaStoreDocuments.canRename(context, uri) -> RenameSupport.CON_PERMISO
            else -> RenameSupport.NO_DISPONIBLE
        }
    }

    suspend fun isAvailable(entity: DocumentEntity): Boolean =
        SafDocuments.isAvailable(context, Uri.parse(entity.uri))

    private fun ensurePdfExtension(name: String): String =
        if (name.endsWith(".pdf", ignoreCase = true)) name else "$name.pdf"
}

/** Resultado de intentar renombrar un documento. */
sealed interface RenameResult {
    data class Renombrado(val entity: DocumentEntity) : RenameResult

    /** Android necesita que el usuario autorice modificar SU archivo. */
    data class PermisoRequerido(val intentSender: IntentSender) : RenameResult

    data class Fallo(val error: Throwable) : RenameResult
}

/** Vias por las que un documento puede renombrarse. */
enum class RenameSupport {
    /** El proveedor lo admite: se renombra sin molestar al usuario. */
    DIRECTO,

    /** Solo por MediaStore, que pide el consentimiento del usuario. */
    CON_PERMISO,

    /** Ni una cosa ni la otra (Drive, proveedores de solo lectura...). */
    NO_DISPONIBLE,
}
