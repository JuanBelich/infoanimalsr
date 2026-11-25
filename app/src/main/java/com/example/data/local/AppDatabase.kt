package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        PostEntity::class,
        CommentEntity::class,
        LikeEntity::class,
        ReportEntity::class,
        ProfileReportEntity::class,
        UserStatusEntity::class,
        StatusLikeEntity::class,
        VeterinariaEntity::class,
        CampaignEntity::class,
        CampaignLikeEntity::class,
        NewsEntity::class,
        NotificationEntity::class,
        ColaboradorEntity::class,
        UserBlockEntity::class,
        AdminActionEntity::class,
        UserFollowEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "infoanimals_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
