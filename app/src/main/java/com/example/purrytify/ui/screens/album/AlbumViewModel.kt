import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.purrytify.data.auth.OnlineSongRepository
import com.example.purrytify.data.model.OnlineSongResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val onlineSongRepository = OnlineSongRepository.getInstance(application)
    private val tag = "AlbumViewModel"

    private val _songs = MutableStateFlow<List<OnlineSongResponse>>(emptyList())
    val songs: StateFlow<List<OnlineSongResponse>> = _songs

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun loadSongs(region: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val fetchedSongs = if (region.uppercase(Locale.ROOT) == "GLOBAL") {
                    onlineSongRepository.getTopGlobalSongs()
                } else {
                    onlineSongRepository.getTopCountrySongs(region.uppercase(Locale.ROOT))
                }

                if (fetchedSongs != null) {
                    _songs.value = fetchedSongs
                    Log.d(tag, "Fetched ${fetchedSongs.size} online songs for region $region")
                } else {
                    _errorMessage.value = "Failed to fetch songs for $region"
                    Log.e(tag, "Failed to fetch songs for $region")
                }
            } catch (e: Exception) {
                _errorMessage.value = "An error occurred: ${e.localizedMessage}"
                Log.e(tag, "Error loading songs for $region: ${e.localizedMessage}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    class Factory(val application: Application) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return AlbumViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}