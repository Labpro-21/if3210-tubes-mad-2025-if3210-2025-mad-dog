package com.example.purrytify.ui.screens.profile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import com.example.purrytify.R
import com.example.purrytify.ui.theme.SpotifyGreen
import com.example.purrytify.utils.CountryCodeValidator
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    onNavigateToLocationPicker: () -> Unit = {},
    countryCode: String? = null,
    countryName: String? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profile by viewModel.profile.collectAsState()
    val updateStatus by viewModel.updateProfileStatus.collectAsState()
    val currentLocation by viewModel.currentLocation.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    var locationInput by remember { mutableStateOf(profile?.location ?: "") }
    var selectedCountryName by remember { mutableStateOf<String?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var validationResult by remember { mutableStateOf(CountryCodeValidator.ValidationResult(false, "", null)) }
    var hasUserInteracted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.getProfile()
    }
      // Handle country code and country name from navigation parameters
    LaunchedEffect(countryCode, countryName) {
        if (!countryCode.isNullOrEmpty()) {
            locationInput = countryCode
            selectedCountryName = countryName
            validationResult = CountryCodeValidator.validateAndFormat(countryCode)
        }
    }
    
    // Validate location input whenever it changes
    LaunchedEffect(locationInput) {
        if (hasUserInteracted) {
            validationResult = CountryCodeValidator.validateAndFormat(locationInput)
            // Update selectedCountryName from validation result if available
            if (validationResult.isValid && validationResult.countryName != null) {
                selectedCountryName = validationResult.countryName
            } else if (!validationResult.isValid) {
                selectedCountryName = null
            }
        }
    }
    

    fun createImageUri(context: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }
      
    // Permission launchers
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions -> 
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.getCurrentLocation(context) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location permission denied")
                }
            }
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Location permission denied")
            }
        }
    }

    // Photo pickers
    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            photoUri = it
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            photoUri = tempCameraUri
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            tempCameraUri = createImageUri(context)
            cameraLauncher.launch(tempCameraUri!!)
        } else {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle location permission
    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // Handle camera permission
    fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                tempCameraUri = createImageUri(context)
                cameraLauncher.launch(tempCameraUri!!)
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    // Google Maps intent
    fun openGoogleMaps() {
        onNavigateToLocationPicker()
    }
    LaunchedEffect(currentLocation) {
        currentLocation?.let {
            if (it != "Unknown") {
                locationInput = it
                hasUserInteracted = true
                validationResult = CountryCodeValidator.validateAndFormat(it)
                if (validationResult.isValid && validationResult.countryName != null) {
                    selectedCountryName = validationResult.countryName
                }
            }
        }
    }
      LaunchedEffect(profile) {
        // Update location input when profile changes
        profile?.let {
            if (locationInput.isEmpty()) {
                locationInput = it.location
                // Validate the initial location from profile
                if (it.location.isNotEmpty()) {
                    validationResult = CountryCodeValidator.validateAndFormat(it.location)
                    if (validationResult.isValid && validationResult.countryName != null) {
                        selectedCountryName = validationResult.countryName
                    }
                }
            }
        }
    }

    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is ProfileViewModel.UpdateProfileStatus.Success -> {
                snackbarHostState.showSnackbar("Profile updated successfully")
                onNavigateBack()
            }
            is ProfileViewModel.UpdateProfileStatus.Error -> {
                snackbarHostState.showSnackbar((updateStatus as ProfileViewModel.UpdateProfileStatus.Error).message)
            }
            is ProfileViewModel.UpdateProfileStatus.NoInternet -> {
                snackbarHostState.showSnackbar("No internet connection")
            }
            else -> {}
        }
    }
    fun updateProfile() {
        hasUserInteracted = true
        
        if (locationInput.isEmpty()) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Country code is required")
            }
            return
        }

        val validation = CountryCodeValidator.validateAndFormat(locationInput)
        if (!validation.isValid) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(validation.errorMessage ?: "Invalid country code")
            }
            return
        }

        viewModel.updateProfile(validation.formattedCode, photoUri)
    }

    // Photo picker dialog
    if (showPhotoPickerDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerDialog = false },
            title = { Text("Change Profile Photo") },
            text = { Text("Choose a source for your profile picture") },
            confirmButton = {
                TextButton(onClick = {
                    showPhotoPickerDialog = false
                    requestCameraPermission()
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Camera")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Camera")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPhotoPickerDialog = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gallery")
                    }
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00667B)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0xFF00667B),
                            0.5f to Color.Black,
                            1.0f to Color.Black
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            if (updateStatus is ProfileViewModel.UpdateProfileStatus.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Profile photo
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clickable { showPhotoPickerDialog = true },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                model = photoUri ?: if (!profile?.profilePhoto.isNullOrBlank()) {
                                    profile!!.profilePhoto
                                } else {
                                    R.drawable.ic_profile_placeholder
                                }
                            ),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SpotifyGreen)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Change Photo",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Username",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = profile?.username ?: "",
                        onValueChange = { },
                        enabled = false,
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),

                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Location",
                        fontSize = 14.sp,
                        color = Color.LightGray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = locationInput,
                        onValueChange = { newValue ->
                            hasUserInteracted = true
                            val filtered = CountryCodeValidator.filterInput(newValue)
                            locationInput = filtered
                        },
                        label = { Text("Location (country code)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        maxLines = 1,
                        isError = hasUserInteracted && !validationResult.isValid && locationInput.isNotEmpty(),
                        supportingText = {
                            if (hasUserInteracted && !validationResult.isValid && locationInput.isNotEmpty()) {
                                Text(
                                    text = validationResult.errorMessage ?: "Invalid country code",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { requestLocationPermission() },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SpotifyGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Get current location"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Current Location", color = Color.White)
                        }
                        OutlinedButton(
                            onClick = { openGoogleMaps() },
                            modifier = Modifier.weight(1f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SpotifyGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = SpotifyGreen
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Open Maps"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Select on Map", color = Color.White)
                        }
                    }
                    Text(
                        text = when {
                            validationResult.isValid && validationResult.countryName != null -> {
                                "✓ Selected country: ${validationResult.countryName} (${validationResult.formattedCode})"
                            }
                            hasUserInteracted && !validationResult.isValid && locationInput.isNotEmpty() -> {
                                "Invalid country code. Please use ISO 3166-1 alpha-2 format"
                            }
                            selectedCountryName != null && !hasUserInteracted -> {
                                "Selected country: $selectedCountryName ($locationInput)"
                            }
                            else -> {
                                "Enter a 2-letter country code (ISO 3166-1 alpha-2)\nExamples: US, ID, GB, FR, DE, JP"
                            }
                        },
                        fontSize = 12.sp,
                        color = when {
                            validationResult.isValid -> SpotifyGreen
                            hasUserInteracted && !validationResult.isValid && locationInput.isNotEmpty() -> MaterialTheme.colorScheme.error
                            selectedCountryName != null && !hasUserInteracted -> SpotifyGreen
                            else -> Color.Gray
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Button(
                        onClick = { updateProfile() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(SpotifyGreen)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
