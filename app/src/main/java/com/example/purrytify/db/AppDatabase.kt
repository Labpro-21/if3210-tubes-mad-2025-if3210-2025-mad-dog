package com.example.purrytify.db
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.entity.Songs


@Database(entities = [Songs::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songsDao(): SongsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "purrytify_database"
                ).fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}