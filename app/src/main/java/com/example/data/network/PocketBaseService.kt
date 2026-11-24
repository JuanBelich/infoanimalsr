package com.example.data.network

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface PocketBaseService {
    suspend fun login(body: Map<String, String>): Response<PocketBaseAuthResponse>
    suspend fun signup(body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseUser>
    suspend fun requestPasswordReset(body: Map<String, String>): Response<Unit>
    suspend fun updateProfile(token: String, userId: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseUser>
    suspend fun getPosts(page: Int, perPage: Int, sort: String = "-created", filter: String? = null, expand: String? = "user_id"): Response<PocketBaseListResponse<PocketBasePost>>
    suspend fun getPostById(id: String, expand: String? = "user_id"): Response<PocketBasePost>
    suspend fun createPost(token: String, body: RequestBody): Response<PocketBasePost>
    suspend fun createPostJson(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBasePost>
    suspend fun createPostMultipart(token: String, partMap: Map<String, @JvmSuppressWildcards RequestBody>, fotos: List<@JvmSuppressWildcards MultipartBody.Part>?): Response<PocketBasePost>
    suspend fun updatePost(token: String, id: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBasePost>
    suspend fun deletePost(token: String, id: String): Response<Unit>
    suspend fun getComments(filter: String? = null, sort: String = "created", expand: String? = "user_id"): Response<PocketBaseListResponse<PocketBaseComment>>
    suspend fun createComment(token: String, body: Map<String, String>): Response<PocketBaseComment>
    suspend fun deleteComment(token: String, id: String): Response<Unit>
    suspend fun getLikes(filter: String? = null): Response<PocketBaseListResponse<PocketBaseLike>>
    suspend fun createLike(token: String, body: Map<String, String>): Response<PocketBaseLike>
    suspend fun deleteLike(token: String, id: String): Response<Unit>
    suspend fun getVeterinarias(sort: String = "nombre", filter: String? = null): Response<PocketBaseListResponse<PocketBaseVeterinaria>>
    suspend fun createVeterinaria(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseVeterinaria>
    suspend fun getCampaigns(sort: String = "-created"): Response<PocketBaseListResponse<PocketBaseCampaign>>
    suspend fun createCampaign(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseCampaign>
    suspend fun getNews(sort: String = "-created"): Response<PocketBaseListResponse<PocketBaseNews>>
    suspend fun createNews(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseNews>
    suspend fun getUserStatuses(sort: String = "-created"): Response<PocketBaseListResponse<PocketBaseUserStatus>>
    suspend fun createUserStatus(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseUserStatus>
    suspend fun getStatusLikes(filter: String? = null): Response<PocketBaseListResponse<PocketBaseStatusLike>>
    suspend fun createStatusLike(token: String, body: Map<String, String>): Response<PocketBaseStatusLike>
    suspend fun deleteStatusLike(token: String, id: String): Response<Unit>
    suspend fun getReports(token: String): Response<PocketBaseListResponse<PocketBaseReport>>
    suspend fun createReport(token: String, body: Map<String, String>): Response<PocketBaseReport>
    suspend fun updateReport(token: String, id: String, body: Map<String, String>): Response<PocketBaseReport>
    suspend fun getProfileReports(token: String): Response<PocketBaseListResponse<PocketBaseProfileReport>>
    suspend fun createProfileReport(token: String, body: Map<String, String>): Response<PocketBaseProfileReport>
    suspend fun updateProfileReport(token: String, id: String, body: Map<String, String>): Response<PocketBaseProfileReport>
    suspend fun getNotifications(token: String, filter: String? = null, sort: String = "-created"): Response<PocketBaseListResponse<PocketBaseNotification>>
    suspend fun createNotification(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseNotification>
    suspend fun updateNotification(token: String, id: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseNotification>
    suspend fun getColaboradores(sort: String = "nombre"): Response<PocketBaseListResponse<PocketBaseColaborador>>
    suspend fun createColaborador(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseColaborador>
    suspend fun updateColaborador(token: String, id: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseColaborador>
    suspend fun deleteColaborador(token: String, id: String): Response<Unit>
    suspend fun getBlocks(filter: String? = null): Response<PocketBaseListResponse<PocketBaseUserBlock>>
    suspend fun createBlock(token: String, body: Map<String, String>): Response<PocketBaseUserBlock>
    suspend fun deleteBlock(token: String, id: String): Response<Unit>
    suspend fun getAdminActions(token: String, sort: String = "-created"): Response<PocketBaseListResponse<PocketBaseAdminAction>>
    suspend fun createAdminAction(token: String, body: Map<String, @JvmSuppressWildcards Any>): Response<PocketBaseAdminAction>
    suspend fun getFollows(filter: String? = null): Response<PocketBaseListResponse<PocketBaseUserFollow>>
    suspend fun createFollow(token: String, body: Map<String, String>): Response<PocketBaseUserFollow>
    suspend fun deleteFollow(token: String, id: String): Response<Unit>
}
