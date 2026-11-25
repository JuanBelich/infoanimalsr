package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.local.ServiceLocator
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.data.network.ApiClient
import com.example.data.network.PocketBaseClient
import com.example.data.network.ErrorMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {
    init {
        ServiceLocator.init(application.applicationContext)
    }

    private val repository: AppRepository = ServiceLocator.repository!!

    val currentUser: StateFlow<UserEntity?> = repository.currentUser

    // --- State flows from Repository ---
    val colaboradores: StateFlow<List<ColaboradorEntity>> = repository.colaboradores

    val blockedUsers: StateFlow<List<UserBlockEntity>> = repository.blockedUsers

    val allPosts: StateFlow<List<PostEntity>> = combine(
        repository.posts,
        blockedUsers
    ) { posts, blocks ->
        val blockedIds = blocks.map { it.blockedUserId }.toSet()
        posts.filter { it.userId !in blockedIds }.sortedByDescending { it.created }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val veterinarias: StateFlow<List<VeterinariaEntity>> = repository.veterinarias

    val campaigns: StateFlow<List<CampaignEntity>> = repository.campaigns

    val news: StateFlow<List<NewsEntity>> = repository.news

    val statuses: StateFlow<List<UserStatusEntity>> = combine(
        repository.statuses,
        blockedUsers
    ) { statusList, blocks ->
        val blockedIds = blocks.map { it.blockedUserId }.toSet()
        statusList.filter { it.userId !in blockedIds }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = repository.reports

    val profileReports: StateFlow<List<ProfileReportEntity>> = repository.profileReports

    val follows: StateFlow<List<UserFollowEntity>> = repository.follows

    val followingUserIds: StateFlow<Set<String>> = repository.follows.map { list ->
        list.map { it.followingId }.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // --- Stats flows for Admin Panel calculated Reactively in Memory ---
    val totalPostsCount = repository.posts.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activePostsCount = repository.posts.map { list -> list.count { it.estado == "activo" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val resolvedPostsCount = repository.posts.map { list -> list.count { it.estado == "resuelta" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalUsersCount = repository.posts.map { list -> list.map { it.userId }.distinct().size.coerceAtLeast(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 3)

    // --- Pull-to-Refresh State ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshPosts() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                repository.syncPosts()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error refreshing posts", e)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    // --- UI Filters ---
    private val _selectedDistrito = MutableStateFlow("Todos")
    val selectedDistrito: StateFlow<String> = _selectedDistrito.asStateFlow()

    private val _selectedTab = MutableStateFlow("Todos")
    val selectedTab: StateFlow<String> = _selectedTab.asStateFlow()

    fun setDistritoFilter(distrito: String) {
        _selectedDistrito.value = distrito
    }

    fun setTabFilter(tab: String) {
        _selectedTab.value = tab
    }

    // --- Session Startup ---
    private val _isInitializing = MutableStateFlow(true)
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.initSession()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Failed to initialize user session", e)
            }
            _isInitializing.value = false
        }
    }

    // --- Auth Actions ---
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val success = repository.login(email, password)
                if (success) {
                    onSuccess()
                } else {
                    onError("Correo o contraseña incorrectos. Verifica tus datos e intenta de nuevo.")
                }
            } catch (e: Exception) {
                val mapped = ErrorMapper.map(e.message, "Correo o contraseña incorrectos. Verifica tus datos e intenta de nuevo.")
                onError(mapped)
            }
        }
    }

    fun signup(
        email: String,
        name: String,
        phone: String,
        password: String,
        passwordConfirm: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (password.length < 6) {
            onError("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        if (password != passwordConfirm) {
            onError("Las contraseñas no coinciden.")
            return
        }
        viewModelScope.launch {
            try {
                repository.signup(email, name, phone, password)
                onSuccess()
            } catch (e: Exception) {
                onError(ErrorMapper.map(e.message, "Error al registrarse."))
            }
        }
    }

    fun logout(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            onSuccess()
        }
    }

    fun requestPasswordReset(email: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.requestPasswordReset(email)
            onResult(success)
        }
    }

    fun updateProfile(name: String, bio: String, phone: String, zone: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.updateProfile(name, bio, phone, zone)
                onSuccess()
            } catch (e: Exception) {
                onError(ErrorMapper.map(e.message, "Error al actualizar perfil."))
            }
        }
    }

    private val _isUploadingAvatar = MutableStateFlow(false)
    val isUploadingAvatar: StateFlow<Boolean> = _isUploadingAvatar.asStateFlow()

    private val _isUploadingCover = MutableStateFlow(false)
    val isUploadingCover: StateFlow<Boolean> = _isUploadingCover.asStateFlow()

    fun uploadAvatar(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isUploadingAvatar.value = true
            try {
                val res = repository.uploadAndUpdateAvatar(uri)
                if (res.isSuccess) {
                    onResult(true, "Foto de perfil actualizada correctamente")
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "No se pudo subir la imagen, intenta de nuevo.")
                }
            } catch (e: Exception) {
                onResult(false, "No se pudo subir la imagen, intenta de nuevo.")
            } finally {
                _isUploadingAvatar.value = false
            }
        }
    }

    fun uploadCover(uri: Uri, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            _isUploadingCover.value = true
            try {
                val res = repository.uploadAndUpdateCover(uri)
                if (res.isSuccess) {
                    onResult(true, "Foto de portada actualizada correctamente")
                } else {
                    onResult(false, res.exceptionOrNull()?.message ?: "No se pudo subir la imagen, intenta de nuevo.")
                }
            } catch (e: Exception) {
                onResult(false, "No se pudo subir la imagen, intenta de nuevo.")
            } finally {
                _isUploadingCover.value = false
            }
        }
    }

    fun updateProfilePhotos(avatar: String?, cover: String?) {
        viewModelScope.launch {
            repository.updateProfilePhotos(avatar, cover)
        }
    }

    // --- Post Actions ---
    fun savePost(
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
        telefonoContacto: String,
        onResult: (String?, String?) -> Unit
    ) {
        viewModelScope.launch {
            val finalId = id ?: ("post_" + UUID.randomUUID().toString().take(8))
            val result = repository.savePost(
                id = finalId,
                tipo = tipo,
                especie = especie,
                tamano = tamano,
                nombre = nombre,
                descripcion = descripcion,
                rasgos = rasgos,
                distrito = distrito,
                zona = zona,
                fotos = fotos,
                lat = lat,
                lng = lng,
                telefonoVisible = telefonoVisible,
                telefonoContacto = telefonoContacto
            )
            if (result.isSuccess && result.getOrNull() == true) {
                onResult(finalId, null)
            } else {
                val exception = result.exceptionOrNull()
                val errorMsg = ErrorMapper.map(exception?.message, "No pudimos conectar con el servidor. Verifica tu conexión a internet e intenta de nuevo.")
                Log.e("MainViewModel", "savePost falló: $errorMsg", exception)
                onResult(null, errorMsg)
            }
        }
    }

    fun markPostAsResolved(postId: String) {
        viewModelScope.launch {
            val token = ApiClient.token
            try {
                val response = PocketBaseClient.getService().updatePost(
                    "Bearer $token",
                    postId,
                    mapOf("estado" to "resuelta")
                )
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "markPostAsResolved: Publicación marcada como resuelta")
                    repository.updatePostEstadoLocally(postId, "resuelta")
                    repository.syncPosts()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "¡Publicación marcada como resuelta!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val rawErr = response.errorBody()?.string()
                    Log.e("MainViewModel", "markPostAsResolved: Error: $rawErr")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), ErrorMapper.map(rawErr, "No se pudo actualizar el estado."), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "markPostAsResolved: Excepción", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error de red al actualizar la publicación.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun togglePostEstado(postId: String) {
        val post = repository.posts.value.find { it.id == postId } ?: return
        val nuevoEstado = if (post.estado == "activo") "resuelta" else "activo"
        viewModelScope.launch {
            val token = ApiClient.token
            try {
                val response = PocketBaseClient.getService().updatePost(
                    "Bearer $token",
                    postId,
                    mapOf("estado" to nuevoEstado)
                )
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "togglePostEstado: Estado de publicación actualizado")
                    repository.updatePostEstadoLocally(postId, nuevoEstado)
                    repository.syncPosts()
                    withContext(Dispatchers.Main) {
                        val msg = if (nuevoEstado == "resuelta") "¡Publicación marcada como resuelta!" else "Publicación marcada como activa."
                        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val rawErr = response.errorBody()?.string()
                    Log.e("MainViewModel", "togglePostEstado: Error: $rawErr")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), ErrorMapper.map(rawErr, "No se pudo actualizar el estado."), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "togglePostEstado: Excepción", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error de red al actualizar el estado.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val token = ApiClient.token
                val response = PocketBaseClient.getService().deletePost("Bearer $token", postId)
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "deletePost: Publicación eliminada de Supabase")
                    repository.deletePostLocally(postId)
                    repository.syncPosts()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "La publicación fue eliminada correctamente.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val rawErr = response.errorBody()?.string()
                    Log.e("MainViewModel", "deletePost: Error: $rawErr")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), ErrorMapper.map(rawErr, "No se pudo eliminar la publicación. Intenta de nuevo."), Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "deletePost: Excepción", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "No pudimos conectar con el servidor. Verifica tu conexión a internet e intenta de nuevo.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- Comments & Likes ---
    fun getComments(postId: String): Flow<List<CommentEntity>> = flow {
        emit(repository.getCommentsForPost(postId))
    }.flowOn(Dispatchers.IO)

    fun insertComment(postId: String, texto: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val token = PocketBaseClient.token
            if (token != null) {
                try {
                    val response = PocketBaseClient.getService().createComment(
                        "Bearer $token",
                        mapOf(
                            "post_id" to postId,
                            "user_id" to user.id,
                            "texto" to texto
                        )
                    )
                    if (response.isSuccessful) {
                        Log.d("MainViewModel", "insertComment: Comentario creado correctamente")
                        
                        val post = repository.posts.value.find { it.id == postId }
                        if (post != null && post.userId != user.id) {
                            ApiClient.getService().createNotification(
                                mapOf(
                                    "user_id" to post.userId,
                                    "mensaje" to "${user.nombre} comentó tu publicación de ${post.nombreMascota.ifEmpty { "mascota" }}."
                                )
                            )
                        }
                        repository.syncNotifications()
                    } else {
                        Log.e("MainViewModel", "insertComment: Error: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "insertComment: Excepción", e)
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().deleteComment(commentId)
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "deleteComment: Comentario eliminado")
                } else {
                    Log.e("MainViewModel", "deleteComment: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "deleteComment: Excepción", e)
            }
        }
    }

    fun toggleLike(postId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            val token = PocketBaseClient.token
            if (token != null) {
                try {
                    val existingResponse = ApiClient.getService().getLikes(postIdQuery = "eq.$postId", userIdQuery = "eq.${user.id}")
                    val post = repository.posts.value.find { it.id == postId } ?: return@launch
                    if (existingResponse.isSuccessful && !existingResponse.body().isNullOrEmpty()) {
                        val likeId = existingResponse.body()!!.first().id
                        ApiClient.getService().deleteLike(likeId)
                        val newCount = (post.likesCount - 1).coerceAtLeast(0)
                        PocketBaseClient.getService().updatePost("Bearer $token", postId, mapOf("likes_count" to newCount))
                        Log.d("MainViewModel", "toggleLike: Like removido")
                    } else {
                        ApiClient.getService().createLike(mapOf("post_id" to postId, "user_id" to user.id))
                        val newCount = post.likesCount + 1
                        PocketBaseClient.getService().updatePost("Bearer $token", postId, mapOf("likes_count" to newCount))
                        Log.d("MainViewModel", "toggleLike: Like registrado")
                        
                        if (post.userId != user.id) {
                            ApiClient.getService().createNotification(
                                mapOf(
                                    "user_id" to post.userId,
                                    "mensaje" to "${user.nombre} le dio me gusta a tu publicación de ${post.nombreMascota.ifEmpty { "mascota" }}."
                                )
                            )
                        }
                    }
                    repository.syncPosts()
                    repository.syncNotifications()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "toggleLike: Excepción", e)
                }
            }
        }
    }

    suspend fun listLikesForUser(): List<String> = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext emptyList()
        try {
            val response = ApiClient.getService().getLikes(userIdQuery = "eq.${user.id}")
            if (response.isSuccessful && response.body() != null) {
                response.body()!!.map { it.postId }
            } else {
                Log.e("MainViewModel", "listLikesForUser: Error: ${response.errorBody()?.string()}")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "listLikesForUser: Excepción", e)
            emptyList()
        }
    }

    // --- Report Actions ---
    fun reportPost(postId: String, razon: String, onSuccess: () -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().createReport(
                    mapOf(
                        "post_id" to postId,
                        "reporter_id" to user.id,
                        "motivo" to razon,
                        "estado" to "pendiente"
                    )
                )
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "reportPost: Reporte de publicación creado")
                    repository.syncReports()
                    onSuccess()
                } else {
                    Log.e("MainViewModel", "reportPost: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "reportPost: Excepción", e)
            }
        }
    }

    fun reportProfile(reportedUserId: String, razon: String, onSuccess: () -> Unit) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().createProfileReport(
                    mapOf(
                        "profile_id" to reportedUserId,
                        "reporter_id" to user.id,
                        "motivo" to razon,
                        "estado" to "pendiente"
                    )
                )
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "reportProfile: Reporte de perfil creado")
                    repository.syncProfileReports()
                    onSuccess()
                } else {
                    Log.e("MainViewModel", "reportProfile: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "reportProfile: Excepción", e)
            }
        }
    }

    fun reviewReport(reportId: String, resolve: Boolean) {
        viewModelScope.launch {
            try {
                val nuevoEstado = if (resolve) "resuelto" else "revisado"
                val response = ApiClient.getService().updateReport(
                    idQuery = "eq.$reportId",
                    body = mapOf("estado" to nuevoEstado)
                )
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "reviewReport: Reporte de publicación procesado")
                    repository.syncReports()
                } else {
                    Log.e("MainViewModel", "reviewReport: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "reviewReport: Excepción", e)
            }
        }
    }

    // --- User Statuses ---
    fun postStatus(texto: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val result = repository.postStatus(texto)
                if (result.isSuccess) {
                    Log.d("MainViewModel", "postStatus: Estado publicado correctamente")
                    onSuccess()
                } else {
                    Log.e("MainViewModel", "postStatus: Falló el guardado del estado")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "postStatus: Excepción", e)
            }
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().deleteStatus("eq.$statusId")
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "deleteStatus: Estado eliminado")
                    repository.syncStatuses()
                } else {
                    Log.e("MainViewModel", "deleteStatus: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "deleteStatus: Excepción", e)
            }
        }
    }

    fun toggleStatusLike(statusId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val existingResponse = ApiClient.getService().getStatusLikes(statusIdQuery = "eq.$statusId", userIdQuery = "eq.${user.id}")
                val status = repository.statuses.value.find { it.id == statusId } ?: return@launch
                if (existingResponse.isSuccessful && !existingResponse.body().isNullOrEmpty()) {
                    val likeId = existingResponse.body()!!.first().id
                    ApiClient.getService().deleteStatusLike("eq.$likeId")
                    val newCount = (status.likesCount - 1).coerceAtLeast(0)
                    ApiClient.getService().updateStatus("eq.$statusId", mapOf("likes_count" to newCount))
                    Log.d("MainViewModel", "toggleStatusLike: Like de estado removido")
                } else {
                    ApiClient.getService().createStatusLike(mapOf("status_id" to statusId, "user_id" to user.id))
                    val newCount = status.likesCount + 1
                    ApiClient.getService().updateStatus("eq.$statusId", mapOf("likes_count" to newCount))
                    Log.d("MainViewModel", "toggleStatusLike: Like de estado registrado")
                }
                repository.syncStatuses()
            } catch (e: Exception) {
                Log.e("MainViewModel", "toggleStatusLike: Excepción", e)
            }
        }
    }

    // --- Veterinarias ---
    fun saveVeterinaria(
        id: String?,
        nombre: String,
        direccion: String,
        telefono: String,
        staff: String,
        horarios: String,
        distrito: String,
        clasificacion: String,
        lat: Double,
        lng: Double,
        logo: String,
        activa: Boolean = true,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                val finalId = id ?: ("vet_" + UUID.randomUUID().toString().take(8))
                val body: Map<String, Any> = mapOf(
                    "id" to finalId,
                    "nombre" to nombre,
                    "direccion" to direccion,
                    "telefono" to telefono,
                    "staff" to staff,
                    "horarios" to horarios,
                    "distrito" to distrito,
                    "clasificacion" to clasificacion,
                    "ubicacion_lat" to lat,
                    "ubicacion_lng" to lng,
                    "logo" to logo.ifEmpty { "default_vet_logo" },
                    "activa" to activa
                )
                val response = if (id == null) {
                    ApiClient.getService().createVeterinaria(body)
                } else {
                    ApiClient.getService().updateVeterinaria("eq.$id", body)
                }
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "saveVeterinaria: Veterinaria guardada exitosamente")
                    repository.syncVeterinarias()
                    onSuccess()
                } else {
                    Log.e("MainViewModel", "saveVeterinaria: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "saveVeterinaria: Excepción", e)
            }
        }
    }

    fun deleteVeterinaria(id: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().deleteVeterinaria("eq.$id")
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "deleteVeterinaria: Veterinaria eliminada de Supabase")
                    repository.syncVeterinarias()
                } else {
                    Log.e("MainViewModel", "deleteVeterinaria: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "deleteVeterinaria: Excepción", e)
            }
        }
    }

    // --- Campaigns ---
    fun saveCampaign(
        id: String?,
        titulo: String,
        descripcion: String,
        contenido: String,
        fechaInicio: String,
        fechaFin: String,
        estado: String,
        imagen: String,
        onSuccess: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val finalId = id ?: ("camp_" + UUID.randomUUID().toString().take(8))
                val body: Map<String, Any> = mapOf(
                    "id" to finalId,
                    "titulo" to titulo,
                    "descripcion" to descripcion,
                    "contenido_detallado" to contenido,
                    "fecha_inicio" to fechaInicio,
                    "fecha_fin" to fechaFin,
                    "ong_id" to user.id,
                    "ong_nombre" to user.nombre,
                    "estado" to estado,
                    "imagen_portada" to imagen.ifEmpty { "default_camp_img" }
                )
                val response = if (id == null) {
                    ApiClient.getService().createCampaign(body)
                } else {
                    ApiClient.getService().updateCampaign("eq.$id", body)
                }
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "saveCampaign: Campaña guardada exitosamente")
                    repository.syncCampaigns()
                    onSuccess()
                } else {
                    Log.e("MainViewModel", "saveCampaign: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "saveCampaign: Excepción", e)
            }
        }
    }

    fun deleteCampaign(id: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().deleteCampaign("eq.$id")
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "deleteCampaign: Campaña eliminada de Supabase")
                    repository.syncCampaigns()
                } else {
                    Log.e("MainViewModel", "deleteCampaign: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "deleteCampaign: Excepción", e)
            }
        }
    }

    fun toggleCampaignLike(campaignId: String) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val existingResponse = ApiClient.getService().getLikes(postIdQuery = "eq.$campaignId", userIdQuery = "eq.${user.id}")
                val camp = repository.campaigns.value.find { it.id == campaignId } ?: return@launch
                if (existingResponse.isSuccessful && !existingResponse.body().isNullOrEmpty()) {
                    val likeId = existingResponse.body()!!.first().id
                    ApiClient.getService().deleteLike(likeId)
                    val newCount = (camp.likesCount - 1).coerceAtLeast(0)
                    ApiClient.getService().updateCampaign("eq.$campaignId", mapOf("likes_count" to newCount))
                    Log.d("MainViewModel", "toggleCampaignLike: Like de campaña removido")
                } else {
                    ApiClient.getService().createLike(mapOf("post_id" to campaignId, "user_id" to user.id))
                    val newCount = camp.likesCount + 1
                    ApiClient.getService().updateCampaign("eq.$campaignId", mapOf("likes_count" to newCount))
                    Log.d("MainViewModel", "toggleCampaignLike: Like de campaña registrado")
                }
                repository.syncCampaigns()
            } catch (e: Exception) {
                Log.e("MainViewModel", "toggleCampaignLike: Excepción", e)
            }
        }
    }

    suspend fun isCampaignSupported(campId: String): Boolean = withContext(Dispatchers.IO) {
        val user = currentUser.value ?: return@withContext false
        try {
            val response = ApiClient.getService().getLikes(postIdQuery = "eq.$campId", userIdQuery = "eq.${user.id}")
            response.isSuccessful && !response.body().isNullOrEmpty()
        } catch (e: Exception) {
            Log.e("MainViewModel", "isCampaignSupported: Excepción", e)
            false
        }
    }

    // --- News Actions ---
    fun saveNews(
        id: String?,
        titular: String,
        contenido: String,
        imagen: String,
        imagenSecundaria: String,
        onSuccess: () -> Unit
    ) {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val finalId = id ?: ("news_" + UUID.randomUUID().toString().take(8))
                val body: Map<String, Any> = mapOf(
                    "id" to finalId,
                    "titular" to titular,
                    "contenido" to contenido,
                    "autor_id" to user.id,
                    "autor_nombre" to user.nombre,
                    "imagen_portada" to imagen,
                    "imagen_noticia" to imagenSecundaria
                )
                val response = if (id == null) {
                    ApiClient.getService().createNews(body)
                } else {
                    ApiClient.getService().updateNews("eq.$id", body)
                }
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "saveNews: Noticia guardada exitosamente")
                    repository.syncNews()
                    onSuccess()
                } else {
                    Log.e("MainViewModel", "saveNews: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "saveNews: Excepción", e)
            }
        }
    }

    fun deleteNews(id: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().deleteNews("eq.$id")
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "deleteNews: Noticia eliminada de Supabase")
                    repository.syncNews()
                } else {
                    Log.e("MainViewModel", "deleteNews: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "deleteNews: Excepción", e)
            }
        }
    }

    // --- Notifications ---
    fun getNotifications(): Flow<List<NotificationEntity>> = repository.notifications

    fun markNotificationsRead() {
        val user = currentUser.value ?: return
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().getNotifications(userIdQuery = "eq.${user.id}")
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.forEach { notif ->
                        ApiClient.getService().updateNotification(notif.id, mapOf("leido" to true))
                    }
                    repository.syncNotifications()
                    Log.d("MainViewModel", "markNotificationsRead: Todas las notificaciones marcadas como leídas")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "markNotificationsRead: Excepción", e)
            }
        }
    }

    // --- Colaboradores ---
    fun saveColaborador(
        id: String?,
        nombre: String,
        descripcion: String,
        logo: String,
        enlace: String,
        telefono: String,
        whatsapp: String,
        instagram: String,
        direccion: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveColaborador(
                id, nombre, descripcion, logo, enlace, telefono, whatsapp, instagram, direccion
            )
            if (res) {
                if (id == null) {
                    logAdminAction("crear_colaborador", nombre, "Creado colaborador $nombre")
                } else {
                    logAdminAction("editar_colaborador", id, "Editado colaborador $nombre")
                }
                onSuccess()
            }
        }
    }

    fun deleteColaborador(id: String, nombre: String) {
        viewModelScope.launch {
            val res = repository.deleteColaborador(id)
            if (res) {
                logAdminAction("eliminar_colaborador", id, "Eliminado colaborador $nombre")
            }
        }
    }

    // --- User Blocks ---
    fun blockUser(blockedUserId: String, blockedNombre: String, blockedFoto: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val res = repository.blockUser(blockedUserId, blockedNombre, blockedFoto)
            if (res) {
                onSuccess()
            }
        }
    }

    fun unblockUser(blockedUserId: String) {
        viewModelScope.launch {
            repository.unblockUser(blockedUserId)
        }
    }

    // --- User Follows ---
    fun followUser(targetUserId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.followUser(targetUserId)
            onSuccess?.invoke()
        }
    }

    fun unfollowUser(targetUserId: String, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.unfollowUser(targetUserId)
            onSuccess?.invoke()
        }
    }

    // --- Log Admin Action ---
    fun logAdminAction(accion: String, targetId: String, detalles: String) {
        viewModelScope.launch {
            repository.logAdminAction(accion, targetId, detalles)
        }
    }

    // --- Individual Notification Read ---
    fun markNotificationRead(notif: NotificationEntity) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getService().updateNotification(notif.id, mapOf("leido" to true))
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "markNotificationRead: Notificación marcada como leída")
                    repository.syncNotifications()
                } else {
                    Log.e("MainViewModel", "markNotificationRead: Error: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "markNotificationRead: Excepción", e)
            }
        }
    }
}
