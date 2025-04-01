package com.example.purrytify.ui.screens.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tokenManager = TokenManager(application)
    private val authRepository = AuthRepository(tokenManager)
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState
    
    fun login(email: String, password: String) {
        if (email.isBlank()) {
            _loginState.value = LoginState.Error("email cannot be empty")
            return
        }else if (password.isBlank()) {
            _loginState.value = LoginState.Error("password cannot be empty")
            return
        }
        
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            authRepository.login(email, password)
                .onSuccess {
                    _loginState.value = LoginState.Success
                }
                .onFailure { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Unknown error")
                }
        }
    }

    fun logout() {
    viewModelScope.launch {
        tokenManager.clearTokens()
        _loginState.value = LoginState.Idle
    }
}
}

sealed class LoginState {
    data object Idle : LoginState()
    data object Loading : LoginState()
    data object Success : LoginState()
    data class Error(val message: String) : LoginState()
}