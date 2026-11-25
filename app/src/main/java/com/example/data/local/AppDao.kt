package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Users ---
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    fun getUsersCountFlow(): Flow<Int>

    // --- Posts ---
    @Query("SELECT * FROM posts ORDER BY created DESC LIMIT :limit")
    fun getRecentPostsFlow(limit: Int): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts ORDER BY created DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: String): PostEntity?

    @Query("SELECT * FROM posts WHERE id = :id")
    fun getPostByIdFlow(id: String): Flow<PostEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :id")
    suspend fun deletePostById(id: String)

    @Query("SELECT COUNT(*) FROM posts")
    fun getPostsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM posts WHERE estado = 'activo'")
    fun getActivePostsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM posts WHERE estado = 'resuelta'")
    fun getResolvedPostsCountFlow(): Flow<Int>

    // --- Comments ---
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY created ASC")
    fun getCommentsForPostFlow(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :id")
    suspend fun deleteCommentById(id: String)

    // --- Likes ---
    @Query("SELECT * FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun getLike(postId: String, userId: String): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: String, userId: String)

    // --- Reports ---
    @Query("SELECT * FROM reports ORDER BY id DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("UPDATE reports SET estado = :estado WHERE id = :id")
    suspend fun updateReportEstado(id: String, estado: String)

    // --- Profile Reports ---
    @Query("SELECT * FROM profile_reports ORDER BY id DESC")
    fun getAllProfileReportsFlow(): Flow<List<ProfileReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileReport(report: ProfileReportEntity)

    // --- User Statuses ---
    @Query("SELECT * FROM user_statuses WHERE userId = :userId ORDER BY created DESC")
    fun getStatusesForUserFlow(userId: String): Flow<List<UserStatusEntity>>

    @Query("SELECT * FROM user_statuses ORDER BY created DESC")
    fun getAllStatusesFlow(): Flow<List<UserStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: UserStatusEntity)

    @Query("DELETE FROM user_statuses WHERE id = :id")
    suspend fun deleteStatusById(id: String)

    @Query("UPDATE user_statuses SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementStatusLikes(id: String)

    @Query("UPDATE user_statuses SET likesCount = CASE WHEN likesCount > 0 THEN likesCount - 1 ELSE 0 END WHERE id = :id")
    suspend fun decrementStatusLikes(id: String)

    // --- Status Likes ---
    @Query("SELECT * FROM status_likes WHERE statusId = :statusId AND userId = :userId")
    suspend fun getStatusLike(statusId: String, userId: String): StatusLikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusLike(like: StatusLikeEntity)

    @Query("DELETE FROM status_likes WHERE statusId = :statusId AND userId = :userId")
    suspend fun deleteStatusLike(statusId: String, userId: String)

    // --- Veterinarias ---
    @Query("SELECT * FROM veterinarias ORDER BY nombre ASC")
    fun getAllVeterinariasFlow(): Flow<List<VeterinariaEntity>>

    @Query("SELECT * FROM veterinarias WHERE clasificacion = :clasificacion ORDER BY nombre ASC")
    fun getVeterinariasByClasificacionFlow(clasificacion: String): Flow<List<VeterinariaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVeterinaria(veterinaria: VeterinariaEntity)

    @Query("DELETE FROM veterinarias WHERE id = :id")
    suspend fun deleteVeterinariaById(id: String)

    // --- Campaigns ---
    @Query("SELECT * FROM campaigns ORDER BY id DESC")
    fun getAllCampaignsFlow(): Flow<List<CampaignEntity>>

    @Query("SELECT * FROM campaigns WHERE id = :id")
    fun getCampaignByIdFlow(id: String): Flow<CampaignEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity)

    @Query("DELETE FROM campaigns WHERE id = :id")
    suspend fun deleteCampaignById(id: String)

    @Query("UPDATE campaigns SET likesCount = likesCount + 1 WHERE id = :id")
    suspend fun incrementCampaignLikes(id: String)

    @Query("UPDATE campaigns SET likesCount = CASE WHEN likesCount > 0 THEN likesCount - 1 ELSE 0 END WHERE id = :id")
    suspend fun decrementCampaignLikes(id: String)

    // --- Campaign Likes ---
    @Query("SELECT * FROM campaign_likes WHERE campaignId = :campaignId AND userId = :userId")
    suspend fun getCampaignLike(campaignId: String, userId: String): CampaignLikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaignLike(like: CampaignLikeEntity)

    @Query("DELETE FROM campaign_likes WHERE campaignId = :campaignId AND userId = :userId")
    suspend fun deleteCampaignLike(campaignId: String, userId: String)

    // --- News ---
    @Query("SELECT * FROM news ORDER BY created DESC")
    fun getAllNewsFlow(): Flow<List<NewsEntity>>

    @Query("SELECT * FROM news WHERE id = :id")
    fun getNewsByIdFlow(id: String): Flow<NewsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: NewsEntity)

    @Query("DELETE FROM news WHERE id = :id")
    suspend fun deleteNewsById(id: String)

    // --- Notifications ---
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY created DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET leida = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)

    // --- Colaboradores ---
    @Query("SELECT * FROM colaboradores ORDER BY nombre ASC")
    fun getAllColaboradoresFlow(): Flow<List<ColaboradorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColaborador(colaborador: ColaboradorEntity)

    @Query("DELETE FROM colaboradores WHERE id = :id")
    suspend fun deleteColaboradorById(id: String)

    // --- User Blocks ---
    @Query("SELECT * FROM user_blocks WHERE userId = :userId")
    fun getBlocksForUserFlow(userId: String): Flow<List<UserBlockEntity>>

    @Query("SELECT * FROM user_blocks WHERE userId = :userId")
    suspend fun getBlocksForUser(userId: String): List<UserBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: UserBlockEntity)

    @Query("DELETE FROM user_blocks WHERE userId = :userId AND blockedUserId = :blockedUserId")
    suspend fun deleteBlock(userId: String, blockedUserId: String)

    // --- Admin Actions ---
    @Query("SELECT * FROM admin_actions ORDER BY created DESC")
    fun getAllAdminActionsFlow(): Flow<List<AdminActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdminAction(action: AdminActionEntity)

    // --- User Follows ---
    @Query("SELECT * FROM user_follows WHERE followerId = :followerId")
    fun getFollowsForUserFlow(followerId: String): Flow<List<UserFollowEntity>>

    @Query("SELECT * FROM user_follows WHERE followerId = :followerId")
    suspend fun getFollowsForUser(followerId: String): List<UserFollowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: UserFollowEntity)

    @Query("DELETE FROM user_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)
}
