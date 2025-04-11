package com.example.purrytify.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.purrytify.ui.theme.SpotifyGreen

@Composable
fun LeftNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF121212))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center // Align items vertically to the center
    ) {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
            label = { Text("Library") },
            selected = currentRoute == "library",
            onClick = { onNavigate("library") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
            label = { Text("Profile") },
            selected = currentRoute == "profile",
            onClick = { onNavigate("profile") },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = SpotifyGreen,
                selectedTextColor = SpotifyGreen,
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color.Gray,
                unselectedTextColor = Color.Gray
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun LeftNavigationBarPreviewHomeSelected() {
    LeftNavigationBar(currentRoute = "home", onNavigate = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun LeftNavigationBarPreviewLibrarySelected() {
    LeftNavigationBar(currentRoute = "library", onNavigate = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun LeftNavigationBarPreviewProfileSelected() {
    LeftNavigationBar(currentRoute = "profile", onNavigate = {})
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
fun LeftNavigationBarPreviewNoneSelected() {
    LeftNavigationBar(currentRoute = "", onNavigate = {})
}