package com.example.purrytify.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.dao.UsersDao
import com.example.purrytify.db.dao.UserWithSongsDao
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.entity.Users

@Database(entities = [Songs::class, Users::class], version = 4) // Increment version!
abstract class AppDatabase : RoomDatabase() {

    abstract fun songsDao(): SongsDao
    abstract fun usersDao(): UsersDao
    abstract fun userWithSongsDao(): UserWithSongsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "purrytify_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}