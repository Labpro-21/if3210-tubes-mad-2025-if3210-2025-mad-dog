package com.example.purrytify.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.R
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.ui.components.BottomNavigationBar
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.example.purrytify.ui.theme.PurrytifyTheme

@Composable
fun HomeScreen(
    onNavigate: (String) -> Unit = {}
) {
    val currentRoute = "home"
    
    Scaffold(
        containerColor = SpotifyBlack,
        bottomBar = { 
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            ) 
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun HomeScreenContent(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            TopBar()
        }

        item {
            Text(
                text = "Good evening",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        item {
            RecentlyPlayedSection()
        }

        item {
            Text(
                text = "Made for you",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            RecommendedPlaylistsSection()
        }

        item {
            Text(
                text = "Popular playlists",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            PopularPlaylistsSection()
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
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
fun RecentlyPlayedSection() {
    val recentlyPlayed = listOf(
        "Lo-fi Study Mix" to R.drawable.bg,
        "Daily Mix 1" to R.drawable.bg,
        "Chill Vibes" to R.drawable.bg,
        "Top Hits 2023" to R.drawable.bg
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(recentlyPlayed) { (name, image) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(140.dp)
            ) {
                Image(
                    painter = painterResource(id = image),
                    contentDescription = name,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RecommendedPlaylistsSection() {
    val playlists = listOf(
        "Rainy Day Jazz" to R.drawable.bg,
        "Workout Motivation" to R.drawable.bg,
        "Indie Discoveries" to R.drawable.bg,
        "Sleep Sounds" to R.drawable.bg
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(playlists) { (name, image) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(160.dp)
            ) {
                Image(
                    painter = painterResource(id = image),
                    contentDescription = name,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PopularPlaylistsSection() {
    val playlists = listOf(
        "Today's Top Hits" to R.drawable.bg,
        "Global Hits" to R.drawable.bg,
        "Viral Tracks" to R.drawable.bg,
        "Classic Rock" to R.drawable.bg
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(playlists) { (name, image) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(160.dp)
            ) {
                Image(
                    painter = painterResource(id = image),
                    contentDescription = name,
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable   @Preview
fun HomeScreenPreview() {
    PurrytifyTheme {
        HomeScreen()
    }
}