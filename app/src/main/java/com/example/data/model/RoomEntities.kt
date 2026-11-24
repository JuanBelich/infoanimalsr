package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val nombre: String,
    val telefono: String = "",
    val zonaPrincipal: String = "",
    val rol: String = "user", // "user", "ong", "admin", "super_admin"
    val bloqueado: Boolean = false,
    val fotoPerfil: String = "",
    val fotoPortada: String = "",
    val biografia: String = ""
)

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userNombreCompleto: String,
    val userFotoPerfil: String,
    val tipo: String, // "perdida", "encontrada", "avistamiento", "en_transito"
    val especie: String, // "perro", "gato", "otros"
    val tamano: String, // "pequeño", "mediano", "grande"
    val nombreMascota: String = "",
    val descripcion: String,
    val rasgos: String = "",
    val estado: String = "activo", // "activo", "resuelta"
    val fotosJson: String = "",
    val ubicacionLat: Double = 0.0,
    val ubicacionLng: Double = 0.0,
    val distrito: String, // "San Rafael", "General Alvear", "Malargüe"
    val zona: String = "",
    val telefonoContacto: String = "",
    val telefonoVisible: Boolean = true,
    val likesCount: Int = 0,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String,
    val texto: String,
    val userNombreCompleto: String,
    val userFotoPerfil: String,
    val userIsOng: Boolean = false,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "likes")
data class LikeEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val userId: String
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val reporterUserId: String,
    val razon: String,
    val estado: String = "pendiente"
)

@Entity(tableName = "profile_reports")
data class ProfileReportEntity(
    @PrimaryKey val id: String,
    val reportedUserId: String,
    val reporterUserId: String,
    val razon: String,
    val estado: String = "pendiente"
)

@Entity(tableName = "user_statuses")
data class UserStatusEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userNombreCompleto: String,
    val userFotoPerfil: String,
    val texto: String,
    val likesCount: Int = 0,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "status_likes")
data class StatusLikeEntity(
    @PrimaryKey val id: String,
    val statusId: String,
    val userId: String
)

@Entity(tableName = "veterinarias")
data class VeterinariaEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val staff: String = "",
    val horarios: String = "",
    val distrito: String,
    val clasificacion: String,
    val ubicacionLat: Double = 0.0,
    val ubicacionLng: Double = 0.0,
    val logo: String = "",
    val activa: Boolean = true
)

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: String,
    val titulo: String,
    val descripcion: String,
    val imagenPortada: String,
    val contenidoDetallado: String,
    val fechaInicio: String,
    val fechaFin: String,
    val ongId: String,
    val ongNombre: String,
    val estado: String = "activa",
    val likesCount: Int = 0
)

@Entity(tableName = "campaign_likes")
data class CampaignLikeEntity(
    @PrimaryKey val id: String,
    val campaignId: String,
    val userId: String
)

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: String,
    val titular: String,
    val contenido: String,
    val autorId: String,
    val autorNombre: String,
    val imagenPortada: String,
    val imagenNoticia: String = "",
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val tipo: String,
    val postId: String = "",
    val commentId: String = "",
    val fromUserId: String = "",
    val fromUserNombre: String = "",
    val mensaje: String,
    val leida: Boolean = false,
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "colaboradores")
data class ColaboradorEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val logo: String = "",
    val enlace: String = "",
    val telefono: String = "",
    val whatsapp: String = "",
    val instagram: String = "",
    val direccion: String = ""
)

@Entity(tableName = "user_blocks")
data class UserBlockEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val blockedUserId: String,
    val blockedUserNombre: String = "",
    val blockedUserFoto: String = ""
)

@Entity(tableName = "admin_actions")
data class AdminActionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val accion: String,
    val targetId: String = "",
    val detalles: String = "",
    val created: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_follows")
data class UserFollowEntity(
    @PrimaryKey val id: String,
    val followerId: String,
    val followingId: String,
    val created: Long = System.currentTimeMillis()
)
