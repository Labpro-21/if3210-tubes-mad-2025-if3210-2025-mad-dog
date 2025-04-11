package com.example.purrytify.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.purrytify.MainViewModel
import com.example.purrytify.ui.components.BottomNavigationBar
import com.example.purrytify.ui.components.LeftNavigationBar
import com.example.purrytify.ui.components.MiniPlayer
import com.example.purrytify.ui.screens.home.HomeScreen
import com.example.purrytify.ui.screens.library.LibraryScreen
import com.example.purrytify.ui.screens.profile.ProfileScreen
import com.example.purrytify.ui.screens.setting.SettingScreen
import com.example.purrytify.ui.screens.songdetail.SongDetailScreen
import com.example.purrytify.ui.theme.SpotifyBlack

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object SongDetail : Screen("songDetails/{songId}")
}

@Composable
fun AppNavigation(
    onLogout: () -> Unit = {},
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Home.route
    val isMiniPlayerActive by mainViewModel.isMiniPlayerActive.collectAsState()
    val showMiniPlayer = (currentRoute == Screen.Home.route || currentRoute == Screen.Library.route) && isMiniPlayerActive

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val showNavigationBar = when (currentRoute) {
        Screen.Home.route, Screen.Library.route, Screen.Profile.route, Screen.SongDetail.route -> true
        else -> false
    }

    Row(modifier = Modifier.fillMaxHeight()) {
        if (isLandscape && showNavigationBar) {
            LeftNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(Screen.Home.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.fillMaxHeight()
            )
        }

        Scaffold(
            containerColor = SpotifyBlack,
            bottomBar = {
                if (showNavigationBar && (isLandscape || !isLandscape)) { // Apply bottomBar logic in both orientations
                    Column {
                        if (showMiniPlayer) {
                            MiniPlayer(
                                mainViewModel = mainViewModel,
                                onMiniPlayerClick = {
                                    if (mainViewModel.currentSong.value != null) {
                                        navController.navigate("songDetails/${mainViewModel.currentSong.value!!.id}")
                                    }
                                }
                            )
                        }
                        if (!isLandscape) { // Show BottomNavigationBar only in portrait
                            BottomNavigationBar(
                                currentRoute = currentRoute,
                                onNavigate = { route ->
                                    navController.navigate(route) {
                                        popUpTo(Screen.Home.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(start = if (isLandscape && showNavigationBar) 8.dp else 0.dp) // Adjust content start padding
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(navController = navController, mainViewModel = mainViewModel)
                }
                composable(Screen.Library.route) {
                    LibraryScreen(
                        navController = navController,
                        mainViewModel = mainViewModel
                    )
                }
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onLogout = onLogout
                    )
                }
                composable(
                    Screen.SongDetail.route,
                    arguments = listOf(navArgument("songId") { type = NavType.IntType })
                ) { backStackEntry ->
                    val songId = backStackEntry.arguments?.getInt("songId") ?: -1
                    SongDetailScreen(
                        songId = songId,
                        navController = navController,
                        mainViewModel = mainViewModel
                    )
                }
            }
        }
    }
}