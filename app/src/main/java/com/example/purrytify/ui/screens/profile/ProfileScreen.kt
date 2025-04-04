package com.example.purrytify.ui.screens.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.ui.theme.SpotifyGreen
import androidx.compose.material.icons.filled.Settings
import com.example.purrytify.R
import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import coil.compose.rememberAsyncImagePainter

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {}
) {
    var profileImage by remember { mutableStateOf<Uri?>(null) }
    var name by remember { mutableStateOf("13522xxx") }
    var country by remember { mutableStateOf("Indonesia") }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        profileImage = uri
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colorStops = arrayOf(
                    0.0f to Color(0xFF004E92),
                    0.5f to Color.Black,
                    1.0f to Color.Black
                )
            )
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Spacer(modifier = Modifier.height(40.dp))

        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = if (profileImage != null) rememberAsyncImagePainter(profileImage)
                else painterResource(id = R.drawable.ic_profile_placeholder),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            IconButton(
                onClick = { launcher.launch("image/*") },
                modifier = Modifier
                    .size(30.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text(country, fontSize = 14.sp, color = Color.LightGray)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
            },
            colors = ButtonDefaults.buttonColors(Color.DarkGray)
        ) {
            Text("Edit Profile", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStat("135", "SONGS")
            ProfileStat("32", "LIKED")
            ProfileStat("50", "LISTENED")
        }


        Spacer(modifier = Modifier.height(60.dp))

        Box(

        ){
            ProfileMenuItem(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings
            )
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
@Composable @Preview
fun ProfileScreenPreview() {
    PurrytifyTheme {
        ProfileScreen()
    }
}