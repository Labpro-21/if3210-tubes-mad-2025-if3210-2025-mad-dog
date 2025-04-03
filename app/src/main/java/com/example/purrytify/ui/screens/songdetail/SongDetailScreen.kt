// SongDetailScreen.kt

package com.example.purrytify.ui.screens.songdetail

import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.db.entity.Songs
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.imageLoader
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.ui.theme.SpotifyLightGray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.purrytify.R
import com.example.purrytify.ui.screens.library.UploadButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    songId: Int,
    viewModel: SongDetailViewModel = viewModel(),
    navController: NavController
) {
    val uiState = viewModel.songDetails.collectAsState().value
    var showEditDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    LaunchedEffect(songId) {
        viewModel.loadSongDetails(songId)
    }

    when (uiState) {
        SongDetailUiState.Loading -> {
            Text("Loading...", color = Color.White)
        }
        is SongDetailUiState.Success -> {
            SongDetailsContent(
                song = uiState.song,
                navController = navController,
                showBorder = false,
                viewModel = viewModel,
                onEditClick = { showEditDialog = true }
            )
            if (showEditDialog) {
                ModalBottomSheet(
                    onDismissRequest = { showEditDialog = false },
                    sheetState = sheetState
                ) {
                    EditSongDialogContent(
                        song = uiState.song,
                        onDismiss = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    showEditDialog = false
                                }
                            }
                        },
                        viewModel = viewModel,
                        navController= navController
                    )
                }
            }
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
fun SongDetailsContent(
    song: Songs,
    navController: NavController,
    showBorder: Boolean,
    viewModel: SongDetailViewModel,
    onEditClick: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var dominantColor by remember { mutableStateOf(SpotifyBlack) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var showOptions by remember { mutableStateOf(false) }
    var optionsAnchor by remember { mutableStateOf(DpOffset.Zero) }
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
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

    LaunchedEffect(isPlaying) {
        if (isPlaying && mediaPlayer != null) {
            while (isPlaying) {
                withContext(Dispatchers.Main) {
                    currentPosition = mediaPlayer?.currentPosition ?: 0
                    sliderPosition = currentPosition.toFloat() / song.duration.toFloat()
                }
                delay(1000) // Update every 1 second
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
                optionsAnchor = DpOffset(
                    x = (-50).dp,
                    y = (-50).dp
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
                DropdownMenuItem(
                    text = { Text("Edit", color = SpotifyLightGray) },
                    onClick = {
                        onEditClick()
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
                painter = painter,
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
                mediaPlayer?.seekTo(currentPosition)
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
            IconButton(onClick = {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    if (mediaPlayer == null) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(context, Uri.fromFile(File(song.filePath)))
                            prepare()
                            start()
                            isPlaying = true
                        }
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }
            }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skip Next",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
}

@Composable
fun EditSongDialogContent(song: Songs, onDismiss: () -> Unit, viewModel: SongDetailViewModel,navController: NavController) {
    var title by remember { mutableStateOf(song.name) }
    var artist by remember { mutableStateOf(song.artist) }
    var photoUri by remember { mutableStateOf<Uri?>(song.artwork.let { File(it).toUri() }) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        photoUri = uri
    }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        fileUri = uri
        if (fileUri != null) {
            val metadata = viewModel.getMetadata(fileUri)
            title = metadata.first ?: ""
            artist = metadata.second ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Edit Song",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UploadButton(
                text = "Edit Photo",
                icon = R.drawable.ic_image,
                onClick = { photoPickerLauncher.launch("image/*") },
                isSelected = photoUri != null
            )
            UploadButton(
                text = "Edit File",
                icon = R.drawable.ic_file,
                onClick = { filePickerLauncher.launch("audio/*") },
                isSelected = fileUri != null
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    viewModel.updateSong(song.copy(name = title, artist = artist),photoUri= photoUri,fileUri= fileUri)
                    onDismiss()
                    navController.popBackStack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        }
    }
}