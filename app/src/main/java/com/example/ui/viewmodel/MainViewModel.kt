package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AppRepository(application)

    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    val posts: StateFlow<List<PostEntity>> = repository.posts
    val veterinarias: StateFlow<List<VeterinariaEntity>> = repository.veterinarias
    val campaigns: StateFlow<List<CampaignEntity>> = repository.campaigns
    val news: StateFlow<List<NewsEntity>> = repository.news
    val statuses: StateFlow<List<UserStatusEntity>> = repository.statuses
    val colaboradores: StateFlow<List<ColaboradorEntity>> = repository.colaboradores
    val blockedUsers: StateFlow<List<UserBlockEntity>> = repository.blockedUsers
    val follows: StateFlow<List<UserFollowEntity>> = repository.follows
    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
    val reports: StateFlow<List<ReportEntity>> = repository.reports
    val profileReports: StateFlow<List<ProfileReportEntity>> = repository.profileReports

    private val _selectedDistrito = MutableStateFlow("Todos")
    val selectedDistrito: StateFlow<String> = _selectedDistrito.asStateFlow()

    private val _selectedTipo = MutableStateFlow("Todos")
    val selectedTipo: StateFlow<String> = _selectedTipo.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredPosts: StateFlow<List<PostEntity>> = combine(
        posts,
        _selectedDistrito,
        _selectedTipo,
        _searchQuery,
        blockedUsers
    ) { postList, distrito, tipo, query, blocks ->
        val blockedIds = blocks.map { it.blockedUserId }.toSet()
        postList.filter { post ->
            post.userId !in blockedIds &&
            (distrito == "Todos" || post.distrito.equals(distrito, ignoreCase = true)) &&
            (tipo == "Todos" || post.tipo.equals(tipo, ignoreCase = true)) &&
            (query.isBlank() ||
                post.nombreMascota.contains(query, ignoreCase = true) ||
                post.descripcion.contains(query, ignoreCase = true) ||
                post.rasgos.contains(query, ignoreCase = true) ||
                post.distrito.contains(query, ignoreCase = true) ||
                post.zona.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.initSession()
        }
    }

    fun setDistrito(distrito: String) {
        _selectedDistrito.value = distrito
    }

    fun setTipo(tipo: String) {
        _selectedTipo.value = tipo
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun refreshAll() {
        viewModelScope.launch {
            repository.syncAll()
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val ok = repository.login(email, pass)
                if (ok) onSuccess() else onError("Error al iniciar sesión")
            } catch (e: Exception) {
                onError(e.message ?: "Error de red")
            }
        }
    }

    fun signup(email: String, name: String, phone: String, pass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.signup(email, name, phone, pass)
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Error al registrarse")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    fun createPost(
        tipo: String, especie: String, tamano: String, nombre: String,
        desc: String, rasgos: String, distrito: String, zona: String,
        telefono: String, visible: Boolean, fotos: String, lat: Double, lng: Double,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.createPost(
                tipo, especie, tamano, nombre, desc, rasgos,
                distrito, zona, telefono, visible, fotos, lat, lng
            )
            onComplete(result.isSuccess)
        }
    }

    fun updatePost(
        postId: String, tipo: String, especie: String, tamano: String, nombre: String,
        desc: String, rasgos: String, distrito: String, zona: String,
        telefono: String, visible: Boolean, fotos: String, estado: String,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updatePost(
                postId, tipo, especie, tamano, nombre, desc, rasgos,
                distrito, zona, telefono, visible, fotos, estado
            )
            onComplete(result.isSuccess)
        }
    }

    fun deletePost(postId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deletePost(postId)
            onComplete(result.isSuccess)
        }
    }

    fun toggleLikePost(postId: String) {
        viewModelScope.launch {
            repository.toggleLikePost(postId)
        }
    }

    fun addComment(postId: String, text: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.addComment(postId, text)
            onComplete(result.isSuccess)
        }
    }

    fun deleteComment(commentId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteComment(commentId)
            onComplete(result.isSuccess)
        }
    }

    fun reportPost(postId: String, reason: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.reportPost(postId, reason)
            onComplete(result.isSuccess)
        }
    }

    fun reportProfile(userId: String, reason: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = repository.reportProfile(userId, reason)
            onComplete(result.isSuccess)
        }
    }

    fun blockUser(userId: String, nombre: String, foto: String) {
        viewModelScope.launch {
            repository.blockUser(userId, nombre, foto)
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            repository.unblockUser(userId)
        }
    }

    fun updateProfile(
        nombre: String, telefono: String, zona: String, bio: String,
        foto: String, portada: String, onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.updateUserProfile(nombre, telefono, zona, bio, foto, portada)
            onComplete(res.isSuccess)
        }
    }

    fun saveVeterinaria(
        id: String?, nombre: String, dir: String, tel: String, staff: String,
        horarios: String, distrito: String, clasificacion: String, lat: Double, lng: Double,
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveVeterinaria(id, nombre, dir, tel, staff, horarios, distrito, clasificacion, lat, lng)
            onComplete(res.isSuccess)
        }
    }

    fun deleteVeterinaria(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteVeterinaria(id)
            onComplete(res.isSuccess)
        }
    }

    fun saveCampaign(
        id: String?, titulo: String, desc: String, portada: String,
        contenido: String, inicio: String, fin: String, onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveCampaign(id, titulo, desc, portada, contenido, inicio, fin)
            onComplete(res.isSuccess)
        }
    }

    fun deleteCampaign(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteCampaign(id)
            onComplete(res.isSuccess)
        }
    }

    fun toggleLikeCampaign(id: String) {
        viewModelScope.launch {
            repository.toggleLikeCampaign(id)
        }
    }

    fun saveNews(
        id: String?, titular: String, contenido: String, portada: String,
        imagen: String, onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveNews(id, titular, contenido, portada, imagen)
            onComplete(res.isSuccess)
        }
    }

    fun deleteNews(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteNews(id)
            onComplete(res.isSuccess)
        }
    }

    fun saveColaborador(
        id: String?, nombre: String, desc: String, logo: String, enlace: String,
        tel: String, whatsapp: String, instagram: String, dir: String, onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.saveColaborador(id, nombre, desc, logo, enlace, tel, whatsapp, instagram, dir)
            onComplete(res.isSuccess)
        }
    }

    fun deleteColaborador(id: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteColaborador(id)
            onComplete(res.isSuccess)
        }
    }
}
