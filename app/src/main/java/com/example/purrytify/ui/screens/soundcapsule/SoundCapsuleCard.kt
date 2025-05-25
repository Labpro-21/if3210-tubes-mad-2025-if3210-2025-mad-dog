package com.example.purrytify.ui.screens.soundcapsule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.purrytify.R
import com.example.purrytify.ui.screens.profile.ProfileViewModel.SoundCapsuleViewModel
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign

@Composable
fun SoundCapsuleCard(
    soundCapsule: SoundCapsuleViewModel?,
    onTimeListenedClick: () -> Unit = {},
    onTopSongClick: () -> Unit = {},
    onTopArtistClick: () -> Unit = {},
    onDownloadClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Sound Capsule",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDownloadClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download CSV",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (soundCapsule != null) {
                Text(
                    text = soundCapsule.monthYear,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 20.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Row 1: Time Listened Card (Full Width)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTimeListenedClick() },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF141414)
                    )
                ) {
                    DetailRow(
                        label = "Time listened",
                        value = "${TimeUnit.MILLISECONDS.toMinutes(soundCapsule.totalTimeListened)} minutes",
                        useLargerIcon = true,
                        textColor = Color.White,
                        showArrow = true
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Row 2: Top Artist and Top Song Cards (Two Cards in a Row)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Card Top Artist
                    TopArtistSongCard(
                        title = "Top artist",
                        name = soundCapsule.topArtist ?: "-",
                        artworkUrl = findArtistArtwork(soundCapsule),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTopArtistClick() },
                        backgroundColor = Color(0xFF212121),
                        textColor = Color.White
                    )

                    // Card Top Song
                    TopArtistSongCard(
                        title = "Top song",
                        name = soundCapsule.topSong?.name ?: "-",
                        artworkUrl = soundCapsule.topSong?.artwork,
                        artistName = soundCapsule.topSong?.artist,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onTopSongClick() },
                        backgroundColor = Color(0xFF212121),
                        textColor = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Row 3: Listening Streak Card (Full Width) with Streak Song inside
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF212121)
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DetailRow(
                            label = "Longest song streak",
                            value = "${soundCapsule.listeningDayStreak} days",
                            useLargerIcon = true,
                            textColor = Color.White
                        )
                        
                        // Streak Song Section (inside the same card)
                        if (soundCapsule.streakSongs.isNotEmpty() && soundCapsule.topStreakSong != null) {
                            val topStreakSong = soundCapsule.topStreakSong
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF383838))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                ) {
                                    // Full artwork as background
                                    AsyncImage(
                                        model = topStreakSong.artwork,
                                        contentDescription = "Song artwork",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight(),
                                        contentScale = ContentScale.Crop,
                                        placeholder = painterResource(id = R.drawable.music_placeholder),
                                        error = painterResource(id = R.drawable.music_placeholder)
                                    )
                                    
                                    // Gradient overlay and text at bottom
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(
                                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.7f),
                                                        Color.Black.copy(alpha = 0.9f)
                                                    )
                                                )
                                            )
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "You had a ${soundCapsule.listeningDayStreak}-day streak",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = "You played ${topStreakSong.name} by ${topStreakSong.artist} day after day. You were on fire!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Text(
                                            text = topStreakSong.name ?: "Unknown Song",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Text(
                                            text = topStreakSong.artist ?: "Unknown Artist",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No consistent listening pattern detected yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Data Sound Capsule belum tersedia.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// Function to find an artwork URL for the top artist by looking at their songs
private fun findArtistArtwork(soundCapsule: SoundCapsuleViewModel?): String? {
    if (soundCapsule == null || soundCapsule.topArtist.isNullOrEmpty()) return null
    
    // First, check if the top song is by the top artist
    if (soundCapsule.topSong?.artist == soundCapsule.topArtist) {
        return soundCapsule.topSong.artwork
    }
    
    // Next, check if the top streak song is by the top artist
    if (soundCapsule.topStreakSong?.artist == soundCapsule.topArtist) {
        return soundCapsule.topStreakSong.artwork
    }
    
    // Finally, look through all streak songs to find one by the top artist
    val artistSong = soundCapsule.streakSongs.find { 
        it.artist == soundCapsule.topArtist && !it.artwork.isNullOrEmpty()
    }
    
    return artistSong?.artwork
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    iconId: Int? = null,
    useLargerIcon: Boolean = false,
    textColor: Color,
    showArrow: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconId != null) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = label,
                    tint = textColor,
                    modifier = Modifier.size(if (useLargerIcon) 36.dp else 24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column {
                Text(
                    text = label,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Text(
                    text = value,
                    color = Color(0xFF4CAF50),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        if (showArrow) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = textColor.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}


@Composable
fun TopArtistSongCard(
    title: String,
    name: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    artistName: String? = null,
    backgroundColor: Color,
    textColor: Color
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(160.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View details",
                    tint = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = name,
                color = textColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            if (!artistName.isNullOrEmpty() && title == "Top song") {
                Text(
                    text = artistName,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (!artworkUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "$title artwork",
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape),
                    placeholder = painterResource(id = R.drawable.music_placeholder),
                    error = painterResource(id = R.drawable.music_placeholder)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.music_placeholder),
                    contentDescription = "No artwork for $title",
                    tint = Color.Gray,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}