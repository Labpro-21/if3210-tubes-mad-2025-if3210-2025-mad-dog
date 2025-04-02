package com.example.purrytify.ui.screens.setting

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut

    fun logout(onLogoutComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoggingOut.value = true
            Log.d(TAG, "Starting logout")

            try {
                authRepository.logout()
                    .onSuccess {
                        Log.d(TAG, "Logout successful")
                        _isLoggingOut.value = false
                        onLogoutComplete()
                    }
                    .onFailure { error ->
                        Log.e(TAG, "Logout failed: ${error.message}")
                        _isLoggingOut.value = false
                        onLogoutComplete()
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Exception during logout: ${e.message}")
                _isLoggingOut.value = false
                onLogoutComplete()
            }
        }
    }
    companion object {
        private const val TAG = "SettingViewModel"
    }
}