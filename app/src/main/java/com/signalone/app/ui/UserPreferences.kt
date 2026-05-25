package com.signalone.app.ui

import android.content.Context
import androidx.core.content.edit

/**
 * Capa de persistencia local usando SharedPreferences.
 * Guarda los datos del usuario registrado mientras la app esté instalada.
 * No requiere backend ni internet.
 */
object UserPreferences {

    private const val PREF_NAME   = "signalone_user"
    private const val KEY_NOMBRE  = "nombre"
    private const val KEY_CORREO  = "correo"
    private const val KEY_PASS    = "password"
    private const val KEY_EXISTE  = "cuenta_registrada"

    // ── Guardar cuenta al registrarse ──────────────────────────────────────
    fun guardarCuenta(context: Context, nombre: String, correo: String, password: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_NOMBRE, nombre)
            putString(KEY_CORREO, correo.lowercase().trim())
            putString(KEY_PASS,   password)
            putBoolean(KEY_EXISTE, true)
        }
    }

    // ── Validar credenciales al hacer login ────────────────────────────────
    fun validarLogin(context: Context, correo: String, password: String): LoginResult {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (!prefs.getBoolean(KEY_EXISTE, false)) {
            return LoginResult.SinCuenta
        }

        val correoGuardado = prefs.getString(KEY_CORREO, "") ?: ""
        val passGuardada   = prefs.getString(KEY_PASS,   "") ?: ""

        return when {
            correo.lowercase().trim() != correoGuardado -> LoginResult.CorreoInvalido
            password != passGuardada                    -> LoginResult.PassInvalida
            else -> {
                val nombre = prefs.getString(KEY_NOMBRE, "Usuario") ?: "Usuario"
                LoginResult.Exitoso(nombre)
            }
        }
    }

    // ── Verificar si ya hay cuenta registrada ──────────────────────────────
    fun hayCuentaRegistrada(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_EXISTE, false)
    }

    // ── Obtener correo guardado (para pantalla de recuperar) ───────────────
    fun getCorreoGuardado(context: Context): String {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CORREO, "") ?: ""
    }

    // ── Resultados posibles del login ──────────────────────────────────────
    sealed class LoginResult {
        data class Exitoso(val nombre: String) : LoginResult()
        object CorreoInvalido : LoginResult()
        object PassInvalida   : LoginResult()
        object SinCuenta      : LoginResult()
    }
}
