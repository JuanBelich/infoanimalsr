package com.example.data.network

import android.content.Context
import android.util.Log
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * PocketBaseClient is preserved as a legacy wrapper / adapter of our new ApiClient.
 * This ensures that existing files (such as AppScreens.kt, AppRepository.kt,
 * and MainViewModel.kt) continue to compile and function perfectly,
 * routing all requests seamlessly to the new Supabase REST API backend.
 */
object PocketBaseClient {
    val baseUrl: String get() = ApiClient.baseUrl
    val token: String? get() = ApiClient.token
    val authenticatedUser: PocketBaseUser? get() = ApiClient.authenticatedUser
    val isOfflineMode: Boolean get() = ApiClient.isOfflineMode

    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        ApiClient.init(context)
    }

    fun updateBaseUrl(context: Context, newUrl: String) {
        ApiClient.updateBaseUrl(context, newUrl)
    }

    fun setOfflineMode(context: Context, enabled: Boolean) {
        // In production, offline mode is disabled; app relies on direct Supabase REST requests
    }

    fun saveAuth(context: Context, newToken: String, user: PocketBaseUser) {
        ApiClient.saveAuth(context, newToken, user)
    }

    fun clearAuth(context: Context) {
        ApiClient.clearAuth(context)
    }

    fun getFileUrl(collectionName: String, recordId: String, fileName: String, thumbSize: String? = null): String {
        return ApiClient.getFileUrl(collectionName, recordId, fileName, thumbSize)
    }

    suspend fun uploadAvatar(filePath: String, filePart: okhttp3.MultipartBody.Part) = ApiClient.uploadAvatar(filePath, filePart)
    suspend fun uploadPostImage(filePath: String, filePart: okhttp3.MultipartBody.Part) = ApiClient.uploadPostImage(filePath, filePart)
    suspend fun uploadAvatar(filePath: String, fileBytes: ByteArray) = ApiClient.uploadAvatar(filePath, fileBytes)
    suspend fun uploadPostImage(filePath: String, fileBytes: ByteArray) = ApiClient.uploadPostImage(filePath, fileBytes)
    suspend fun uploadAvatarBase64(fileName: String, base64Data: String) = ApiClient.uploadAvatarBase64(fileName, base64Data)
    suspend fun uploadPostImageBase64(fileName: String, base64Data: String) = ApiClient.uploadPostImageBase64(fileName, base64Data)

    // --- Helper mappers for Supabase to legacy PocketBase ---

    internal fun SupabasePost.toPocketBasePost(): PocketBasePost {
        val photoList = when (val f = this.fotos) {
            is List<*> -> f.map { it.toString() }
            is String -> {
                if (f.startsWith("[")) {
                    try {
                        f.trim('[', ']').split(",").map { it.trim(' ', '"') }.filter { it.isNotEmpty() }
                    } catch (e: Exception) {
                        listOf(f)
                    }
                } else {
                    listOf(f)
                }
            }
            else -> emptyList()
        }
        return PocketBasePost(
            id = this.id,
            userId = this.userId ?: "",
            tipo = this.tipo ?: "",
            especie = this.especie ?: "",
            tamano = this.tamano ?: "",
            nombreMascota = this.nombreMascota ?: "",
            descripcion = this.descripcion ?: "",
            rasgos = this.rasgos ?: "",
            estado = this.estado ?: "activo",
            fotos = photoList,
            ubicacionLat = this.ubicacionLat ?: 0.0,
            ubicacionLng = this.ubicacionLng ?: 0.0,
            distrito = this.distrito ?: "",
            zona = this.zona ?: "",
            telefonoContacto = this.telefonoContacto ?: "",
            telefonoVisible = this.telefonoVisible ?: true,
            likesCount = this.likesCount ?: 0,
            userNombreCompleto = if (this.users?.nombre.isNullOrBlank()) "InfoAnimal Vecino" else this.users?.nombre ?: "InfoAnimal Vecino",
            userFotoPerfil = this.users?.fotoPerfil ?: "",
            userIdExpandido = this.userId ?: "",
            created = this.created ?: ""
        )
    }

    internal fun SupabaseComment.toPocketBaseComment(): PocketBaseComment {
        return PocketBaseComment(
            id = this.id,
            postId = this.postId ?: "",
            userId = this.userId ?: "",
            texto = this.texto ?: "",
            userNombreCompleto = this.users?.nombre ?: "Vecino",
            userFotoPerfil = this.users?.fotoPerfil ?: "",
            userIdExpandido = this.userId ?: "",
            created = this.created ?: ""
        )
    }

    internal fun SupabaseUser.toPocketBaseUser(): PocketBaseUser {
        return PocketBaseUser(
            id = this.id,
            email = this.email ?: "",
            nombre = this.nombre ?: "",
            telefono = this.telefono ?: "",
            zonaPrincipal = this.zonaPrincipal ?: "San Rafael",
            rol = this.rol ?: "user",
            bloqueado = false,
            fotoPerfil = this.fotoPerfil ?: "",
            fotoPortada = "gradient_blue",
            biografia = "",
            created = ""
        )
    }

    /**
     * Creates an implementation of the legacy PocketBaseService that intercepts and adapts 
     * the incoming data to work on top of the new Supabase ApiService.
     */
    fun getService(): PocketBaseService {
        return object : PocketBaseService {

            override suspend fun login(body: Map<String, String>): Response<PocketBaseAuthResponse> {
                try {
                    val email = (body["identity"] ?: body["email"] ?: "").trim()
                    val password = (body["password"] ?: "").trim()

                    // 1. Call Supabase Auth login
                    val authResponse = ApiClient.getService().login(mapOf("email" to email, "password" to password))
                    if (!authResponse.isSuccessful || authResponse.body() == null) {
                        val errBody = authResponse.errorBody() ?: ResponseBody.create(null, "Login failed")
                        return Response.error(authResponse.code(), errBody)
                    }
                    val supabaseAuth = authResponse.body()!!
                    val accessToken = supabaseAuth.accessToken
                    val userId = supabaseAuth.user.id

                    // Establish the token dynamically so subsequent requests in this session are authenticated
                    ApiClient.setToken(accessToken)

                    // Bloqueamos de manera estricta y síncrona hasta que el token esté inyectado en ApiClient
                    var waitAttempts = 0
                    while (ApiClient.token != accessToken && waitAttempts < 10) {
                        kotlinx.coroutines.delay(50)
                        waitAttempts++
                    }
                    if (ApiClient.token != accessToken) {
                        Log.e("PocketBaseClient", "Advertencia: El token de ApiClient no se sincronizó a tiempo.")
                    }

                    // 2. Fetch public profile from 'users' table in Supabase (con mecanismo estricto de reintento ante latencia de RLS)
                    var profileResponse: Response<List<com.example.data.network.SupabaseUser>>? = null
                    try {
                        profileResponse = ApiClient.getService().getUserProfile("eq.$userId")
                    } catch (e: Exception) {
                        Log.w("PocketBaseClient", "Primer intento de consultar perfil falló por excepción: ${e.message}. Forzando token y reintentando...")
                    }

                    if (profileResponse == null || !profileResponse.isSuccessful) {
                        Log.w("PocketBaseClient", "Perfil no disponible o rechazado (Código ${profileResponse?.code() ?: "N/A"}). Reintentando con token limpio y delay de sincronización...")
                        ApiClient.setToken(accessToken)
                        kotlinx.coroutines.delay(300)
                        try {
                            profileResponse = ApiClient.getService().getUserProfile("eq.$userId")
                        } catch (e: Exception) {
                            Log.e("PocketBaseClient", "Excepción crítica en el segundo intento de obtener perfil", e)
                        }
                    }

                    val pbUser = if (profileResponse != null && profileResponse.isSuccessful && !profileResponse.body().isNullOrEmpty()) {
                        profileResponse.body()!!.first().toPocketBaseUser()
                    } else {
                        // Si recibimos un error de autenticación explícito (401 o 403), la causa raíz es RLS o credenciales inválidas
                        if (profileResponse != null && (profileResponse.code() == 401 || profileResponse.code() == 403)) {
                            val errorString = profileResponse.errorBody()?.string() ?: "Acceso Denegado por RLS"
                            Log.e("CRITICAL_SUPABASE_ERROR", "Error de RLS al leer perfil tras iniciar sesión: $errorString")
                            return Response.error(profileResponse.code(), ResponseBody.create(null, "Error de seguridad (RLS): $errorString"))
                        }

                        // Profile doesn't exist in users table yet; create one automatically
                        val placeholderUser = PocketBaseUser(
                            id = userId,
                            email = email,
                            nombre = supabaseAuth.user.userMetadata?.get("nombre")?.toString() ?: email.substringBefore("@"),
                            telefono = supabaseAuth.user.userMetadata?.get("telefono")?.toString() ?: "",
                            rol = "user",
                            bloqueado = false,
                            fotoPerfil = "avatar_placeholder",
                            fotoPortada = "gradient_blue",
                            created = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                            }.format(java.util.Date())
                        )
                        try {
                            val insertMap: Map<String, Any> = mapOf(
                                "id" to userId,
                                "email" to email,
                                "nombre" to placeholderUser.nombre,
                                "telefono" to (placeholderUser.telefono ?: ""),
                                "rol" to (placeholderUser.rol ?: "user"),
                                "bloqueado" to (placeholderUser.bloqueado ?: false),
                                "foto_perfil" to (placeholderUser.fotoPerfil ?: "avatar_placeholder"),
                                "foto_portada" to (placeholderUser.fotoPortada ?: "gradient_blue")
                            )
                            val createResponse = ApiClient.getService().createUserProfile(insertMap)
                            if (!createResponse.isSuccessful) {
                                val errorString = createResponse.errorBody()?.string() ?: "Error desconocido"
                                Log.e("CRITICAL_SUPABASE_ERROR", "Error de RLS al crear perfil: $errorString")
                                return Response.error(createResponse.code(), ResponseBody.create(null, "Error al inicializar perfil: $errorString"))
                            }
                        } catch (e: Exception) {
                            Log.e("PocketBaseClient", "Could not insert profile upon login", e)
                            return Response.error(500, ResponseBody.create(null, "Excepción al crear perfil: ${e.message}"))
                        }
                        placeholderUser
                    }

                    // Save auth locally to keep MVVM app state synchronized
                    appContext?.let {
                        ApiClient.saveAuth(it, accessToken, pbUser)
                    }

                    return Response.success(PocketBaseAuthResponse(token = accessToken, record = pbUser))
                } catch (e: Exception) {
                    Log.e("PocketBaseClient", "Exception in legacy login bridge", e)
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun signup(body: Map<String, Any>): Response<PocketBaseUser> {
                try {
                    // Validación de seguridad: si ApiClient no se ha inicializado, lo hacemos aquí de forma proactiva
                    val ctx = appContext ?: try {
                        com.example.MainApplication.instance
                    } catch (e: Exception) {
                        null
                    }
                    if (ctx != null) {
                        ApiClient.init(ctx)
                    }

                    val email = body["email"]?.toString() ?: ""
                    val password = body["password"]?.toString() ?: ""
                    val nombre = body["nombre"]?.toString() ?: ""
                    val telefono = body["telefono"]?.toString() ?: ""
                    val rol = body["rol"]?.toString() ?: "user"

                    // 1. Sign up user in Supabase GoTrue Auth
                    val signupBody: Map<String, Any> = mapOf(
                        "email" to email,
                        "password" to password,
                        "data" to mapOf<String, Any>(
                            "nombre" to nombre,
                            "telefono" to telefono
                        )
                    )
                    val signupResponse = ApiClient.getService().signup(signupBody)
                    if (!signupResponse.isSuccessful || signupResponse.body() == null) {
                        val errBody = signupResponse.errorBody() ?: ResponseBody.create(null, "Signup failed")
                        return Response.error(signupResponse.code(), errBody)
                    }
                    val supUser = signupResponse.body()!!

                    // 2. Insert corresponding public user profile record in public 'users' table
                    val pbUser = PocketBaseUser(
                        id = supUser.id,
                        email = email,
                        nombre = nombre,
                        telefono = telefono,
                        rol = rol,
                        bloqueado = false,
                        fotoPerfil = "avatar_placeholder",
                        fotoPortada = "gradient_blue",
                        created = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                        }.format(java.util.Date())
                    )

                    val insertMap: Map<String, Any> = mapOf(
                        "id" to supUser.id,
                        "email" to email,
                        "nombre" to nombre,
                        "telefono" to telefono,
                        "rol" to rol,
                        "bloqueado" to false,
                        "foto_perfil" to "avatar_placeholder",
                        "foto_portada" to "gradient_blue"
                    )
                    try {
                        ApiClient.getService().createUserProfile(insertMap)
                    } catch (e: Exception) {
                        Log.e("PocketBaseClient", "Error creating public users profile record", e)
                    }

                    return Response.success(pbUser)
                } catch (e: Exception) {
                    Log.e("PocketBaseClient", "Exception in legacy signup bridge", e)
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun requestPasswordReset(body: Map<String, String>): Response<Unit> {
                try {
                    val email = body["email"] ?: ""
                    val res = ApiClient.getService().requestPasswordReset(mapOf("email" to email))
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Reset failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun updateProfile(
                token: String,
                userId: String,
                body: Map<String, Any>
            ): Response<PocketBaseUser> {
                try {
                    val res = ApiClient.getService().updateProfile("eq.$userId", body)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        val updatedUser = res.body()!!.first().toPocketBaseUser()
                        if (userId == ApiClient.authenticatedUser?.id) {
                            appContext?.let {
                                ApiClient.saveAuth(it, ApiClient.token ?: "", updatedUser)
                            }
                        }
                        Response.success(updatedUser)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Update profile failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getPosts(
                page: Int,
                perPage: Int,
                sort: String,
                filter: String?,
                expand: String?
            ): Response<PocketBaseListResponse<PocketBasePost>> {
                try {
                    val limit = perPage
                    val offset = (page - 1) * perPage
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    
                    var idQuery: String? = null
                    if (!filter.isNullOrBlank()) {
                        if (filter.contains("id=")) {
                            val parsedId = filter.substringAfter("id=").substringBefore("&&").trim('\'', '"', ' ')
                            idQuery = "eq.$parsedId"
                        } else if (filter.contains("id='")) {
                            val parsedId = filter.substringAfter("id='").substringBefore("'").trim()
                            idQuery = "eq.$parsedId"
                        }
                    }

                    val isGuest = ApiClient.token.isNullOrBlank()
                    Log.d("PocketBaseClient", "getPosts: Ejecutando consulta de publicaciones (isGuest=$isGuest)...")
                    var res = try {
                        ApiClient.getService().getPosts(
                            select = "*,users(nombre,foto_perfil)",
                            order = order,
                            limit = limit,
                            offset = offset,
                            idQuery = idQuery
                        )
                    } catch (e: Exception) {
                        Log.e("PocketBaseClient", "getPosts with select exception, falling back to simple select", e)
                        null
                    }

                    if (res == null || !res.isSuccessful) {
                        val errorCode = res?.code() ?: -1
                        val errorMsg = res?.errorBody()?.string() ?: "Exception"
                        Log.w("PocketBaseClient", "getPosts failed or was restricted (code $errorCode: $errorMsg). Trying simple select.")
                        res = try {
                            ApiClient.getService().getPosts(
                                select = "*",
                                order = order,
                                limit = limit,
                                offset = offset,
                                idQuery = idQuery
                            )
                        } catch (e: Exception) {
                            Log.e("PocketBaseClient", "getPosts simple fallback exception", e)
                            null
                        }
                    }

                    val finalRes = res
                    return if (finalRes != null && finalRes.isSuccessful && finalRes.body() != null) {
                        val supabasePosts = finalRes.body()!!
                        val items = supabasePosts.map { it.toPocketBasePost() }

                        val contentRange = finalRes.headers().get("content-range")
                        val totalItems = if (!contentRange.isNullOrBlank() && contentRange.contains("/")) {
                            contentRange.substringAfter("/").toIntOrNull() ?: items.size
                        } else {
                            items.size
                        }
                        val totalPages = if (perPage > 0) (totalItems + perPage - 1) / perPage else 1

                        Response.success(
                            PocketBaseListResponse(
                                page = page,
                                perPage = perPage,
                                totalItems = totalItems,
                                totalPages = totalPages,
                                items = items
                            )
                        )
                    } else {
                        val code = finalRes?.code() ?: 500
                        val errBody = finalRes?.errorBody() ?: ResponseBody.create(null, "Get posts failed")
                        Response.error(code, errBody)
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getPostById(id: String, expand: String?): Response<PocketBasePost> {
                try {
                    val res = ApiClient.getService().getPosts(idQuery = "eq.$id")
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first().toPocketBasePost())
                    } else {
                        Response.error(404, ResponseBody.create(null, "Post not found"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createPost(token: String, body: RequestBody): Response<PocketBasePost> {
                // Not used directly, but we delegate to generic or throw
                return Response.error(501, ResponseBody.create(null, "Not implemented directly"))
            }

            override suspend fun createPostJson(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBasePost> {
                val cleaned = body.toMutableMap()
                cleaned.remove("user_id_expandido")
                cleaned.remove("user_nombre_completo")
                cleaned.remove("user_foto_perfil")

                if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                    cleaned["id"] = "pst_" + java.util.UUID.randomUUID().toString().take(8)
                }

                // Map keys from UI model to Supabase DB columns
                if (cleaned.containsKey("nombre")) {
                    cleaned["nombre_mascota"] = cleaned["nombre"] ?: ""
                    cleaned.remove("nombre")
                }
                if (cleaned.containsKey("lat")) {
                    cleaned["ubicacion_lat"] = cleaned["lat"] ?: 0.0
                    cleaned.remove("lat")
                }
                if (cleaned.containsKey("lng")) {
                    cleaned["ubicacion_lng"] = cleaned["lng"] ?: 0.0
                    cleaned.remove("lng")
                }

                val res = ApiClient.getService().createPost(cleaned)
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    return Response.success(res.body()!!.first().toPocketBasePost())
                } else {
                    val errorMsg = res.errorBody()?.string() ?: "Create post failed with code ${res.code()}"
                    throw Exception(errorMsg)
                }
            }

            override suspend fun createPostMultipart(
                token: String,
                partMap: Map<String, RequestBody>,
                fotos: List<MultipartBody.Part>?
            ): Response<PocketBasePost> {
                return Response.error(501, ResponseBody.create(null, "Multipart not supported on direct Supabase REST"))
            }

            override suspend fun updatePost(
                token: String,
                id: String,
                body: Map<String, Any>
            ): Response<PocketBasePost> {
                val cleaned = body.toMutableMap()
                cleaned.remove("user_id_expandido")
                cleaned.remove("user_nombre_completo")
                cleaned.remove("user_foto_perfil")

                // Map keys from UI model to Supabase DB columns
                if (cleaned.containsKey("nombre")) {
                    cleaned["nombre_mascota"] = cleaned["nombre"] ?: ""
                    cleaned.remove("nombre")
                }
                if (cleaned.containsKey("lat")) {
                    cleaned["ubicacion_lat"] = cleaned["lat"] ?: 0.0
                    cleaned.remove("lat")
                }
                if (cleaned.containsKey("lng")) {
                    cleaned["ubicacion_lng"] = cleaned["lng"] ?: 0.0
                    cleaned.remove("lng")
                }

                val res = ApiClient.getService().updatePost("eq.$id", cleaned)
                if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                    return Response.success(res.body()!!.first().toPocketBasePost())
                } else {
                    val errorMsg = res.errorBody()?.string() ?: "Update post failed with code ${res.code()}"
                    throw Exception(errorMsg)
                }
            }

            override suspend fun deletePost(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deletePost("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete post failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getComments(
                filter: String?,
                sort: String,
                expand: String?
            ): Response<PocketBaseListResponse<PocketBaseComment>> {
                try {
                    val postIdQuery = if (!filter.isNullOrBlank() && filter.contains("post_id=")) {
                        val parsedId = filter.substringAfter("post_id=").substringBefore("&&").trim('\'', '"', ' ')
                        "eq.$parsedId"
                    } else if (!filter.isNullOrBlank() && filter.contains("post_id='")) {
                        val parsedId = filter.substringAfter("post_id='").substringBefore("'").trim()
                        "eq.$parsedId"
                    } else null

                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"

                    var res = try {
                        ApiClient.getService().getComments(
                            select = "*,users(nombre,foto_perfil)",
                            postIdQuery = postIdQuery,
                            order = order
                        )
                    } catch (e: Exception) {
                        Log.e("PocketBaseClient", "getComments with users join exception, falling back to simple select", e)
                        null
                    }

                    if (res == null || !res.isSuccessful) {
                        val errorCode = res?.code() ?: -1
                        val errorMsg = res?.errorBody()?.string() ?: "Exception"
                        Log.w("PocketBaseClient", "getComments with users join failed (code $errorCode: $errorMsg). Trying simple select.")
                        res = ApiClient.getService().getComments(
                            select = "*",
                            postIdQuery = postIdQuery,
                            order = order
                        )
                    }

                    val finalRes = res
                    return if (finalRes.isSuccessful && finalRes.body() != null) {
                        val items = finalRes.body()!!.map { it.toPocketBaseComment() }
                        Response.success(
                            PocketBaseListResponse(
                                page = 1,
                                perPage = items.size,
                                totalItems = items.size,
                                totalPages = 1,
                                items = items
                            )
                        )
                    } else {
                        Response.error(finalRes.code(), finalRes.errorBody() ?: ResponseBody.create(null, "Get comments failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createComment(
                token: String,
                body: Map<String, String>
            ): Response<PocketBaseComment> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "cmt_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createComment(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first().toPocketBaseComment())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create comment failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun deleteComment(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deleteComment("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete comment failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getLikes(filter: String?): Response<PocketBaseListResponse<PocketBaseLike>> {
                try {
                    var postIdQuery: String? = null
                    var userIdQuery: String? = null
                    if (!filter.isNullOrBlank()) {
                        if (filter.contains("post_id=")) {
                            val parsedId = filter.substringAfter("post_id=").substringBefore("&&").trim('\'', '"', ' ')
                            postIdQuery = "eq.$parsedId"
                        } else if (filter.contains("post_id='")) {
                            val parsedId = filter.substringAfter("post_id='").substringBefore("'").trim()
                            postIdQuery = "eq.$parsedId"
                        }

                        if (filter.contains("user_id=")) {
                            val parsedId = filter.substringAfter("user_id=").substringBefore("&&").trim('\'', '"', ' ')
                            userIdQuery = "eq.$parsedId"
                        } else if (filter.contains("user_id='")) {
                            val parsedId = filter.substringAfter("user_id='").substringBefore("'").trim()
                            userIdQuery = "eq.$parsedId"
                        }
                    }

                    val res = ApiClient.getService().getLikes(postIdQuery, userIdQuery)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get likes failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createLike(
                token: String,
                body: Map<String, String>
            ): Response<PocketBaseLike> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "lik_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createLike(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create like failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun deleteLike(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deleteLike("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete like failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getVeterinarias(
                sort: String,
                filter: String?
            ): Response<PocketBaseListResponse<PocketBaseVeterinaria>> {
                try {
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getVeterinarias(order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get vets failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createVeterinaria(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseVeterinaria> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "vet_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createVeterinaria(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create vet failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getCampaigns(sort: String): Response<PocketBaseListResponse<PocketBaseCampaign>> {
                try {
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getCampaigns(order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get campaigns failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createCampaign(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseCampaign> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "cmp_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createCampaign(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create campaign failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getNews(sort: String): Response<PocketBaseListResponse<PocketBaseNews>> {
                try {
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getNews(order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get news failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createNews(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseNews> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "nws_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createNews(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create news failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getUserStatuses(sort: String): Response<PocketBaseListResponse<PocketBaseUserStatus>> {
                try {
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getUserStatuses(order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get user statuses failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createUserStatus(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseUserStatus> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "ust_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createUserStatus(cleaned)
                    return if (res.isSuccessful) {
                        val item = res.body()?.firstOrNull() ?: PocketBaseUserStatus(
                            id = cleaned["id"].toString(),
                            userId = cleaned["user_id"].toString(),
                            texto = cleaned["texto"].toString(),
                            likesCount = 0,
                            created = ""
                        )
                        Response.success(item)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create status failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getStatusLikes(filter: String?): Response<PocketBaseListResponse<PocketBaseStatusLike>> {
                try {
                    var statusIdQuery: String? = null
                    var userIdQuery: String? = null
                    if (!filter.isNullOrBlank()) {
                        if (filter.contains("status_id=")) {
                            val parsedId = filter.substringAfter("status_id=").substringBefore("&&").trim('\'', '"', ' ')
                            statusIdQuery = "eq.$parsedId"
                        } else if (filter.contains("status_id='")) {
                            val parsedId = filter.substringAfter("status_id='").substringBefore("'").trim()
                            statusIdQuery = "eq.$parsedId"
                        }

                        if (filter.contains("user_id=")) {
                            val parsedId = filter.substringAfter("user_id=").substringBefore("&&").trim('\'', '"', ' ')
                            userIdQuery = "eq.$parsedId"
                        } else if (filter.contains("user_id='")) {
                            val parsedId = filter.substringAfter("user_id='").substringBefore("'").trim()
                            userIdQuery = "eq.$parsedId"
                        }
                    }

                    val res = ApiClient.getService().getStatusLikes(statusIdQuery, userIdQuery)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get status likes failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createStatusLike(
                token: String,
                body: Map<String, String>
            ): Response<PocketBaseStatusLike> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "slk_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createStatusLike(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create status like failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun deleteStatusLike(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deleteStatusLike("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete status like failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getReports(token: String): Response<PocketBaseListResponse<PocketBaseReport>> {
                try {
                    val res = ApiClient.getService().getReports()
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get reports failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createReport(
                token: String,
                body: Map<String, String>
            ): Response<PocketBaseReport> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "rep_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createReport(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create report failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun updateReport(
                token: String,
                id: String,
                body: Map<String, String>
            ): Response<PocketBaseReport> {
                try {
                    val res = ApiClient.getService().updateReport("eq.$id", body)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Update report failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getProfileReports(token: String): Response<PocketBaseListResponse<PocketBaseProfileReport>> {
                try {
                    val res = ApiClient.getService().getProfileReports()
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get profile reports failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createProfileReport(
                token: String,
                body: Map<String, String>
            ): Response<PocketBaseProfileReport> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "prp_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createProfileReport(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create profile report failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun updateProfileReport(
                token: String,
                id: String,
                body: Map<String, String>
            ): Response<PocketBaseProfileReport> {
                try {
                    val res = ApiClient.getService().updateProfileReport("eq.$id", body)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Update profile report failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getNotifications(
                token: String,
                filter: String?,
                sort: String
            ): Response<PocketBaseListResponse<PocketBaseNotification>> {
                try {
                    var userIdQuery: String? = null
                    if (!filter.isNullOrBlank()) {
                        if (filter.contains("user_id=")) {
                            val parsedId = filter.substringAfter("user_id=").substringBefore("&&").trim('\'', '"', ' ')
                            userIdQuery = "eq.$parsedId"
                        } else if (filter.contains("user_id='")) {
                            val parsedId = filter.substringAfter("user_id='").substringBefore("'").trim()
                            userIdQuery = "eq.$parsedId"
                        }
                    }

                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getNotifications(userIdQuery, order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get notifications failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createNotification(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseNotification> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "ntf_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createNotification(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create notification failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun updateNotification(
                token: String,
                id: String,
                body: Map<String, Any>
            ): Response<PocketBaseNotification> {
                try {
                    val res = ApiClient.getService().updateNotification("eq.$id", body)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Update notification failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getColaboradores(sort: String): Response<PocketBaseListResponse<PocketBaseColaborador>> {
                try {
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getColaboradores(order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get colaboradores failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createColaborador(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseColaborador> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "col_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createColaborador(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create colaborador failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun updateColaborador(
                token: String,
                id: String,
                body: Map<String, Any>
            ): Response<PocketBaseColaborador> {
                try {
                    val res = ApiClient.getService().updateColaborador("eq.$id", body)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Update colaborador failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun deleteColaborador(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deleteColaborador("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete colaborador failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getBlocks(filter: String?): Response<PocketBaseListResponse<PocketBaseUserBlock>> {
                try {
                    var userIdQuery: String? = null
                    if (!filter.isNullOrBlank()) {
                        if (filter.contains("user_id=")) {
                            val parsedId = filter.substringAfter("user_id=").substringBefore("&&").trim('\'', '"', ' ')
                            userIdQuery = "eq.$parsedId"
                        } else if (filter.contains("user_id='")) {
                            val parsedId = filter.substringAfter("user_id='").substringBefore("'").trim()
                            userIdQuery = "eq.$parsedId"
                        }
                    }

                    val res = ApiClient.getService().getBlocks(userIdQuery)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get blocks failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createBlock(
                token: String,
                body: Map<String, String>
            ): Response<PocketBaseUserBlock> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "blk_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createBlock(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create block failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun deleteBlock(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deleteBlock("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete block failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getAdminActions(
                token: String,
                sort: String
            ): Response<PocketBaseListResponse<PocketBaseAdminAction>> {
                try {
                    val order = if (sort.startsWith("-")) "${sort.substring(1)}.desc" else "$sort.asc"
                    val res = ApiClient.getService().getAdminActions(order)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get admin actions failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createAdminAction(
                token: String,
                body: Map<String, Any>
            ): Response<PocketBaseAdminAction> {
                try {
                    val cleaned = body.toMutableMap()
                    if (cleaned["id"] == null || cleaned["id"].toString().isBlank()) {
                        cleaned["id"] = "adm_" + java.util.UUID.randomUUID().toString().take(8)
                    }
                    val res = ApiClient.getService().createAdminAction(cleaned)
                    return if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                        Response.success(res.body()!!.first())
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create admin action failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun getFollows(filter: String?): Response<PocketBaseListResponse<PocketBaseUserFollow>> {
                try {
                    val followerIdQuery = filter?.let {
                        if (it.contains("follower_id=")) "eq." + it.substringAfter("follower_id=").replace("'", "")
                        else null
                    }
                    val res = ApiClient.getService().getFollows(followerIdQuery)
                    return if (res.isSuccessful && res.body() != null) {
                        val list = res.body()!!
                        Response.success(PocketBaseListResponse(1, list.size, list.size, 1, list))
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Get follows failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun createFollow(token: String, body: Map<String, String>): Response<PocketBaseUserFollow> {
                try {
                    val cleaned = body.toMutableMap()
                    val res = ApiClient.getService().createFollow(cleaned)
                    return if (res.isSuccessful) {
                        val item = res.body()?.firstOrNull() ?: PocketBaseUserFollow(
                            id = cleaned["id"] ?: "",
                            followerId = cleaned["follower_id"] ?: "",
                            followingId = cleaned["following_id"] ?: ""
                        )
                        Response.success(item)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Create follow failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }

            override suspend fun deleteFollow(token: String, id: String): Response<Unit> {
                try {
                    val res = ApiClient.getService().deleteFollow("eq.$id")
                    return if (res.isSuccessful) {
                        Response.success(Unit)
                    } else {
                        Response.error(res.code(), res.errorBody() ?: ResponseBody.create(null, "Delete follow failed"))
                    }
                } catch (e: Exception) {
                    return Response.error(500, ResponseBody.create(null, e.message ?: "Exception"))
                }
            }
        }
    }
}
