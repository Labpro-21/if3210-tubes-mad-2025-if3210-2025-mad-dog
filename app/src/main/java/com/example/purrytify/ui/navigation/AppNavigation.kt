package com.example.purrytify.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.purrytify.ui.screens.album.AlbumScreen
import com.example.purrytify.ui.screens.home.HomeScreen
import com.example.purrytify.ui.screens.home.HomeViewModel
import com.example.purrytify.ui.screens.library.LibraryScreen
import com.example.purrytify.ui.screens.playlist.DailyPlaylistDetailScreen
import com.example.purrytify.ui.screens.profile.EditProfileScreen
import com.example.purrytify.ui.screens.profile.MapsPickerScreen
import com.example.purrytify.ui.screens.profile.ProfileScreen
import com.example.purrytify.ui.screens.setting.SettingScreen
import com.example.purrytify.ui.screens.songdetail.SongDetailScreen
import com.example.purrytify.ui.screens.statistics.ListeningStatsScreen
import com.example.purrytify.ui.theme.SpotifyBlack

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Library : Screen("library")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    object SongDetail : Screen("songDetails/{songId}")
    object SongDetailOnline : Screen("songDetails/online/{region}/{songId}")
    object Album: Screen("album/{region}")
    object EditProfile : Screen("editProfile")
    object LocationPicker : Screen("locationPicker")
    object ListeningStats: Screen("listeningstats")
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
    val showMiniPlayer = (currentRoute == Screen.Home.route || currentRoute == Screen.Library.route || currentRoute == Screen.Album.route ) && isMiniPlayerActive
    val isOnlinePlaying by mainViewModel.isOnlineSong.collectAsState() // Get the isOnlinePlaying state

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val showNavigationBar = when (currentRoute) {
        Screen.Home.route, Screen.Library.route, Screen.Profile.route, Screen.SongDetail.route, Screen.SongDetailOnline.route, Screen.Album.route -> true
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
                if (showNavigationBar && (isLandscape || !isLandscape)) {
                    Column {
                        if (showMiniPlayer) {
                            MiniPlayer(
                                mainViewModel = mainViewModel,
                                onMiniPlayerClick = {
                                    if (mainViewModel.currentSong.value != null) {
                                        val route = if (isOnlinePlaying) {
                                            Screen.SongDetailOnline.route
                                                .replace("{region}", mainViewModel.currentSong.value!!.description)
                                                .replace("{songId}", mainViewModel.currentSong.value!!.id.toString())
                                        } else {
                                            Screen.SongDetail.route.replace("{songId}", mainViewModel.currentSong.value!!.id.toString())
                                        }
                                        navController.navigate(route)
                                    }
                                },
                            )
                        }
                        if (!isLandscape) {
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
                    .padding(start = if (isLandscape && showNavigationBar) 8.dp else 0.dp)
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
                    val backStackEntry = navController.previousBackStackEntry
                    val profileUpdated = backStackEntry?.savedStateHandle?.get<Boolean>("profileUpdated") ?: false
                    
                    ProfileScreen(
                        onNavigateToSettings = {
                            navController.navigate(Screen.Settings.route)
                        },
                        onNavigateToEditProfile = {
                            navController.navigate(Screen.EditProfile.route)
                        },
                        onNavigateToListeningStats = {
                            navController.navigate(Screen.ListeningStats.route)
                        }
                    )
                    
                    if (profileUpdated) {
                        if (backStackEntry != null) {
                            backStackEntry.savedStateHandle.remove<Boolean>("profileUpdated")
                        }
                    }
                }
                composable(Screen.EditProfile.route) {
                    val countryCode = navController.currentBackStackEntry?.savedStateHandle?.get<String>("selectedCountryCode")
                    val countryName = navController.currentBackStackEntry?.savedStateHandle?.get<String>("selectedCountryName")
                    
                    EditProfileScreen(
                        onNavigateBack = {
                            navController.previousBackStackEntry?.savedStateHandle?.set("profileUpdated", true)
                            navController.popBackStack()
                        },
                        onNavigateToLocationPicker = {
                            navController.navigate(Screen.LocationPicker.route)
                        },
                        countryCode = countryCode,
                        countryName = countryName
                    )
                    
                    // If data was passed from the location picker, remove it to prevent reprocessing
                    LaunchedEffect(countryCode, countryName) {
                        if (countryCode != null && countryName != null) {
                            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selectedCountryCode")
                            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("selectedCountryName")
                        }
                    }
                }
                composable(Screen.LocationPicker.route) {
                    MapsPickerScreen(
                        onNavigateBack = { countryCode, countryName ->
                            if (countryCode.isNotEmpty()) {
                                navController.previousBackStackEntry?.savedStateHandle?.set("selectedCountryCode", countryCode)
                                navController.previousBackStackEntry?.savedStateHandle?.set("selectedCountryName", countryName)
                            }
                            navController.popBackStack()
                        }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingScreen(
                        onLogout = onLogout,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(Screen.ListeningStats.route) {
                    ListeningStatsScreen(
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                composable(
                    Screen.Album.route,
                    arguments = listOf(navArgument("region") {type = NavType.StringType}))
                {backStackEntry ->
                    val region = backStackEntry.arguments?.getString("region")?:"GLOBAL"
                    AlbumScreen(
                        region = region,
                        navController = navController,
                        mainViewModel = mainViewModel,
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
                        mainViewModel = mainViewModel,
                        isOnline = false
                    )
                    
                    val songDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.purrytify.ui.screens.songdetail.SongDetailViewModel>()
                    
                    LaunchedEffect(Unit) {
                        mainViewModel.registerNavigationCallbacks(
                            skipToNext = songDetailViewModel::skipNext,
                            skipToPrevious = songDetailViewModel::skipPrevious
                        )
                    }
                }
                composable(
                    Screen.SongDetailOnline.route,
                    arguments = listOf(
                        navArgument("region") { type = NavType.StringType },
                        navArgument("songId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val region = backStackEntry.arguments?.getString("region") ?: "GLOBAL"
                    val songId = backStackEntry.arguments?.getInt("songId") ?: -1
                    SongDetailScreen(
                        songId = songId,
                        navController = navController,
                        mainViewModel = mainViewModel,
                        isOnline = true,
                        region = region
                    )
                    
                    val songDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.example.purrytify.ui.screens.songdetail.SongDetailViewModel>()
                    
                    LaunchedEffect(Unit) {
                        mainViewModel.registerNavigationCallbacks(
                            skipToNext = songDetailViewModel::skipNext,
                            skipToPrevious = songDetailViewModel::skipPrevious
                        )
                    }
                }

                composable(
                    route = "album/{region}?isDailyPlaylist={isDailyPlaylist}",
                    arguments = listOf(
                        navArgument("region") { type = NavType.StringType },
                        navArgument("isDailyPlaylist") { 
                            type = NavType.BoolType
                            defaultValue = false 
                        }
                    )
                ) { backStackEntry ->
                    val region = backStackEntry.arguments?.getString("region") ?: "GLOBAL"
                    val isDailyPlaylist = backStackEntry.arguments?.getBoolean("isDailyPlaylist") ?: false
                    
                    AlbumScreen(
                        region = region,
                        navController = navController,
                        mainViewModel = mainViewModel,
                        isDailyPlaylist = isDailyPlaylist
                    )
                }
            }
        }
    }
}