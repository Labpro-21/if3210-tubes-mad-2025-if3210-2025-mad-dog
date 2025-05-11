package com.example.purrytify.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.purrytify.data.model.OnlineSongResponse
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date

object DownloadUtils {
    private const val TAG = "DownloadUtils"

    suspend fun downloadFile(context: Context, url: String, destinationDir: File, filename: String): Uri? = withContext(Dispatchers.IO) {
        try {
            val outputFile = File(destinationDir, filename)
            val urlConnection = java.net.URL(url).openConnection()
            urlConnection.connect()

            val inputStream = urlConnection.getInputStream()
            val outputStream = FileOutputStream(outputFile)

            val buffer = ByteArray(4 * 1024) // 4KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            return@withContext Uri.fromFile(outputFile)
        } catch (e: IOException) {
            Log.e(TAG, "Error downloading file from $url to $destinationDir/$filename: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun downloadAndInsertSingleSong(
        context: Context,
        onlineSong: OnlineSongResponse,
        userId: Int,
        songsDao: SongsDao
    ) = withContext(Dispatchers.IO) {
        val songsDir = File(context.getExternalFilesDir(null), "songs")
        if (!songsDir.exists()) {
            songsDir.mkdirs()
        }
        val artworkDir = File(context.getExternalFilesDir(null), "artwork")
        if (!artworkDir.exists()) {
            artworkDir.mkdirs()
        }

        val songFilename = "${onlineSong.id}_${onlineSong.title.replace("\\s+".toRegex(), "_")}.mp3"
        val artworkFilename = "${onlineSong.id}_${onlineSong.title.replace("\\s+".toRegex(), "_")}.png" // Assuming PNG

        val localSongUri = downloadFile(context, onlineSong.url, songsDir, songFilename)
        val localArtworkUri = onlineSong.artwork?.let {
            downloadFile(context, it, artworkDir, artworkFilename)
        }

        if (localSongUri != null) {
            val songEntity = Songs(
                userId = userId,
                name = onlineSong.title,
                artist = onlineSong.artist,
                description = onlineSong.country ?: "",
                filePath = localSongUri.toString(), // Store local URI
                artwork = localArtworkUri?.toString(), // Store local URI
                duration = MediaUtils.parseDuration(onlineSong.duration),
                uploadDate = Date()
            )
            songsDao.insertSong(songEntity)
            Log.d(TAG, "Inserted song: ${songEntity.name}, Local URI: ${songEntity.filePath}, Artwork URI: ${songEntity.artwork}")
            return@withContext true
        } else {
            Log.e(TAG, "Failed to download song: ${onlineSong.title}")
            return@withContext false
        }
    }
}