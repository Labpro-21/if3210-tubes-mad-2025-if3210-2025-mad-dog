// SongDetailUiState.kt

package com.example.purrytify.ui.screens.songdetail

import com.example.purrytify.db.entity.Songs

sealed class SongDetailUiState {
    object Loading : SongDetailUiState()
    data class Success(val song: Songs) : SongDetailUiState()
    data class Error(val message: String) : SongDetailUiState()
}