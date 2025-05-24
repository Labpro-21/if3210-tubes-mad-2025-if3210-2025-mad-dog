package com.example.purrytify.ui.screens.album

import AlbumViewModel
import android.app.Application
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.purrytify.MainViewModel
import com.example.purrytify.R
import com.example.purrytify.data.model.OnlineSongResponse
import com.example.purrytify.ui.navigation.Screen
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.utils.ColorUtils
import kotlinx.coroutines.launch
import java.util.Locale

// Helper function to calculate total duration
private fun calculateTotalDuration(songs: List<OnlineSongResponse>): String {
    var totalSeconds = 0
    songs.forEach { song ->
        val parts = song.duration.split(":")
        if (parts.size == 2) {
            val minutes = parts[0].toIntOrNull() ?: 0
            val seconds = parts[1].toIntOrNull() ?: 0
            totalSeconds += minutes * 60 + seconds
        }
    }
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d hr %02d min", hours, minutes)
    } else {
        String.format("%d min %02d sec", minutes, seconds)
    }
}

@Composable
fun AlbumScreen(
    region: String,
    navController: NavController,
    albumViewModel: AlbumViewModel = viewModel(factory = AlbumViewModel.Factory(LocalContext.current.applicationContext as Application)),
    mainViewModel: MainViewModel,
    isDailyPlaylist: Boolean = false 
) {
    LaunchedEffect(region, isDailyPlaylist) {
        if (isDailyPlaylist) {
            albumViewModel.loadDailyPlaylist(region)
        } else {
            albumViewModel.loadSongs(region)
        }
    }

    val songs by albumViewModel.songs.collectAsState()
    val isLoading by albumViewModel.isLoading.collectAsState()
    val errorMessage by albumViewModel.errorMessage.collectAsState()
    val downloadProgress by albumViewModel.downloadProgress.collectAsState()
    val context = LocalContext.current
    val TAG = "AlbumScreen"
    
    // Cache song sequences in MainViewModel whenever songs list changes
    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            Log.d(TAG,"cacheOnlineSongSequence region: $region, $songs")
            mainViewModel.cacheOnlineSongSequence(region, songs)
        }
    }

    // Determine the correct image and title based on type
    val (imageName, title, description) = if (isDailyPlaylist) {
        Triple(
            "daily_${region.lowercase(Locale.ROOT)}", 
            "Daily Mix - ${region.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}", 
            "Your daily personalized playlist based on your listening habits in ${region.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}."
        )
    } else {
        Triple(
            "top50${region.lowercase(Locale.ROOT)}", 
            "Top 50 ${region.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}", 
            "The hottest tracks in ${region.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }} right now. Updated daily."
        )
    }
    
    val imageId = context.resources.getIdentifier(imageName, "drawable", context.packageName)
    val artworkResource = if (imageId != 0) imageId else R.drawable.music_placeholder

    var dominantColor by remember { mutableStateOf<Brush>(Brush.verticalGradient(colors = listOf(SpotifyBlack, SpotifyBlack))) }
    LaunchedEffect(artworkResource) {
        launch {
            dominantColor = ColorUtils.generateDominantColorGradient(context, artworkResource)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(dominantColor)
            .padding(horizontal = 16.dp)
    ) {
        // Album Cover
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(8.dp),
                        spotColor = Color.Black.copy(alpha = 0.5f),
                        ambientColor = Color.Black.copy(alpha = 0.5f)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                Image(
                    painter = painterResource(id = artworkResource),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Title and Description
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        )

        // Total duration
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.music_placeholder), // Replace with actual clock icon
                contentDescription = "Duration",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Total: ${calculateTotalDuration(songs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }

        // Download and Play Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    albumViewModel.downloadSongs(
                        region = region, 
                        userId = albumViewModel.getCurrentUserid(),
                        isDailyPlaylist = isDailyPlaylist
                    ) 
                },
                modifier = Modifier
                    .size(36.dp)
                    .padding(start = 12.dp),
                enabled = !isLoading && downloadProgress == null // Disable button when loading or downloading
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.download),
                    contentDescription = "Download",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Play button
            Button(
                onClick = { /* Handle play action */ },
                modifier = Modifier
                    .size(32.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954) // Spotify green
                ),
                contentPadding = PaddingValues(0.dp),
                enabled = songs.isNotEmpty() && downloadProgress == null // Enable only when songs are loaded and not downloading
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.play), // Replace with actual play icon
                    contentDescription = "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Download Progress Indicator
        if (downloadProgress != null) {
            val (downloaded, total) = downloadProgress!!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = if (total > 0) downloaded.toFloat() / total.toFloat() else 0f,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mengunduh $downloaded dari $total lagu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (isLoading) {
            // Loading Indicator for initial song fetch
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null) {
            // Error message
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            // Song List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(songs) { _, song ->
                    SongItemRow(
                        song = song, 
                        navController = navController, 
                        region = region,
                        isDailyPlaylist = isDailyPlaylist
                    )
                }
            }
        }
    }
}

@Composable
fun SongItemRow(
    song: OnlineSongResponse, 
    navController: NavController, 
    region: String,
    isDailyPlaylist: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f))
            .clickable {
                val route = if (isDailyPlaylist) {
                    Screen.SongDetailOnline.route
                        .replace("{region}", region)
                        .replace("{songId}", song.id.toString()) + 
                        (if (isDailyPlaylist) "?isDailyPlaylist=true" else "")
                } else {
                    Screen.SongDetailOnline.route
                        .replace("{region}", region)
                        .replace("{songId}", song.id.toString())
                }
                navController.navigate(route)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isDailyPlaylist) {
            Text(
                text = song.rank.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .width(24.dp)
            )
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
        
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(song.artwork)
                .crossfade(true)
                .build(),
            contentDescription = song.title,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(id = R.drawable.music_placeholder),
            error = painterResource(id = R.drawable.music_placeholder)
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}