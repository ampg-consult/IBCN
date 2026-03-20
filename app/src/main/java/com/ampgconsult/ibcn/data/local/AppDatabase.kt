package com.ampgconsult.ibcn.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ampgconsult.ibcn.data.local.dao.ProjectDao
import com.ampgconsult.ibcn.data.local.entities.ChatMessageEntity
import com.ampgconsult.ibcn.data.local.entities.ProjectEntity

@Database(entities = [ProjectEntity::class, ChatMessageEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ibcn_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
