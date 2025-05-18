package com.example.purrytify.ui.screens.soundcapsule

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.purrytify.db.dao.ListeningActivityDao
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.clickable

@Composable
fun SoundCapsuleCard(
    soundCapsule: ListeningActivityDao.SoundCapsule?,
    onTimeListenedClick: () -> Unit = {},
    onTopSongClick: () -> Unit = {},
    onTopArtistClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Your Sound Capsule",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (soundCapsule != null) {
                Text(
                    text = soundCapsule.monthYear,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Row 1: Time Listened Card (Full Width)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTimeListenedClick() },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF212121)
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
                        artworkUrl = null,
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

                // Row 3: Listening Streak Card (Full Width)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF212121) // Warna abu-abu gelap
                    )
                ) {
                    DetailRow(
                        label = "Listening streak",
                        value = "${soundCapsule.listeningDayStreak} days",
                        useLargerIcon = true,
                        textColor = Color.White // Sesuaikan warna teks jika perlu
                    )
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (!artistName.isNullOrEmpty() && title == "Top song") {
                Text(
                    text = artistName,
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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