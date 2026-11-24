package com.example.data.network

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    private const val BASE_URL = "https://xuxwfgrygswqowwivvku.supabase.co/"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Inh1eHdmZ3J5Z3N3cW93d2l2dmt1Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDAwMjgyNDAsImV4cCI6MjA1NTYwNDI0MH0.K1K494Zf-Wf-b9V772q9f0Yh8f3lS97i7vV_z5FjN6k"

    var token: String? = null

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder()
            .header("apikey", ANON_KEY)
            .header("Content-Type", "application/json")
            .header("Prefer", "return=representation")

        val currentToken = token
        if (!currentToken.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $currentToken")
        } else {
            builder.header("Authorization", "Bearer $ANON_KEY")
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

    private val service: ApiService = retrofit.create(ApiService::class.java)

    fun getService(): ApiService = service

    fun init(context: Context) {
        // Inicialización básica
    }
}
