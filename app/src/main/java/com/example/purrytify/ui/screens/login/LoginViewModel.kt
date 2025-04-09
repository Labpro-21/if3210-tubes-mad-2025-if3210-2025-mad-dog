package com.example.purrytify.ui.screens.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.db.AppDatabase
import com.example.purrytify.db.entity.Users
import com.example.purrytify.services.TokenServiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository.getInstance(application)
    private val usersDao = AppDatabase.getDatabase(application).usersDao()

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
                    insertOrUpdateUser() // Insert atau update user setelah login berhasil
                    _loginState.value = LoginState.Success
                    TokenServiceManager.startTokenValidationService(getApplication())
                }
                .onFailure { error ->
                    Log.e("LoginViewModel", "Login failed: ${error.message}")
                    _loginState.value = LoginState.Error(error.message ?: "Unknown error")
                    _isUserInitiatedLogin = false
                }
        }
    }

    private fun insertOrUpdateUser() {
        viewModelScope.launch {
            authRepository.verifyToken()
                .onSuccess { userData ->
                    val existingUser = usersDao.getUserById(userData.user.id)
                    if (existingUser == null) {
                        val newUser = Users(id = userData.user.id, name = userData.user.username)
                        usersDao.insertUser(newUser)
                        Log.d("LoginViewModel", "User inserted: ${userData.user.id}, ${userData.user.username}")
                    } else {
                        Log.d("LoginViewModel", "User already exists: ${userData.user.id}, ${userData.user.username}")
                    }
                }
                .onFailure { error ->
                    Log.e("LoginViewModel", "Failed to get user data from token: ${error.message}")
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