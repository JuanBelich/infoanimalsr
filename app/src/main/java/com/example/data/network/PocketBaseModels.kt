package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@JsonClass(generateAdapter = true)
data class PocketBaseListResponse<T>(
    @Json(name = "page") val page: Int,
    @Json(name = "perPage") val perPage: Int,
    @Json(name = "totalItems") val totalItems: Int,
    @Json(name = "totalPages") val totalPages: Int,
    @Json(name = "items") val items: List<T>
)

@JsonClass(generateAdapter = true)
data class PocketBaseUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String,
    @Json(name = "nombre") val nombre: String,
    @Json(name = "telefono") val telefono: String? = "",
    @Json(name = "zona_principal") val zonaPrincipal: String? = "",
    @Json(name = "rol") val rol: String? = "user",
    @Json(name = "bloqueado") val bloqueado: Boolean? = false,
    @Json(name = "foto_perfil") val fotoPerfil: String? = "",
    @Json(name = "foto_portada") val fotoPortada: String? = "",
    @Json(name = "biografia") val biografia: String? = "",
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseAuthResponse(
    @Json(name = "token") val token: String,
    @Json(name = "record") val record: PocketBaseUser
)

@JsonClass(generateAdapter = true)
data class PocketBasePost(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "tipo") val tipo: String, // "perdida", "encontrada", "avistamiento", "en_transito"
    @Json(name = "especie") val especie: String, // "perro", "gato", "otros"
    @Json(name = "tamano") val tamano: String, // "pequeño", "mediano", "grande"
    @Json(name = "nombre_mascota") val nombreMascota: String? = "",
    @Json(name = "descripcion") val descripcion: String,
    @Json(name = "rasgos") val rasgos: String? = "",
    @Json(name = "estado") val estado: String? = "activo",
    @Json(name = "fotos") val fotos: List<String>? = emptyList(), // JSON array of photo filenames or single values
    @Json(name = "ubicacion_lat") val ubicacionLat: Double? = 0.0,
    @Json(name = "ubicacion_lng") val ubicacionLng: Double? = 0.0,
    @Json(name = "distrito") val distrito: String,
    @Json(name = "zona") val zona: String? = "",
    @Json(name = "telefono_contacto") val telefonoContacto: String? = "",
    @Json(name = "telefono_visible") val telefonoVisible: Any? = true, // PB can return a boolean or string "true" / "false"
    @Json(name = "likes_count") val likesCount: Int? = 0,
    @Json(name = "user_nombre_completo") val userNombreCompleto: String? = "",
    @Json(name = "user_foto_perfil") val userFotoPerfil: String? = "",
    @Json(name = "user_id_expandido") val userIdExpandido: String? = "",
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseComment(
    @Json(name = "id") val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "texto") val texto: String,
    @Json(name = "user_nombre_completo") val userNombreCompleto: String? = "",
    @Json(name = "user_foto_perfil") val userFotoPerfil: String? = "",
    @Json(name = "user_id_expandido") val userIdExpandido: String? = "",
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseLike(
    @Json(name = "id") val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class PocketBaseReport(
    @Json(name = "id") val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "reporter_id") val reporterUserId: String,
    @Json(name = "razon") val razon: String,
    @Json(name = "estado") val estado: String? = "pendiente"
)

@JsonClass(generateAdapter = true)
data class PocketBaseProfileReport(
    @Json(name = "id") val id: String,
    @Json(name = "profile_id") val reportedUserId: String,
    @Json(name = "reporter_id") val reporterUserId: String,
    @Json(name = "razon") val razon: String,
    @Json(name = "estado") val estado: String? = "pendiente"
)

@JsonClass(generateAdapter = true)
data class PocketBaseUserStatus(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "texto") val texto: String,
    @Json(name = "likes_count") val likesCount: Int? = 0,
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseStatusLike(
    @Json(name = "id") val id: String,
    @Json(name = "status_id") val statusId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class PocketBaseVeterinaria(
    @Json(name = "id") val id: String,
    @Json(name = "nombre") val nombre: String,
    @Json(name = "direccion") val direccion: String,
    @Json(name = "telefono") val telefono: String,
    @Json(name = "staff") val staff: String? = "",
    @Json(name = "horarios") val horarios: String? = "",
    @Json(name = "distrito") val distrito: String,
    @Json(name = "clasificacion") val clasificacion: String,
    @Json(name = "ubicacion_lat") val ubicacionLat: Double? = 0.0,
    @Json(name = "ubicacion_lng") val ubicacionLng: Double? = 0.0,
    @Json(name = "logo") val logo: String? = "",
    @Json(name = "activa") val activa: Boolean? = true
)

@JsonClass(generateAdapter = true)
data class PocketBaseCampaign(
    @Json(name = "id") val id: String,
    @Json(name = "titulo") val titulo: String,
    @Json(name = "descripcion") val descripcion: String,
    @Json(name = "imagen_portada") val imagenPortada: String? = "",
    @Json(name = "contenido_detallado") val contenidoDetallado: String? = "",
    @Json(name = "fecha_inicio") val fechaInicio: String? = "",
    @Json(name = "fecha_fin") val fechaFin: String? = "",
    @Json(name = "ong_id") val ongId: String,
    @Json(name = "estado") val estado: String? = "activa",
    @Json(name = "likes_count") val likesCount: Int? = 0,
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseCampaignLike(
    @Json(name = "id") val id: String,
    @Json(name = "campaign_id") val campaignId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class PocketBaseNews(
    @Json(name = "id") val id: String,
    @Json(name = "titular") val titular: String,
    @Json(name = "contenido") val contenido: String,
    @Json(name = "autor_id") val autorId: String,
    @Json(name = "autor_nombre") val autorNombre: String? = "",
    @Json(name = "imagen_portada") val imagenPortada: String? = "",
    @Json(name = "imagen_noticia") val imagenNoticia: String? = "",
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseNotification(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "tipo") val tipo: String,
    @Json(name = "post_id") val postId: String? = "",
    @Json(name = "comment_id") val commentId: String? = "",
    @Json(name = "from_user_id") val fromUserId: String? = "",
    @Json(name = "mensaje") val mensaje: String,
    @Json(name = "leida") val leida: Boolean? = false,
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseColaborador(
    @Json(name = "id") val id: String,
    @Json(name = "nombre") val nombre: String,
    @Json(name = "descripcion") val descripcion: String,
    @Json(name = "logo") val logo: String? = "",
    @Json(name = "enlace") val enlace: String? = "",
    @Json(name = "telefono") val telefono: String? = "",
    @Json(name = "whatsapp") val whatsapp: String? = "",
    @Json(name = "instagram") val instagram: String? = "",
    @Json(name = "direccion") val direccion: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseUserBlock(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "blocked_user_id") val blockedUserId: String,
    @Json(name = "blocked_user_nombre") val blockedUserNombre: String? = "",
    @Json(name = "blocked_user_foto") val blockedUserFoto: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseAdminAction(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "accion") val accion: String,
    @Json(name = "target_id") val targetId: String? = "",
    @Json(name = "detalles") val detalles: String? = "",
    @Json(name = "created") val created: String? = ""
)

@JsonClass(generateAdapter = true)
data class PocketBaseUserFollow(
    @Json(name = "id") val id: String,
    @Json(name = "follower_id") val followerId: String,
    @Json(name = "following_id") val followingId: String,
    @Json(name = "created") val created: String? = ""
)

// --- Mapper Helpers & Extension Functions ---

fun PocketBaseColaborador.toEntity() = ColaboradorEntity(
    id = id,
    nombre = nombre,
    descripcion = descripcion,
    logo = logo ?: "",
    enlace = enlace ?: "",
    telefono = telefono ?: "",
    whatsapp = whatsapp ?: "",
    instagram = instagram ?: "",
    direccion = direccion ?: ""
)

fun PocketBaseUserBlock.toEntity() = UserBlockEntity(
    id = id,
    userId = userId,
    blockedUserId = blockedUserId,
    blockedUserNombre = blockedUserNombre ?: "",
    blockedUserFoto = blockedUserFoto ?: ""
)

fun PocketBaseAdminAction.toEntity() = AdminActionEntity(
    id = id,
    userId = userId,
    accion = accion,
    targetId = targetId ?: "",
    detalles = detalles ?: "",
    created = parsePbDate(created)
)

fun PocketBaseUserFollow.toEntity() = UserFollowEntity(
    id = id,
    followerId = followerId,
    followingId = followingId,
    created = parsePbDate(created)
)


fun parsePbDate(dateStr: String?): Long {
    if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
    return try {
        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        var parsedTime: Long? = null
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                val clean = dateStr.replace("T", " ").replace("Z", "")
                parsedTime = sdf.parse(clean)?.time
                if (parsedTime != null) break
            } catch (e: Exception) {
                // Try next
            }
        }
        parsedTime ?: System.currentTimeMillis()
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun PocketBaseUser.toEntity(): UserEntity {
    val assignedRol = if (com.example.BuildConfig.ENABLE_TEST_ROLE_MAPPING && email.endsWith("@infoanimals.com", ignoreCase = true)) {
        when {
            email.contains("superadmin", ignoreCase = true) -> "super_admin"
            email.contains("admin", ignoreCase = true) -> "admin"
            email.contains("ong", ignoreCase = true) -> "ong"
            else -> rol ?: "user"
        }
    } else {
        val rawRol = rol ?: "user"
        if (rawRol == "superadmin") "super_admin" else rawRol
    }
    return UserEntity(
        id = id,
        email = email,
        nombre = nombre,
        telefono = telefono ?: "",
        zonaPrincipal = zonaPrincipal ?: "",
        rol = assignedRol,
        bloqueado = bloqueado ?: false,
        fotoPerfil = fotoPerfil ?: "",
        fotoPortada = fotoPortada ?: "",
        biografia = biografia ?: ""
    )
}

fun PocketBasePost.toEntity() = PostEntity(
    id = id,
    userId = userId,
    userNombreCompleto = userNombreCompleto ?: "InfoAnimal Vecino",
    userFotoPerfil = userFotoPerfil ?: "",
    tipo = tipo,
    especie = especie,
    tamano = tamano,
    nombreMascota = nombreMascota ?: "",
    descripcion = descripcion,
    rasgos = rasgos ?: "",
    estado = estado ?: "activo",
    fotosJson = fotos?.joinToString(",") ?: "",
    ubicacionLat = ubicacionLat ?: 0.0,
    ubicacionLng = ubicacionLng ?: 0.0,
    distrito = distrito,
    zona = zona ?: "",
    telefonoContacto = telefonoContacto ?: "",
    telefonoVisible = when (telefonoVisible) {
        is Boolean -> telefonoVisible
        is String -> telefonoVisible.toBoolean()
        else -> true
    },
    likesCount = likesCount ?: 0,
    created = parsePbDate(created)
)

fun PocketBaseComment.toEntity() = CommentEntity(
    id = id,
    postId = postId,
    userId = userId,
    texto = texto,
    userNombreCompleto = userNombreCompleto ?: "Vecino",
    userFotoPerfil = userFotoPerfil ?: "",
    userIsOng = false, // Resolve at UI if needed
    created = parsePbDate(created)
)

fun PocketBaseLike.toEntity() = LikeEntity(
    id = id,
    postId = postId,
    userId = userId
)

fun PocketBaseVeterinaria.toEntity() = VeterinariaEntity(
    id = id,
    nombre = nombre,
    direccion = direccion,
    telefono = telefono,
    staff = staff ?: "",
    horarios = horarios ?: "",
    distrito = distrito,
    clasificacion = clasificacion,
    ubicacionLat = ubicacionLat ?: 0.0,
    ubicacionLng = ubicacionLng ?: 0.0,
    logo = logo ?: "",
    activa = activa ?: true
)

fun PocketBaseCampaign.toEntity() = CampaignEntity(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    imagenPortada = imagenPortada ?: "",
    contenidoDetallado = contenidoDetallado ?: "",
    fechaInicio = fechaInicio ?: "",
    fechaFin = fechaFin ?: "",
    ongId = ongId,
    ongNombre = "Estación ONG",
    estado = estado ?: "activa",
    likesCount = likesCount ?: 0
)

fun PocketBaseNews.toEntity() = NewsEntity(
    id = id,
    titular = titular,
    contenido = contenido,
    autorId = autorId,
    autorNombre = autorNombre ?: "InfoAnimal Staff",
    imagenPortada = imagenPortada ?: "",
    imagenNoticia = imagenNoticia ?: "",
    created = parsePbDate(created)
)

fun PocketBaseNotification.toEntity() = NotificationEntity(
    id = id,
    userId = userId,
    tipo = tipo,
    postId = postId ?: "",
    commentId = commentId ?: "",
    fromUserId = fromUserId ?: "",
    fromUserNombre = "",
    mensaje = mensaje,
    leida = leida ?: false,
    created = parsePbDate(created)
)

