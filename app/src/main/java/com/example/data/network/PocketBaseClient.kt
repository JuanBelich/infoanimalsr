package com.example.data.network

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class PocketBaseAuthResponse(
    val token: String,
    val record: PocketBaseUser
)

interface PocketBaseService {
    @POST("api/collections/users/auth-with-password")
    suspend fun login(@Body body: Map<String, String>): Response<PocketBaseAuthResponse>

    @POST("api/collections/users/records")
    suspend fun signup(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseUser>

    @GET("api/collections/posts/records")
    suspend fun getPosts(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100,
        @Query("sort") sort: String = "-created"
    ): Response<PocketBaseListResponse<PocketBasePost>>

    @GET("api/collections/veterinarias/records")
    suspend fun getVeterinarias(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100
    ): Response<PocketBaseListResponse<PocketBaseVeterinaria>>

    @GET("api/collections/campaigns/records")
    suspend fun getCampaigns(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100
    ): Response<PocketBaseListResponse<PocketBaseCampaign>>

    @GET("api/collections/news/records")
    suspend fun getNews(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100,
        @Query("sort") sort: String = "-created"
    ): Response<PocketBaseListResponse<PocketBaseNews>>

    @GET("api/collections/colaboradores/records")
    suspend fun getColaboradores(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100
    ): Response<PocketBaseListResponse<PocketBaseColaborador>>

    @GET("api/collections/comments/records")
    suspend fun getComments(
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String = "created"
    ): Response<PocketBaseListResponse<PocketBaseComment>>

    @GET("api/collections/user_statuses/records")
    suspend fun getUserStatuses(
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 100,
        @Query("sort") sort: String = "-created"
    ): Response<PocketBaseListResponse<PocketBaseUserStatus>>

    @POST("api/collections/user_statuses/records")
    suspend fun createUserStatus(
        @Header("Authorization") auth: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>
}

object PocketBaseClient {
    private const val BASE_URL = "https://xuxwfgrygswqowwivvku.supabase.co/"
    var token: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
        val currentToken = token
        if (!currentToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $currentToken")
        }
        chain.proceed(builder.build())
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
        .build()

    private val service: PocketBaseService = retrofit.create(PocketBaseService::class.java)

    fun getService(): PocketBaseService = service

    fun saveAuth(context: Context, token: String, record: PocketBaseUser) {
        this.token = token
        ApiClient.token = token
    }

    fun clearAuth(context: Context) {
        this.token = null
        ApiClient.token = null
    }
}
