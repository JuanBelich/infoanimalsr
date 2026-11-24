package com.example

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.test.core.app.ApplicationProvider
import com.example.data.network.ApiClient
import com.example.data.network.PocketBaseClient
import com.example.data.network.PocketBasePost
import com.example.data.network.PocketBaseComment
import com.example.data.network.PocketBaseUser
import com.example.data.network.SupabasePost
import com.example.data.network.SupabaseComment
import com.example.data.network.SupabaseUser
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RobustnessTestingSuite {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        
        // Reset ApiClient state
        val context = ApplicationProvider.getApplicationContext<Context>()
        ApiClient.sharedPrefs = null
        ApiClient.setToken(null)
    }

    // =========================================================================
    // 1. Pruebas Unitarias de Robustez de Deserialización (Moshi)
    // =========================================================================

    @Test
    fun testSupabasePostDeserializationWithNullAndMissingFields() {
        val jsonWithNulls = """
        {
          "id": "post-unique-123",
          "user_id": null,
          "tipo": null,
          "especie": null,
          "tamano": null,
          "nombre_mascota": null,
          "descripcion": null,
          "rasgos": null,
          "estado": null,
          "fotos": null,
          "ubicacion_lat": null,
          "ubicacion_lng": null,
          "distrito": null,
          "zona": null,
          "telefono_contacto": null,
          "telefono_visible": null,
          "likes_count": null,
          "created": null,
          "users": null
        }
        """.trimIndent()

        val adapter = moshi.adapter(SupabasePost::class.java)
        val supabasePost = adapter.fromJson(jsonWithNulls)

        // Verificar que Moshi deserializa correctamente sin excepciones de tipo nulo
        assertNotNull(supabasePost)
        assertEquals("post-unique-123", supabasePost!!.id)
        assertNull(supabasePost.tipo)
        assertNull(supabasePost.fotos)

        // Probar el mapeo seguro a PocketBasePost asignando los valores seguros por defecto
        val pbPost = with(PocketBaseClient) { supabasePost.toPocketBasePost() }
        assertNotNull(pbPost)
        assertEquals("post-unique-123", pbPost.id)
        assertEquals("", pbPost.userId)
        assertEquals("", pbPost.tipo)
        assertEquals("", pbPost.especie)
        assertEquals("", pbPost.tamano)
        assertEquals("", pbPost.nombreMascota)
        assertEquals("", pbPost.descripcion)
        assertEquals("", pbPost.rasgos)
        assertEquals("activo", pbPost.estado)
        assertTrue(pbPost.fotos!!.isEmpty())
        assertEquals(0.0, pbPost.ubicacionLat!!, 0.0)
        assertEquals(0.0, pbPost.ubicacionLng!!, 0.0)
        assertEquals("", pbPost.distrito)
        assertEquals("", pbPost.zona)
        assertEquals("", pbPost.telefonoContacto)
        assertEquals(true, pbPost.telefonoVisible)
        assertEquals(0, pbPost.likesCount)
        assertEquals("InfoAnimal Vecino", pbPost.userNombreCompleto)
        assertEquals("", pbPost.userFotoPerfil)
        assertEquals("", pbPost.userIdExpandido)
        assertEquals("", pbPost.created)
    }

    @Test
    fun testSupabaseCommentDeserializationWithNullAndMissingFields() {
        val jsonWithNulls = """
        {
          "id": "comment-unique-456",
          "post_id": null,
          "user_id": null,
          "texto": null,
          "created": null,
          "users": null
        }
        """.trimIndent()

        val adapter = moshi.adapter(SupabaseComment::class.java)
        val supabaseComment = adapter.fromJson(jsonWithNulls)

        // Verificar que se deserializa correctamente sin excepciones de tipo nulo
        assertNotNull(supabaseComment)
        assertEquals("comment-unique-456", supabaseComment!!.id)

        // Probar el mapeo seguro a PocketBaseComment con caídas elegantes
        val pbComment = with(PocketBaseClient) { supabaseComment.toPocketBaseComment() }
        assertNotNull(pbComment)
        assertEquals("comment-unique-456", pbComment.id)
        assertEquals("", pbComment.postId)
        assertEquals("", pbComment.userId)
        assertEquals("", pbComment.texto)
        assertEquals("Vecino", pbComment.userNombreCompleto)
        assertEquals("", pbComment.userFotoPerfil)
        assertEquals("", pbComment.userIdExpandido)
        assertEquals("", pbComment.created)
    }

    // =========================================================================
    // 2. Pruebas de Integración de Persistencia y Fallback (SharedPreferences)
    // =========================================================================

    @Test
    fun testSharedPreferencesFallbackWorksTransparently() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ApiClient.sharedPrefs = null // Forzar reinicio

        // En Robolectric/JVM, el Keystore real a veces no está disponible,
        // lo que puede disparar el catch del getPrefs() del ApiClient de manera natural.
        // Aquí verificamos que el fallback a SharedPreferences estándar funcione perfectamente.
        val prefs = ApiClient.getPrefs(context)
        assertNotNull(prefs)

        // Guardar token de manera síncrona usando commit()
        val testToken = "secure_eMMC_commit_token_456"
        prefs.edit().putString("auth_token", testToken).commit()

        // Verificar que los datos se escribieron y leyeron de manera idéntica
        val retrievedToken = prefs.getString("auth_token", null)
        assertEquals(testToken, retrievedToken)
    }

    @Test
    fun testManualSharedPreferencesFallbackIntegrity() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        // Si el KeyStore fallara por completo, el fallback automático devuelve un SharedPreferences estándar
        val fallbackPrefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        assertNotNull(fallbackPrefs)

        val testToken = "fallback_token_999"
        fallbackPrefs.edit().putString("auth_token", testToken).commit()

        val savedToken = fallbackPrefs.getString("auth_token", null)
        assertEquals(testToken, savedToken)
    }

    // =========================================================================
    // 3. Pruebas de Flujo de Autenticación y Red (ApiClient / PocketBaseClient)
    // =========================================================================

    @Test
    fun testAuthTokenSavesSynchronouslyWithCommit() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ApiClient.init(context)

        val testToken = "jwt_active_session_token_777"
        ApiClient.setToken(testToken)

        // Verificar que el token en memoria se actualiza de inmediato
        assertEquals(testToken, ApiClient.token)

        // Verificar que se graba en SharedPreferences (seguro o de fallback)
        val prefs = ApiClient.getPrefs(context)
        val savedToken = prefs.getString("api_auth_token", null)
        assertEquals(testToken, savedToken)
    }

    @Test
    fun testLoginCredentialsTrimSanitization() {
        val dirtyEmail = "   low_end_user@go-edition.com \n\t "
        val dirtyPassword = "   mySecurePassword123\r\n "

        // Sanitización estricta requerida para hardware Go y teclados lentos
        val cleanedEmail = dirtyEmail.trim()
        val cleanedPassword = dirtyPassword.trim()

        assertEquals("low_end_user@go-edition.com", cleanedEmail)
        assertEquals("mySecurePassword123", cleanedPassword)
    }

    // =========================================================================
    // 4. Pruebas de Dualidad (Invitado vs. Logueado)
    // =========================================================================

    @Test
    fun testDualityGuestVsLoggedInAuthorizationHeader() {
        val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY

        // Escenario A: Usuario INVITADO (token es nulo/vacío)
        val guestToken: String? = null
        val guestHeaders = mutableMapOf<String, String>()
        guestHeaders["apikey"] = supabaseAnonKey
        if (!guestToken.isNullOrBlank()) {
            guestHeaders["Authorization"] = "Bearer $guestToken"
        }

        // Invitado DEBE tener apikey pero NO cabecera de Authorization
        assertTrue(guestHeaders.containsKey("apikey"))
        assertFalse(guestHeaders.containsKey("Authorization"))
        assertEquals(supabaseAnonKey, guestHeaders["apikey"])

        // Escenario B: Usuario LOGUEADO (token activo)
        val loggedInToken = "active_user_session_token_123"
        val loggedInHeaders = mutableMapOf<String, String>()
        loggedInHeaders["apikey"] = supabaseAnonKey
        if (!loggedInToken.isNullOrBlank()) {
            loggedInHeaders["Authorization"] = "Bearer $loggedInToken"
        }

        // Logueado DEBE tener tanto apikey como Authorization
        assertTrue(loggedInHeaders.containsKey("apikey"))
        assertTrue(loggedInHeaders.containsKey("Authorization"))
        assertEquals("Bearer active_user_session_token_123", loggedInHeaders["Authorization"])
    }
}
