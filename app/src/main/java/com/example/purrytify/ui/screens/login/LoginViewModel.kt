package com.example.purrytify.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private var _isUserInitiatedLogin = false
    val isUserInitiatedLogin: Boolean get() = _isUserInitiatedLogin

    fun setUserInitiatedLogin(value: Boolean) {
        _isUserInitiatedLogin = value
        Log.d("LoginViewModel", "User initiated login set to: $value")
    }

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                val isLoggedIn = authRepository.isLoggedIn()
                if (isLoggedIn) {
                    Log.d("LoginViewModel", "Initial check shows already logged in")
                    _loginState.value = LoginState.Success
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error checking login status: ${e.message}")
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank()) {
            _loginState.value = LoginState.Error("Email cannot be empty")
            return
        } else if (password.isBlank()) {
            _loginState.value = LoginState.Error("Password cannot be empty")
            return
        }

        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            Log.d("LoginViewModel", "Logging in with email: $email")

            authRepository.login(email, password)
                .onSuccess {
                    Log.d("LoginViewModel", "Login successful")
                    _loginState.value = LoginState.Success
                }
                .onFailure { error ->
                    Log.e("LoginViewModel", "Login failed: ${error.message}")
                    _loginState.value = LoginState.Error(error.message ?: "Unknown error")
                    _isUserInitiatedLogin = false
                }
        }
    }
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}