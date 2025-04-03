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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
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
            SongDetailsContent(song = uiState.song, navController = navController, showBorder = true)
        }
        is SongDetailUiState.Error -> {
            Text(uiState.message, color = Color.Red)
        }
        else -> {}
    }
}

@Composable
fun SongDetailsContent(song: Songs, navController: NavController, showBorder: Boolean) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var dominantColor by remember { mutableStateOf(SpotifyBlack) } // Default color

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
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
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

                Icon(
                    imageVector = Icons.Filled.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Slider
        Slider(
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "1:44", color = Color.Gray)
            Text(text = "3:50", color = Color.Gray)
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

@Preview(showBackground = false)
@Composable
fun SongDetailsScreenPreview() {
    val dummySong = Songs(
        id = 1,
        userId = 129,
        name = "Starboy",
        artist = "The Weeknd, Daft Punk",
        duration = 230000,
        artwork = "content://media/external/audio/albumart/1",
        description = "This is a dummy song description.",
        filePath = "/path/to/dummy/song.mp3"
    )
    val navController = rememberNavController()

    Column(modifier = Modifier.background(Color.Black)) {
        SongDetailsContent(song = dummySong, navController = navController, showBorder = true)
    }
}