package com.example.purrytify.ui.screens.home

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.MainViewModel
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.relationship.RecentlyPlayedWithSong

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    mainViewModel: MainViewModel

) {
    val recentlyPlayedSongs = homeViewModel.recentlyPlayedSongs.collectAsState(initial = emptyList()).value
    val newAddedSongs = homeViewModel.newAddedSongs.collectAsState(initial = emptyList()).value
    Log.d("Init home played songs: ", "$recentlyPlayedSongs")
    Log.d("Init home new songs: ", "$newAddedSongs")
    Log.d("USER ID INIT: ", "${homeViewModel.userId}")


    Scaffold { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues),
            recentlyPlayedSongs = recentlyPlayedSongs,
            newAddedSongs = newAddedSongs,
            onNavigate = { route -> navController.navigate(route) }
        )
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier,
    recentlyPlayedSongs: List<RecentlyPlayedWithSong>,
    newAddedSongs: List<Songs>,
    onNavigate: (String) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isLandscape) 8.dp else 16.dp), // Adjust horizontal padding in landscape
    ) {
        Spacer(modifier = Modifier.height(if (isLandscape) 0.dp else 8.dp)) // Reduce/remove top spacer in portrait
        TopBar(isLandscape = isLandscape) // Pass isLandscape to TopBar

        Spacer(modifier = Modifier.height(if (isLandscape) 12.dp else 24.dp))

        if (isLandscape) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "New Songs For You",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NewSongsSectionLandscape(newAddedSongs, onNavigate)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recently Played",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    RecentlyPlayedSectionLandscape(recentlyPlayedSongs, onNavigate)
                }
            }
        } else {
            Text(
                text = "New Songs For You",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            RecommendedPlaylistsSection(newAddedSongs, onNavigate, isLandscape = false)

            if (recentlyPlayedSongs.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Recently Played",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                RecentlyPlayedSectionPortrait(recentlyPlayedSongs, onNavigate)
            }
        }

        Spacer(modifier = Modifier.height(if (isLandscape) 8.dp else 16.dp)) // Reduce bottom spacer
    }
}
@Composable
fun TopBar(isLandscape: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = if (isLandscape) 8.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Purrytify",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun NewSongsSectionLandscape(newAddedSongs: List<Songs>, onNavigate: (String) -> Unit) {
    LazyColumn {
        items(newAddedSongs) { song ->
            SongItemLandscape(song, onNavigate, isRecommended = true)
        }
    }
}

@Composable
fun RecentlyPlayedSectionLandscape(recentlyPlayedSongs: List<RecentlyPlayedWithSong>, onNavigate: (String) -> Unit) {
    LazyColumn {
        items(recentlyPlayedSongs) { recentlyPlayedWithSong ->
            SongItemLandscape(recentlyPlayedWithSong.song, onNavigate)
        }
    }
}

@Composable
fun RecentlyPlayedSectionPortrait(recentlyPlayedSongs: List<RecentlyPlayedWithSong>, onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(recentlyPlayedSongs) { recentlyPlayedWithSong ->
            SongItemPortrait(recentlyPlayedWithSong.song, onNavigate)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun RecommendedPlaylistsSection(newAddedSongs: List<Songs>, onNavigate: (String) -> Unit, isLandscape: Boolean) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 8.dp else 16.dp)
    ) {
        items(newAddedSongs) { song ->
            SongItemRecommendedPortrait(song, onNavigate, isLandscape = isLandscape)
        }
    }
}

@Composable
fun SongItemLandscape(
    song: Songs,
    onNavigate: (String) -> Unit,
    isRecommended: Boolean = false
) {
    val context = LocalContext.current
    val artworkUri = song.artwork?.let { Uri.parse(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onNavigate("songDetails/${song.id}") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context).data(data = artworkUri).apply {
                    crossfade(true)
                }.build()
            ),
            contentDescription = if (isRecommended) "New Song Artwork" else "Recently Played Artwork",
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = song.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, maxLines = 1)
            Text(text = song.artist, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun SongItemRecommendedPortrait(
    song: Songs,
    onNavigate: (String) -> Unit,
    isLandscape: Boolean
) {
    val context = LocalContext.current
    val artworkUri = song.artwork?.let { Uri.parse(it) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(if (isLandscape) 80.dp else 140.dp).clickable { onNavigate("songDetails/${song.id}") }
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context).data(data = artworkUri).apply {
                    crossfade(true)
                }.build()
            ),
            contentDescription = song.name,
            modifier = Modifier
                .size(if (isLandscape) 80.dp else 140.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.name,
            color = Color.White,
            fontSize = if (isLandscape) 12.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
        Text(
            text = song.artist,
            color = Color.Gray,
            fontSize = if (isLandscape) 10.sp else 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
fun SongItemPortrait(
    song: Songs,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val artworkUri = song.artwork?.let { Uri.parse(it) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onNavigate("songDetails/${song.id}") },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(context).data(data = artworkUri).apply {
                    crossfade(true)
                }.build()
            ),
            contentDescription = "Recently Played Artwork",
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = song.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp, maxLines = 1)
            Text(text = song.artist, fontSize = 12.sp, color = Color.Gray, maxLines = 1)
        }
    }
}