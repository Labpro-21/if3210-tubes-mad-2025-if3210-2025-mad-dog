// SongDetailScreen.kt

package com.example.purrytify.ui.screens.songdetail

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.db.entity.Songs
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.palette.graphics.Palette
import coil.imageLoader
import com.example.purrytify.MainViewModel
import com.example.purrytify.ui.theme.SpotifyBlack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.purrytify.R
import com.example.purrytify.ui.screens.library.UploadButton
import com.example.purrytify.utils.MediaUtils

@Composable
fun SongDetailScreen(
    songId: Int,
    viewModel: SongDetailViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel
) {
    val uiState = viewModel.songDetails.collectAsState().value
    var showOptionsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val isUpdateSuccessful by viewModel.isUpdateSuccessful.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(songId) {
        viewModel.loadSongDetails(songId)
    }
    LaunchedEffect(isUpdateSuccessful) {
        if (isUpdateSuccessful) {
            mainViewModel.stopPlaying()
            Log.d("UpdatedSong: ", "Berhasil uppdate")
            viewModel.resetUpdateSuccessful() // Reset state setelah digunakan
        }
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
                onEditClick = { showEditDialog = true },
                mainViewModel = mainViewModel,
                isLandscape = isLandscape, // Pass the orientation state
                onOptionClick = { showOptionsDialog = true }
            )
            if (showEditDialog) {
                Dialog(
                    onDismissRequest = { showEditDialog = false },
                    properties = DialogProperties(dismissOnClickOutside = false)
                ) {
                    EditSongDialogContent(
                        song = uiState.song,
                        onDismiss = { showEditDialog = false },
                        viewModel = viewModel,
                        navController = navController,
                    )
                }
            }
            if (showOptionsDialog) {
                OptionsDialog(
                    onDismissRequest = { showOptionsDialog = false },
                    onEdit = {
                        showEditDialog = true
                        showOptionsDialog = false
                    },
                    onDelete = {
                        viewModel.deleteSong(uiState.song)
                        mainViewModel.stopPlaying()
                        navController.popBackStack()
                        showOptionsDialog = false
                    },
                    isLandscape = isLandscape // Pass the orientation state here
                )
            }
        }
        is SongDetailUiState.Error -> {
            Text(uiState.message, color = Color.Red)
        }
        else -> {}
    }
}

@Composable
fun OptionsDialog(
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isLandscape: Boolean // Receive orientation state
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .run {
                if (isLandscape) {
                    width(IntrinsicSize.Min) // Don't fill width in landscape
                } else {
                    fillMaxWidth()
                }
            }
            .padding(horizontal = 48.dp)
            .clip(RoundedCornerShape(8.dp)),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        title = { Text("Options", color = MaterialTheme.colorScheme.onSurfaceVariant) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton( // Use OutlinedButton for border
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit")
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton( // Use OutlinedButton for border
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, Color.White),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
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
    onEditClick: () -> Unit,
    mainViewModel: MainViewModel,
    isLandscape: Boolean, // Receive the orientation state
    onOptionClick: () -> Unit
) {
    var dominantColor by remember { mutableStateOf(SpotifyBlack) }
    val currentPosition by mainViewModel.currentPosition.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val currentSong by mainViewModel.currentSong.collectAsState()
    val context = LocalContext.current
    val isSameSong = currentSong?.id == song.id
    val sliderPosition by remember(currentPosition, song.duration) {
        derivedStateOf {
            if (isSameSong) {
                currentPosition.toFloat() / song.duration.toFloat()
            } else {
                0f
            }
        }
    }
    val artworkUri = song.artwork?.let { Uri.parse(it) }

    val painter = rememberAsyncImagePainter(
        ImageRequest.Builder(context).data(data = artworkUri).apply {
            crossfade(true)
        }.build()
    )
    LaunchedEffect(currentPosition) {

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
            .padding(if (isLandscape) 8.dp else 16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (isLandscape) 16.dp else 32.dp),
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
            IconButton(onClick = onOptionClick) {
                Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.White)
            }
        }

        // Scrollable Content Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()), // Make this Column scrollable
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painter,
                contentDescription = "Artist Artwork",
                modifier = Modifier
                    .size(if (isLandscape) 200.dp else 300.dp)
                    .then(if (showBorder) Modifier.border(2.dp, Color.White, RectangleShape) else Modifier),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp))

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
                        fontSize = if (isLandscape) 20.sp else 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = song.artist,
                        fontSize = if (isLandscape) 16.sp else 18.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                IconButton(onClick = {
                    viewModel.toggleFavoriteStatus(song)
                }) {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

            // Slider
            Slider(
                value = sliderPosition,
                onValueChange = {
                    if (isSameSong) {
                        val newPosition = (it * song.duration.toInt()).toInt()
                        mainViewModel.seekTo(newPosition)
                    }
                },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                enabled = isSameSong
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = if (isSameSong) formatTime(currentPosition) else "00:00", color = Color.Gray, fontSize = if (isLandscape) 12.sp else 14.sp)
                Text(text = formatTime(song.duration.toInt()), color = Color.Gray, fontSize = if (isLandscape) 12.sp else 14.sp)
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Skip Previous",
                    tint = Color.White,
                    modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                )
                IconButton(onClick = {
                    mainViewModel.playSong(song)
                    viewModel.insertRecentlyPlayed(song.id)
                }) {
                    Icon(
                        imageVector = if (isPlaying && isSameSong) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 48.dp else 64.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Skip Next",
                    tint = Color.White,
                    modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                )
            }
        }
    }
}



@Composable
fun EditSongDialogContent(
    song: Songs,
    onDismiss: () -> Unit,
    viewModel: SongDetailViewModel,
    navController: NavController
) {
    var title by remember { mutableStateOf(song.name) }
    var artist by remember { mutableStateOf(song.artist) }
    var photoUri by remember { mutableStateOf<Uri?>(song.artwork?.let { Uri.parse(it) }) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var audioUploaded by remember { mutableStateOf(false) }
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                photoUri = it
            } catch (e: SecurityException) {
                Toast.makeText(context, "Gagal mendapatkan izin foto", Toast.LENGTH_SHORT).show()
                photoUri = null
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                fileUri = it
                val metadata = MediaUtils.getMetadata(it, context)
                title = metadata.first ?: ""
                artist = metadata.second ?: ""
                audioUploaded = true
            } catch (e: SecurityException) {
                Toast.makeText(context, "Gagal mendapatkan izin file", Toast.LENGTH_SHORT).show()
                fileUri = null
                audioUploaded = false
            }
        } ?: run {
            Toast.makeText(context, "Tidak ada file yang dipilih", Toast.LENGTH_SHORT).show()
            audioUploaded = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Cancel")
                }
                Text(
                    text = "Edit Song",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    viewModel.updateSong(song.copy(name = title, artist = artist), photoUri = photoUri, fileUri = fileUri)
                    onDismiss()
                }) {
                    Icon(Icons.Filled.Check, contentDescription = "Save")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = "Edit Song",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UploadButton(
                text = "Upload Photo",
                icon = R.drawable.ic_image,
                onClick = { photoPickerLauncher.launch(arrayOf("image/*")) },
                photoUri = photoUri
            )

            UploadButton(
                text = "Upload File",
                icon = R.drawable.ic_file,
                onClick = {
                    filePickerLauncher.launch(arrayOf("audio/*"))
                    audioUploaded = true
                },
                isFileUploaded = audioUploaded
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            placeholder = { Text(song.name) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        )

        OutlinedTextField(
            value = artist,
            onValueChange = { artist = it },
            label = { Text("Artist") },
            placeholder = { Text(song.artist) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )

        if (!isLandscape) {
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
                        viewModel.updateSong(song.copy(name = title, artist = artist), photoUri = photoUri, fileUri = fileUri)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            }
        }
    }
}