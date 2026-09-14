package com.giosoft.lectorpdf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DocumentEntity::class, CategoryEntity::class],
    version = 6,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        /** Anade la ubicacion legible del documento. Se migra sin perder el historial. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN location TEXT")
            }
        }

        /** Anade la huella del contenido, para deduplicar lo conservado. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE documents ADD COLUMN contentHash TEXT")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_documents_contentHash ON documents(contentHash)")
            }
        }

        /** Anade las categorias. Los documentos existentes quedan sin categoria. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS categories (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "colorArgb INTEGER NOT NULL, " +
                        "position INTEGER NOT NULL)",
                )
                db.execSQL("ALTER TABLE documents ADD COLUMN categoryId INTEGER")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_documents_categoryId " +
                        "ON documents(categoryId)",
                )
            }
        }

        /** Anade la marca de documento protegido con desbloqueo. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        /** Anade la marca de PDF cifrado con contrasena propia. */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE documents ADD COLUMN hasPassword INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "lectorpdf.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
        }
    }
}
