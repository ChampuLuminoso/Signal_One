package com.signalone.app.ui

import android.content.Context
import android.graphics.Color
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object UserPreferences {

    private const val PREF_USER      = "signalone_user"
    private const val PREF_APP       = "signalone_app"
    private const val KEY_NOMBRE     = "nombre"
    private const val KEY_CORREO     = "correo"
    private const val KEY_PASS       = "password"
    private const val KEY_EXISTE     = "cuenta_registrada"
    private const val KEY_CONTACTOS  = "contactos_json"
    private const val KEY_HISTORIAL  = "historial_json"
    private const val KEY_VOL        = "discreto_volumen"
    private const val KEY_AGITAR     = "discreto_agitar"
    private const val KEY_BLOQUEADO  = "discreto_bloqueado"

    // ═══════════════════════════════════════════════════════════════════════
    // CUENTA
    // ═══════════════════════════════════════════════════════════════════════

    fun guardarCuenta(context: Context, nombre: String, correo: String, password: String) {
        context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE).edit {
            putString(KEY_NOMBRE, nombre)
            putString(KEY_CORREO, correo.lowercase().trim())
            putString(KEY_PASS, password)
            putBoolean(KEY_EXISTE, true)
        }
    }

    fun validarLogin(context: Context, correo: String, password: String): LoginResult {
        val p = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
        if (!p.getBoolean(KEY_EXISTE, false)) return LoginResult.SinCuenta
        val correoG = p.getString(KEY_CORREO, "") ?: ""
        val passG   = p.getString(KEY_PASS,   "") ?: ""
        return when {
            correo.lowercase().trim() != correoG -> LoginResult.CorreoInvalido
            password != passG                    -> LoginResult.PassInvalida
            else -> LoginResult.Exitoso(p.getString(KEY_NOMBRE, "Usuario") ?: "Usuario")
        }
    }

    fun hayCuentaRegistrada(context: Context) =
        context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE).getBoolean(KEY_EXISTE, false)

    fun getCorreoGuardado(context: Context) =
        context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE).getString(KEY_CORREO, "") ?: ""

    // ═══════════════════════════════════════════════════════════════════════
    // CONTACTOS
    // ═══════════════════════════════════════════════════════════════════════

    fun guardarContactos(context: Context, contactos: List<Contacto>) {
        val arr = JSONArray()
        contactos.forEach { c ->
            arr.put(JSONObject().apply {
                put("nombre",   c.nombre)
                put("telefono", c.telefono)
                put("color",    c.color)
            })
        }
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).edit {
            putString(KEY_CONTACTOS, arr.toString())
        }
    }

    fun cargarContactos(context: Context): MutableList<Contacto> {
        val json = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
            .getString(KEY_CONTACTOS, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Contacto(o.getString("nombre"), o.getString("telefono"), o.getInt("color"))
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HISTORIAL
    // ═══════════════════════════════════════════════════════════════════════

    fun guardarHistorial(context: Context, historial: List<AlertaHistorial>) {
        val arr = JSONArray()
        historial.take(50).forEach { h ->   // máximo 50 entradas
            arr.put(JSONObject().apply {
                put("tipo",       h.tipo)
                put("emoji",      h.emoji)
                put("colorHex",   h.colorHex)
                put("ubicacion",  h.ubicacionUrl ?: "")
                put("fecha",      h.fecha)
                put("contactos",  JSONArray(h.contactosNotificados))
            })
        }
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).edit {
            putString(KEY_HISTORIAL, arr.toString())
        }
    }

    fun cargarHistorial(context: Context): MutableList<AlertaHistorial> {
        val json = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
            .getString(KEY_HISTORIAL, null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val contactosArr = o.getJSONArray("contactos")
                val contactos = (0 until contactosArr.length()).map { contactosArr.getString(it) }
                AlertaHistorial(
                    tipo                = o.getString("tipo"),
                    emoji               = o.getString("emoji"),
                    colorHex            = o.getString("colorHex"),
                    ubicacionUrl        = o.getString("ubicacion").ifEmpty { null },
                    contactosNotificados = contactos,
                    fecha               = o.getString("fecha")
                )
            }.toMutableList()
        } catch (e: Exception) { mutableListOf() }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // MODO DISCRETO
    // ═══════════════════════════════════════════════════════════════════════

    fun guardarModoDiscreto(context: Context, volumen: Boolean, agitar: Boolean, bloqueado: Boolean) {
        context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE).edit {
            putBoolean(KEY_VOL,       volumen)
            putBoolean(KEY_AGITAR,    agitar)
            putBoolean(KEY_BLOQUEADO, bloqueado)
        }
    }

    fun cargarModoDiscreto(context: Context): Triple<Boolean, Boolean, Boolean> {
        val p = context.getSharedPreferences(PREF_APP, Context.MODE_PRIVATE)
        return Triple(
            p.getBoolean(KEY_VOL,       false),
            p.getBoolean(KEY_AGITAR,    false),
            p.getBoolean(KEY_BLOQUEADO, false)
        )
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LOGIN RESULT
    // ═══════════════════════════════════════════════════════════════════════

    sealed class LoginResult {
        data class Exitoso(val nombre: String) : LoginResult()
        object CorreoInvalido : LoginResult()
        object PassInvalida   : LoginResult()
        object SinCuenta      : LoginResult()
    }
}
