package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// =========================================================================
// MODELOS SUPABASE TOTALMENTE CORREGIDOS
// =========================================================================

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
    @Json(name = "post_id") val postId: String? = "",
    @Json(name = "user_id") val userId: String? = "",
    @Json(name = "texto") val texto: String? = "",
    @Json(name = "created") val created: String? = "",
    @Json(name = "users") val users: SupabaseEmbeddedUser? = null
)
// =========================================================================
// INTERFAZ API SERVICE RECONECTADA
// =========================================================================

interface ApiService {

    // --- Supabase Auth (GoTrue API) ---

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body body: Map<String, String>
    ): Response<SupabaseAuthResponse>

    @POST("auth/v1/signup")
    suspend fun signup(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<SupabaseUserContainer>

    @POST("auth/v1/recover")
    suspend fun requestPasswordReset(
        @Body body: Map<String, String>
    ): Response<Unit>

    // --- Supabase 'users' public profile table ---

    @GET("rest/v1/users")
    suspend fun getUserProfile(
        @Query("id") idQuery: String
    ): Response<List<SupabaseUser>>

    @POST("rest/v1/users")
    suspend fun createUserProfile(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<SupabaseUser>>

    @PATCH("rest/v1/users")
    suspend fun updateProfile(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<SupabaseUser>>

    // --- Posts Table ---

    @GET("rest/v1/posts")
    suspend fun getPosts(
        @Query("select") select: String = "*,users(nombre,foto_perfil,avatar_url)",
        @Query("order") order: String = "created.desc",
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("id") idQuery: String? = null
    ): Response<List<SupabasePost>>

    @POST("rest/v1/posts")
    suspend fun createPost(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<SupabasePost>>

    @PATCH("rest/v1/posts")
    suspend fun updatePost(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<SupabasePost>>

    @DELETE("rest/v1/posts")
    suspend fun deletePost(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Comments Table ---

    @GET("rest/v1/comments")
    suspend fun getComments(
        @Query("select") select: String = "*,users(nombre,foto_perfil,avatar_url)",
        @Query("post_id") postIdQuery: String? = null,
        @Query("order") order: String = "created.asc"
    ): Response<List<SupabaseComment>>

    @POST("rest/v1/comments")
    suspend fun createComment(
        @Body body: Map<String, String>
    ): Response<List<SupabaseComment>>

    @DELETE("rest/v1/comments")
    suspend fun deleteComment(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Likes Table ---

    @GET("rest/v1/likes")
    suspend fun getLikes(
        @Query("post_id") postIdQuery: String? = null,
        @Query("user_id") userIdQuery: String? = null
    ): Response<List<PocketBaseLike>>

    @POST("rest/v1/likes")
    suspend fun createLike(
        @Body body: Map<String, String>
    ): Response<List<PocketBaseLike>>

    @DELETE("rest/v1/likes")
    suspend fun deleteLike(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Veterinarias Table ---

    @GET("rest/v1/veterinarias")
    suspend fun getVeterinarias(
        @Query("order") order: String = "nombre.asc"
    ): Response<List<PocketBaseVeterinaria>>

    @POST("rest/v1/veterinarias")
    suspend fun createVeterinaria(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseVeterinaria>>

    @PATCH("rest/v1/veterinarias")
    suspend fun updateVeterinaria(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseVeterinaria>>

    @DELETE("rest/v1/veterinarias")
    suspend fun deleteVeterinaria(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Campaigns Table ---

    @GET("rest/v1/campaigns")
    suspend fun getCampaigns(
        @Query("order") order: String = "created.desc"
    ): Response<List<PocketBaseCampaign>>

    @POST("rest/v1/campaigns")
    suspend fun createCampaign(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseCampaign>>

    @PATCH("rest/v1/campaigns")
    suspend fun updateCampaign(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseCampaign>>

    @DELETE("rest/v1/campaigns")
    suspend fun deleteCampaign(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- News Table ---

    @GET("rest/v1/news")
    suspend fun getNews(
        @Query("order") order: String = "created.desc"
    ): Response<List<PocketBaseNews>>

    @POST("rest/v1/news")
    suspend fun createNews(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseNews>>

    @PATCH("rest/v1/news")
    suspend fun updateNews(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseNews>>

    @DELETE("rest/v1/news")
    suspend fun deleteNews(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- User Statuses Table ---

    @GET("rest/v1/user_statuses")
    suspend fun getUserStatuses(
        @Query("order") order: String = "created.desc"
    ): Response<List<PocketBaseUserStatus>>

    @POST("rest/v1/user_statuses")
    suspend fun createUserStatus(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseUserStatus>>

    @PATCH("rest/v1/user_statuses")
    suspend fun updateStatus(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseUserStatus>>

    @DELETE("rest/v1/user_statuses")
    suspend fun deleteStatus(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Status Likes Table ---

    @GET("rest/v1/status_likes")
    suspend fun getStatusLikes(
        @Query("status_id") statusIdQuery: String? = null,
        @Query("user_id") userIdQuery: String? = null
    ): Response<List<PocketBaseStatusLike>>

    @POST("rest/v1/status_likes")
    suspend fun createStatusLike(
        @Body body: Map<String, String>
    ): Response<List<PocketBaseStatusLike>>

    @DELETE("rest/v1/status_likes")
    suspend fun deleteStatusLike(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Reports Table ---

    @GET("rest/v1/reports")
    suspend fun getReports(): Response<List<PocketBaseReport>>

    @POST("rest/v1/reports")
    suspend fun createReport(
        @Body body: Map<String, String>
    ): Response<List<PocketBaseReport>>

    @PATCH("rest/v1/reports")
    suspend fun updateReport(
        @Query("id") idQuery: String,
        @Body body: Map<String, String>
    ): Response<List<PocketBaseReport>>

    // --- Profile Reports Table ---

    @GET("rest/v1/profile_reports")
    suspend fun getProfileReports(): Response<List<PocketBaseProfileReport>>

    @POST("rest/v1/profile_reports")
    suspend fun createProfileReport(
        @Body body: Map<String, String>
    ): Response<List<PocketBaseProfileReport>>

    @PATCH("rest/v1/profile_reports")
    suspend fun updateProfileReport(
        @Query("id") idQuery: String,
        @Body body: Map<String, String>
    ): Response<List<PocketBaseProfileReport>>

    // --- Notifications Table ---

    @GET("rest/v1/notifications")
    suspend fun getNotifications(
        @Query("user_id") userIdQuery: String? = null,
        @Query("order") order: String = "created.desc"
    ): Response<List<PocketBaseNotification>>

    @POST("rest/v1/notifications")
    suspend fun createNotification(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseNotification>>

    @PATCH("rest/v1/notifications")
    suspend fun updateNotification(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseNotification>>

    // --- Colaboradores Table ---

    @GET("rest/v1/colaboradores")
    suspend fun getColaboradores(
        @Query("order") order: String = "nombre.asc"
    ): Response<List<PocketBaseColaborador>>

    @POST("rest/v1/colaboradores")
    suspend fun createColaborador(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseColaborador>>

    @PATCH("rest/v1/colaboradores")
    suspend fun updateColaborador(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseColaborador>>

    @DELETE("rest/v1/colaboradores")
    suspend fun deleteColaborador(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- User Blocks Table ---

    @GET("rest/v1/user_blocks")
    suspend fun getBlocks(
        @Query("user_id") userIdQuery: String? = null
    ): Response<List<PocketBaseUserBlock>>

    @POST("rest/v1/user_blocks")
    suspend fun createBlock(
        @Body body: Map<String, String>
    ): Response<List<PocketBaseUserBlock>>

    @DELETE("rest/v1/user_blocks")
    suspend fun deleteBlock(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Admin Actions Table ---

    @GET("rest/v1/admin_actions")
    suspend fun getAdminActions(
        @Query("order") order: String = "created.desc"
    ): Response<List<PocketBaseAdminAction>>

    @POST("rest/v1/admin_actions")
    suspend fun createAdminAction(
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<PocketBaseAdminAction>>

    // --- User Follows Table ---

    @GET("rest/v1/user_follows")
    suspend fun getFollows(
        @Query("follower_id") followerIdQuery: String? = null
    ): Response<List<PocketBaseUserFollow>>

    @POST("rest/v1/user_follows")
    suspend fun createFollow(
        @Body body: Map<String, String>
    ): Response<List<PocketBaseUserFollow>>

    @DELETE("rest/v1/user_follows")
    suspend fun deleteFollow(
        @Query("id") idQuery: String
    ): Response<Unit>

    // --- Supabase Storage Upload ---

    @POST("storage/v1/object/{bucket}/{file_path}")
    suspend fun uploadStorageObject(
        @Path("bucket") bucket: String,
        @Path(value = "file_path", encoded = true) filePath: String,
        @Body body: okhttp3.RequestBody,
        @Header("Content-Type") contentType: String = "image/jpeg",
        @Header("x-upsert") upsert: String = "true"
    ): Response<SupabaseStorageResponse>

    @Multipart
    @POST("storage/v1/object/avatars/{file_path}")
    suspend fun uploadAvatar(
        @Path("file_path") filePath: String,
        @Part file: MultipartBody.Part
    ): Response<SupabaseStorageResponse>

    @Multipart
    @POST("storage/v1/object/posts-images/{file_path}")
    suspend fun uploadPostImage(
        @Path("file_path") filePath: String,
        @Part file: MultipartBody.Part
    ): Response<SupabaseStorageResponse>

    // --- Supabase User Deletion ---
    @DELETE("auth/v1/user")
    suspend fun deleteCurrentUser(): Response<Unit>
}

@JsonClass(generateAdapter = true)
data class SupabaseStorageResponse(
    @Json(name = "Id") val id: String? = null,
    @Json(name = "Key") val key: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)