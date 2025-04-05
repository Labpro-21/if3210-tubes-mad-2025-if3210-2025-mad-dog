// MiniPlayer.kt

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(mainViewModel: MainViewModel, onMiniPlayerClick: () -> Unit) {
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val currentPosition by mainViewModel.currentPosition.collectAsState()
    var dominantColor by remember { mutableStateOf(SpotifyBlack) }

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
                        val color = palette.getDarkVibrantColor(palette.getMutedColor(SpotifyBlack.toArgb()))
                        withContext(Dispatchers.Main) {
                            dominantColor = Color(color)
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(dominantColor.copy(alpha = 0.8f), shape = RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .clickable { onMiniPlayerClick() }
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painter,
                contentDescription = "Artist Artwork",
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(text = song.name, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, fontSize = 16.sp)
                Text(text = song.artist, color = Color.Gray, maxLines = 1, fontSize = 14.sp)
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        val newPosition = (it * song.duration.toInt()).toInt()
                        mainViewModel.seekTo(newPosition)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    valueRange = 0f..1f,
                    thumb = {},
                    track = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sliderPosition)
                                    .height(4.dp)
                                    .background(Color.White, shape = RoundedCornerShape(2.dp))
                            )
                        }
                    }
                )
            }

            IconButton(onClick = { mainViewModel.playSong(song) }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}