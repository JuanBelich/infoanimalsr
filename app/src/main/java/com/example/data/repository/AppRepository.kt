package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.*
import com.example.data.local.AppDatabase
import com.example.data.network.ApiClient
import com.example.data.network.PocketBaseClient
import com.example.data.network.toEntity
import com.example.data.network.parsePbDate
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
                    val userEntity = UserEntity(
                        id = user.id,
                        email = user.email ?: "",
                        nombre = user.nombre ?: "",
                        telefono = user.telefono ?: "",
                        zonaPrincipal = user.zonaPrincipal ?: "San Rafael",
                        rol = user.rol ?: "user",
                        fotoPerfil = user.fotoPerfil ?: "avatar_placeholder",
                        fotoPortada = if (!user.fotoPortada.isNullOrBlank()) user.fotoPortada else "gradient_blue",
                        biografia = user.biografia ?: ""
                    )
                    _currentUser.value = userEntity
                    Log.d("AppRepository", "initSession: Perfil recuperado de Supabase para $userId")
                    syncAll()
                } else {
                    Log.e("AppRepository", "initSession: Falló la recuperación del perfil desde Supabase. Código: ${response.code()}")
                    context.dataStore.edit { it.remove(KEY_USER_ID) }
                    syncAll() // Sincronizamos datos públicos para que la app no quede vacía
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "initSession: Error al conectarse a Supabase", e)
                context.dataStore.edit { it.remove(KEY_USER_ID) }
                syncAll() // Sincronizamos datos públicos igualmente
            }
        } ?: run {
            Log.d("AppRepository", "initSession: No hay sesión activa guardada. Iniciando sincronización de datos públicos...")
            syncAll() // Sincronizamos datos públicos (Posts, Veterinarias, Campañas, Noticias) para usuario Invitado
        }
    }

    suspend fun login(email: String, password: String): Boolean {
        try {
            val response = PocketBaseClient.getService().login(
                mapOf("identity" to email, "password" to password)
            )
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                PocketBaseClient.saveAuth(context, auth.token, auth.record)
                
                val userEntity = auth.record.toEntity()
                _currentUser.value = userEntity
                context.dataStore.edit { prefs ->
                    prefs[KEY_USER_ID] = userEntity.id
                }
                Log.d("AppRepository", "login: Inicio de sesión exitoso. Iniciando sincronización en segundo plano...")
                // Iniciar sincronización secundaria en segundo plano para no demorar la navegación inmediata
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        syncAll()
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Error en syncAll en segundo plano tras login", e)
                    }
                }
                return true
            } else {
                val errStr = response.errorBody()?.string() ?: "Error desconocido"
                Log.e("AppRepository", "login: Error de autenticación: $errStr")
                throw Exception(errStr)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "login: Excepción al iniciar sesión", e)
            throw e
        }
    }

    suspend fun signup(email: String, name: String, phone: String, password: String): UserEntity {
        val assignedRol = if (email.endsWith("@infoanimals.com", ignoreCase = true)) {
            when {
                email.contains("superadmin", ignoreCase = true) -> "super_admin"
                email.contains("admin", ignoreCase = true) -> "admin"
                email.contains("ong", ignoreCase = true) -> "ong"
                else -> "user"
            }
        } else {
            "user"
        }
        val signupData = mapOf(
            "email" to email,
            "password" to password,
            "passwordConfirm" to password,
            "nombre" to name,
            "telefono" to phone,
            "rol" to assignedRol,
            "bloqueado" to false
        )
        
        try {
            val response = PocketBaseClient.getService().signup(signupData)
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                val userEntity = user.toEntity()
                Log.d("AppRepository", "signup: Registro exitoso en Supabase")
                try {
                    login(email, password)
                } catch (loginEx: Exception) {
                    Log.e("AppRepository", "signup: El inicio de sesión automático falló pero la cuenta se creó correctamente", loginEx)
                }
                return userEntity
            } else {
                val errorMsg = response.errorBody()?.string() ?: ""
                Log.e("AppRepository", "signup: Falló el registro: $errorMsg")
                throw Exception("Error de registro en Supabase: $errorMsg")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "signup: Excepción en el registro", e)
            throw e
        }
    }

    suspend fun logout() {
        Log.d("AppRepository", "logout: Cerrando sesión y limpiando datos locales")
        PocketBaseClient.clearAuth(context)
        _currentUser.value = null
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
        }
        _posts.value = emptyList()
        _veterinarias.value = emptyList()
        _campaigns.value = emptyList()
        _news.value = emptyList()
        _statuses.value = emptyList()
        _colaboradores.value = emptyList()
        _blockedUsers.value = emptyList()
        _notifications.value = emptyList()
        _reports.value = emptyList()
        _profileReports.value = emptyList()
        try {
            syncPosts()
        } catch (e: Exception) {
            Log.e("AppRepository", "Error re-syncing public posts post-logout", e)
        }
    }

    suspend fun requestPasswordReset(email: String): Boolean {
        return try {
            val response = PocketBaseClient.getService().requestPasswordReset(mapOf("email" to email))
            if (response.isSuccessful) {
                Log.d("AppRepository", "requestPasswordReset: Solicitud enviada exitosamente")
                true
            } else {
                Log.e("AppRepository", "requestPasswordReset: Error: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "requestPasswordReset: Excepción", e)
            false
        }
    }

    suspend fun updateProfile(name: String, bio: String, phone: String, zone: String) {
        val current = _currentUser.value ?: return
        val updated = current.copy(
            nombre = name,
            biografia = bio,
            telefono = phone,
            zonaPrincipal = zone
        )
        _currentUser.value = updated
        try {
            appDao.insertUser(updated)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error updating user in Room DB", e)
        }

        val token = PocketBaseClient.token
        if (!token.isNullOrBlank()) {
            try {
                val response = PocketBaseClient.getService().updateProfile(
                    token = "Bearer $token",
                    userId = current.id,
                    body = mapOf(
                        "nombre" to name,
                        "biografia" to bio,
                        "telefono" to phone,
                        "zona_principal" to zone
                    )
                )
                if (response.isSuccessful) {
                    Log.d("AppRepository", "updateProfile: Perfil actualizado correctamente en Supabase")
                    try { syncPosts() } catch (_: Exception) {}
                } else {
                    Log.e("AppRepository", "updateProfile: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "updateProfile: Excepción", e)
            }
        }
    }

    suspend fun uploadAndUpdateAvatar(uri: android.net.Uri): Result<String> {
        val user = _currentUser.value ?: return Result.failure(Exception("Debe iniciar sesión para cambiar la foto de perfil."))
        val uploadResult = ApiClient.uploadProfileImage(context, user.id, uri, isCover = false)
        if (uploadResult.isSuccess) {
            val publicUrl = uploadResult.getOrThrow()
            updateProfilePhotos(avatarUrl = publicUrl, coverUrl = null)
            return Result.success(publicUrl)
        } else {
            return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error al subir foto de perfil"))
        }
    }

    suspend fun uploadAndUpdateCover(uri: android.net.Uri): Result<String> {
        val user = _currentUser.value ?: return Result.failure(Exception("Debe iniciar sesión para cambiar la foto de portada."))
        val uploadResult = ApiClient.uploadProfileImage(context, user.id, uri, isCover = true)
        if (uploadResult.isSuccess) {
            val publicUrl = uploadResult.getOrThrow()
            updateProfilePhotos(avatarUrl = null, coverUrl = publicUrl)
            return Result.success(publicUrl)
        } else {
            return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Error al subir foto de portada"))
        }
    }

    suspend fun updateProfilePhotos(avatarUrl: String?, coverUrl: String?) {
        val current = _currentUser.value ?: return
        var updated = current
        val updateMap = mutableMapOf<String, Any>()
        if (avatarUrl != null) {
            updated = updated.copy(fotoPerfil = avatarUrl)
            updateMap["foto_perfil"] = avatarUrl
        }
        if (coverUrl != null) {
            updated = updated.copy(fotoPortada = coverUrl)
            updateMap["foto_portada"] = coverUrl
        }
        _currentUser.value = updated
        try {
            appDao.insertUser(updated)
        } catch (e: Exception) {
            Log.e("AppRepository", "Error updating user in Room DB", e)
        }

        val token = PocketBaseClient.token
        if (!token.isNullOrBlank() && updateMap.isNotEmpty()) {
            try {
                val response = PocketBaseClient.getService().updateProfile(
                    token = "Bearer $token",
                    userId = current.id,
                    body = updateMap
                )
                if (response.isSuccessful) {
                    Log.d("AppRepository", "updateProfilePhotos: Fotos actualizadas en Supabase")
                    try { syncPosts() } catch (_: Exception) {}
                } else {
                    Log.e("AppRepository", "updateProfilePhotos: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "updateProfilePhotos: Excepción", e)
            }
        }
    }

    suspend fun registerSystemAccounts() {
        val accountTypes = listOf(
            Triple("admin@infoanimals.com", "Admin Central", "admin"),
            Triple("ong@infoanimals.com", "Refugio Patitas", "ong"),
            Triple("user@infoanimals.com", "Vecino Alvear", "user"),
            Triple("superadmin@infoanimals.com", "Super Admin S.R.", "super_admin"),
            Triple("admin_sr@infoanimals.com", "Admin San Rafael", "admin"),
            Triple("ong_sr@infoanimals.com", "Refugio Patitas SR", "ong"),
            Triple("user_sr@infoanimals.com", "Vecino San Rafael", "user"),
            Triple("superadmin_sr@infoanimals.com", "Super Admin San Rafael", "super_admin")
        )
        for ((email, name, role) in accountTypes) {
            try {
                val signupData = mapOf(
                    "email" to email,
                    "password" to "Admin123!",
                    "passwordConfirm" to "Admin123!",
                    "nombre" to name,
                    "telefono" to "2604123456",
                    "rol" to role,
                    "bloqueado" to false
                )
                val response = PocketBaseClient.getService().signup(signupData)
                if (response.isSuccessful) {
                    Log.d("AppRepository", "System account auto-registered: $email")
                }
            } catch (e: Exception) {
                Log.e("AppRepository", "System account auto-registration failed for $email", e)
            }
        }
    }

    suspend fun syncAll() {
        Log.d("AppRepository", "syncAll: Iniciando sincronización 100% ONLINE con Supabase")
        if (com.example.BuildConfig.ENABLE_TEST_ROLE_MAPPING) {
            try {
                registerSystemAccounts()
            } catch (e: Exception) {
                Log.e("AppRepository", "System accounts auto-registration failed", e)
            }
        }
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
        } catch (e: Exception) {
            Log.e("AppRepository", "Complete synchronization failed", e)
        }
    }

    suspend fun syncPosts() {
        val activeToken = ApiClient.token
        if (activeToken.isNullOrBlank()) {
            Log.d("AppRepository", "syncPosts: Sincronizando publicaciones de forma pública (usuario invitado / sin token activo)")
        }

        try {
            // Bypass directo a apiService de Supabase para obtener las publicaciones 100% online
            val response = PocketBaseClient.getService().getPosts(page = 1, perPage = 100)
            if (response.isSuccessful && response.body() != null) {
                val postEntities = response.body()!!.items.map { it.toEntity() }
                
                // Actualiza la UI de inmediato de forma online
                _posts.value = postEntities
                Log.d("AppRepository", "syncPosts: Sincronizadas online ${_posts.value.size} publicaciones exitosamente")

                // Sincronización secundaria a Room en segundo plano para persistencia local offline
                try {
                    val appDao = AppDatabase.getDatabase(context).appDao()
                    postEntities.forEach { appDao.insertPost(it) }
                    Log.d("AppRepository", "syncPosts: Sincronización local en Room realizada con éxito")
                } catch (dbEx: Exception) {
                    Log.e("AppRepository", "syncPosts: Error al sincronizar con Room", dbEx)
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error de red al obtener publicaciones"
                Log.e("CRITICAL_SUPABASE_ERROR", "syncPosts: Error al obtener publicaciones de red: $errorMsg")
                // Limpiamos las publicaciones para que no queden datos viejos en la UI
                _posts.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e("CRITICAL_SUPABASE_ERROR", "syncPosts: Excepción en la red al obtener publicaciones: ${e.message}", e)
            // Limpiamos las publicaciones para que no queden datos viejos en la UI
            _posts.value = emptyList()
        }
    }

    suspend fun syncVeterinarias() {
        try {
            val response = PocketBaseClient.getService().getVeterinarias()
            if (response.isSuccessful && response.body() != null) {
                _veterinarias.value = response.body()!!.items.map { it.toEntity() }
                Log.d("AppRepository", "syncVeterinarias: Sincronizadas ${_veterinarias.value.size} veterinarias")
            } else {
                Log.e("AppRepository", "syncVeterinarias: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncVeterinarias: Excepción en la red", e)
        }
    }

    suspend fun syncCampaigns() {
        try {
            val response = PocketBaseClient.getService().getCampaigns()
            if (response.isSuccessful && response.body() != null) {
                _campaigns.value = response.body()!!.items.map { it.toEntity() }
                Log.d("AppRepository", "syncCampaigns: Sincronizadas ${_campaigns.value.size} campañas")
            } else {
                Log.e("AppRepository", "syncCampaigns: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncCampaigns: Excepción en la red", e)
        }
    }

    suspend fun syncNews() {
        try {
            val response = PocketBaseClient.getService().getNews()
            if (response.isSuccessful && response.body() != null) {
                _news.value = response.body()!!.items.map { it.toEntity() }
                Log.d("AppRepository", "syncNews: Sincronizadas ${_news.value.size} noticias")
            } else {
                Log.e("AppRepository", "syncNews: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncNews: Excepción en la red", e)
        }
    }

    suspend fun syncStatuses() {
        try {
            // Cargar primero datos locales si la lista está vacía
            try {
                val localStatuses = appDao.getStatusesForUserFlow(_currentUser.value?.id ?: "").firstOrNull() ?: emptyList()
                if (localStatuses.isNotEmpty() && _statuses.value.isEmpty()) {
                    _statuses.value = localStatuses
                }
            } catch (dbEx: Exception) {
                Log.e("AppRepository", "syncStatuses: Error al leer Room local", dbEx)
            }

            val response = PocketBaseClient.getService().getUserStatuses()
            if (response.isSuccessful && response.body() != null) {
                val remoteStatuses = response.body()!!.items.map { pbStatus ->
                    val matchedUser = _posts.value.find { it.userId == pbStatus.userId }
                    val nombre = matchedUser?.userNombreCompleto ?: if (pbStatus.userId == _currentUser.value?.id) _currentUser.value?.nombre ?: "Vecino Colaborador" else "Vecino Colaborador"
                    val foto = matchedUser?.userFotoPerfil ?: if (pbStatus.userId == _currentUser.value?.id) _currentUser.value?.fotoPerfil ?: "avatar_placeholder" else "avatar_placeholder"
                    UserStatusEntity(
                        id = pbStatus.id,
                        userId = pbStatus.userId,
                        userNombreCompleto = nombre,
                        userFotoPerfil = foto,
                        texto = pbStatus.texto,
                        likesCount = pbStatus.likesCount ?: 0,
                        created = parsePbDate(pbStatus.created)
                    )
                }

                // Guardar en Room local
                try {
                    remoteStatuses.forEach { appDao.insertStatus(it) }
                } catch (dbEx: Exception) {
                    Log.e("AppRepository", "syncStatuses: Error al guardar en Room", dbEx)
                }

                // Preservar estados recientes que tal vez aún no bajaron de la red
                val remoteIds = remoteStatuses.map { it.id }.toSet()
                val localOnly = _statuses.value.filter { it.id !in remoteIds }
                _statuses.value = localOnly + remoteStatuses
                Log.d("AppRepository", "syncStatuses: Sincronizados ${_statuses.value.size} estados")
            } else {
                Log.e("AppRepository", "syncStatuses: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncStatuses: Excepción en la red", e)
        }
    }

    suspend fun postStatus(texto: String): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No hay usuario autenticado"))
        
        var token = PocketBaseClient.token
        if (token.isNullOrBlank()) {
            ApiClient.init(context)
            token = PocketBaseClient.token
        }
        
        val statusId = "ust_" + java.util.UUID.randomUUID().toString().take(8)
        val now = System.currentTimeMillis()
        val newStatus = UserStatusEntity(
            id = statusId,
            userId = user.id,
            userNombreCompleto = user.nombre,
            userFotoPerfil = if (user.fotoPerfil.isNotBlank()) user.fotoPerfil else "avatar_placeholder",
            texto = texto,
            likesCount = 0,
            created = now
        )

        // 1. Guardar localmente en Room y actualizar Flow inmediatamente para reactividad instantánea
        try {
            appDao.insertStatus(newStatus)
        } catch (dbEx: Exception) {
            Log.e("AppRepository", "postStatus: Error al insertar en Room", dbEx)
        }
        _statuses.value = listOf(newStatus) + _statuses.value.filter { it.id != statusId }

        // 2. Enviar a la base de datos remota
        return try {
            val body = mapOf(
                "id" to statusId,
                "user_id" to user.id,
                "texto" to texto
            )
            
            val response = if (!token.isNullOrBlank()) {
                PocketBaseClient.getService().createUserStatus("Bearer $token", body)
            } else {
                ApiClient.getService().createUserStatus(body)
            }

            if (response.isSuccessful) {
                Log.d("AppRepository", "postStatus: Estado publicado exitosamente en Supabase")
            } else {
                Log.w("AppRepository", "postStatus: Respuesta de red código ${response.code()}")
            }

            syncStatuses()
            Result.success(true)
        } catch (e: Exception) {
            Log.e("AppRepository", "postStatus: Excepción de red, mantenido en local", e)
            Result.success(true)
        }
    }

    suspend fun getCommentsForPost(postId: String): List<CommentEntity> {
        return try {
            val response = PocketBaseClient.getService().getComments(filter = "post_id='$postId'")
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.items.map { it.toEntity() }
            } else {
                Log.e("AppRepository", "getCommentsForPost: Error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "getCommentsForPost: Excepción en la red", e)
            emptyList()
        }
    }

    fun getAllColaboradoresFlow(): Flow<List<ColaboradorEntity>> = colaboradores

    suspend fun syncColaboradores() {
        try {
            val response = PocketBaseClient.getService().getColaboradores()
            if (response.isSuccessful && response.body() != null) {
                _colaboradores.value = response.body()!!.items.map { it.toEntity() }
                Log.d("AppRepository", "syncColaboradores: Sincronizados ${_colaboradores.value.size} colaboradores")
            } else {
                Log.e("AppRepository", "syncColaboradores: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncColaboradores: Excepción en la red", e)
        }
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
    ): Boolean {
        val token = PocketBaseClient.token ?: return false
        return try {
            val body: Map<String, Any> = mapOf(
                "nombre" to nombre,
                "descripcion" to descripcion,
                "logo" to logo,
                "enlace" to enlace,
                "telefono" to telefono,
                "whatsapp" to whatsapp,
                "instagram" to instagram,
                "direccion" to direccion
            )
            val service = PocketBaseClient.getService()
            val response = if (id == null) {
                service.createColaborador("Bearer $token", body)
            } else {
                service.updateColaborador("Bearer $token", id, body)
            }
            if (response.isSuccessful) {
                Log.d("AppRepository", "saveColaborador: Guardado correctamente en Supabase")
                syncColaboradores()
                true
            } else {
                Log.e("AppRepository", "saveColaborador: Error: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "saveColaborador: Excepción", e)
            false
        }
    }

    suspend fun savePost(
        id: String?,
        tipo: String,
        especie: String,
        tamano: String,
        nombre: String,
        descripcion: String,
        rasgos: String,
        distrito: String,
        zona: String,
        fotos: String,
        lat: Double,
        lng: Double,
        telefonoVisible: Boolean,
        telefonoContacto: String
    ): Result<Boolean> {
        val user = _currentUser.value ?: return Result.failure(Exception("No hay sesión de usuario activa"))
        
        // Verificación estricta del token, con intento de reinicialización si es nulo o vacío
        var token = PocketBaseClient.token
        if (token.isNullOrBlank()) {
            Log.w("AppRepository", "savePost: El token es nulo o vacío. Forzando reinicialización de ApiClient...")
            ApiClient.init(context)
            token = PocketBaseClient.token
        }
        
        if (token.isNullOrBlank()) {
            val errMsg = "Token de sesión nulo o vacío después de forzar reinicialización. Por favor re-inicie sesión."
            Log.e("CRITICAL_SUPABASE_ERROR", "Error al guardar: $errMsg")
            return Result.failure(Exception(errMsg))
        }
        
        return try {
            val fotosList = if (fotos.isNotEmpty()) fotos.split(",").filter { it.isNotEmpty() } else emptyList()
            val finalId = id ?: ("post_" + java.util.UUID.randomUUID().toString().take(8))
            val isNew = id == null || _posts.value.none { it.id == finalId }
            val body: Map<String, Any> = mapOf(
                "id" to finalId,
                "user_id" to user.id,
                "tipo" to tipo,
                "especie" to especie,
                "tamano" to tamano,
                "nombre_mascota" to nombre,
                "descripcion" to descripcion,
                "rasgos" to rasgos,
                "distrito" to distrito,
                "zona" to zona,
                "fotos" to fotosList,
                "telefono_contacto" to telefonoContacto,
                "telefono_visible" to telefonoVisible,
                "estado" to "activo"
            )
            val service = PocketBaseClient.getService()
            val response = if (isNew) {
                service.createPostJson("Bearer $token", body)
            } else {
                service.updatePost("Bearer $token", finalId, body)
            }
            if (response.isSuccessful && response.body() != null) {
                Log.d("AppRepository", "savePost: Publicación guardada exitosamente en Supabase")
                val pbPost = response.body()!!
                val postEntity = pbPost.toEntity()
                
                // Room local save dependending on confirmation from Supabase
                try {
                    val appDao = AppDatabase.getDatabase(context).appDao()
                    appDao.insertPost(postEntity)
                    Log.d("AppRepository", "savePost: Publicación guardada localmente en Room con éxito")
                } catch (dbEx: Exception) {
                    Log.e("AppRepository", "savePost: Error al guardar en base de datos Room", dbEx)
                }

                syncPosts()
                Result.success(true)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Error de red al guardar la publicación"
                Log.e("CRITICAL_SUPABASE_ERROR", "Error al guardar (Código ${response.code()}): $errorMsg")
                Result.failure(Exception("Error en servidor Supabase (Código ${response.code()}): $errorMsg"))
            }
        } catch (e: Exception) {
            Log.e("CRITICAL_SUPABASE_ERROR", "Error al guardar: " + e.message, e)
            Result.failure(e)
        }
    }

    suspend fun deletePostLocally(postId: String) {
        try {
            val appDao = AppDatabase.getDatabase(context).appDao()
            appDao.deletePostById(postId)
        } catch (e: Exception) {
            Log.e("AppRepository", "deletePostLocally: Error en Room", e)
        }
        _posts.value = _posts.value.filter { it.id != postId }
    }

    suspend fun updatePostEstadoLocally(postId: String, nuevoEstado: String) {
        val currentList = _posts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == postId }
        if (index != -1) {
            val updated = currentList[index].copy(estado = nuevoEstado)
            try {
                val appDao = AppDatabase.getDatabase(context).appDao()
                appDao.insertPost(updated)
            } catch (e: Exception) {
                Log.e("AppRepository", "updatePostEstadoLocally: Error en Room", e)
            }
            currentList[index] = updated
            _posts.value = currentList
        }
    }

    suspend fun deleteColaborador(id: String): Boolean {
        val token = PocketBaseClient.token ?: return false
        return try {
            val response = PocketBaseClient.getService().deleteColaborador("Bearer $token", id)
            if (response.isSuccessful) {
                Log.d("AppRepository", "deleteColaborador: Eliminado exitosamente de Supabase")
                syncColaboradores()
                true
            } else {
                Log.e("AppRepository", "deleteColaborador: Error: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "deleteColaborador: Excepción", e)
            false
        }
    }

    fun getBlocksForUserFlow(userId: String): Flow<List<UserBlockEntity>> = blockedUsers

    suspend fun syncBlocks() {
        val user = _currentUser.value ?: return
        try {
            val response = PocketBaseClient.getService().getBlocks(filter = "user_id='${user.id}'")
            if (response.isSuccessful && response.body() != null) {
                _blockedUsers.value = response.body()!!.items.map { it.toEntity() }
                Log.d("AppRepository", "syncBlocks: Sincronizados ${_blockedUsers.value.size} bloqueos")
            } else {
                Log.e("AppRepository", "syncBlocks: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncBlocks: Excepción", e)
        }
    }

    suspend fun blockUser(blockedUserId: String, blockedNombre: String, blockedFoto: String): Boolean {
        val user = _currentUser.value ?: return false
        val token = PocketBaseClient.token ?: return false
        
        return try {
            val response = PocketBaseClient.getService().createBlock(
                token = "Bearer $token",
                body = mapOf(
                    "user_id" to user.id,
                    "blocked_user_id" to blockedUserId,
                    "blocked_user_nombre" to blockedNombre,
                    "blocked_user_foto" to blockedFoto
                )
            )
            if (response.isSuccessful) {
                Log.d("AppRepository", "blockUser: Bloqueo exitoso guardado en Supabase")
                syncBlocks()
                true
            } else {
                Log.e("AppRepository", "blockUser: Error: ${response.errorBody()?.string()}")
                false
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "blockUser: Excepción", e)
            false
        }
    }

    suspend fun unblockUser(blockedUserId: String): Boolean {
        val user = _currentUser.value ?: return false
        val token = PocketBaseClient.token ?: return false
        
        return try {
            val service = PocketBaseClient.getService()
            val response = service.getBlocks("user_id='${user.id}' && blocked_user_id='$blockedUserId'")
            if (response.isSuccessful && response.body() != null && response.body()!!.items.isNotEmpty()) {
                val pbId = response.body()!!.items[0].id
                val deleteResponse = service.deleteBlock("Bearer $token", pbId)
                if (deleteResponse.isSuccessful) {
                    Log.d("AppRepository", "unblockUser: Desbloqueo exitoso en Supabase")
                    syncBlocks()
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("AppRepository", "unblockUser: Excepción", e)
            false
        }
    }

    suspend fun syncFollows() {
        val user = _currentUser.value ?: return
        try {
            val localFollows = try { appDao.getFollowsForUser(user.id) } catch (e: Exception) { emptyList() }
            if (localFollows.isNotEmpty() && _follows.value.isEmpty()) {
                _follows.value = localFollows
            }

            val response = PocketBaseClient.getService().getFollows(filter = "follower_id='${user.id}'")
            if (response.isSuccessful && response.body() != null) {
                val remoteFollows = response.body()!!.items.map { it.toEntity() }
                _follows.value = remoteFollows
                try {
                    remoteFollows.forEach { appDao.insertFollow(it) }
                } catch (dbEx: Exception) {
                    Log.e("AppRepository", "syncFollows: Error guardando en Room", dbEx)
                }
                Log.d("AppRepository", "syncFollows: Sincronizados ${_follows.value.size} seguidos")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncFollows: Excepción", e)
        }
    }

    suspend fun followUser(targetUserId: String): Boolean {
        val user = _currentUser.value ?: return false
        if (user.id == targetUserId) return false

        val id = "${user.id}_${targetUserId}"
        val newFollow = UserFollowEntity(id = id, followerId = user.id, followingId = targetUserId)

        try {
            appDao.insertFollow(newFollow)
        } catch (dbEx: Exception) {
            Log.e("AppRepository", "followUser: Error guardando en Room", dbEx)
        }
        _follows.value = _follows.value.filter { it.followingId != targetUserId } + newFollow

        return try {
            var token = PocketBaseClient.token
            if (token.isNullOrBlank()) {
                ApiClient.init(context)
                token = PocketBaseClient.token
            }
            val response = PocketBaseClient.getService().createFollow(
                token = if (!token.isNullOrBlank()) "Bearer $token" else "",
                body = mapOf(
                    "id" to id,
                    "follower_id" to user.id,
                    "following_id" to targetUserId
                )
            )
            if (response.isSuccessful) {
                Log.d("AppRepository", "followUser: Guardado en backend")
            }
            syncFollows()
            true
        } catch (e: Exception) {
            Log.e("AppRepository", "followUser: Excepción de red", e)
            true
        }
    }

    suspend fun unfollowUser(targetUserId: String): Boolean {
        val user = _currentUser.value ?: return false

        try {
            appDao.deleteFollow(user.id, targetUserId)
        } catch (dbEx: Exception) {
            Log.e("AppRepository", "unfollowUser: Error en Room", dbEx)
        }
        _follows.value = _follows.value.filter { it.followingId != targetUserId }

        return try {
            var token = PocketBaseClient.token
            if (token.isNullOrBlank()) {
                ApiClient.init(context)
                token = PocketBaseClient.token
            }
            val id = "${user.id}_${targetUserId}"
            PocketBaseClient.getService().deleteFollow(
                token = if (!token.isNullOrBlank()) "Bearer $token" else "",
                id = id
            )
            syncFollows()
            true
        } catch (e: Exception) {
            Log.e("AppRepository", "unfollowUser: Excepción de red", e)
            true
        }
    }

    suspend fun logAdminAction(accion: String, targetId: String, detalles: String) {
        val user = _currentUser.value ?: return
        val token = PocketBaseClient.token ?: return
        try {
            val response = PocketBaseClient.getService().createAdminAction(
                token = "Bearer $token",
                body = mapOf(
                    "user_id" to user.id,
                    "accion" to accion,
                    "target_id" to targetId,
                    "detalles" to detalles
                )
            )
            if (response.isSuccessful) {
                Log.d("AppRepository", "logAdminAction: Acción de administración registrada en Supabase")
            } else {
                Log.e("AppRepository", "logAdminAction: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "logAdminAction: Excepción", e)
        }
    }

    suspend fun syncReports() {
        try {
            val response = ApiClient.getService().getReports()
            if (response.isSuccessful && response.body() != null) {
                _reports.value = response.body()!!.map { pbReport ->
                    ReportEntity(
                        id = pbReport.id,
                        postId = pbReport.postId,
                        reporterUserId = pbReport.reporterUserId,
                        razon = pbReport.razon,
                        estado = pbReport.estado ?: "pendiente"
                    )
                }
                Log.d("AppRepository", "syncReports: Sincronizados ${_reports.value.size} reportes de publicaciones")
            } else {
                Log.e("AppRepository", "syncReports: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncReports: Excepción", e)
        }
    }

    suspend fun syncProfileReports() {
        try {
            val response = ApiClient.getService().getProfileReports()
            if (response.isSuccessful && response.body() != null) {
                _profileReports.value = response.body()!!.map { pbProfileReport ->
                    ProfileReportEntity(
                        id = pbProfileReport.id,
                        reportedUserId = pbProfileReport.reportedUserId,
                        reporterUserId = pbProfileReport.reporterUserId,
                        razon = pbProfileReport.razon,
                        estado = pbProfileReport.estado ?: "pendiente"
                    )
                }
                Log.d("AppRepository", "syncProfileReports: Sincronizados ${_profileReports.value.size} reportes de perfiles")
            } else {
                Log.e("AppRepository", "syncProfileReports: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncProfileReports: Excepción", e)
        }
    }

    suspend fun syncNotifications() {
        val user = _currentUser.value ?: return
        try {
            val response = ApiClient.getService().getNotifications(userIdQuery = "eq.${user.id}")
            if (response.isSuccessful && response.body() != null) {
                _notifications.value = response.body()!!.map { pbNotif ->
                    NotificationEntity(
                        id = pbNotif.id,
                        userId = pbNotif.userId,
                        tipo = "comentario",
                        postId = "",
                        commentId = "",
                        fromUserId = "",
                        fromUserNombre = "",
                        mensaje = pbNotif.mensaje,
                        leida = pbNotif.leida ?: false
                    )
                }
                Log.d("AppRepository", "syncNotifications: Sincronizadas ${_notifications.value.size} notificaciones")
            } else {
                Log.e("AppRepository", "syncNotifications: Error: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "syncNotifications: Excepción", e)
        }
    }

    suspend fun seedMockData() {
        // En un esquema 100% online y sin base de datos local Room, ya no sembramos datos de mock localmente.
        // Los datos se administran directamente en el backend de Supabase.
    }
}
