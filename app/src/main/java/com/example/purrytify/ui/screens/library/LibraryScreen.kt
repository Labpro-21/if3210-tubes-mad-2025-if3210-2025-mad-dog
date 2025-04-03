package com.example.purrytify.ui.screens.library

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.purrytify.R
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.ui.theme.PurrytifyTheme
import java.io.File

@Composable
fun LibraryScreen(libraryViewModel: LibraryViewModel = viewModel()) {
    var showAllSongs by remember { mutableStateOf(true) }
    var showAddSongDialog by remember { mutableStateOf(false) }


    val songsFlow = if (showAllSongs) {
        libraryViewModel.allSongs.collectAsState(initial = emptyList()).value
    } else {
        libraryViewModel.favoriteSongs.collectAsState(initial = emptyList()).value
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Library",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { showAllSongs = true },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Show All Songs")
                }

                Button(
                    onClick = { showAllSongs = false },
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(text = "Show Liked Songs")
                }

                IconButton(
                    onClick = { showAddSongDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Song")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(songsFlow) { song ->
                    SongItem(song, libraryViewModel)
                }
            }
        }

        if (showAddSongDialog) {
            SlideUpDialog(
                onDismiss = { showAddSongDialog = false },
                content = {
                    AddSongDialogContent(
                        onDismiss = { showAddSongDialog = false },
                        libraryViewModel = libraryViewModel
                    )
                }
            )
        }
    }
}
@Composable
fun SongItem(song: Songs, libraryViewModel: LibraryViewModel) {
    var showOptions by remember { mutableStateOf(false) }
    var optionsAnchor by remember { mutableStateOf(DpOffset.Zero) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),

        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val artworkUri = song.artwork.let { File(it).toUri() }

        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context).data(data = artworkUri).apply {
                    crossfade(true)
                }.build()
            ),
            contentDescription = "Artist Artwork",
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = song.name, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = song.artist, fontSize = 12.sp, color = Color.Gray)
        }

        IconButton(onClick = {
            showOptions = !showOptions
            optionsAnchor = DpOffset(0.dp, 40.dp)
        }) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options")
        }

        DropdownMenu(
            expanded = showOptions,
            onDismissRequest = { showOptions = false },
            offset = optionsAnchor
        ) {
            DropdownMenuItem(
                text = { Text("Like") },
                onClick = {
                    libraryViewModel.toggleFavoriteStatus(song)
                    showOptions = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = Color.Yellow
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("Delete", color = Color.Red) },
                onClick = {
                    libraryViewModel.deleteSong(song)
                    showOptions = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            )
        }
    }
}

@Composable
fun SlideUpDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val offsetY = remember { Animatable(with(density) { 300.dp.toPx() }) }

    LaunchedEffect(Unit) {
        offsetY.animateTo(0f, animationSpec = tween(durationMillis = 300))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, offsetY.value.toInt()) }
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) // Rounded corners at the top
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            content()
        }
    }
}
@Composable
fun AddSongDialogContent(onDismiss: () -> Unit, libraryViewModel: LibraryViewModel) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
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
            val metadata = libraryViewModel.getMetadata(fileUri)
            title = metadata.first ?: ""
            artist = metadata.second ?: ""
        }
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Upload Song",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            UploadButton(
                text = "Upload Photo",
                icon = R.drawable.ic_image,
                onClick = { photoPickerLauncher.launch("image/*")}
            )
            UploadButton(
                text = "Upload Audio File",
                icon = R.drawable.ic_file,
                onClick = { filePickerLauncher.launch("audio/*") }
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
                    if (fileUri == null) {
                        Toast.makeText(context, "Please select a file.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    libraryViewModel.addSong(
                        fileUri,
                        photoUri,
                        title,
                        artist
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save")
            }
        }
    }
}
@Composable
fun UploadButton(text: String, icon: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            contentScale = ContentScale.Fit
        )
        Text(
            text = text,
            style = TextStyle(fontSize = 14.sp),
            modifier = Modifier.padding(top = 8.dp)
        )
        Icon(
            imageVector = Icons.Default.AttachFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}
