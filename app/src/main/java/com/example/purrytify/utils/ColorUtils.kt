// ColorUtils.kt
package com.example.purrytify.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.purrytify.ui.theme.SpotifyBlack

object ColorUtils {
    suspend fun generateDominantColorGradient(context: Context, imageUri: Any?): Brush {
        if (imageUri == null) {
            return Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to SpotifyBlack,
                    1.0f to SpotifyBlack
                )
            )
        }

        val drawable: Drawable? = when (imageUri) {
            is Uri -> {
                val request = ImageRequest.Builder(context)
                    .data(imageUri)
                    .allowHardware(false)
                    .build()
                context.imageLoader.execute(request).drawable
            }
            is Int -> {
                ContextCompat.getDrawable(context, imageUri)
            }
            else -> null
        }

        return withContext(Dispatchers.IO) {
            if (drawable is BitmapDrawable && drawable.bitmap != null) {
                val bitmap = drawable.bitmap
                val palette = Palette.from(bitmap).generate()
                val dominantColor = palette.getVibrantColor(palette.getMutedColor(SpotifyBlack.toArgb()))
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(dominantColor),
                        0.1f to Color(dominantColor),
                        0.7f to SpotifyBlack,
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
        }
    }
}