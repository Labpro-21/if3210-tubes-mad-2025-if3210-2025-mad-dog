package com.example.purrytify

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        checkLoginStatus()
        observeAuthState()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val loggedIn = authRepository.isLoggedIn()
                _isLoggedIn.value = loggedIn
                Log.d(TAG, "Initial login status: ${_isLoggedIn.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking login status: ${e.message}")
                _isLoggedIn.value = false
            }
        }
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.authState.collect { state ->
                Log.d(TAG, "Auth state changed: $state")
                when (state) {
                    is AuthState.Authenticated -> {
                        _isLoggedIn.value = true

                    }
                    is AuthState.NotAuthenticated -> {
                        _isLoggedIn.value = false
                    }
                    else -> {}
                }
            }
        }
    }

    fun setLoggedIn(value: Boolean) {
        _isLoggedIn.value = value
    }

    fun logout() {
        viewModelScope.launch {
            Log.d(TAG, "MainViewModel: Logout called")
            try {
                authRepository.logout()
                Log.d(TAG, "Logout completed, isLoggedIn = ${_isLoggedIn.value}")
            } catch (e: Exception) {
                Log.e(TAG, "Error during logout: ${e.message}")
                // Force Logout
                _isLoggedIn.value = false
            }
        }
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}