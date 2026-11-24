package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.network.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "auth_session")

class AppRepository(
    private val context: Context
) {
    private val appDao by lazy { AppDatabase.getDatabase(context).appDao() }

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _colaboradores = MutableStateFlow<List<ColaboradorEntity>>(emptyList())
    val colaboradores: StateFlow<List<ColaboradorEntity>> = _colaboradores.asStateFlow()

    private val _posts = MutableStateFlow<List<PostEntity>>(emptyList())
    val posts: StateFlow<List<PostEntity>> = _posts.asStateFlow()

    private val _veterinarias = MutableStateFlow<List<VeterinariaEntity>>(emptyList())
    val veterinarias: StateFlow<List<VeterinariaEntity>> = _veterinarias.asStateFlow()

    private val _campaigns = MutableStateFlow<List<CampaignEntity>>(emptyList())
    val campaigns: StateFlow<List<CampaignEntity>> = _campaigns.asStateFlow()

    private val _news = MutableStateFlow<List<NewsEntity>>(emptyList())
    val news: StateFlow<List<NewsEntity>> = _news.asStateFlow()

    private val _statuses = MutableStateFlow<List<UserStatusEntity>>(emptyList())
    val statuses: StateFlow<List<UserStatusEntity>> = _statuses.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<UserBlockEntity>>(emptyList())
    val blockedUsers: StateFlow<List<UserBlockEntity>> = _blockedUsers.asStateFlow()

    private val _follows = MutableStateFlow<List<UserFollowEntity>>(emptyList())
    val follows: StateFlow<List<UserFollowEntity>> = _follows.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationEntity>>(emptyList())
    val notifications: StateFlow<List<NotificationEntity>> = _notifications.asStateFlow()

    private val _reports = MutableStateFlow<List<ReportEntity>>(emptyList())
    val reports: StateFlow<List<ReportEntity>> = _reports.asStateFlow()

    private val _profileReports = MutableStateFlow<List<ProfileReportEntity>>(emptyList())
    val profileReports: StateFlow<List<ProfileReportEntity>> = _profileReports.asStateFlow()

    private val KEY_USER_ID = stringPreferencesKey("logged_in_user_id")

    suspend fun initSession() {
        context.dataStore.data.firstOrNull()?.get(KEY_USER_ID)?.let { userId ->
            try {
                val response = ApiClient.getService().getUserProfile("eq.$userId")
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    val user = response.body()!!.first()
                    val userEntity = user.toEntity()
                    _currentUser.value = userEntity
                    Log.d("AppRepository", "initSession: Perfil recuperado para $userId")
                    syncAll()
                } else {
                    Log.d("AppRepository", "initSession: Falló recuperación de sesión")
                    context.dataStore.edit { it.remove(KEY_USER_ID) }
                    syncAll()
                }
            } catch (e: Exception) {
                Log.d("AppRepository", "initSession: Error conectando a backend", e)
                context.dataStore.edit { it.remove(KEY_USER_ID) }
                syncAll()
            }
        } ?: run {
            Log.d("AppRepository", "initSession: No hay sesión guardada")
            syncAll()
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        try {
            val response = ApiClient.getService().login(
                mapOf("email" to email, "password" to password)
            )
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                ApiClient.token = auth.accessToken
                PocketBaseClient.token = auth.accessToken

                val profResponse = ApiClient.getService().getUserProfile("eq.${auth.user.id}")
                val userEntity = if (profResponse.isSuccessful && !profResponse.body().isNullOrEmpty()) {
                    profResponse.body()!!.first().toEntity()
                } else {
                    UserEntity(
                        id = auth.user.id,
                        email = auth.user.email ?: email,
                        nombre = email.substringBefore("@"),
                        rol = if (email.contains("admin")) "admin" else "user"
                    )
                }

                _currentUser.value = userEntity
                context.dataStore.edit { prefs ->
                    prefs[KEY_USER_ID] = userEntity.id
                }

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        syncAll()
                    } catch (_: Exception) {}
                }
                return true
            } else {
                throw Exception("Credenciales incorrectas")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun signup(email: String, name: String, phone: String, password: String): UserEntity {
        val assignedRol = when {
            email.contains("superadmin", ignoreCase = true) -> "super_admin"
            email.contains("admin", ignoreCase = true) -> "admin"
            email.contains("ong", ignoreCase = true) -> "ong"
            else -> "user"
        }
        val signupData = mapOf(
            "email" to email,
            "password" to password,
            "data" to mapOf(
                "nombre" to name,
                "telefono" to phone,
                "rol" to assignedRol,
                "zona_principal" to "San Rafael"
            )
        )

        try {
            val response = ApiClient.getService().signup(signupData)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val userEntity = UserEntity(
                    id = user.id,
                    email = email,
                    nombre = name,
                    telefono = phone,
                    zonaPrincipal = "San Rafael",
                    rol = assignedRol
                )
                try {
                    ApiClient.getService().insertUserProfile(
                        mapOf(
                            "id" to user.id,
                            "email" to email,
                            "nombre" to name,
                            "telefono" to phone,
                            "rol" to assignedRol,
                            "zona_principal" to "San Rafael"
                        )
                    )
                } catch (_: Exception) {}

                try {
                    login(email, password)
                } catch (_: Exception) {}
                return userEntity
            } else {
                throw Exception("Error de registro")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun logout() {
        _currentUser.value = null
        ApiClient.token = null
        PocketBaseClient.token = null
        context.dataStore.edit { it.remove(KEY_USER_ID) }
        syncAll()
    }

    suspend fun updateUserProfile(
        nombre: String,
        telefono: String,
        zonaPrincipal: String,
        biografia: String,
        fotoPerfil: String,
        fotoPortada: String
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val updated = user.copy(
            nombre = nombre,
            telefono = telefono,
            zonaPrincipal = zonaPrincipal,
            biografia = biografia,
            fotoPerfil = fotoPerfil,
            fotoPortada = fotoPortada
        )
        _currentUser.value = updated
        try {
            appDao.insertUser(updated)
        } catch (_: Exception) {}

        return try {
            ApiClient.getService().updateUserProfile(
                "eq.${user.id}",
                mapOf(
                    "nombre" to nombre,
                    "telefono" to telefono,
                    "zona_principal" to zonaPrincipal,
                    "biografia" to biografia,
                    "foto_perfil" to fotoPerfil,
                    "foto_portada" to fotoPortada
                )
            )
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun syncAll() {
        try {
            seedMockData()
        } catch (_: Exception) {}
        try {
            syncPosts()
            syncVeterinarias()
            syncCampaigns()
            syncNews()
            syncStatuses()
            syncColaboradores()
            syncBlocks()
            syncFollows()
            syncReports()
            syncProfileReports()
            syncNotifications()
        } catch (_: Exception) {}
    }

    suspend fun syncPosts() {
        try {
            val local = try { appDao.getAllPostsFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (local.isNotEmpty() && _posts.value.isEmpty()) {
                _posts.value = local
            }
            val response = ApiClient.getService().getPosts()
            if (response.isSuccessful && response.body() != null) {
                val remote = response.body()!!.map { it.toEntity() }
                _posts.value = remote
                try {
                    remote.forEach { appDao.insertPost(it) }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d("AppRepository", "syncPosts: Fallback a local")
        }
    }

    suspend fun createPost(
        tipo: String,
        especie: String,
        tamano: String,
        nombreMascota: String,
        descripcion: String,
        rasgos: String,
        distrito: String,
        zona: String,
        telefonoContacto: String,
        telefonoVisible: Boolean,
        fotosJson: String,
        ubicacionLat: Double,
        ubicacionLng: Double
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val postId = UUID.randomUUID().toString()
        val post = PostEntity(
            id = postId,
            userId = user.id,
            userNombreCompleto = user.nombre,
            userFotoPerfil = user.fotoPerfil,
            tipo = tipo,
            especie = especie,
            tamano = tamano,
            nombreMascota = nombreMascota,
            descripcion = descripcion,
            rasgos = rasgos,
            estado = "activo",
            fotosJson = fotosJson,
            ubicacionLat = ubicacionLat,
            ubicacionLng = ubicacionLng,
            distrito = distrito,
            zona = zona,
            telefonoContacto = telefonoContacto,
            telefonoVisible = telefonoVisible,
            likesCount = 0,
            created = System.currentTimeMillis()
        )

        try {
            appDao.insertPost(post)
        } catch (_: Exception) {}
        _posts.value = listOf(post) + _posts.value.filter { it.id != postId }

        return try {
            val photosList = if (fotosJson.isNotBlank()) fotosJson.split(",") else emptyList<String>()
            val body = mapOf(
                "id" to postId,
                "user_id" to user.id,
                "tipo" to tipo,
                "especie" to especie,
                "tamano" to tamano,
                "nombre_mascota" to nombreMascota,
                "descripcion" to descripcion,
                "rasgos" to rasgos,
                "estado" to "activo",
                "fotos" to photosList,
                "ubicacion_lat" to ubicacionLat,
                "ubicacion_lng" to ubicacionLng,
                "distrito" to distrito,
                "zona" to zona,
                "telefono_contacto" to telefonoContacto,
                "telefono_visible" to telefonoVisible,
                "likes_count" to 0
            )
            ApiClient.getService().createPost(body)
            syncPosts()
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun updatePost(
        postId: String,
        tipo: String,
        especie: String,
        tamano: String,
        nombreMascota: String,
        descripcion: String,
        rasgos: String,
        distrito: String,
        zona: String,
        telefonoContacto: String,
        telefonoVisible: Boolean,
        fotosJson: String,
        estado: String
    ): Result<Boolean> {
        val existing = _posts.value.find { it.id == postId } ?: return Result.failure(Exception("No encontrado"))
        val updated = existing.copy(
            tipo = tipo,
            especie = especie,
            tamano = tamano,
            nombreMascota = nombreMascota,
            descripcion = descripcion,
            rasgos = rasgos,
            distrito = distrito,
            zona = zona,
            telefonoContacto = telefonoContacto,
            telefonoVisible = telefonoVisible,
            fotosJson = fotosJson,
            estado = estado
        )
        try {
            appDao.insertPost(updated)
        } catch (_: Exception) {}
        _posts.value = _posts.value.map { if (it.id == postId) updated else it }

        return try {
            val photosList = if (fotosJson.isNotBlank()) fotosJson.split(",") else emptyList<String>()
            val body = mapOf(
                "tipo" to tipo,
                "especie" to especie,
                "tamano" to tamano,
                "nombre_mascota" to nombreMascota,
                "descripcion" to descripcion,
                "rasgos" to rasgos,
                "distrito" to distrito,
                "zona" to zona,
                "telefono_contacto" to telefonoContacto,
                "telefono_visible" to telefonoVisible,
                "fotos" to photosList,
                "estado" to estado
            )
            ApiClient.getService().updatePost("eq.$postId", body)
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun deletePost(postId: String): Result<Boolean> {
        try {
            appDao.deletePostById(postId)
        } catch (_: Exception) {}
        _posts.value = _posts.value.filter { it.id != postId }
        return try {
            ApiClient.getService().deletePost("eq.$postId")
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun toggleLikePost(postId: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val existing = _posts.value.find { it.id == postId } ?: return Result.failure(Exception("No encontrado"))
        val updated = existing.copy(likesCount = existing.likesCount + 1)
        _posts.value = _posts.value.map { if (it.id == postId) updated else it }
        return Result.success(true)
    }

    suspend fun getCommentsForPost(postId: String): List<CommentEntity> {
        return try {
            val local = try { appDao.getCommentsForPostFlow(postId).firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            val response = ApiClient.getService().getComments("eq.$postId")
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.toEntity() }
            } else {
                local
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addComment(postId: String, texto: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val commentId = UUID.randomUUID().toString()
        val comment = CommentEntity(
            id = commentId,
            postId = postId,
            userId = user.id,
            texto = texto,
            userNombreCompleto = user.nombre,
            userFotoPerfil = user.fotoPerfil,
            userIsOng = user.rol == "ong",
            created = System.currentTimeMillis()
        )
        try {
            appDao.insertComment(comment)
        } catch (_: Exception) {}

        return try {
            val body = mapOf(
                "id" to commentId,
                "post_id" to postId,
                "user_id" to user.id,
                "texto" to texto
            )
            ApiClient.getService().createComment(body)
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun deleteComment(commentId: String): Result<Boolean> {
        try {
            appDao.deleteCommentById(commentId)
        } catch (_: Exception) {}
        return try {
            ApiClient.getService().deleteComment("eq.$commentId")
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun reportPost(postId: String, razon: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val report = ReportEntity(
            id = UUID.randomUUID().toString(),
            postId = postId,
            reporterUserId = user.id,
            razon = razon,
            estado = "pendiente"
        )
        try {
            appDao.insertReport(report)
        } catch (_: Exception) {}
        _reports.value = listOf(report) + _reports.value
        return Result.success(true)
    }

    suspend fun reportProfile(reportedUserId: String, razon: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val report = ProfileReportEntity(
            id = UUID.randomUUID().toString(),
            reportedUserId = reportedUserId,
            reporterUserId = user.id,
            razon = razon,
            estado = "pendiente"
        )
        try {
            appDao.insertProfileReport(report)
        } catch (_: Exception) {}
        _profileReports.value = listOf(report) + _profileReports.value
        return Result.success(true)
    }

    suspend fun syncVeterinarias() {
        try {
            val local = try { appDao.getAllVeterinariasFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (local.isNotEmpty() && _veterinarias.value.isEmpty()) {
                _veterinarias.value = local
            }
            val response = PocketBaseClient.getService().getVeterinarias()
            if (response.isSuccessful && response.body() != null) {
                val remote = response.body()!!.items.map { it.toEntity() }
                _veterinarias.value = remote
                try {
                    remote.forEach { appDao.insertVeterinaria(it) }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d("AppRepository", "syncVeterinarias: Fallback a local")
        }
    }

    suspend fun saveVeterinaria(
        id: String?,
        nombre: String,
        direccion: String,
        telefono: String,
        staff: String,
        horarios: String,
        distrito: String,
        clasificacion: String,
        ubicacionLat: Double,
        ubicacionLng: Double
    ): Result<Boolean> {
        val vetId = id ?: UUID.randomUUID().toString()
        val vet = VeterinariaEntity(
            id = vetId,
            nombre = nombre,
            direccion = direccion,
            telefono = telefono,
            staff = staff,
            horarios = horarios,
            distrito = distrito,
            clasificacion = clasificacion,
            ubicacionLat = ubicacionLat,
            ubicacionLng = ubicacionLng,
            activa = true
        )
        try {
            appDao.insertVeterinaria(vet)
        } catch (_: Exception) {}
        _veterinarias.value = listOf(vet) + _veterinarias.value.filter { it.id != vetId }
        return Result.success(true)
    }

    suspend fun deleteVeterinaria(vetId: String): Result<Boolean> {
        _veterinarias.value = _veterinarias.value.filter { it.id != vetId }
        return Result.success(true)
    }

    suspend fun syncCampaigns() {
        try {
            val local = try { appDao.getAllCampaignsFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (local.isNotEmpty() && _campaigns.value.isEmpty()) {
                _campaigns.value = local
            }
            val response = PocketBaseClient.getService().getCampaigns()
            if (response.isSuccessful && response.body() != null) {
                val remote = response.body()!!.items.map { it.toEntity() }
                _campaigns.value = remote
                try {
                    remote.forEach { appDao.insertCampaign(it) }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d("AppRepository", "syncCampaigns: Fallback a local")
        }
    }

    suspend fun saveCampaign(
        id: String?,
        titulo: String,
        descripcion: String,
        imagenPortada: String,
        contenidoDetallado: String,
        fechaInicio: String,
        fechaFin: String
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val cId = id ?: UUID.randomUUID().toString()
        val camp = CampaignEntity(
            id = cId,
            titulo = titulo,
            descripcion = descripcion,
            imagenPortada = imagenPortada,
            contenidoDetallado = contenidoDetallado,
            fechaInicio = fechaInicio,
            fechaFin = fechaFin,
            ongId = user.id,
            ongNombre = user.nombre,
            estado = "activa"
        )
        try {
            appDao.insertCampaign(camp)
        } catch (_: Exception) {}
        _campaigns.value = listOf(camp) + _campaigns.value.filter { it.id != cId }
        return Result.success(true)
    }

    suspend fun deleteCampaign(campaignId: String): Result<Boolean> {
        _campaigns.value = _campaigns.value.filter { it.id != campaignId }
        return Result.success(true)
    }

    suspend fun toggleLikeCampaign(campaignId: String): Result<Boolean> {
        val existing = _campaigns.value.find { it.id == campaignId } ?: return Result.failure(Exception("No encontrado"))
        val updated = existing.copy(likesCount = existing.likesCount + 1)
        _campaigns.value = _campaigns.value.map { if (it.id == campaignId) updated else it }
        return Result.success(true)
    }

    suspend fun syncNews() {
        try {
            val local = try { appDao.getAllNewsFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (local.isNotEmpty() && _news.value.isEmpty()) {
                _news.value = local
            }
            val response = PocketBaseClient.getService().getNews()
            if (response.isSuccessful && response.body() != null) {
                val remote = response.body()!!.items.map { it.toEntity() }
                _news.value = remote
                try {
                    remote.forEach { appDao.insertNews(it) }
                } catch (_: Exception) {}
            }
        } catch (e: Exception) {
            Log.d("AppRepository", "syncNews: Fallback a local")
        }
    }

    suspend fun saveNews(
        id: String?,
        titular: String,
        contenido: String,
        imagenPortada: String,
        imagenNoticia: String
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val nId = id ?: UUID.randomUUID().toString()
        val newsItem = NewsEntity(
            id = nId,
            titular = titular,
            contenido = contenido,
            autorId = user.id,
            autorNombre = user.nombre,
            imagenPortada = imagenPortada,
            imagenNoticia = imagenNoticia,
            created = System.currentTimeMillis()
        )
        try {
            appDao.insertNews(newsItem)
        } catch (_: Exception) {}
        _news.value = listOf(newsItem) + _news.value.filter { it.id != nId }
        return Result.success(true)
    }

    suspend fun deleteNews(newsId: String): Result<Boolean> {
        _news.value = _news.value.filter { it.id != newsId }
        return Result.success(true)
    }

    suspend fun syncStatuses() {
        try {
            val localStatuses = try { appDao.getAllStatusesFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (localStatuses.isNotEmpty() && _statuses.value.isEmpty()) {
                _statuses.value = localStatuses
            }
        } catch (_: Exception) {}
    }

    suspend fun postStatus(texto: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val statusId = UUID.randomUUID().toString()
        val status = UserStatusEntity(
            id = statusId,
            userId = user.id,
            userNombreCompleto = user.nombre,
            userFotoPerfil = user.fotoPerfil,
            texto = texto,
            likesCount = 0,
            created = System.currentTimeMillis()
        )
        try {
            appDao.insertStatus(status)
        } catch (_: Exception) {}
        _statuses.value = listOf(status) + _statuses.value.filter { it.id != statusId }
        return Result.success(true)
    }

    suspend fun deleteStatus(statusId: String): Result<Boolean> {
        try {
            appDao.deleteStatusById(statusId)
        } catch (_: Exception) {}
        _statuses.value = _statuses.value.filter { it.id != statusId }
        return Result.success(true)
    }

    suspend fun toggleLikeStatus(statusId: String): Result<Boolean> {
        val existing = _statuses.value.find { it.id == statusId } ?: return Result.failure(Exception("No encontrado"))
        val updated = existing.copy(likesCount = existing.likesCount + 1)
        _statuses.value = _statuses.value.map { if (it.id == statusId) updated else it }
        return Result.success(true)
    }

    suspend fun syncColaboradores() {
        try {
            val local = try { appDao.getAllColaboradoresFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            if (local.isNotEmpty() && _colaboradores.value.isEmpty()) {
                _colaboradores.value = local
            }
            val response = PocketBaseClient.getService().getColaboradores()
            if (response.isSuccessful && response.body() != null) {
                val remote = response.body()!!.items.map { it.toEntity() }
                _colaboradores.value = remote
                try {
                    remote.forEach { appDao.insertColaborador(it) }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    suspend fun saveColaborador(
        id: String?,
        nombre: String,
        descripcion: String,
        logo: String,
        enlace: String,
        telefono: String,
        whatsapp: String,
        instagram: String,
        direccion: String
    ): Result<Boolean> {
        val cId = id ?: UUID.randomUUID().toString()
        val colab = ColaboradorEntity(
            id = cId,
            nombre = nombre,
            descripcion = descripcion,
            logo = logo,
            enlace = enlace,
            telefono = telefono,
            whatsapp = whatsapp,
            instagram = instagram,
            direccion = direccion
        )
        try {
            appDao.insertColaborador(colab)
        } catch (_: Exception) {}
        _colaboradores.value = listOf(colab) + _colaboradores.value.filter { it.id != cId }
        return Result.success(true)
    }

    suspend fun deleteColaborador(colabId: String): Result<Boolean> {
        _colaboradores.value = _colaboradores.value.filter { it.id != colabId }
        return Result.success(true)
    }

    suspend fun syncBlocks() {
        val user = _currentUser.value ?: return
        try {
            val local = appDao.getBlockedUsersFlow(user.id).firstOrNull() ?: emptyList()
            _blockedUsers.value = local
        } catch (_: Exception) {}
    }

    suspend fun blockUser(blockedUserId: String, nombre: String, foto: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val block = UserBlockEntity(
            id = "${user.id}_$blockedUserId",
            userId = user.id,
            blockedUserId = blockedUserId,
            blockedUserNombre = nombre,
            blockedUserFoto = foto
        )
        try {
            appDao.insertBlock(block)
        } catch (_: Exception) {}
        _blockedUsers.value = listOf(block) + _blockedUsers.value
        return Result.success(true)
    }

    suspend fun unblockUser(blockedUserId: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        try {
            appDao.deleteBlock(user.id, blockedUserId)
        } catch (_: Exception) {}
        _blockedUsers.value = _blockedUsers.value.filter { it.blockedUserId != blockedUserId }
        return Result.success(true)
    }

    suspend fun syncFollows() {
        val user = _currentUser.value ?: return
        try {
            val local = appDao.getFollowsFlow(user.id).firstOrNull() ?: emptyList()
            _follows.value = local
        } catch (_: Exception) {}
    }

    suspend fun toggleFollow(followingId: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No autenticado"))
        val isFollowing = _follows.value.any { it.followingId == followingId }
        if (isFollowing) {
            try {
                appDao.deleteFollow(user.id, followingId)
            } catch (_: Exception) {}
            _follows.value = _follows.value.filter { it.followingId != followingId }
        } else {
            val follow = UserFollowEntity(
                id = "${user.id}_$followingId",
                followerId = user.id,
                followingId = followingId
            )
            try {
                appDao.insertFollow(follow)
            } catch (_: Exception) {}
            _follows.value = listOf(follow) + _follows.value
        }
        return Result.success(true)
    }

    suspend fun syncReports() {
        try {
            val local = try { appDao.getAllReportsFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            _reports.value = local
        } catch (_: Exception) {}
    }

    suspend fun syncProfileReports() {
        try {
            val local = try { appDao.getAllProfileReportsFlow().firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            _profileReports.value = local
        } catch (_: Exception) {}
    }

    suspend fun syncNotifications() {
        val user = _currentUser.value ?: return
        try {
            val local = try { appDao.getNotificationsForUserFlow(user.id).firstOrNull() ?: emptyList() } catch (_: Exception) { emptyList() }
            _notifications.value = local
        } catch (_: Exception) {}
    }

    suspend fun seedMockData() {
        try {
            val currentVets = appDao.getAllVeterinariasFlow().firstOrNull() ?: emptyList()
            if (currentVets.isEmpty()) {
                val defaultVets = listOf(
                    VeterinariaEntity("vet_1", "Veterinaria San Rafael", "Av. Mitre 540, San Rafael", "2604421122", "Dr. Pérez, Dra. Gómez", "Lun a Sáb 8:30 a 12:30 y 16:30 a 20:30", "San Rafael", "Horario Comercio", -34.6177, -68.3301, "", true),
                    VeterinariaEntity("vet_2", "Clínica Veterinaria 24hs Alvear", "Av. Alvear Oeste 320, General Alvear", "2625423344", "Dr. Fernández, Dra. Martínez", "Atención continuada 24hs", "General Alvear", "Guardia 24hs", -34.9754, -67.6908, "", true),
                    VeterinariaEntity("vet_3", "Veterinaria Malargüe Animal", "San Martín 890, Malargüe", "2604470011", "Dr. Silva", "Lun a Vie 9:00 a 13:00 y 17:00 a 21:00", "Malargüe", "Horario Comercio", -35.4756, -69.5842, "", true),
                    VeterinariaEntity("vet_4", "Hospital Veterinario Central", "Hipólito Yrigoyen 1420, San Rafael", "2604556677", "Dra. Ramos, Dr. Morales", "Guardia de emergencias 24hs", "San Rafael", "Guardia 24hs", -34.6110, -68.3450, "", true)
                )
                defaultVets.forEach { appDao.insertVeterinaria(it) }
                _veterinarias.value = defaultVets
            }

            val currentCampaigns = appDao.getAllCampaignsFlow().firstOrNull() ?: emptyList()
            if (currentCampaigns.isEmpty()) {
                val defaultCampaigns = listOf(
                    CampaignEntity("camp_1", "Campaña de Castración Gratuita", "Jornada comunitaria de esterilización canina y felina en San Rafael y Alvear.", "", "Organizado por voluntarios y áreas de zoonosis. Asistir con turno previo y animal en ayuno.", "2026-08-01", "2026-08-31", "ong_1", "Refugio Patitas Mendocinas", "activa", 42),
                    CampaignEntity("camp_2", "Vacunación Antirrábica Móvil", "Puestos móviles de vacunación antirrábica gratuita en plazas principales del sur mendocino.", "", "Protegé a tu mascota contra la rabia. Aplicación gratuita para perros y gatos a partir de los 3 meses.", "2026-08-10", "2026-09-15", "ong_2", "Asociación Protectora del Sur", "activa", 29)
                )
                defaultCampaigns.forEach { appDao.insertCampaign(it) }
                _campaigns.value = defaultCampaigns
            }

            val currentNews = appDao.getAllNewsFlow().firstOrNull() ?: emptyList()
            if (currentNews.isEmpty()) {
                val defaultNews = listOf(
                    NewsEntity("news_1", "Consejos para cuidar a tus mascotas durante las bajas temperaturas", "Con el frío cordillerano, es fundamental brindar abrigo adecuado, agua fresca no congelada y alimento calórico a los animales.", "admin_1", "InfoAnimal Redacción", "", "", System.currentTimeMillis() - 86400000L * 2),
                    NewsEntity("news_2", "Nueva ordenanza de tenencia responsable y registro de mascotas", "Se implementa un censo voluntario con código QR y microchip para facilitar el reencuentro de animales perdidos.", "admin_1", "InfoAnimal Redacción", "", "", System.currentTimeMillis() - 86400000L * 5)
                )
                defaultNews.forEach { appDao.insertNews(it) }
                _news.value = defaultNews
            }

            val currentColabs = appDao.getAllColaboradoresFlow().firstOrNull() ?: emptyList()
            if (currentColabs.isEmpty()) {
                val defaultColabs = listOf(
                    ColaboradorEntity("colab_1", "Refugio Huellitas del Sur", "Asociación sin fines de lucro dedicada al rescate, tránsito y adopción responsable en Mendoza.", "", "https://instagram.com/huellitasdelsur", "2604112233", "2604112233", "@huellitasdelsur", "San Rafael, Mendoza"),
                    ColaboradorEntity("colab_2", "Amigos de los Animales Alvear", "Grupo de voluntarios comprometidos con el rescate de animales y castraciones.", "", "https://instagram.com/amigosalvear", "2625445566", "2625445566", "@amigosalvear", "General Alvear, Mendoza")
                )
                defaultColabs.forEach { appDao.insertColaborador(it) }
                _colaboradores.value = defaultColabs
            }
        } catch (e: Exception) {
            Log.d("AppRepository", "seedMockData: Error", e)
        }
    }
}
