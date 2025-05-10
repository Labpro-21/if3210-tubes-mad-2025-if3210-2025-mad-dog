// ColorUtils.kt
package com.example.purrytify.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.purrytify.ui.theme.SpotifyBlack

object ColorUtils {
    suspend fun generateDominantColorGradient(context: Context, imageUri: Uri?): Brush {
        if (imageUri == null) {
            return Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to SpotifyBlack,
                    1.0f to SpotifyBlack
                )
            )
        }

        val request = ImageRequest.Builder(context)
            .data(imageUri)
            .allowHardware(false)
            .build()

        val result = context.imageLoader.execute(request)
        val drawable = result.drawable

        return withContext(Dispatchers.IO) {
            if (drawable is BitmapDrawable) {
                val bitmap = drawable.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val dominantColor = palette.getVibrantColor(palette.getMutedColor(SpotifyBlack.toArgb()))
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(dominantColor),
                            0.1f to Color(dominantColor),
                            1.0f to SpotifyBlack
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to SpotifyBlack,
                            1.0f to SpotifyBlack
                        )
                    )
                }
            } else {
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to SpotifyBlack,
                        1.0f to SpotifyBlack
                    )
                )
            }
        }
    }
}