package com.example.purrytify.ui.screens.setting

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.purrytify.ui.theme.PurrytifyTheme


@Composable
fun SettingScreen(){
    Box(
        modifier = Modifier.fillMaxSize()
    ){

    }
}


@Composable @Preview
fun SettingScreenPreview() {
    PurrytifyTheme {
        SettingScreen()
    }
}
