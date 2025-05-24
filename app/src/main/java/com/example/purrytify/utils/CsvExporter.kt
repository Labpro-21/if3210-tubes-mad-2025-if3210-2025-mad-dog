package com.example.purrytify.utils

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.purrytify.ui.screens.profile.ProfileViewModel.SoundCapsuleViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {
    
    /**
     * Exports a SoundCapsule to a CSV file
     * @param context The application context
     * @param soundCapsule The SoundCapsule data to export
     * @return The File that was created
     */
    fun exportSoundCapsuleToCSV(context: Context, soundCapsule: SoundCapsuleViewModel): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val filename = "sound_capsule_${timestamp}.csv"
        
        // Create CSV content
        val csvContent = buildCsvContent(soundCapsule)
        
        // Choose the appropriate method based on Android version
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveFileApi29AndAbove(context, filename, csvContent)
        } else {
            saveFileLegacy(filename, csvContent)
        }
    }
    
    private fun buildCsvContent(soundCapsule: SoundCapsuleViewModel): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("Sound Capsule Data - ${soundCapsule.monthYear}")
        sb.appendLine()
        
        // Summary section
        sb.appendLine("Summary")
        sb.appendLine("Total Time Listened (ms),${soundCapsule.totalTimeListened}")
        sb.appendLine("Total Time Listened (min),${soundCapsule.totalTimeListened / 60000}")
        sb.appendLine("Top Artist,${soundCapsule.topArtist ?: "N/A"}")
        sb.appendLine("Top Song,${soundCapsule.topSong?.name ?: "N/A"}")
        sb.appendLine("Top Song Artist,${soundCapsule.topSong?.artist ?: "N/A"}")
        sb.appendLine("Listening Day Streak,${soundCapsule.listeningDayStreak}")
        sb.appendLine()
        
        // Streak songs section
        sb.appendLine("Daily Streak Songs")
        sb.appendLine("Date,Song Name,Artist,Play Count")
        
        soundCapsule.streakSongs.forEach { song ->
            sb.appendLine("${song.playDate},${song.name ?: "N/A"},${song.artist ?: "N/A"},${song.playCount}")
        }
        
        return sb.toString()
    }
    
    // For Android 10 (API 29) and above
    private fun saveFileApi29AndAbove(context: Context, filename: String, content: String): File {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri).use { outputStream ->
                if (outputStream != null) {
                    writeToOutputStream(outputStream, content)
                }
            }
        }
        
        // Return a File object that represents the saved file
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filename)
        return file
    }
    
    // For Android 9 (API 28) and below
    private fun saveFileLegacy(filename: String, content: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        
        val file = File(downloadsDir, filename)
        FileOutputStream(file).use { outputStream ->
            writeToOutputStream(outputStream, content)
        }
        
        return file
    }
    
    private fun writeToOutputStream(outputStream: OutputStream, content: String) {
        outputStream.write(content.toByteArray())
        outputStream.flush()
    }
} 