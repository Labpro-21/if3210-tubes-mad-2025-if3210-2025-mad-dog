package com.example.purrytify.data.auth

import android.app.Application
import com.example.purrytify.db.AppDatabase
import android.content.Context
import android.net.Uri
import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.api.ProfileApi
import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.data.model.UpdateProfileResponse
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.dao.UsersDao
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.create
import java.io.File
import java.io.FileOutputStream

class ProfileRepository private constructor(
    private val tokenManager: TokenManager,
    private val songsdao: SongsDao,
    private val usersdao: UsersDao,
    private val authRepository: AuthRepository,
    private val application: Application
){
    private val api = NetworkModule.profileApi;

    suspend fun getProfile(): ProfileResponse? = withContext(Dispatchers.IO){
        val token = tokenManager.getAccessToken();
        if (token!=null){
            val response = api.getProfile("Bearer $token");
            if (response.isSuccessful){
                val profile = response.body();
                profile?.apply {
                    profilePhoto = NetworkModule.getProfileImageUrl(profilePhoto);
                }

                return@withContext profile;
            }
        }
        return@withContext null;
    };
    
    suspend fun updateProfile(location: String, profilePhotoUri: Uri?): Result<ProfileResponse> = withContext(Dispatchers.IO) {
        val token = tokenManager.getAccessToken()
        if (token == null) {
            return@withContext Result.failure(Exception("Token not found"))
        }
        
        try {
            val locationRequestBody = RequestBody.create("text/plain".toMediaType(), location)
            
            // Klo ada profile photo
            val profilePhotoPart = if (profilePhotoUri != null) {
                val context = getApplication().applicationContext
                val file = getFileFromUri(context, profilePhotoUri)
                val requestFile = RequestBody.create(
                    (context.contentResolver.getType(profilePhotoUri) ?: "image/*").toMediaType(),
                    file
                )
                MultipartBody.Part.createFormData("profilePhoto", file.name, requestFile)
            } else {
                null
            }
            
            val response = api.updateProfile("Bearer $token", locationRequestBody, profilePhotoPart)
            
            if (response.isSuccessful && response.body()?.success == true) {
                val updatedProfile = response.body()?.data
                updatedProfile?.apply {
                    profilePhoto = NetworkModule.getProfileImageUrl(profilePhoto)
                }
                return@withContext Result.success(updatedProfile!!)
            } else {
                return@withContext Result.failure(Exception(response.body()?.message ?: "Failed to update profile"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
      private fun getFileFromUri(context: Context, uri: Uri): File {
        val tempFile = File.createTempFile("profile_photo", ".jpg", context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }
    
    private fun getApplication(): Application {
        return application
    }

    suspend fun getSongsCount():Int = withContext(Dispatchers.IO){
        return@withContext songsdao.getSongsAmount();
    };

    suspend fun getSongsLiked():Int = withContext(Dispatchers.IO) {
        return@withContext songsdao.getFavoriteSongsAmount();
    }
    suspend fun getTotalListened(): Int = withContext(Dispatchers.IO) {
        return@withContext usersdao.getTotalPlayedById(authRepository.currentUserId) ?: 0
    }
    companion object {
        @Volatile
        private var INSTANCE: ProfileRepository? = null

        fun getInstance(application: Application): ProfileRepository {
            return INSTANCE ?: synchronized(this) {
                val context = application.applicationContext
                val tokenManager = TokenManager.getInstance(context)
                val authRepository = AuthRepository.getInstance(application)
                val songsdao = AppDatabase.getDatabase(context).songsDao()
                val usersdao = AppDatabase.getDatabase(context).usersDao()
                val instance = ProfileRepository(
                    tokenManager, 
                    songsdao = songsdao,
                    usersdao = usersdao,
                    authRepository = authRepository,
                    application = application
                )
                INSTANCE = instance
                instance
            }
        }
    }
}