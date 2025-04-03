// MediaUtils.kt

package com.example.purrytify.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object MediaUtils {
    fun copyArtworkToInternalStorage(artworkUri: Uri, context: Context): String? {
        val inputStream = context.contentResolver.openInputStream(artworkUri) ?: return null
        val fileName = "artwork_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return file.absolutePath
    }

    fun copyAudioToInternalStorage(audioUri: Uri, context: Context): String? {
        val inputStream = context.contentResolver.openInputStream(audioUri) ?: return null
        val fileName = "audio_${System.currentTimeMillis()}.mp3"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)

        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()

        return file.absolutePath
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

    fun getMetadata(uri: Uri?, context: Context): Pair<String?, String?> {
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
}