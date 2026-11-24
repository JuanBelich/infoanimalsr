package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "token_type") val tokenType: String,
    @Json(name = "user") val user: SupabaseAuthUser
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUserContainer(
    @Json(name = "id") val rawId: String? = null,
    @Json(name = "email") val rawEmail: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any>? = null,
    @Json(name = "user") val user: SupabaseAuthUser? = null
) {
    val id: String
        get() = rawId ?: user?.id ?: ""

    val email: String?
        get() = rawEmail ?: user?.email
}

@JsonClass(generateAdapter = true)
data class SupabaseEmbeddedUser(
    @Json(name = "nombre") val nombre: String? = "",
    @Json(name = "foto_perfil") val rawFotoPerfil: String? = "",
    @Json(name = "avatar_url") val rawAvatarUrl: String? = ""
) {
    val fotoPerfil: String
        get() = when {
            !rawFotoPerfil.isNullOrBlank() -> rawFotoPerfil
            !rawAvatarUrl.isNullOrBlank() -> rawAvatarUrl
            else -> ""
        }
}

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String? = "",
    @Json(name = "nombre") val nombre: String? = "",
    @Json(name = "telefono") val telefono: String? = "",
    @Json(name = "zona_principal") val zonaPrincipal: String? = "San Rafael",
    @Json(name = "rol") val rol: String? = "user",
    @Json(name = "foto_perfil") val rawFotoPerfil: String? = "",
    @Json(name = "avatar_url") val rawAvatarUrl: String? = "",
    @Json(name = "foto_portada") val fotoPortada: String? = "",
    @Json(name = "biografia") val biografia: String? = ""
) {
    val fotoPerfil: String
        get() = when {
            !rawFotoPerfil.isNullOrBlank() -> rawFotoPerfil
            !rawAvatarUrl.isNullOrBlank() -> rawAvatarUrl
            else -> ""
        }
}

@JsonClass(generateAdapter = true)
data class SupabasePost(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String? = "",
    @Json(name = "tipo") val tipo: String? = "",
    @Json(name = "especie") val especie: String? = "",
    @Json(name = "tamano") val tamano: String? = "",
    @Json(name = "nombre_mascota") val nombreMascota: String? = "",
    @Json(name = "descripcion") val descripcion: String? = "",
    @Json(name = "rasgos") val rasgos: String? = "",
    @Json(name = "estado") val estado: String? = "activo",
    @Json(name = "fotos") val fotos: Any? = null,
    @Json(name = "ubicacion_lat") val ubicacionLat: Double? = 0.0,
    @Json(name = "ubicacion_lng") val ubicacionLng: Double? = 0.0,
    @Json(name = "distrito") val distrito: String? = "",
    @Json(name = "zona") val zona: String? = "",
    @Json(name = "telefono_contacto") val telefonoContacto: String? = "",
    @Json(name = "telefono_visible") val telefonoVisible: Any? = true,
    @Json(name = "likes_count") val likesCount: Int? = 0,
    @Json(name = "created") val created: String? = "",
    @Json(name = "users") val users: SupabaseEmbeddedUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseComment(
    @Json(name = "id") val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "texto") val texto: String,
    @Json(name = "created") val created: String? = "",
    @Json(name = "users") val users: SupabaseEmbeddedUser? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseReport(
    @Json(name = "id") val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "reporter_user_id") val reporterUserId: String,
    @Json(name = "razon") val razon: String,
    @Json(name = "estado") val estado: String? = "pendiente"
)

@JsonClass(generateAdapter = true)
data class SupabaseProfileReport(
    @Json(name = "id") val id: String,
    @Json(name = "reported_user_id") val reportedUserId: String,
    @Json(name = "reporter_user_id") val reporterUserId: String,
    @Json(name = "razon") val razon: String,
    @Json(name = "estado") val estado: String? = "pendiente"
)

@JsonClass(generateAdapter = true)
data class SupabaseNotification(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "mensaje") val mensaje: String,
    @Json(name = "leida") val leida: Boolean? = false
)

interface ApiService {
    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body credentials: Map<String, String>): Response<SupabaseAuthResponse>

    @POST("auth/v1/signup")
    suspend fun signup(@Body data: Map<String, @JvmSuppressWildcards Any>): Response<SupabaseUserContainer>

    @GET("rest/v1/users")
    suspend fun getUserProfile(@Query("id") idQuery: String): Response<List<SupabaseUser>>

    @POST("rest/v1/users")
    suspend fun insertUserProfile(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("rest/v1/users")
    suspend fun updateUserProfile(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @GET("rest/v1/posts?select=*,users(nombre,foto_perfil,avatar_url)&order=created.desc")
    suspend fun getPosts(): Response<List<SupabasePost>>

    @POST("rest/v1/posts")
    suspend fun createPost(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @PATCH("rest/v1/posts")
    suspend fun updatePost(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<Unit>

    @DELETE("rest/v1/posts")
    suspend fun deletePost(@Query("id") idQuery: String): Response<Unit>

    @GET("rest/v1/comments?select=*,users(nombre,foto_perfil,avatar_url)&order=created.asc")
    suspend fun getComments(@Query("post_id") postIdQuery: String): Response<List<SupabaseComment>>

    @POST("rest/v1/comments")
    suspend fun createComment(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @DELETE("rest/v1/comments")
    suspend fun deleteComment(@Query("id") idQuery: String): Response<Unit>

    @POST("rest/v1/user_statuses")
    suspend fun createUserStatus(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>

    @GET("rest/v1/reports")
    suspend fun getReports(): Response<List<SupabaseReport>>

    @GET("rest/v1/profile_reports")
    suspend fun getProfileReports(): Response<List<SupabaseProfileReport>>

    @GET("rest/v1/notifications")
    suspend fun getNotifications(@Query("user_id") userIdQuery: String): Response<List<SupabaseNotification>>

    @POST("rest/v1/admin_actions")
    suspend fun logAdminAction(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<Unit>
}
