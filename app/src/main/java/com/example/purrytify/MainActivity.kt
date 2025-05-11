package com.example.purrytify

import android.content.pm.PackageManager
import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.purrytify.ui.screens.login.LoginScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.navigation.AppNavigation
import com.example.purrytify.ui.theme.PurrytifyTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NetworkMonitor.initialize(applicationContext)
        checkNotificationPermission()
        
        // Start and bind the music service
        viewModel.startMusicService(this)
        viewModel.bindMusicService(this)
        
        // Handle intent
        handleIntent(intent)

        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent() // Tidak perlu meneruskan NetworkMonitor
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        // Handle any media-related intents if needed
        Log.d("MainActivity", "Handling intent: ${intent?.action}")
    }

    override fun onDestroy() {
        super.onDestroy()
        NetworkMonitor.unregisterNetworkCallback(applicationContext) // Unregister Singleton di onDestroy
    }



    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                Log.d("MainActivity", "Requesting notification permission")
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            } else {
                Log.d("MainActivity", "Notification permission already granted")
            }
        } 
    }

    companion object {
        private const val REQUEST_NOTIFICATION_PERMISSION = 1
    }
}

@Composable
fun AppContent() {
    val isConnected by NetworkMonitor.isConnected.collectAsState()
    ConnectivityStatus(isConnected = isConnected)
    AppSelector()
}

@Composable
fun AppSelector(viewModel: MainViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        Log.d("AppSelector", "Login state changed: isLoggedIn = $isLoggedIn")
    }

    if (isLoggedIn) {
        AppNavigation(
            mainViewModel = viewModel,
            onLogout = {
                Log.d("AppSelector", "Logout callback triggered")
                viewModel.logout()
            }
        )
    } else {
        Log.d("AppSelector", "Showing login screen")
        LoginScreen(
            onLoginSuccess = {
                Log.d("AppSelector", "Login success callback triggered")
                viewModel.setLoggedIn(true)
            }
        )
    }
}

@Composable
fun ConnectivityStatus(isConnected: Boolean) {
    val showDialog = remember { mutableStateOf(!isConnected) }

    LaunchedEffect(isConnected) {
        showDialog.value = !isConnected
    }

    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                if (!isConnected) {
                    Log.d("ConnectivityStatus", "Tombol kembali ditekan saat tidak ada koneksi.")
                } else {
                    showDialog.value = false
                }
            },
            title = { Text("Koneksi Terputus") },
            text = { Text("Saat ini perangkat Anda tidak terhubung ke internet.") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog.value = false
                }) {
                    Text("OK")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    PurrytifyTheme {
        LoginScreen()
    }
}