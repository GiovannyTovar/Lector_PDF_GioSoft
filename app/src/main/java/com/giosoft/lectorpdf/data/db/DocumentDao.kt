package com.giosoft.lectorpdf.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query("SELECT * FROM documents ORDER BY lastOpened DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    @Query("SELECT * FROM documents WHERE uri = :uri")
    suspend fun findByUri(uri: String): DocumentEntity?

    /** Busca un documento ya conservado con el mismo contenido exacto. */
    @Query("SELECT * FROM documents WHERE contentHash = :hash AND persistable = 1 LIMIT 1")
    suspend fun findByHash(hash: String): DocumentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(document: DocumentEntity)

    @Query("DELETE FROM documents WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("UPDATE documents SET name = :name WHERE uri = :uri")
    suspend fun updateName(uri: String, name: String)

    @Query("UPDATE documents SET isFavorite = :favorite WHERE uri = :uri")
    suspend fun updateFavorite(uri: String, favorite: Boolean)

    @Query("UPDATE documents SET lastPage = :page WHERE uri = :uri")
    suspend fun updateLastPage(uri: String, page: Int)

    @Query("UPDATE documents SET lastOpened = :timestamp WHERE uri = :uri")
    suspend fun touch(uri: String, timestamp: Long)

    @Query("UPDATE documents SET pageCount = :pageCount WHERE uri = :uri")
    suspend fun updatePageCount(uri: String, pageCount: Int)

    @Query("UPDATE documents SET location = :location WHERE uri = :uri")
    suspend fun updateLocation(uri: String, location: String?)

    @Query("UPDATE documents SET categoryId = :categoryId WHERE uri = :uri")
    suspend fun updateCategory(uri: String, categoryId: Long?)

    @Query("UPDATE documents SET categoryId = :categoryId WHERE uri IN (:uris)")
    suspend fun updateCategoryForAll(uris: List<String>, categoryId: Long?)

    /**
     * Cambiar el nombre de un archivo por SAF puede devolver una URI nueva.
     * Como la URI es la clave primaria, hay que reinsertar la fila con la
     * clave nueva; el llamante borra despues la antigua.
     */
    @Query("SELECT COUNT(*) FROM documents")
    suspend fun count(): Int
}
