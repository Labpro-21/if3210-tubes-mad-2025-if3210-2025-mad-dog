package com.example.purrytify.ui.screens.playlist

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.db.entity.Songs
import com.example.purrytify.MainViewModel

@Composable
fun DailyPlaylistDetailScreen(
    dailyPlaylist: List<Songs>,
    navController: NavController,
    mainViewModel: MainViewModel
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Daily Playlist", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Button(
                onClick = {
                    if (dailyPlaylist.isNotEmpty()) {
                        mainViewModel.setIsOnlineSong(false)
                        mainViewModel.playSong(dailyPlaylist.first())
                    }
                },
                enabled = dailyPlaylist.isNotEmpty()
            ) {
                Text("Play All")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn {
            items(dailyPlaylist.size) { idx ->
                val song = dailyPlaylist[idx]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            mainViewModel.setIsOnlineSong(false)
                            mainViewModel.playDailySong(song)
                            navController.navigate("songDetail/${song.id}")
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(song.artwork),
                        contentDescription = song.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(song.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(song.artist, color = Color.Gray)
                    }
                }
            }
        }
    }
}