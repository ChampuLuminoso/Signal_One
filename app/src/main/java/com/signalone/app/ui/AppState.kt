package com.signalone.app.ui

import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

data class Contacto(
    val nombre: String,
    val telefono: String,
    val color: Int
) {
    val inicial: String get() = nombre.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
}

data class AlertaHistorial(
    val tipo: String,
    val emoji: String,
    val colorHex: String,
    val ubicacionUrl: String?,
    val contactosNotificados: List<String>,
    val fecha: String
)

object AppState {
    val contactos = mutableListOf(
        Contacto("Mamá",        "+57 310 000 0001", Color.parseColor("#DC2626")),
        Contacto("Hermana",     "+57 311 000 0002", Color.parseColor("#059669")),
        Contacto("Amiga Laura", "+57 312 000 0003", Color.parseColor("#2563EB")),
    )

    val avatarColors = listOf(
        Color.parseColor("#DC2626"), Color.parseColor("#059669"),
        Color.parseColor("#2563EB"), Color.parseColor("#BE185D"),
        Color.parseColor("#7C3AED"), Color.parseColor("#B45309")
    )

    var nombreUsuario: String = "July Tatiana"

    // Funciones discretas — por defecto APAGADAS
    var volumenActivo: Boolean   = false
    var agitarActivo: Boolean    = false
    var bloqueadoActivo: Boolean = false

    val modoDiscretoActivo: Boolean
        get() = volumenActivo || agitarActivo || bloqueadoActivo

    var ultimaUbicacionUrl: String? = null
    var origenAlertaActual: String = "Botón de pánico"

    // Historial real de alertas
    val historial = mutableListOf<AlertaHistorial>()

    fun registrarAlerta(tipo: String, emoji: String, colorHex: String, ubicacionUrl: String?) {
        val fecha = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date())
        val contactosNombres = contactos.map { it.nombre }
        historial.add(0, AlertaHistorial(tipo, emoji, colorHex, ubicacionUrl, contactosNombres, fecha))
    }
}
