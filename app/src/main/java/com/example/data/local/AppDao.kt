package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Users
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    // Posts
    @Query("SELECT * FROM posts ORDER BY created DESC")
    fun getAllPostsFlow(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): PostEntity?

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY created DESC")
    fun getPostsByUserIdFlow(userId: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: String)

    // Comments
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY created ASC")
    fun getCommentsForPostFlow(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: String)

    // Likes
    @Query("SELECT * FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun getLike(postId: String, userId: String): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Delete
    suspend fun deleteLike(like: LikeEntity)

    // Reports
    @Query("SELECT * FROM reports ORDER BY id DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: String)

    // Profile Reports
    @Query("SELECT * FROM profile_reports ORDER BY id DESC")
    fun getAllProfileReportsFlow(): Flow<List<ProfileReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileReport(report: ProfileReportEntity)

    // User Statuses
    @Query("SELECT * FROM user_statuses ORDER BY created DESC")
    fun getAllStatusesFlow(): Flow<List<UserStatusEntity>>

    @Query("SELECT * FROM user_statuses WHERE userId = :userId ORDER BY created DESC")
    fun getStatusesForUserFlow(userId: String): Flow<List<UserStatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: UserStatusEntity)

    @Query("DELETE FROM user_statuses WHERE id = :statusId")
    suspend fun deleteStatusById(statusId: String)

    // Status Likes
    @Query("SELECT * FROM status_likes WHERE statusId = :statusId AND userId = :userId")
    suspend fun getStatusLike(statusId: String, userId: String): StatusLikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatusLike(like: StatusLikeEntity)

    @Delete
    suspend fun deleteStatusLike(like: StatusLikeEntity)

    // Veterinarias
    @Query("SELECT * FROM veterinarias WHERE activa = 1")
    fun getAllVeterinariasFlow(): Flow<List<VeterinariaEntity>>

    @Query("SELECT * FROM veterinarias WHERE clasificacion = :clasificacion AND activa = 1")
    fun getVeterinariasByClasificacionFlow(clasificacion: String): Flow<List<VeterinariaEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVeterinaria(veterinaria: VeterinariaEntity)

    // Campaigns
    @Query("SELECT * FROM campaigns ORDER BY id DESC")
    fun getAllCampaignsFlow(): Flow<List<CampaignEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: CampaignEntity)

    // News
    @Query("SELECT * FROM news ORDER BY created DESC")
    fun getAllNewsFlow(): Flow<List<NewsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: NewsEntity)

    // Notifications
    @Query("SELECT * FROM notifications WHERE userId = :userId ORDER BY created DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    // Colaboradores
    @Query("SELECT * FROM colaboradores")
    fun getAllColaboradoresFlow(): Flow<List<ColaboradorEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertColaborador(colaborador: ColaboradorEntity)

    // User Blocks
    @Query("SELECT * FROM user_blocks WHERE userId = :userId")
    fun getBlockedUsersFlow(userId: String): Flow<List<UserBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: UserBlockEntity)

    @Query("DELETE FROM user_blocks WHERE userId = :userId AND blockedUserId = :blockedId")
    suspend fun deleteBlock(userId: String, blockedId: String)

    // User Follows
    @Query("SELECT * FROM user_follows WHERE followerId = :followerId")
    fun getFollowsFlow(followerId: String): Flow<List<UserFollowEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: UserFollowEntity)

    @Query("DELETE FROM user_follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)
}
