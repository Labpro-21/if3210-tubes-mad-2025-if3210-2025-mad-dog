package com.example.purrytify

import android.os.Bundle
import android.util.Log
//import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
//import com.example.purrytify.ui.screens.home.HomeScreen
import com.example.purrytify.ui.screens.login.LoginScreen
import com.example.purrytify.ui.theme.PurrytifyTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.navigation.AppNavigation


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PurrytifyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppSelector()
                }
            }
        }
    }
}

@Composable
fun AppSelector(viewModel: MainViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        Log.d("AppSelector", "Login state changed: isLoggedIn = $isLoggedIn")
    }

    if (isLoggedIn) {
        AppNavigation(
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
@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    PurrytifyTheme {
        LoginScreen()
    }
}