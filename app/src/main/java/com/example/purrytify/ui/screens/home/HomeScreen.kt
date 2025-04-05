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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.purrytify.MainViewModel
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.db.relationship.RecentlyPlayedWithSong
import java.io.File

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
            recentlyPlayedSongs= recentlyPlayedSongs,
            newAddedSongs= newAddedSongs,
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


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        TopBar()

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "New Songs For You",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))
        RecommendedPlaylistsSection(newAddedSongs, onNavigate)

        if (recentlyPlayedSongs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Recently Played",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            RecentlyPlayedSection(recentlyPlayedSongs, onNavigate)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TopBar() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Purrytify",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Profile",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun RecentlyPlayedSection(recentlyPlayedSongs: List<RecentlyPlayedWithSong>, onNavigate: (String) -> Unit) {
    LazyColumn {
        items(recentlyPlayedSongs) { recentlyPlayedWithSong ->
            SongItem(recentlyPlayedWithSong.song, onNavigate, photoSize = 60)
        }
    }
}

@Composable
fun RecommendedPlaylistsSection(newAddedSongs: List<Songs>, onNavigate: (String) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(newAddedSongs) { song ->
            SongItem(song, onNavigate, photoSize = 140, isRecommended = true)
        }
    }
}

@Composable
fun SongItem(
    song: Songs,
    onNavigate: (String) -> Unit,
    isRecommended: Boolean = false,
    photoSize: Int
) {
    val context = LocalContext.current
    val artworkUri = song.artwork?.let { Uri.parse(it) }

    if (isRecommended) {
        // Tampilan untuk Recommended (New Added)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(140.dp).clickable { onNavigate("songDetails/${song.id}") }
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context).data(data = artworkUri).apply {
                        crossfade(true)
                    }.build()
                ),
                contentDescription = song.name,
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = song.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable {
                    onNavigate("songDetails/${song.id}")
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context).data(data = artworkUri).apply {
                        crossfade(true)
                    }.build()
                ),
                contentDescription = "Artist Artwork",
                modifier = Modifier
                    .size(photoSize.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = song.name, fontWeight = FontWeight.Bold, color = Color.White)
                Text(text = song.artist, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}