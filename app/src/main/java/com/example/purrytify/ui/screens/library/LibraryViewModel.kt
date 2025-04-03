package com.example.purrytify.ui.screens.library

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val songDao = AppDatabase.getDatabase(application).songsDao()
    private val context = application.applicationContext
    private val authRepository = AuthRepository.getInstance(application)


    val userId: Int?
        get() = authRepository.currentUserId

    val allSongs: Flow<List<Songs>>
        get() {
            val userId = this.userId
            return if (userId != null) {
                songDao.getAllSongsForUser(userId)
            } else {
                songDao.getAllSongs()
            }
        }

    val favoriteSongs: Flow<List<Songs>>
        get() {
            val userId = this.userId
            return if (userId != null) {
                songDao.getFavoriteSongsForUser(userId)
            } else {
                songDao.getFavoriteSongs()
            }
        }

    fun addSong( uri: Uri?, artworkUri: Uri?,title: String, artist:String) {
        val userId = authRepository.currentUserId

        if (userId == null) {
            Toast.makeText(context, "You must be logged in to add songs.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            if (uri != null && isAudioFile(uri)) {
                val audioFilePath = copyAudioToInternalStorage(uri)
                if (audioFilePath != null) {
                    val duration = getAudioDuration(Uri.fromFile(File(audioFilePath)), context)
                    var artworkFilePath: String? = null

                    if (artworkUri != null) {
                        artworkFilePath = copyArtworkToInternalStorage(artworkUri)
                    }

                    val newSong = Songs(artist= artist,name= title, description = "", userId = userId, duration = duration, filePath = audioFilePath, artwork = artworkFilePath ?: "") // Use userId
                    songDao.insertSong(newSong)
                } else {
                    Toast.makeText(context, "Error copying audio file.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please select a valid audio file.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleFavoriteStatus(song: Songs) {
        viewModelScope.launch {
            songDao.updateFavoriteStatus(song.id, !song.isFavorite)
        }
    }

    fun deleteSong(song: Songs) {
        viewModelScope.launch {
            songDao.deleteById(song.id)
        }
    }

    private fun isAudioFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("audio/") == true
    }

    fun getAudioDuration(uri: Uri, context: Context): Long {
        val retriever = MediaMetadataRetriever()
        try {
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            }

            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            Log.d("Metadata", "Duration: $durationStr  URI: $uri")

            return durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            return 0L
        } finally {
            retriever.release()
        }
    }

    fun getMetadata(uri: Uri?): Pair<String?, String?> {
        if (uri == null) return Pair(null, null)

        val retriever = MediaMetadataRetriever()
        var title: String? = null
        var artist: String? = null

        try {
            if (uri.scheme == "content") {
                retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                retriever.setDataSource(uri.path)
            }

            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)

        } catch (e: Exception) {
            Log.e("Player", "Error getting metadata: ${e.message}")
        } finally {
            retriever.release()
        }

        return Pair(title, artist)
    }

    private fun copyArtworkToInternalStorage(artworkUri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(artworkUri) ?: return null
        val fileName = "artwork_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return file.absolutePath
    }
    private fun copyAudioToInternalStorage(audioUri: Uri): String? {
        val inputStream = context.contentResolver.openInputStream(audioUri) ?: return null
        val fileName = "audio_${System.currentTimeMillis()}.mp3" // Ensure to use the correct extension
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return file.absolutePath
    }
}