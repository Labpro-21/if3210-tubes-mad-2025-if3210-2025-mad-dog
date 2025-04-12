import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object NetworkMonitor {
    private lateinit var connectivityManager: ConnectivityManager
    private val _isConnected = MutableStateFlow(false) // Inisialisasi dengan nilai default yang sesuai
    val isConnected: StateFlow<Boolean> = _isConnected
    private var isInitialized = false

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isConnected.value = true
        }

        override fun onLost(network: Network) {
            _isConnected.value = false
        }

        override fun onUnavailable() {
            _isConnected.value = false
        }
    }

    fun initialize(context: Context) {
        if (!isInitialized) {
            connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
            _isConnected.value = isCurrentlyConnected(context) // Set status awal saat inisialisasi
            isInitialized = true
        }
    }

    private fun isCurrentlyConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun unregisterNetworkCallback(context: Context) {
        if (isInitialized) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback)
                isInitialized = false
            } catch (e: Exception) {
                // Handle jika callback belum terdaftar (kemungkinan tidak terjadi dengan struktur ini)
            }
        }
    }
}