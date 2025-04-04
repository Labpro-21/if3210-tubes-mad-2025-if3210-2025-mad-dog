package com.example.purrytify.ui.screens.library

import android.app.Application
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.example.purrytify.utils.MediaUtils
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date

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
                val audioFilePath = MediaUtils.copyAudioToInternalStorage(uri,context= context)
                if (audioFilePath != null) {
                    val duration = MediaUtils.getAudioDuration(Uri.fromFile(File(audioFilePath)), context)

                    var artworkFilePath: String? = null

                    if (artworkUri != null) {
                        artworkFilePath = MediaUtils.copyArtworkToInternalStorage(artworkUri, context = context)
                    }

                    val newSong = Songs(artist= artist,name= title, description = "", userId = userId, duration = duration, filePath = audioFilePath, uploadDate = Date(),artwork = artworkFilePath ?: "") // Use userId
                    songDao.insertSong(newSong)
                } else {
                    Toast.makeText(context, "Error copying audio file.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please select a valid audio file.", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun isAudioFile(uri: Uri): Boolean {
        val mimeType = context.contentResolver.getType(uri)
        return mimeType?.startsWith("audio/") == true
    }


}