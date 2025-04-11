package com.example.purrytify.ui.screens.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.purrytify.R
import com.example.purrytify.ui.theme.PurrytifyTheme
import com.example.purrytify.ui.theme.SpotifyBlack
import com.example.purrytify.ui.theme.SpotifyGreen

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(loginState) {
        Log.d("LoginScreen", "Login state: $loginState")
        if (loginState is LoginState.Success) {
            Log.d("LoginScreen", "Login success detected, checking if it was user-initiated")
            if (viewModel.isUserInitiatedLogin) {
                Log.d("LoginScreen", "User initiated login, triggering success callback")
                onLoginSuccess()
            } else {
                Log.d("LoginScreen", "Automatic login detected, not triggering callback")
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image (sebagai lapisan paling bawah)
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = "Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Lapisan gradient di atas background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            SpotifyBlack.copy(alpha = 0.8f),
                            SpotifyBlack
                        ),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        )

        // Konten login (logo dan form)
        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Bagian kiri: Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Purrytify Logo",
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .weight(1f)
                        .padding(end = 16.dp),
                    contentScale = ContentScale.Fit
                )

                // Bagian kanan: Input Login
                Card(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = SpotifyBlack.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email", color = Color(0xFFAAAAAA)) },
                            modifier = Modifier
                                .border(1.dp, Color.Gray, RoundedCornerShape(15.dp))
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF212121),
                                unfocusedContainerColor = Color(0xFF212121),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = Color(0xFFAAAAAA),
                                unfocusedLabelColor = Color(0xFF888888),
                                cursorColor = Color.White
                            )
                        )
                        TextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Password", color = Color(0xFFAAAAAA)) },
                            modifier = Modifier
                                .border(1.dp, Color.Gray, RoundedCornerShape(15.dp))
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                        tint = Color(0xFFAAAAAA)
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF212121),
                                unfocusedContainerColor = Color(0xFF212121),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedLabelColor = Color(0xFFAAAAAA),
                                unfocusedLabelColor = Color(0xFF888888),
                                cursorColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.login(email, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            enabled = !isLoading && email.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("LOG IN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (loginState is LoginState.Error) {
                            Text(text = (loginState as LoginState.Error).message, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } else {
            // Layout untuk mode portrait (kode yang sudah ada)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top section with logo
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(200.dp))
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Purrytify Logo",
                        modifier = Modifier
                            .size(160.dp)
                            .padding(bottom = 16.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SpotifyBlack.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextField(value = email, onValueChange = { email = it }, label = { Text("Email", color = Color(0xFFAAAAAA)) }, modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(15.dp)).fillMaxWidth(), shape = RoundedCornerShape(15.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF212121), unfocusedContainerColor = Color(0xFF212121), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedLabelColor = Color(0xFFAAAAAA), unfocusedLabelColor = Color(0xFF888888), cursorColor = Color.White))
                        TextField(value = password, onValueChange = { password = it }, label = { Text("Password", color = Color(0xFFAAAAAA)) }, modifier = Modifier.border(1.dp, Color.Gray, RoundedCornerShape(15.dp)).fillMaxWidth(), shape = RoundedCornerShape(15.dp), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), singleLine = true, visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFFAAAAAA)
                                )
                            }
                        }, colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFF212121), unfocusedContainerColor = Color(0xFF212121), focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedLabelColor = Color(0xFFAAAAAA), unfocusedLabelColor = Color(0xFF888888), cursorColor = Color.White))
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen), enabled = !isLoading && email.isNotBlank()) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("LOG IN", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (loginState is LoginState.Error) {
                            Text(text = (loginState as LoginState.Error).message, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun LoginScreenLandscapePreview() {
    PurrytifyTheme {
        LoginScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPortraitPreview() {
    PurrytifyTheme {
        LoginScreen()
    }
}