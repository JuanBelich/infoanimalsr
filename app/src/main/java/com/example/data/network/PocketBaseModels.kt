package com.example.data.network

import com.example.data.model.*
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun parsePbDate(dateStr: String?): Long {
    if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX",
        "yyyy-MM-dd HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss"
    )
    for (pattern in formats) {
        try {
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            return sdf.parse(dateStr)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {}
    }
    return System.currentTimeMillis()
}

@JsonClass(generateAdapter = true)
data class PocketBaseListResponse<T>(
    val page: Int = 1,
    val perPage: Int = 100,
    val totalItems: Int = 0,
    val totalPages: Int = 1,
    val items: List<T> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PocketBaseUser(
    val id: String,
    val email: String? = "",
    val nombre: String? = "",
    val telefono: String? = "",
    @Json(name = "zona_principal") val zonaPrincipal: String? = "San Rafael",
    val rol: String? = "user",
    val bloqueado: Boolean? = false,
    @Json(name = "foto_perfil") val rawFotoPerfil: String? = "",
    @Json(name = "avatar_url") val rawAvatarUrl: String? = "",
    @Json(name = "foto_portada") val fotoPortada: String? = "",
    val biografia: String? = ""
) {
    val fotoPerfil: String
        get() = when {
            !rawFotoPerfil.isNullOrBlank() -> rawFotoPerfil
            !rawAvatarUrl.isNullOrBlank() -> rawAvatarUrl
            else -> ""
        }
}

fun PocketBaseUser.toEntity() = UserEntity(
    id = id,
    email = email ?: "",
    nombre = nombre ?: "",
    telefono = telefono ?: "",
    zonaPrincipal = zonaPrincipal ?: "San Rafael",
    rol = rol ?: "user",
    bloqueado = bloqueado ?: false,
    fotoPerfil = fotoPerfil,
    fotoPortada = fotoPortada ?: "",
    biografia = biografia ?: ""
)

fun SupabaseUser.toEntity() = UserEntity(
    id = id,
    email = email ?: "",
    nombre = nombre ?: "",
    telefono = telefono ?: "",
    zonaPrincipal = zonaPrincipal ?: "San Rafael",
    rol = rol ?: "user",
    bloqueado = false,
    fotoPerfil = fotoPerfil,
    fotoPortada = fotoPortada ?: "",
    biografia = biografia ?: ""
)

fun SupabaseUserContainer.toEntity() = UserEntity(
    id = id,
    email = email ?: "",
    nombre = (userMetadata?.get("nombre") as? String) ?: "",
    telefono = (userMetadata?.get("telefono") as? String) ?: "",
    zonaPrincipal = (userMetadata?.get("zona_principal") as? String) ?: "San Rafael",
    rol = (userMetadata?.get("rol") as? String) ?: "user",
    bloqueado = false,
    fotoPerfil = (userMetadata?.get("foto_perfil") as? String) ?: "",
    fotoPortada = (userMetadata?.get("foto_portada") as? String) ?: "",
    biografia = (userMetadata?.get("biografia") as? String) ?: ""
)

@JsonClass(generateAdapter = true)
data class PocketBasePost(
    val id: String,
    @Json(name = "user_id") val userId: String? = "",
    val tipo: String? = "",
    val especie: String? = "",
    val tamano: String? = "",
    @Json(name = "nombre_mascota") val nombreMascota: String? = "",
    val descripcion: String? = "",
    val rasgos: String? = "",
    val estado: String? = "activo",
    val fotos: Any? = null,
    @Json(name = "ubicacion_lat") val ubicacionLat: Double? = 0.0,
    @Json(name = "ubicacion_lng") val ubicacionLng: Double? = 0.0,
    val distrito: String? = "",
    val zona: String? = "",
    @Json(name = "telefono_contacto") val telefonoContacto: String? = "",
    @Json(name = "telefono_visible") val telefonoVisible: Any? = true,
    @Json(name = "likes_count") val likesCount: Int? = 0,
    val created: String? = "",
    val expand: Map<String, Any>? = null
)

fun PocketBasePost.toEntity(): PostEntity {
    val photosList = when (fotos) {
        is List<*> -> (fotos as List<*>).filterNotNull().map { it.toString() }
        is String -> if (fotos.isNotBlank()) listOf(fotos) else emptyList()
        else -> emptyList()
    }
    return PostEntity(
        id = id,
        userId = userId ?: "",
        userNombreCompleto = "",
        userFotoPerfil = "",
        tipo = tipo ?: "perdida",
        especie = especie ?: "perro",
        tamano = tamano ?: "mediano",
        nombreMascota = nombreMascota ?: "",
        descripcion = descripcion ?: "",
        rasgos = rasgos ?: "",
        estado = estado ?: "activo",
        fotosJson = photosList.joinToString(","),
        ubicacionLat = ubicacionLat ?: 0.0,
        ubicacionLng = ubicacionLng ?: 0.0,
        distrito = distrito ?: "San Rafael",
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
}

fun SupabasePost.toEntity(): PostEntity {
    val photosList = when (fotos) {
        is List<*> -> (fotos as List<*>).filterNotNull().map { it.toString() }
        is String -> if (fotos.isNotBlank()) listOf(fotos) else emptyList()
        else -> emptyList()
    }
    return PostEntity(
        id = id,
        userId = userId ?: "",
        userNombreCompleto = users?.nombre ?: "",
        userFotoPerfil = users?.fotoPerfil ?: "",
        tipo = tipo ?: "perdida",
        especie = especie ?: "perro",
        tamano = tamano ?: "mediano",
        nombreMascota = nombreMascota ?: "",
        descripcion = descripcion ?: "",
        rasgos = rasgos ?: "",
        estado = estado ?: "activo",
        fotosJson = photosList.joinToString(","),
        ubicacionLat = ubicacionLat ?: 0.0,
        ubicacionLng = ubicacionLng ?: 0.0,
        distrito = distrito ?: "San Rafael",
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
}

@JsonClass(generateAdapter = true)
data class PocketBaseComment(
    val id: String,
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String,
    val texto: String,
    val created: String? = ""
)

fun PocketBaseComment.toEntity() = CommentEntity(
    id = id,
    postId = postId,
    userId = userId,
    texto = texto,
    userNombreCompleto = "",
    userFotoPerfil = "",
    userIsOng = false,
    created = parsePbDate(created)
)

fun SupabaseComment.toEntity() = CommentEntity(
    id = id,
    postId = postId,
    userId = userId,
    texto = texto,
    userNombreCompleto = users?.nombre ?: "",
    userFotoPerfil = users?.fotoPerfil ?: "",
    userIsOng = false,
    created = parsePbDate(created)
)

@JsonClass(generateAdapter = true)
data class PocketBaseVeterinaria(
    val id: String,
    val nombre: String,
    val direccion: String,
    val telefono: String,
    val staff: String? = "",
    val horarios: String? = "",
    val distrito: String? = "San Rafael",
    val clasificacion: String? = "Horario Comercio",
    @Json(name = "ubicacion_lat") val ubicacionLat: Double? = 0.0,
    @Json(name = "ubicacion_lng") val ubicacionLng: Double? = 0.0,
    val logo: String? = "",
    val activa: Boolean? = true
)

fun PocketBaseVeterinaria.toEntity() = VeterinariaEntity(
    id = id,
    nombre = nombre,
    direccion = direccion,
    telefono = telefono,
    staff = staff ?: "",
    horarios = horarios ?: "",
    distrito = distrito ?: "San Rafael",
    clasificacion = clasificacion ?: "Horario Comercio",
    ubicacionLat = ubicacionLat ?: 0.0,
    ubicacionLng = ubicacionLng ?: 0.0,
    logo = logo ?: "",
    activa = activa ?: true
)

@JsonClass(generateAdapter = true)
data class PocketBaseCampaign(
    val id: String,
    val titulo: String,
    val descripcion: String,
    @Json(name = "imagen_portada") val imagenPortada: String? = "",
    @Json(name = "contenido_detallado") val contenidoDetallado: String? = "",
    @Json(name = "fecha_inicio") val fechaInicio: String? = "",
    @Json(name = "fecha_fin") val fechaFin: String? = "",
    @Json(name = "ong_id") val ongId: String? = "",
    @Json(name = "ong_nombre") val ongNombre: String? = "",
    val estado: String? = "activa",
    @Json(name = "likes_count") val likesCount: Int? = 0
)

fun PocketBaseCampaign.toEntity() = CampaignEntity(
    id = id,
    titulo = titulo,
    descripcion = descripcion,
    imagenPortada = imagenPortada ?: "",
    contenidoDetallado = contenidoDetallado ?: "",
    fechaInicio = fechaInicio ?: "",
    fechaFin = fechaFin ?: "",
    ongId = ongId ?: "",
    ongNombre = ongNombre ?: "",
    estado = estado ?: "activa",
    likesCount = likesCount ?: 0
)

@JsonClass(generateAdapter = true)
data class PocketBaseNews(
    val id: String,
    val titular: String,
    val contenido: String,
    @Json(name = "autor_id") val autorId: String? = "",
    @Json(name = "autor_nombre") val autorNombre: String? = "",
    @Json(name = "imagen_portada") val imagenPortada: String? = "",
    @Json(name = "imagen_noticia") val imagenNoticia: String? = "",
    val created: String? = ""
)

fun PocketBaseNews.toEntity() = NewsEntity(
    id = id,
    titular = titular,
    contenido = contenido,
    autorId = autorId ?: "",
    autorNombre = autorNombre ?: "",
    imagenPortada = imagenPortada ?: "",
    imagenNoticia = imagenNoticia ?: "",
    created = parsePbDate(created)
)

@JsonClass(generateAdapter = true)
data class PocketBaseColaborador(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val logo: String? = "",
    val enlace: String? = "",
    val telefono: String? = "",
    val whatsapp: String? = "",
    val instagram: String? = "",
    val direccion: String? = ""
)

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

@JsonClass(generateAdapter = true)
data class PocketBaseUserStatus(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val texto: String,
    @Json(name = "likes_count") val likesCount: Int? = 0,
    val created: String? = ""
)
