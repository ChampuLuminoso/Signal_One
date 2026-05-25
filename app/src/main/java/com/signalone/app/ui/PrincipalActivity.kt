package com.signalone.app.ui

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.SystemClock
import android.telephony.SmsManager
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.signalone.app.R
import com.signalone.app.databinding.ActivityPrincipalBinding
import kotlin.math.sqrt

class PrincipalActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var b: ActivityPrincipalBinding

    // ── Acelerómetro ───────────────────────────────────────────────────────
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var ultimoAgite = 0L
    private val UMBRAL_AGITE = 12f
    private val COOLDOWN_AGITE = 3000L

    // ── Volumen x5 ─────────────────────────────────────────────────────────
    private var contadorVolumen = 0
    private var ultimoPressVolumen = 0L
    private val VENTANA_VOLUMEN = 2000L

    // ── Permiso ubicación ──────────────────────────────────────────────────
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) obtenerUbicacionYLanzarAlerta()
        else {
            Toast.makeText(this, "Alerta enviada sin ubicación (permiso denegado)", Toast.LENGTH_SHORT).show()
            lanzarAlerta(ubicacionUrl = null, origen = "Botón de pánico")
        }
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(b.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        acelerometro  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // Animación de pulso
        listOf("scaleX", "scaleY").forEach { prop ->
            ObjectAnimator.ofFloat(b.btnPanico, prop, 0.93f, 1.0f).apply {
                duration = 1600; repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }

        // Pedir permiso de ubicación proactivamente
        solicitarPermisoUbicacionSiNecesario()

        b.btnPanico.setOnClickListener { iniciarAlerta("Botón de pánico") }
        b.btnContactos.setOnClickListener { startActivity(Intent(this, ContactosActivity::class.java)) }
        b.btnHistorial.setOnClickListener { startActivity(Intent(this, HistorialActivity::class.java)) }
        b.btnDiscreto.setOnClickListener  { startActivity(Intent(this, ModoDiscretoActivity::class.java)) }
    }

    private fun solicitarPermisoUbicacionSiNecesario() {
        val tieneFine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
        val tieneCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!tieneFine && !tieneCoarse) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // ── Volumen x5 — solo si está activado en Modo Discreto ───────────────
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!AppState.volumenActivo) return super.onKeyDown(keyCode, event)

        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimoPressVolumen > VENTANA_VOLUMEN) contadorVolumen = 0
            contadorVolumen++
            ultimoPressVolumen = ahora
            if (contadorVolumen >= 5) {
                contadorVolumen = 0
                Toast.makeText(this, "🚨 Alerta activada por volumen", Toast.LENGTH_SHORT).show()
                iniciarAlerta("Volumen ×5")
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    // ── Agitar — solo si está activado en Modo Discreto ───────────────────
    override fun onSensorChanged(event: SensorEvent?) {
        if (!AppState.agitarActivo) return
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val fuerzaNeta = sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH
        if (fuerzaNeta > UMBRAL_AGITE) {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimoAgite > COOLDOWN_AGITE) {
                ultimoAgite = ahora
                Toast.makeText(this, "🚨 Alerta activada por agite", Toast.LENGTH_SHORT).show()
                iniciarAlerta("Agitar dispositivo")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun iniciarAlerta(origen: String) {
        val tieneFine   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)   == PackageManager.PERMISSION_GRANTED
        val tieneCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        AppState.origenAlertaActual = origen
        if (tieneFine || tieneCoarse) obtenerUbicacionYLanzarAlerta()
        else locationPermissionLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    private fun obtenerUbicacionYLanzarAlerta() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    lanzarAlerta("https://maps.google.com/?q=${location.latitude},${location.longitude}", AppState.origenAlertaActual)
                } else {
                    val request = com.google.android.gms.location.CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY).setDurationMillis(5000).build()
                    fusedClient.getCurrentLocation(request, null)
                        .addOnSuccessListener { loc ->
                            lanzarAlerta(if (loc != null) "https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else null, AppState.origenAlertaActual)
                        }
                        .addOnFailureListener { lanzarAlerta(null, AppState.origenAlertaActual) }
                }
            }.addOnFailureListener { lanzarAlerta(null, AppState.origenAlertaActual) }
        } catch (e: SecurityException) { lanzarAlerta(null, AppState.origenAlertaActual) }
    }

    private fun lanzarAlerta(ubicacionUrl: String?, origen: String) {
        AppState.ultimaUbicacionUrl = ubicacionUrl
        // Registrar en historial
        AppState.registrarAlerta(
            tipo      = origen,
            emoji     = "⚠",
            colorHex  = "#B91C1C",
            ubicacionUrl = ubicacionUrl
        )
        val contactos = AppState.contactos
        if (contactos.isNotEmpty()) {
            val mensaje = if (ubicacionUrl != null)
                "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. Mi ubicación:\n$ubicacionUrl"
            else
                "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. (Ubicación no disponible)"
            val smsManager = SmsManager.getDefault()
            contactos.forEach { c ->
                try { smsManager.sendTextMessage(c.telefono, null, mensaje, null, null) } catch (e: Exception) {}
            }
        }
        startActivity(Intent(this, AlertaActivaActivity::class.java))
    }

    override fun onResume() {
        super.onResume()
        acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

        val nombre = AppState.nombreUsuario
        b.tvNombreUsuario.text = nombre
        b.tvAvatar.text = if (nombre.isNotEmpty()) nombre[0].uppercaseChar().toString() else "U"

        // Banner e instrucción según estado de funciones discretas
        if (AppState.modoDiscretoActivo) {
            val partes = mutableListOf<String>()
            if (AppState.volumenActivo) partes.add("vol ×5")
            if (AppState.agitarActivo) partes.add("agitar")
            b.tvBannerTexto.text = "Modo discreto activo — ${partes.joinToString(" o ")} = alerta inmediata"
            b.tvInstruccion.visibility = View.VISIBLE
            val instrucciones = mutableListOf<String>()
            if (AppState.volumenActivo) instrucciones.add("botón de volumen ×5")
            if (AppState.agitarActivo)  instrucciones.add("agitar dispositivo")
            b.tvInstruccion.text = if (instrucciones.isNotEmpty())
                "o ${instrucciones.joinToString(" / ")}" else ""
        } else {
            b.tvBannerTexto.text = "Modo discreto inactivo"
            b.tvInstruccion.visibility = View.INVISIBLE  // ocultar instrucción si está apagado
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
