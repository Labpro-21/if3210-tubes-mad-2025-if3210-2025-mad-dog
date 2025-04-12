package com.example.purrytify.ui.screens.library

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.purrytify.MainViewModel
import com.example.purrytify.R
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.utils.MediaUtils
import kotlinx.coroutines.launch
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel = viewModel(),
    navController: NavController,
    mainViewModel: MainViewModel
) {
    var showAllSongs by remember { mutableStateOf(true) }
    var showAddSongDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val userId = libraryViewModel.userId
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val isSearchActive = searchQuery.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        if (userId == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Anda belum login",
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        } else {
            // Determine which songs to show based on search state and filter
            val songsFlow = when {
                isSearchActive -> {
                    libraryViewModel.searchResults.collectAsState(initial = emptyList()).value
                }
                showAllSongs -> {
                    libraryViewModel.allSongs.collectAsState(initial = emptyList()).value
                }
                else -> {
                    libraryViewModel.favoriteSongs.collectAsState(initial = emptyList()).value
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Navbar)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpotifyBlack)
                        .padding(vertical = if (isLandscape) 0.dp else 16.dp)
                        .padding(horizontal = if (isLandscape) 8.dp else 16.dp)
                        .zIndex(1f),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Library",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = { showAddSongDialog = true },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Song")
                        }
                    }

                    // Search bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { libraryViewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text("Search songs or artists", color = Color.Gray) },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.Gray
                            ) 
                        },
                        trailingIcon = { 
                            if (isSearchActive) {
                                IconButton(onClick = { libraryViewModel.updateSearchQuery("") }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF212121),
                            unfocusedContainerColor = Color(0xFF212121),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedIndicatorColor = Color.Gray,
                            unfocusedIndicatorColor = Color.Gray,
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )

                    // Only show filter buttons if not in search mode
                    if (!isSearchActive) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Button(
                                onClick = { showAllSongs = true },
                                modifier = Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (showAllSongs) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                    contentColor = if (showAllSongs) Color.Black else Color.White,
                                )
                            ) {
                                Text(text = "All")
                            }

                            Button(
                                onClick = { showAllSongs = false },
                                modifier = Modifier.padding(top = 8.dp, end = 8.dp, bottom = 8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!showAllSongs) MaterialTheme.colorScheme.primary else Color.DarkGray,
                                    contentColor = if (!showAllSongs) Color.Black else Color.White,
                                )
                            ) {
                                Text(text = "Liked")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.Gray, thickness = 1.dp)
                }

                // RecyclerView Content
                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)) {
                            AndroidView(
                                factory = { context ->
                                    val recyclerView = RecyclerView(context)
                                    recyclerView.layoutManager = LinearLayoutManager(context)
                                    recyclerView
                                },
                                update = { recyclerView ->
                                    recyclerView.adapter = SongAdapter(songsFlow, onNavigate = { route -> navController.navigate(route) })
                                    (recyclerView.adapter as SongAdapter).notifyDataSetChanged()
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    AndroidView(
                        factory = { context ->
                            val recyclerView = RecyclerView(context)
                            recyclerView.layoutManager = LinearLayoutManager(context)
                            recyclerView
                        },
                        update = { recyclerView ->
                            recyclerView.adapter = SongAdapter(songsFlow, onNavigate = { route -> navController.navigate(route) })
                            (recyclerView.adapter as SongAdapter).notifyDataSetChanged()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Add Song Dialog
            if (showAddSongDialog) {
                if (isLandscape) {
                    Dialog(
                        onDismissRequest = { showAddSongDialog = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f) // Atur lebar dialog (contoh 60% layar)
                                .background(MaterialTheme.colorScheme.surface) // Set background color
                        ) {
                            AddSongDialogContent(
                                onDismiss = { showAddSongDialog = false },
                                libraryViewModel = libraryViewModel,
                                isLandscape = true
                            )
                        }
                    }
                } else {
                    ModalBottomSheet(
                        onDismissRequest = { showAddSongDialog = false },
                        sheetState = sheetState
                    ) {
                        AddSongDialogContent(
                            onDismiss = {
                                scope.launch { sheetState.hide() }.invokeOnCompletion {
                                    if (!sheetState.isVisible) {
                                        showAddSongDialog = false
                                    }
                                }
                            },
                            libraryViewModel = libraryViewModel,
                            isLandscape = false
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun AddSongDialogContent(
    onDismiss: () -> Unit,
    libraryViewModel: LibraryViewModel,
    isLandscape: Boolean
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var fileUri by remember { mutableStateOf<Uri?>(null) }
    var uploadSuccess by remember { mutableStateOf(false) }
    var audioUploaded by remember { mutableStateOf(false) }
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
            .padding(8.dp)
            .imePadding(),
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
                    text = "Upload Song",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = {
                    if (fileUri == null) {
                        Toast.makeText(context, "Please select a file.", Toast.LENGTH_SHORT).show()
                        return@IconButton
                    }
                    libraryViewModel.addSong(fileUri, photoUri, title, artist)
                    uploadSuccess = true
                    onDismiss()
                }) {
                    Icon(Icons.Filled.UploadFile, contentDescription = "Upload")
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        } else {
            Text(
                text = "Upload Song",
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
                        if (fileUri == null) {
                            Toast.makeText(context, "Please select a file.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        libraryViewModel.addSong(fileUri, photoUri, title, artist)
                        uploadSuccess = true
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save")
                }
            }
        }

        if (uploadSuccess && !isLandscape) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Upload Successful",
                tint = Color.Green,
                modifier = Modifier.padding(top = 16.dp)
            )
        } else if (uploadSuccess && isLandscape) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Upload Successful",
                tint = Color.Green,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
    }
}

@Composable
fun UploadButton(text: String, icon: Int, onClick: () -> Unit, photoUri: Uri? = null, isFileUploaded: Boolean = false) {
    Box(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp)
            .drawDashedBorder(color = Color.Gray, dashWidth = 10f, gapWidth = 10f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (photoUri == null && !isFileUploaded) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    style = TextStyle(fontSize = 14.sp, color = Color.Gray),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    contentScale = ContentScale.Fit
                )
            }
        } else if (photoUri != null) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(LocalContext.current).data(data = photoUri).apply {
                        crossfade(true)
                    }.build()
                ),
                contentDescription = "Selected Photo",
                modifier = Modifier.fillMaxSize(), // Mengisi penuh Box
                contentScale = ContentScale.Crop // Crop agar mengisi penuh
            )
        } else if (isFileUploaded) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Upload Successful",
                tint = Color.Green,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

fun Modifier.drawDashedBorder(color: Color, dashWidth: Float, gapWidth: Float): Modifier = this.then(
    Modifier.drawBehind {
        val strokeWidth = 1.dp.toPx()
        val width = size.width
        val height = size.height
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

        drawRoundRect(
            color = color,
            style = Stroke(
                width = strokeWidth,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f)
            ),
            cornerRadius = cornerRadius
        )
    }
)