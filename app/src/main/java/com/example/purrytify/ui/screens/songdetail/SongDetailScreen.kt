package com.example.purrytify.ui.screens.songdetail

import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.MainViewModel
import com.example.purrytify.R
import com.example.purrytify.data.model.AudioOutputDevice
import com.example.purrytify.ui.screens.songdetail.AudioOutputViewModel
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.ui.navigation.Screen
import com.example.purrytify.ui.screens.library.UploadButton
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.utils.ColorUtils
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.launch
import android.Manifest
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import com.example.purrytify.ui.qrcode.QrCodeActivity

@Composable
fun SongDetailScreen(
    songId: Int,
    navController: NavController,
    mainViewModel: MainViewModel,
    viewModel: SongDetailViewModel = viewModel(
        factory = SongDetailViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel
        )
    ),
    isOnline: Boolean,
    region: String = "GLOBAL",
    isDailyPlaylist: Boolean = false
) {
    val uiState by viewModel.songDetails.collectAsState()
    //val playbackCompletedState by mainViewModel.isPlaybackCompleted.collectAsState()

    var showOptionsDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    val isUpdateSuccessful by viewModel.isUpdateSuccessful.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val audioOutputViewModel: AudioOutputViewModel = viewModel()
    val devices by audioOutputViewModel.devices.collectAsState()
    val selectedDevice by audioOutputViewModel.selectedDevice.collectAsState()
    val audioError by audioOutputViewModel.error.collectAsState()
    var showDeviceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = LocalContext.current as? android.app.Activity
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            audioOutputViewModel.scanDevices()
            showDeviceDialog = true
        } else {
            Toast.makeText(context, "Bluetooth permission required to scan devices", Toast.LENGTH_SHORT).show()
        }
    }

    /*
    LaunchedEffect(playbackCompletedState) {
        if (playbackCompletedState) {
            Log.d("SongDetailScreen", "Playback completed for song ID: $songId")
            viewModel.updateCompletedAndDuration()

        } else {
            Log.d("SongDetailScreen", "Playback is active or not completed for song ID: $songId")
        }
    }
*/
    LaunchedEffect(songId, isOnline, region) {
        mainViewModel.setIsOnlineSong(isOnline)
        if (isOnline && region == "GLOBAL") {
            viewModel.fetchSongByDeepLink(songId)
        } else {
            viewModel.loadSongDetails(songId, isOnline = isOnline, region = region, isDailyPlaylist = isDailyPlaylist)
        }
    }
    LaunchedEffect(isUpdateSuccessful) {
        if (isUpdateSuccessful) {
            mainViewModel.stopPlaying()
            Log.d("UpdatedSong: ", "Berhasil uppdate")
            viewModel.resetUpdateSuccessful()
        }
    }
    LaunchedEffect(uiState) {
        // Auto-play lagu
        if (uiState is SongDetailViewModel.SongDetailUiState.Success) {
            val song = (uiState as SongDetailViewModel.SongDetailUiState.Success).song
            mainViewModel.setIsOnlineSong(isOnline)
            mainViewModel.playSong(song)
            viewModel.insertRecentlyPlayed(song, isOnline)
        }
    }

    when (uiState) {
        SongDetailViewModel.SongDetailUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SongDetailViewModel.SongDetailUiState.Error -> {
            val errorMessage = (uiState as SongDetailViewModel.SongDetailUiState.Error).message
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "Failed to load song", color = Color.White)
                    Text(text = errorMessage, color = Color.Gray)
                    Button(
                        onClick = { 
                            viewModel.loadSongDetails(songId, isOnline, region) 
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
        is SongDetailViewModel.SongDetailUiState.Success -> {
            SongDetailsContent(
                song = (uiState as SongDetailViewModel.SongDetailUiState.Success).song,
                navController = navController,
                showBorder = false,
                viewModel = viewModel,
                onEditClick = { showEditDialog = true },
                mainViewModel = mainViewModel,
                isLandscape = isLandscape,
                onOptionClick = { showOptionsDialog = true },
                isOnline = isOnline,
                currentRegion = region,
                selectedDevice = selectedDevice,
                onShowDeviceDialog = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                            audioOutputViewModel.scanDevices()
                            showDeviceDialog = true
                        } else {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        }
                    } else {
                        audioOutputViewModel.scanDevices()
                        showDeviceDialog = true
                    }
                },
                isDailyPlaylist = isDailyPlaylist
            )
            if (showEditDialog) {
                Dialog(
                    onDismissRequest = { showEditDialog = false },
                    properties = DialogProperties(dismissOnClickOutside = false)
                ) {
                    EditSongDialogContent(
                        song = (uiState as SongDetailViewModel.SongDetailUiState.Success).song,
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
                        viewModel.deleteSong((uiState as SongDetailViewModel.SongDetailUiState.Success).song)
                        mainViewModel.stopPlaying()
                        navController.popBackStack()
                        showOptionsDialog = false
                    },
                    isLandscape = isLandscape,
                    isOnline = isOnline
                )
            }
            if (audioError != null) {
                AlertDialog(
                    onDismissRequest = { audioOutputViewModel.clearError() },
                    title = { Text("Audio Output Error") },
                    text = { Text(audioError ?: "") },
                    confirmButton = {
                        TextButton(onClick = { audioOutputViewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                )
            }
            if (showDeviceDialog) {
                AudioOutputDeviceDialog(
                    devices = devices,
                    selectedDevice = selectedDevice,
                    onSelect = {
                        audioOutputViewModel.selectDevice(it)
                        showDeviceDialog = false
                    },
                    onDismiss = { showDeviceDialog = false },
                    onRefresh = { audioOutputViewModel.scanDevices() }
                )
            }
        }
        SongDetailViewModel.SongDetailUiState.Empty -> {
            // Handle empty state if needed
        }
    }
}

@Composable
fun OptionsDialog(
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isLandscape: Boolean,
    isOnline: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .run {
                if (isLandscape) {
                    width(IntrinsicSize.Min)
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
                if (!isOnline) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color.White),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                } else {
                    Text("No options available for online songs.", color = Color.Gray)
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

@SuppressLint("DefaultLocale")
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
    isLandscape: Boolean,
    onOptionClick: () -> Unit,
    isOnline: Boolean,
    currentRegion: String,
    selectedDevice: AudioOutputDevice?,
    onShowDeviceDialog: () -> Unit,
    isDailyPlaylist: Boolean
) {
    val currentPosition by mainViewModel.currentPosition.collectAsState()
    val isPlaying by mainViewModel.isPlaying.collectAsState()
    val currentSong by mainViewModel.currentSong.collectAsState()
    val isDownloading by viewModel.isDownloading.collectAsState()
    val isAlreadyDownloaded by viewModel.isAlreadyDownloaded.collectAsState()
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
    var dominantColor by remember { mutableStateOf<Brush>(Brush.verticalGradient(colors = listOf(SpotifyBlack, SpotifyBlack))) }
    LaunchedEffect(artworkUri) {
        launch {
            dominantColor = ColorUtils.generateDominantColorGradient(context, artworkUri)
        }
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColor)
            .padding(if (isLandscape) 8.dp else 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
            Text(
                text = if (isOnline) "Online Song" else "Offline Song",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (!isOnline) {
                IconButton(onClick = onOptionClick) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Options", tint = Color.White)
                }
            } else {
                Spacer(modifier = Modifier.size(24.dp)) // Use a fixed size
            }
        }
        Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painter,
                contentDescription = "Artist Artwork",
                modifier = Modifier
                    .size(if (isLandscape) 200.dp else 300.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = Color.Black.copy(alpha = 0.5f),
                        ambientColor = Color.Black.copy(alpha = 0.5f)
                    )
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

                if (isOnline) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isAlreadyDownloaded -> {
                                // Show downloaded indicator
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = "Already Downloaded",
                                    tint = Color.Green,
                                    modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                                )
                            }
                            isDownloading -> {
                                // Show loading indicator during download
                                CircularProgressIndicator(
                                    modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> {
                                // Show download button if not downloaded
                                IconButton(onClick = {
                                    viewModel.downloadSingleSong()
                                }) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.download),
                                        contentDescription = "Download Song",
                                        tint = Color.White,
                                        modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
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
                Text(
                    text = if (isSameSong) formatTime(currentPosition) else "00:00",
                    color = Color.Gray,
                    fontSize = if (isLandscape) 12.sp else 14.sp
                )
                Text(
                    text = formatTime(song.duration.toInt()),
                    color = Color.Gray,
                    fontSize = if (isLandscape) 12.sp else 14.sp
                )
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                IconButton(onClick = {
                    if (!isOnline) {
                        viewModel.skipPrevious(song.id, isOnline = false) { previousSongId ->
                            navController.navigate(Screen.SongDetail.route.replace("{songId}", previousSongId.toString())) {
                                popUpTo(Screen.SongDetail.route.replace("{songId}", song.id.toString())) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    } else {
                        viewModel.skipPrevious(song.id, isOnline = true, currentRegion = currentRegion) { previousSongId ->
                            navController.navigate(Screen.SongDetailOnline.route.replace("{region}", currentRegion).replace("{songId}", previousSongId.toString())) {
                                popUpTo(Screen.SongDetailOnline.route.replace("{region}", currentRegion).replace("{songId}", song.id.toString())) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Skip Previous",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                    )
                }
                IconButton(onClick = {
                    mainViewModel.playSong(song)
                    viewModel.insertRecentlyPlayed(song, isOnline)
                    //viewModel.insertListeningActivity(song,isOnline)
                }) {
                    Icon(
                        imageVector = if (isPlaying && isSameSong) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 48.dp else 64.dp)
                    )
                }
                IconButton(onClick = {
                    if (!isOnline) {
                        viewModel.skipNext(song.id, isOnline = false, onNavigate = { nextSongId ->
                            navController.navigate(Screen.SongDetail.route.replace("{songId}", nextSongId.toString())) {
                                popUpTo(Screen.SongDetail.route.replace("{songId}", song.id.toString())) { inclusive = true }
                                launchSingleTop = true
                            }
                        })
                    } else {
                        viewModel.skipNext(
                            currentSongId = song.id, 
                            isOnline = true, 
                            currentRegion = currentRegion,
                            onNavigate = { nextSongId ->
                                navController.navigate(Screen.SongDetailOnline.route.replace("{region}", currentRegion).replace("{songId}", nextSongId.toString())) {
                                    popUpTo(Screen.SongDetailOnline.route.replace("{region}", currentRegion).replace("{songId}", song.id.toString())) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            isDailyPlaylist = isDailyPlaylist
                        )
                    }
                }) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Skip Next",
                        tint = Color.White,
                        modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isLandscape) 16.dp else 32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clickable { onShowDeviceDialog() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_baseline_speaker_24),
                            contentDescription = "Output Device",
                            tint = if (selectedDevice != null) Color.Green else Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = selectedDevice?.name ?: "Internal Speaker",
                            color = if (selectedDevice != null) Color.Green else Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
                if (isOnline) {
                    val context = LocalContext.current
                    Row {
                        IconButton(onClick = {
                            // Share link
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://puritify-deeplink.vercel.app/song/${song.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, null))
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_share),
                                contentDescription = "Share Song",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            // Share QR code
                            val qrIntent = Intent(context, QrCodeActivity::class.java).apply {
                                putExtra("DEEP_LINK", "https://puritify-deeplink.vercel.app/song/${song.id}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(qrIntent)
                        }) {
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

@Composable
fun AudioOutputDeviceDialog(
    devices: List<AudioOutputDevice>,
    selectedDevice: AudioOutputDevice?,
    onSelect: (AudioOutputDevice) -> Unit,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Output Device") },
        text = {
            Column {
                devices.forEach { device ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(device) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = device.id == selectedDevice?.id,
                            onClick = { onSelect(device) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(device.name)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(if (device.isConnected) "Connected" else "Available", color = if (device.isConnected) Color.Green else Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh) { Text("Refresh") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}