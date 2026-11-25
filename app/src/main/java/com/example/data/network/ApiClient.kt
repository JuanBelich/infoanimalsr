package com.example.data.network

import android.content.Context
import android.util.Log
import com.example.util.ImageUtils
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val PREFS_NAME = "api_client_config"
    private const val KEY_AUTH_TOKEN = "api_auth_token"
    private const val KEY_AUTH_USER_JSON = "api_auth_user_json"
    private const val KEY_BASE_URL = "api_base_url"

    // Forzamos el uso de las credenciales de Supabase en producción con respaldos seguros
    private val DEFAULT_SUPABASE_URL = if (BuildConfig.SUPABASE_URL.isNotBlank() && 
        !BuildConfig.SUPABASE_URL.contains("YOUR_SUPABASE_URL_HERE") && 
        !BuildConfig.SUPABASE_URL.contains("placeholder-url")) {
        BuildConfig.SUPABASE_URL
    } else {
        "https://doxeatgpswhqlwslywgm.supabase.co/"
    }

    private val SUPABASE_ANON_KEY = if (BuildConfig.SUPABASE_ANON_KEY.isNotBlank() && 
        !BuildConfig.SUPABASE_ANON_KEY.contains("YOUR_SUPABASE_ANON_KEY_HERE") && 
        !BuildConfig.SUPABASE_ANON_KEY.contains("placeholder-key")) {
        BuildConfig.SUPABASE_ANON_KEY
    } else {
        "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRveGVhdGdwc3docWx3c2x5d2dtIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODI4NTE3NTAsImV4cCI6MjA5ODQyNzc1MH0._I7eGzh5laDvyURpGlj3Tpy1obA_5H2pZw9neFQCqwE"
    }

    private var retrofit: Retrofit? = null
    private var service: ApiService? = null
    
    var baseUrl: String = if (DEFAULT_SUPABASE_URL.endsWith("/")) DEFAULT_SUPABASE_URL else "$DEFAULT_SUPABASE_URL/"
        internal set
        
    var token: String? = null
        internal set

    private var appContext: Context? = null
    internal var sharedPrefs: android.content.SharedPreferences? = null

    internal fun getPrefs(context: Context): android.content.SharedPreferences {
        sharedPrefs?.let { return it }
        val prefs = try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                "secured_auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "Error initializing EncryptedSharedPreferences, falling back to standard SharedPreferences", e)
            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        }
        sharedPrefs = prefs
        return prefs
    }

    fun setToken(newToken: String?) {
        token = newToken
        appContext?.let { ctx ->
            val prefs = getPrefs(ctx)
            if (newToken != null) {
                prefs.edit().putString(KEY_AUTH_TOKEN, newToken).apply()
            } else {
                prefs.edit().remove(KEY_AUTH_TOKEN).apply()
            }
        }
        rebuildService()
    }

    var authenticatedUser: PocketBaseUser? = null
        private set

    // Ya no dependemos de un switch en el login, la app siempre intentará ir a la nube
    val isOfflineMode: Boolean = false

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = getPrefs(context)
        
        val savedUrl = prefs.getString(KEY_BASE_URL, null)
        val isLocalOrInvalid = savedUrl != null && (
            savedUrl.contains("localhost") || 
            savedUrl.contains("10.0.2.2") || 
            savedUrl.contains("127.0.0.1") || 
            savedUrl.contains("placeholder") || 
            !savedUrl.startsWith("https://")
        )

        if (!savedUrl.isNullOrBlank() && !isLocalOrInvalid) {
            baseUrl = if (savedUrl.endsWith("/")) savedUrl else "$savedUrl/"
        } else {
            baseUrl = if (DEFAULT_SUPABASE_URL.endsWith("/")) DEFAULT_SUPABASE_URL else "$DEFAULT_SUPABASE_URL/"
            // Reseteamos el valor guardado para limpiar cualquier rastro de IPs de desarrollo antiguas
            prefs.edit().putString(KEY_BASE_URL, baseUrl).apply()
        }
        
        token = prefs.getString(KEY_AUTH_TOKEN, null)
        
        val userJson = prefs.getString(KEY_AUTH_USER_JSON, null)
        if (!userJson.isNullOrBlank()) {
            try {
                authenticatedUser = moshi.adapter(PocketBaseUser::class.java).fromJson(userJson)
            } catch (e: Exception) {
                Log.e("ApiClient", "Error deserializing user", e)
            }
        }

        rebuildService()
    }

    fun updateBaseUrl(context: Context, newUrl: String) {
        val prefs = getPrefs(context)
        val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        prefs.edit().putString(KEY_BASE_URL, formattedUrl).apply()
        baseUrl = formattedUrl
        rebuildService()
    }

    fun saveAuth(context: Context, newToken: String, user: PocketBaseUser) {
        val prefs = getPrefs(context)
        val userJson = moshi.adapter(PocketBaseUser::class.java).toJson(user)
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, newToken)
            .putString(KEY_AUTH_USER_JSON, userJson)
            .apply()
        token = newToken
        authenticatedUser = user
    }

    fun clearAuth(context: Context) {
        val prefs = getPrefs(context)
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_AUTH_USER_JSON)
            .apply()
        token = null
        authenticatedUser = null
    }

    private fun rebuildService() {
        try {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            
            val okHttpClient = OkHttpClient.Builder()
                .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
                .addInterceptor { chain ->
                    val original = chain.request()
                    val requestBuilder = original.newBuilder()

                    // Inyección obligatoria de cabeceras de Supabase
                    requestBuilder.header("apikey", SUPABASE_ANON_KEY)

                    val currentToken = token
                    if (!currentToken.isNullOrBlank()) {
                        requestBuilder.header("Authorization", "Bearer $currentToken")
                    } else {
                        requestBuilder.header("Authorization", "Bearer $SUPABASE_ANON_KEY")
                    }

                    val path = original.url.encodedPath
                    if (path.contains("/rest/v1/")) {
                        if (original.method == "POST" || original.method == "PATCH" || original.method == "PUT") {
                            requestBuilder.header("Prefer", "return=representation")
                        }
                    }

                    try {
                        chain.proceed(requestBuilder.build())
                    } catch (e: java.io.IOException) {
                        val errorMsg = e.message ?: "Error de red eMMC/TLS/DNS lento o nulo"
                        Log.e("ApiClient", "Error de red crítico interceptado: $errorMsg", e)
                        appContext?.let { ctx ->
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                android.widget.Toast.makeText(
                                    ctx,
                                    "Error de Red/TLS: $errorMsg. Verifica tu conexión.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        throw e
                    } catch (e: Exception) {
                        Log.e("ApiClient", "Excepción general en proceed interceptada: ${e.message}", e)
                        throw e
                    }
                }
                .addInterceptor(logging)
                .retryOnConnectionFailure(true)
                .connectTimeout(12, TimeUnit.SECONDS)
                .readTimeout(12, TimeUnit.SECONDS)
                .writeTimeout(12, TimeUnit.SECONDS)
                .build()

            val builder = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))

            val builtRetrofit = builder.build()
            retrofit = builtRetrofit
            service = builtRetrofit.create(ApiService::class.java)
            Log.d("ApiClient", "Retrofit reconstruido exclusivamente para Supabase: $baseUrl")
        } catch (e: Exception) {
            Log.e("ApiClient", "Error al construir Retrofit con url: $baseUrl", e)
        }
    }

    fun getService(): ApiService {
        if (service == null) {
            Log.w("ApiClient", "getService: service es nulo. Intentando auto-inicializar.")
            val ctx = appContext ?: try {
                com.example.MainApplication.instance
            } catch (e: Exception) {
                null
            }
            if (ctx != null) {
                init(ctx)
            }
        }
        return service ?: throw IllegalStateException("ApiClient no inicializado.")
    }

    fun getPublicStorageUrl(bucket: String, filePath: String): String {
        val cleanBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (filePath.startsWith("/")) filePath.substring(1) else filePath
        return "${cleanBase}storage/v1/object/public/$bucket/$cleanPath"
    }

    suspend fun uploadProfileImage(
        context: Context,
        userId: String,
        uri: android.net.Uri,
        isCover: Boolean
    ): Result<String> {
        return try {
            val compressedBytes = ImageUtils.compressImageUri(context, uri, quality = 80)
                ?: return Result.failure(Exception("No se pudo procesar la imagen."))

            val folder = if (isCover) "covers" else "avatars"
            val fileType = if (isCover) "cover" else "profile"
            val filePath = "$folder/${userId}_${fileType}.jpg"

            val mediaType = "image/jpeg".toMediaTypeOrNull()
            val requestBody = compressedBytes.toRequestBody(mediaType)

            var bucket = "profiles"
            var response = getService().uploadStorageObject(
                bucket = bucket,
                filePath = filePath,
                body = requestBody,
                contentType = "image/jpeg",
                upsert = "true"
            )

            if (!response.isSuccessful && response.code() == 404) {
                // Try fallback bucket 'avatars'
                bucket = "avatars"
                response = getService().uploadStorageObject(
                    bucket = bucket,
                    filePath = filePath,
                    body = requestBody,
                    contentType = "image/jpeg",
                    upsert = "true"
                )
            }

            if (response.isSuccessful) {
                val publicUrl = getPublicStorageUrl(bucket, filePath)
                Result.success(publicUrl)
            } else {
                val err = response.errorBody()?.string() ?: ""
                Log.e("ApiClient", "Error uploading profile image (code=${response.code()}): $err")
                if (response.code() == 404 || err.contains("Bucket not found", ignoreCase = true) || err.contains("not_found", ignoreCase = true)) {
                    Result.failure(Exception("Error de configuración en el servidor de almacenamiento. Inténtalo más tarde."))
                } else {
                    Result.failure(Exception("No se pudo subir la imagen, intenta de nuevo."))
                }
            }
        } catch (e: Exception) {
            Log.e("ApiClient", "Exception uploading profile image", e)
            Result.failure(e)
        }
    }

    fun getFileUrl(collectionName: String, recordId: String, fileName: String, thumbSize: String? = null): String {
        if (fileName.isBlank()) return ""
        if (fileName.startsWith("http://") || fileName.startsWith("https://")) {
            return fileName
        }
        val bucket = when (collectionName) {
            "posts" -> "posts-images"
            "users" -> "avatars"
            else -> collectionName
        }
        val path = if (recordId.isBlank()) fileName else "$recordId/$fileName"
        return "${baseUrl}storage/v1/object/public/$bucket/$path"
    }

    fun createMultipartBodyPart(partName: String, fileName: String, fileBytes: ByteArray): okhttp3.MultipartBody.Part {
        val requestFile = fileBytes.toRequestBody("image/*".toMediaTypeOrNull())
        return okhttp3.MultipartBody.Part.createFormData(partName, fileName, requestFile)
    }

    suspend fun uploadAvatar(filePath: String, filePart: okhttp3.MultipartBody.Part): retrofit2.Response<SupabaseStorageResponse> {
        return getService().uploadAvatar(filePath, filePart)
    }

    suspend fun uploadPostImage(filePath: String, filePart: okhttp3.MultipartBody.Part): retrofit2.Response<SupabaseStorageResponse> {
        return getService().uploadPostImage(filePath, filePart)
    }

    suspend fun uploadAvatar(filePath: String, fileBytes: ByteArray): retrofit2.Response<SupabaseStorageResponse> {
        val part = createMultipartBodyPart("file", filePath, fileBytes)
        return getService().uploadAvatar(filePath, part)
    }

    suspend fun uploadPostImage(filePath: String, fileBytes: ByteArray): retrofit2.Response<SupabaseStorageResponse> {
        val part = createMultipartBodyPart("file", filePath, fileBytes)
        return getService().uploadPostImage(filePath, part)
    }

    suspend fun uploadAvatarBase64(fileName: String, base64Data: String): String? {
        return try {
            val cleanBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            val response = uploadAvatar(fileName, bytes)
            if (response.isSuccessful) fileName else null
        } catch (e: Exception) {
            Log.e("ApiClient", "Error uploading avatar", e)
            null
        }
    }

    suspend fun uploadPostImageBase64(fileName: String, base64Data: String): String? {
        return try {
            val cleanBase64 = if (base64Data.contains(",")) base64Data.substringAfter(",") else base64Data
            val bytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT)
            val response = uploadPostImage(fileName, bytes)
            if (response.isSuccessful) fileName else null
        } catch (e: Exception) {
            Log.e("ApiClient", "Error uploading post image", e)
            null
        }
    }
}
