package com.example.purrytify.ui.components

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
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
    val context = LocalContext.current

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

        // Determine optimal height for player
        val playerHeight = if (isLandscape) 70.dp else 95.dp

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(playerHeight)
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

            // Unified 2-Row layout for all songs
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Row 1: Artwork, song info, and controls
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Artwork
                    Image(
                        painter = painter,
                        contentDescription = "Artist Artwork",
                        modifier = Modifier
                            .size(if (isLandscape) 45.dp else 52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Song info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = song.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = if (isLandscape) 14.sp else 16.sp
                        )
                        
                        Text(
                            text = song.artist,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp
                        )
                    }
                    
                    // Controls row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 2.dp else 4.dp)
                    ) {
                        // Control sizes
                        val controlSize = if (isLandscape) 32.dp else 36.dp
                        val playPauseSize = if (isLandscape) 36.dp else 40.dp
                        val iconSize = if (isLandscape) 18.dp else 20.dp
                        
                        // Previous button
                        IconButton(
                            onClick = { 
                                if (song.id > 0) {
                                    mainViewModel.handleSkipPrevious(song.id)
                                }
                            },
                            modifier = Modifier.size(controlSize)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipPrevious,
                                contentDescription = "Previous",
                                tint = Color.White,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        
                        // Play/Pause button
                        IconButton(
                            onClick = { mainViewModel.playSong(song) },
                            modifier = Modifier
                                .size(playPauseSize)
                                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(playPauseSize/2))
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp)
                            )
                        }
                        
                        // Next button
                        IconButton(
                            onClick = { 
                                if (song.id > 0) {
                                    mainViewModel.handleSkipNext(song.id)
                                }
                            },
                            modifier = Modifier.size(controlSize)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Next",
                                tint = Color.White,
                                modifier = Modifier.size(iconSize)
                            )
                        }
                        
                        // Show share and QR buttons only for online songs
                        if (isOnlinePlaying && song.id > 0) {
                            // Share button
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "https://puritify-deeplink.vercel.app/song/${song.id}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, null))
                                },
                                modifier = Modifier.size(controlSize)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_share),
                                    contentDescription = "Share Song",
                                    tint = Color.White,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                            
                            // QR button
                            IconButton(
                                onClick = {
                                    val qrIntent = Intent(context, QrCodeActivity::class.java).apply {
                                        putExtra("DEEP_LINK", "https://puritify-deeplink.vercel.app/song/${song.id}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(qrIntent)
                                },
                                modifier = Modifier.size(controlSize)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_qr_code),
                                    contentDescription = "QR Code",
                                    tint = Color.White,
                                    modifier = Modifier.size(iconSize)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Row 2: Spotify-style progress bar (simple thin line without thumb)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .padding(horizontal = 4.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    // Calculate position based on touch
                                    val seekRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                    val newPosition = (seekRatio * song.duration.toInt()).toInt()
                                    mainViewModel.seekTo(newPosition)
                                },
                                onTap = { offset ->
                                    // Calculate position based on touch
                                    val seekRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                    val newPosition = (seekRatio * song.duration.toInt()).toInt()
                                    mainViewModel.seekTo(newPosition)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Simple thin line progress bar - just a background track and a progress overlay
                    // Background track (grey)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.5.dp)  // Very thin line
                            .background(Color.White.copy(alpha = 0.3f))
                    )
                    
                    // Progress track (white)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(sliderPosition.coerceIn(0f, 1f))
                            .height(1.5.dp)  // Same height as background
                            .background(Color.White)
                            .align(Alignment.CenterStart)  // Align to start to fill from left to right
                    )
                }
            }
        }
    }
}