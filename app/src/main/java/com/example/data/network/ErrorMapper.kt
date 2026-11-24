package com.example.data.network

import org.json.JSONObject
import android.util.Log

object ErrorMapper {

    /**
     * Mapea un mensaje de error técnico o respuesta HTTP fallida a un mensaje amigable en español.
     */
    fun map(error: String?, defaultMessage: String = "Ha ocurrido un error inesperado. Por favor, intenta de nuevo."): String {
        if (error.isNullOrBlank()) return defaultMessage

        Log.d("ErrorMapper", "Mapeando error original: $error")

        // Intenta analizar si viene en formato JSON
        try {
            if (error.trim().startsWith("{")) {
                val json = JSONObject(error)
                if (json.has("message")) {
                    val msg = json.getString("message")
                    return translate(msg)
                }
                if (json.has("error")) {
                    val err = json.getString("error")
                    return translate(err)
                }
                if (json.has("error_description")) {
                    val desc = json.getString("error_description")
                    return translate(desc)
                }
            }
        } catch (e: Exception) {
            // No es un JSON válido o falló el parseo, procedemos con traducción basada en subcadenas
        }

        return translate(error)
    }

    private fun translate(raw: String): String {
        val lower = raw.lowercase()
        return when {
            // Autenticación y Registro
            lower.contains("user_already_exists") || lower.contains("user already exists") || lower.contains("already registered") || lower.contains("422") -> {
                "Este correo electrónico ya está registrado. Intenta iniciar sesión."
            }
            lower.contains("invalid_credentials") || lower.contains("invalid login credentials") || lower.contains("correo o contraseña incorrectos") || lower.contains("not found") && lower.contains("user") -> {
                "Correo o contraseña incorrectos. Verifica tus datos e intenta de nuevo."
            }
            lower.contains("invalid_email") || lower.contains("invalid email") || lower.contains("email must be a valid email") -> {
                "Por favor, ingresa un correo electrónico válido."
            }
            lower.contains("weak_password") || lower.contains("should be at least") || lower.contains("password") && lower.contains("short") || lower.contains("password must be") -> {
                "La contraseña debe tener al menos 6 caracteres."
            }
            lower.contains("network") || lower.contains("timeout") || lower.contains("connect") || lower.contains("unable to resolve host") || lower.contains("host") -> {
                "No pudimos conectar con el servidor. Verifica tu conexión a internet e intenta de nuevo."
            }
            lower.contains("token") || lower.contains("unauthorized") || lower.contains("session expired") -> {
                "Tu sesión ha expirado o no tienes permisos. Por favor, vuelve a iniciar sesión."
            }
            // Confirmación y verificación de correo
            lower.contains("confirmation") || lower.contains("confirm") || lower.contains("verification") || lower.contains("verified") || (lower.contains("email") && lower.contains("required")) -> {
                "¡Cuenta creada con éxito! Ya puedes iniciar sesión."
            }
            // Genéricos de Base de Datos / Operaciones
            lower.contains("unique constraint") || lower.contains("duplicate key") -> {
                "Este registro ya existe en el sistema."
            }
            (lower.contains("required") && !lower.contains("required value")) || lower.contains("missing field") || lower.contains("cannot be blank") || lower.contains("cannot be empty") -> {
                "Por favor, completa todos los campos requeridos."
            }
            else -> {
                // Si ya es un texto amigable en español hecho por nosotros o con formato limpio, mantenerlo
                if (raw.length < 100 && !raw.contains("{") && !raw.contains(":") && !raw.contains("http") && !raw.contains("java.")) {
                    raw
                } else {
                    "No pudimos conectar con el servidor o procesar la solicitud. Verifica tu conexión e intenta de nuevo."
                }
            }
        }
    }
}
