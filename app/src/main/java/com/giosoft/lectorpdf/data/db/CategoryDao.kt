package com.giosoft.lectorpdf.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY position ASC, name ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun count(): Int

    @Query("SELECT MAX(position) FROM categories")
    suspend fun maxPosition(): Int?

    /** Comparacion sin distinguir mayusculas: "Trabajo" y "trabajo" son la misma. */
    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    /**
     * Deja sin categoria los documentos que pertenecian a una que se borra.
     * Nunca borra documentos: la categoria es solo una etiqueta.
     */
    @Query("UPDATE documents SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun clearCategoryFromDocuments(categoryId: Long)
}
