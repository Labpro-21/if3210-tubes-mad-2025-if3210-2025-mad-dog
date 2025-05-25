package com.example.purrytify.data.repository

import android.app.Application
import com.example.purrytify.db.AppDatabase
import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.purrytify.data.api.NetworkModule
import com.example.purrytify.data.auth.AuthRepository
import com.example.purrytify.data.auth.TokenManager
import com.example.purrytify.data.model.ProfileResponse
import com.example.purrytify.db.dao.SongsDao
import com.example.purrytify.db.dao.UsersDao
import com.example.purrytify.db.entity.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
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

    suspend fun updateProfile(location: String, profilePhotoUri: Uri?): Result<String> = withContext(Dispatchers.IO) {
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

            val response = if (profilePhotoPart != null) {
                api.updateProfile("Bearer $token", locationRequestBody, profilePhotoPart)
            } else {
                api.updateProfileNoPhoto("Bearer $token", locationRequestBody)
            }

            if (response.isSuccessful) {
                val userId = authRepository.currentUserId
                if (userId != null) {
                    val user = usersdao.getUserById(userId)
                    if (user != null) {
                        val updatedUser = user.copy(region = location)
                        usersdao.updateUser(updatedUser)
                    }
                }

                val message = response.body()?.message ?: "Profile updated successfully"

                val profileResponse = api.getProfile("Bearer $token")
                if (profileResponse.isSuccessful) {
                    val updatedProfile = profileResponse.body()
                    updatedProfile?.apply {
                        profilePhoto = NetworkModule.getProfileImageUrl(profilePhoto)
                    }
                }

                return@withContext Result.success(message)
            } else {
                return@withContext Result.failure(Exception("Failed to update profile: ${response.code()} ${response.message()}"))
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

    suspend fun getUserById(userId: Int): Users? = withContext(Dispatchers.IO) {
        return@withContext usersdao.getUserById(userId)
    }

    suspend fun updateUser(user: Users) = withContext(Dispatchers.IO) {
        usersdao.updateUser(user)
        Log.d("ProfileRepository", "User updated: $user")
    }
    
    private fun getApplication(): Application {
        return application
    }

    suspend fun getSongsCount():Int = withContext(Dispatchers.IO){
        val userId = authRepository.currentUserId
        return@withContext if (userId != null) {
            songsdao.getAllSongsForUser(userId).firstOrNull()?.size ?: 0
        } else {
            0
        }
    }

    suspend fun getSongsLiked():Int = withContext(Dispatchers.IO) {
        val userId = authRepository.currentUserId
        return@withContext if (userId != null) {
            songsdao.getFavoriteSongsForUser(userId).firstOrNull()?.size ?: 0
        } else {
            0
        }
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