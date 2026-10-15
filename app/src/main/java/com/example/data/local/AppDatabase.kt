package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.local.converter.Converters
import com.example.data.local.dao.DocumentDao
import com.example.data.local.dao.DocumentPageDao
import com.example.data.local.dao.FolderDao
import com.example.data.local.dao.SignatureDao
import com.example.data.local.entity.DocumentEntity
import com.example.data.local.entity.DocumentPageEntity
import com.example.data.local.entity.FolderEntity
import com.example.data.local.entity.SignatureEntity

@Database(
    entities = [
        DocumentEntity::class,
        DocumentPageEntity::class,
        FolderEntity::class,
        SignatureEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun documentPageDao(): DocumentPageDao
    abstract fun folderDao(): FolderDao
    abstract fun signatureDao(): SignatureDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "scanflow_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
