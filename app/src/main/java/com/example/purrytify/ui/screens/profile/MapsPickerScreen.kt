package com.example.purrytify.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.purrytify.ui.theme.SpotifyGreen
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale
import com.google.maps.android.compose.MapUiSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsPickerScreen(
    viewModel: ProfileViewModel = viewModel(),
    onNavigateBack: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedAddress by remember { mutableStateOf<Address?>(null) }
    var selectedCountryCode by remember { mutableStateOf<String?>(null) }
    var selectedCountryName by remember { mutableStateOf<String?>(null) }
    val defaultLocation = LatLng(-6.2, 106.8) // Jakarta, Indonesia
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 5f)
    }

    
    
    // Permission launcher for location
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Get current location and move camera to it
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
    
    // Function to request location permission
    fun requestLocationPermission() {
        locationPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
    
    // Function to search for a location
    fun searchLocation(query: String) {
        if (query.isEmpty()) return
        
        isSearching = true
        coroutineScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    var addressList: List<Address>? = null
                    geocoder.getFromLocationName(query, 1) { addresses ->
                        addressList = addresses
                        if (addresses.isNotEmpty()) {
                            val address = addresses[0]
                            val latLng = LatLng(address.latitude, address.longitude)
                            selectedLocation = latLng
                            selectedAddress = address
                            selectedCountryCode = address.countryCode
                            selectedCountryName = address.countryName
                            
                            // Move camera to the found location
                            cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Location not found")
                            }
                        }
                        isSearching = false
                    }
                    addressList
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocationName(query, 1)
                }
                
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
                    if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val latLng = LatLng(address.latitude, address.longitude)
                        selectedLocation = latLng
                        selectedAddress = address
                        selectedCountryCode = address.countryCode
                        selectedCountryName = address.countryName
                        
                        // Move camera to the found location
                        cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Location not found")
                        }
                    }
                    isSearching = false
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error searching for location: ${e.message}")
                }
                isSearching = false
            }
        }
    }
    
    // Function to handle map click
    fun onMapClick(latLng: LatLng) {
        selectedLocation = latLng
        
        // Reverse geocode to get the address
        coroutineScope.launch {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { addresses ->
                        if (addresses.isNotEmpty()) {
                            selectedAddress = addresses[0]
                            selectedCountryCode = addresses[0].countryCode
                            selectedCountryName = addresses[0].countryName
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                    if (addresses != null && addresses.isNotEmpty()) {
                        selectedAddress = addresses[0]
                        selectedCountryCode = addresses[0].countryCode
                        selectedCountryName = addresses[0].countryName
                    }
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error getting location details: ${e.message}")
                }
            }
        }
    }
    
    // Check if location permission is already granted and get current location
    LaunchedEffect(Unit) {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context)
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                Toast.makeText(context, "Google Play Services needs to be updated", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "This device doesn't support Google Maps", Toast.LENGTH_LONG).show()
//                mapLoadError = true
            }
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.getCurrentLocation(context) {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Location", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack("", null) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF00667B)
                ),
                actions = {
                    if (selectedCountryCode != null) {
                        IconButton(onClick = { 
                            onNavigateBack(
                                selectedCountryCode ?: "",
                                selectedCountryName
                            ) 
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm Location",
                                tint = Color.White
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { requestLocationPermission() },
                containerColor = SpotifyGreen
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search for a location") },
                leadingIcon = { 
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(8.dp),
                            strokeWidth = 2.dp,
                            color = SpotifyGreen
                        )
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
            
            Button(
                onClick = { searchLocation(searchQuery) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpotifyGreen
                ),
                enabled = searchQuery.isNotEmpty() && !isSearching
            ) {
                Text("Search Location")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Display selected location information
            if (selectedCountryCode != null && selectedCountryName != null) {
                Text(
                    text = "Selected: $selectedCountryName ($selectedCountryCode)",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    fontWeight = FontWeight.Bold,
                    color = SpotifyGreen
                )
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Map
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(
                        isMyLocationEnabled = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED,
                        mapType = MapType.NORMAL
                    ),
                    onMapClick = { latLng ->
                        onMapClick(latLng)
                    }
                ) {
                    // Add marker for selected location
                    selectedLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = selectedCountryName ?: "Selected Location",
                            snippet = "Tap to select this location"
                        )
                    }
                }
                
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SpotifyGreen
                    )
                }
            }
            
            // Confirm button at the bottom
            if (selectedCountryCode != null) {
                Button(
                    onClick = { 
                        onNavigateBack(
                            selectedCountryCode ?: "",
                            selectedCountryName
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen
                    )
                ) {
                    Text("Use Selected Location (${selectedCountryCode})")
                }
            }
        }
    }
}
