package com.example.purrytify.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.purrytify.db.dao.RecentlyPlayedDao
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.dao.ListeningActivityDao
import com.example.purrytify.db.dao.SoundCapsuleDao
import com.example.purrytify.db.dao.UsersDao
import com.example.purrytify.db.dao.UserWithSongsDao
import com.example.purrytify.db.entity.DayStreakSong
import com.example.purrytify.db.entity.ListeningActivity
import com.example.purrytify.db.entity.RecentlyPlayed
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.entity.SoundCapsule
import com.example.purrytify.db.entity.Users
import com.example.purrytify.utils.DateConverter

@Database(
    entities = [
        Songs::class,
        Users::class,
        RecentlyPlayed::class,
        ListeningActivity::class,
        SoundCapsule::class,
        DayStreakSong::class
    ],
    version = 16  // Updated version for ListeningActivity schema change
)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songsDao(): SongsDao
    abstract fun usersDao(): UsersDao
    abstract fun userWithSongsDao(): UserWithSongsDao
    abstract fun recentlyPlayedDao(): RecentlyPlayedDao
    abstract fun listeningCapsuleDao(): ListeningActivityDao
    abstract fun soundCapsuleDao(): SoundCapsuleDao

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