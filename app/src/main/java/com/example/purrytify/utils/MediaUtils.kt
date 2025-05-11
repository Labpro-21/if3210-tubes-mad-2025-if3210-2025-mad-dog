package com.example.purrytify.utils

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

object MediaUtils {
    private val tag = "MediaUtils"

    fun getAudioDuration(uri: Uri, context: Context): Long {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            Log.d("MediaUtils", "Duration: $durationStr  URI: $uri")
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
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
        } catch (e: Exception) {
            Log.e("Player", "Error getting metadata: ${e.message}")
        } finally {
            retriever.release()
        }

        return Pair(title, artist)
    }
    fun parseDuration(durationString: String): Long {
        return try {
            val parts = durationString.split(":")

            when (parts.size) {
                2 -> {
                    val minutes = parts[0].toLong()
                    val seconds = parts[1].toLong()
                    (minutes * 60 + seconds) * 1000
                }

                3 -> {
                    val hours = parts[0].toLong()
                    val minutes = parts[1].toLong()
                    val seconds = parts[2].toLong()
                    ((hours * 60 * 60) + (minutes * 60) + seconds) * 1000
                }
                else -> {
                    Log.e(tag, "Unexpected duration format: $durationString")
                    0L
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error parsing duration: ${durationString}, ${e.message}")
            0L
        }
    }
}