package com.example.purrytify.ui.components

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.MainViewModel
import com.example.purrytify.db.entity.Songs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import kotlinx.coroutines.withContext
import androidx.palette.graphics.Palette
import coil.imageLoader
import com.example.purrytify.ui.theme.SpotifyBlack
import kotlin.math.max
import kotlin.math.min
import androidx.compose.ui.res.painterResource
import com.example.purrytify.R
import android.content.Intent
import com.example.purrytify.ui.qrcode.QrCodeActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(mainViewModel: MainViewModel, onMiniPlayerClick: () -> Unit, modifier: Modifier = Modifier) {
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val currentPosition by mainViewModel.currentPosition.collectAsState()
    var dominantColor by remember { mutableStateOf(SpotifyBlack) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val isOnlinePlaying by mainViewModel.isOnlineSong.collectAsState()

    // Function to ensure color isn't too bright and maintains good contrast
    fun adjustColorBrightness(color: Color): Color {
        val red = color.red
        val green = color.green
        val blue = color.blue
        
        // Calculate perceived brightness using relative luminance formula
        val brightness = (0.299 * red + 0.587 * green + 0.114 * blue)
        
        // If the color is too bright (brightness > 0.5), darken it
        return if (brightness > 0.5) {
            val darkenFactor = 0.7f // Adjust this value to control darkness
            Color(
                red = max(0f, red * darkenFactor),
                green = max(0f, green * darkenFactor),
                blue = max(0f, blue * darkenFactor),
                alpha = color.alpha
            )
        } else {
            color
        }
    }

    currentSong?.let { song ->
        val artworkUri = song.artwork?.let { Uri.parse(it) }
        val context = LocalContext.current
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(context).data(data = artworkUri).apply {
                crossfade(true)
            }.build()
        )

        val sliderPosition by remember(currentPosition, song.duration) {
            derivedStateOf {
                currentPosition.toFloat() / song.duration.toFloat()
            }
        }

        LaunchedEffect(artworkUri) {
            launch(Dispatchers.IO) {
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .allowHardware(false)
                    .build()

                val result = context.imageLoader.execute(request)
                val drawable = result.drawable
                if (drawable is BitmapDrawable) {
                    val bitmap = drawable.bitmap
                    if (bitmap != null) {
                        val palette = Palette.from(bitmap).generate()
                        val color = palette.getDarkVibrantColor(palette.getDarkMutedColor(SpotifyBlack.toArgb()))
                        withContext(Dispatchers.Main) {
                            dominantColor = adjustColorBrightness(Color(color))
                        }
                    }
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(if (isLandscape) 64.dp else 80.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            dominantColor,
                            dominantColor.copy(alpha = 0.9f)
                        )
                    )
                )
                .clickable { onMiniPlayerClick() }
        ) {
            // Semi-transparent overlay for better text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.4f)
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painter,
                    contentDescription = "Artist Artwork",
                    modifier = Modifier
                        .size(if (isLandscape) 40.dp else 56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = song.name,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = if (isLandscape) 14.sp else 16.sp
                    )
                    if (!isLandscape) {
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                    Slider(
                        value = sliderPosition,
                        onValueChange = {
                            val newPosition = (it * song.duration.toInt()).toInt()
                            mainViewModel.seekTo(newPosition)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp),
                        valueRange = 0f..1f,
                        thumb = {},
                        track = {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(1.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(sliderPosition)
                                        .height(2.dp)
                                        .background(Color.White, RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    )
                }

                IconButton(
                    onClick = { mainViewModel.playSong(song) },
                    modifier = Modifier
                        .size(if (isLandscape) 40.dp else 48.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 24.dp else 28.dp)
                    )
                }

                if (isOnlinePlaying && song.id > 0) {
                    Row {
                        IconButton(
                            onClick = {
                                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
                                shareIntent.type = "text/plain"
                                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, "https://puritify-deeplink.vercel.app/song/${song.id}")
                                context.startActivity(android.content.Intent.createChooser(shareIntent, null))
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = "Share Song",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val qrCodeIntent = Intent(context, QrCodeActivity::class.java).apply {
                                    putExtra("DEEP_LINK", "https://puritify-deeplink.vercel.app/song/${song.id}")
                                }
                                context.startActivity(qrCodeIntent)
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_qr_code),
                                contentDescription = "Share QR Code",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}