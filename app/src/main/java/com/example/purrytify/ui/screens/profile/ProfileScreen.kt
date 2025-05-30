package com.example.purrytify.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.R
import com.example.purrytify.db.dao.ListeningActivityDao
import com.example.purrytify.ui.screens.soundcapsule.SoundCapsuleCard
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.utils.CsvExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToListeningStats: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToTopSongs: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToTopArtist: (Int, Int) -> Unit = { _, _ -> }
) {
    val profile by viewModel.profile.collectAsState()
    val songsCount by viewModel.songsCount.collectAsState()
    val favoriteCount by viewModel.favoriteCount.collectAsState()
    val playedCount by viewModel.playedCount.collectAsState()
    val isError by viewModel.isError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val noInternet by viewModel.noInternet.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val scrollState = rememberScrollState()
    val soundCapsuleData by viewModel.soundCapsuleData.collectAsState()
    val soundCapsulesData by viewModel.soundCapsulesData.collectAsState()
    val context = LocalContext.current

    // State for showing permission dialog
    var showPermissionDialog by remember { mutableStateOf(false) }

    // Permission launcher for storage access
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            handleCsvDownload(context, soundCapsuleData)
        } else {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permission Required") },
            text = { Text("Storage permission is required to save the CSV file. Please enable it in Settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        openAppSettings(context)
                    }
                ) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Fetch profile data when first loaded and when returning to this screen
    LaunchedEffect(Unit) {
        viewModel.getProfile()
        viewModel.getSongsCount()
        viewModel.getFavoriteSongsCount()
        viewModel.getTotalListenedCount()
        viewModel.getSoundCapsule() // Panggil fungsi untuk mengambil SoundCapsule data
    }

    LaunchedEffect(profile) {
        println("Profile Photo: ${profile?.profilePhoto}")
    }
    val profileImagePainter = rememberAsyncImagePainter(
        model = if (!profile?.profilePhoto.isNullOrBlank()) {
            profile!!.profilePhoto
        } else {
            R.drawable.ic_profile_placeholder
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to Color(0xFF00667B),
                        0.5f to Color.Black,
                        1.0f to Color.Black
                    )
                )
            )
            .verticalScroll(scrollState), // Terapkan verticalScroll di semua orientasi
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Untuk memusatkan konten error/no internet
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Memuat Profil...", color = Color.White)
        } else if (noInternet) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Tidak ada koneksi",
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Tidak ada koneksi internet",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Periksa koneksi Anda dan coba lagi.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.getProfile() },
                    colors = ButtonDefaults.buttonColors(SpotifyGreen)
                ) {
                    Text("Coba Lagi", color = Color.White)
                }
            }
        } else if (isError) {
            Text("Gagal memuat profil.", color = Color.Red)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(40.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    Image(
                        painter = profileImagePainter,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    profile?.username ?: "...",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    profile?.location ?: "...",
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onNavigateToEditProfile() },
                    colors = ButtonDefaults.buttonColors(Color.DarkGray)
                ) {
                    Text("Edit Profile", color = Color.White)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat(songsCount.toString(), "SONGS")
                    ProfileStat(favoriteCount.toString(), "LIKED")
                    ProfileStat(playedCount.toString(), "LISTENED")
                }

                Spacer(modifier = Modifier.height(60.dp))

                ProfileMenuItem(
                    title = "Settings",
                    icon = Icons.Default.Settings,
                    onClick = onNavigateToSettings
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Display multiple Sound Capsule cards (up to 3)
                if (soundCapsulesData.isNotEmpty()) {
                    soundCapsulesData.forEach { capsule ->
                        // Extract year and month from the monthYear string (format: "MMMM yyyy")
                        val parts = capsule.monthYear.split(" ")
                        val monthName = parts[0]
                        val year = parts[1].toInt()
                        val month = getMonthNumber(monthName)
                        
                        SoundCapsuleCard(
                            soundCapsule = capsule,
                            onTimeListenedClick = { onNavigateToListeningStats(year, month) },
                            onTopSongClick = { onNavigateToTopSongs(year, month) },
                            onTopArtistClick = { onNavigateToTopArtist(year, month) },
                            onDownloadClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    // Android 10 (API 29) and above don't need storage permission for Downloads
                                    handleCsvDownload(context, capsule)
                                } else {
                                    // For older versions, request permission
                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // Fallback to the old implementation with a single card if the list is empty
                    val currentDate = java.time.LocalDate.now()
                    val year = currentDate.year
                    val month = currentDate.monthValue
                    
                    SoundCapsuleCard(
                        soundCapsule = soundCapsuleData,
                        onTimeListenedClick = { onNavigateToListeningStats(year, month) },
                        onTopSongClick = { onNavigateToTopSongs(year, month) },
                        onTopArtistClick = { onNavigateToTopArtist(year, month) },
                        onDownloadClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                // Android 10 (API 29) and above don't need storage permission for Downloads
                                handleCsvDownload(context, soundCapsuleData)
                            } else {
                                // For older versions, request permission
                                permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = SpotifyGreen,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )

            Spacer(modifier = Modifier.weight(1f))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color.LightGray)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
                .verticalScroll(rememberScrollState()), // Tambahkan verticalScroll di preview juga
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Image(
                painter = painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("135220xx", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Indonesia", fontSize = 14.sp, color = Color.LightGray)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {}, colors = ButtonDefaults.buttonColors(Color.DarkGray)) {
                Text("Edit Profile", color = Color.White)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ProfileStat("135", "SONGS")
                ProfileStat("32", "LIKED")
                ProfileStat("50", "LISTENED")
            }
            Spacer(modifier = Modifier.height(60.dp))
            ProfileMenuItem(title = "Settings", icon = Icons.Default.Settings, onClick = {})
            Spacer(modifier = Modifier.height(16.dp))
            SoundCapsuleCard(soundCapsule = null) // Tambahkan preview SoundCapsule
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}

private fun handleCsvDownload(context: Context, soundCapsule: ProfileViewModel.SoundCapsuleViewModel?) {
    if (soundCapsule == null) {
        Toast.makeText(context, "No data available to download", Toast.LENGTH_SHORT).show()
        return
    }

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val file = CsvExporter.exportSoundCapsuleToCSV(context, soundCapsule)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "CSV saved to Downloads: ${file.name}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "Failed to save CSV: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}

private fun getMonthNumber(monthName: String): Int {
    return when (monthName.lowercase()) {
        "january" -> 1
        "february" -> 2
        "march" -> 3
        "april" -> 4
        "may" -> 5
        "june" -> 6
        "july" -> 7
        "august" -> 8
        "september" -> 9
        "october" -> 10
        "november" -> 11
        "december" -> 12
        else -> java.time.LocalDate.now().monthValue // fallback to current month
    }
}