// SongDetailScreen.kt

package com.example.purrytify.ui.screens.songdetail

import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.db.entity.Songs
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.palette.graphics.Palette
import coil.imageLoader
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.ui.theme.SpotifyDarkGray
import com.example.purrytify.ui.theme.SpotifyLightGray
import com.example.purrytify.ui.theme.SpotifyWhite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun SongDetailScreen(
    songId: Int,
    viewModel: SongDetailViewModel = viewModel(),
    navController: NavController
) {
    val uiState = viewModel.songDetails.collectAsState().value

    LaunchedEffect(songId) {
        viewModel.loadSongDetails(songId)
    }

    when (uiState) {
        SongDetailUiState.Loading -> {
            Text("Loading...", color = Color.White)
        }
        is SongDetailUiState.Success -> {
            SongDetailsContent(song = uiState.song, navController = navController, showBorder = false, viewModel = viewModel)
        }
        is SongDetailUiState.Error -> {
            Text(uiState.message, color = Color.Red)
        }
        else -> {}
    }
}

fun formatTime(millis: Int): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun SongDetailsContent(song: Songs, navController: NavController, showBorder: Boolean, viewModel: SongDetailViewModel) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var dominantColor by remember { mutableStateOf(SpotifyBlack) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var showOptions by remember { mutableStateOf(false) }
    var optionsAnchor by remember { mutableStateOf(DpOffset.Zero) }

    val context = LocalContext.current
    val artworkUri = song.artwork.let { File(it).toUri() }

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context).data(data = artworkUri).apply {
            crossfade(true)
        }.build()
    )

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
                    val color = palette.getVibrantColor(palette.getMutedColor(SpotifyBlack.toArgb()))
                    withContext(Dispatchers.Main) {
                        dominantColor = Color(color)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to dominantColor,
                        0.1f to dominantColor,
                        1.0f to SpotifyBlack
                    )
                )
            )
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.popBackStack() }
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                showOptions = !showOptions
                // Calculate anchor position relative to the IconButton
                optionsAnchor = DpOffset(
                    x = (-50).dp, // Adjust x-offset to move left
                    y = (-50).dp // Adjust y-offset as needed
                )
            }) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = showOptions,
                onDismissRequest = { showOptions = false },
                offset = optionsAnchor
            ) {
                DropdownMenuItem(
                    text = { Text("Delete", color = SpotifyLightGray) },
                    onClick = {
                        viewModel.deleteSong(song)
                        navController.popBackStack()
                        showOptions = false
                    },
                )
            }
        }

        // Artwork
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context).data(data = artworkUri).apply {
                        crossfade(true)
                    }.build()
                ),
                contentDescription = "Artist Artwork",
                modifier = Modifier
                    .size(300.dp)
                    .then(if (showBorder) Modifier.border(2.dp, Color.White, RectangleShape) else Modifier),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = song.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = song.artist,
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = {
                    viewModel.toggleFavoriteStatus(song)
                }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Slider
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                currentPosition = (it * song.duration.toInt()).toInt()
            },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = formatTime(currentPosition), color = Color.Gray)
            Text(text = formatTime(song.duration.toInt()), color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Icon(
                imageVector = Icons.Filled.SkipPrevious,
                contentDescription = "Skip Previous",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(64.dp)
            )
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skip Next",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

