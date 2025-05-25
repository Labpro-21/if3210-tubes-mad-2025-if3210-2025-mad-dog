package com.example.purrytify.ui.screens.topsongs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.purrytify.R
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopSongsScreen(
    onNavigateBack: () -> Unit,
    year: Int = LocalDate.now().year,
    month: Int = LocalDate.now().monthValue,
    viewModel: TopSongsViewModel = viewModel()
) {
    val topSongs by viewModel.topSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentMonthYear by viewModel.monthYear.collectAsState()
    
    LaunchedEffect(year, month) {
        viewModel.loadTopSongs(year, month)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentMonthYear ?: "Your Top Songs") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    items(topSongs) { song ->
                        TopSongItem(song)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun TopSongItem(song: com.example.purrytify.db.dao.ListeningActivityDao.MonthlyPlayedSong) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF212121)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Artwork
            AsyncImage(
                model = song.artwork,
                contentDescription = "Song artwork",
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                placeholder = painterResource(id = R.drawable.music_placeholder),
                error = painterResource(id = R.drawable.music_placeholder)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Song details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = song.artist,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            // Play count
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${song.playCount}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "plays",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
} 