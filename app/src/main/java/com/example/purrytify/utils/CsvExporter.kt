package com.example.purrytify.utils

import android.content.Context
import android.os.Environment
import com.example.purrytify.db.dao.ListeningActivityDao
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object CsvExporter {
    fun exportSoundCapsuleToCSV(context: Context, soundCapsule: ListeningActivityDao.SoundCapsule): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "sound_capsule_${timestamp}.csv"
        
        // Get the Downloads directory
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        try {
            FileWriter(file).use { writer ->
                writer.append("Category,Value\n")
                writer.append("Month and Year,${soundCapsule.monthYear}\n")
                val minutes = TimeUnit.MILLISECONDS.toMinutes(soundCapsule.totalTimeListened)
                writer.append("Total Listening Time (minutes),${minutes}\n")
                writer.append("Top Artist,${soundCapsule.topArtist ?: "N/A"}\n")
                writer.append("Top Song,${soundCapsule.topSong?.name ?: "N/A"}\n")
                writer.append("Top Song Artist,${soundCapsule.topSong?.artist ?: "N/A"}\n")
                writer.append("Listening Streak (days),${soundCapsule.listeningDayStreak}\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        return file
    }
} 