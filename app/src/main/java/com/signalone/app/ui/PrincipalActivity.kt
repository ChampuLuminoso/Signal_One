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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.telephony.SmsManager
import android.view.KeyEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.signalone.app.R
import com.signalone.app.databinding.ActivityPrincipalBinding
import kotlin.math.sqrt

class PrincipalActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var b: ActivityPrincipalBinding
    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var ultimoAgite = 0L
    private val UMBRAL_AGITE = 12f
    private val COOLDOWN_AGITE = 3000L
    private var contadorVolumen = 0
    private var ultimoPressVolumen = 0L
    private val VENTANA_VOLUMEN = 2000L

    private val locationPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                 perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) { verificarUbicacionSistema(); obtenerUbicacionYLanzarAlerta() }
        else    { lanzarAlerta(null, AppState.origenAlertaActual) }
    }

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()) {}
    private val smsPermLauncher = registerForActivityResult(
    ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityPrincipalBinding.inflate(layoutInflater)
        setContentView(b.root)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        acelerometro  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        listOf("scaleX", "scaleY").forEach { prop ->
            ObjectAnimator.ofFloat(b.btnPanico, prop, 0.93f, 1.0f).apply {
                duration = 1600; repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
            }.start()
        }

        pedirPermisosNecesarios()

        b.btnPanico.setOnClickListener    { iniciarAlerta("Botón de pánico") }
        b.btnContactos.setOnClickListener { startActivity(Intent(this, ContactosActivity::class.java)) }
        b.btnHistorial.setOnClickListener { startActivity(Intent(this, HistorialActivity::class.java)) }
        b.btnDiscreto.setOnClickListener  { startActivity(Intent(this, ModoDiscretoActivity::class.java)) }
        b.tvCerrarSesion.setOnClickListener {com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                            UserPreferences.guardarSesionActiva(this, false)
                                            startActivity(
                                                  Intent(this, BienvenidaActivity::class.java)
                                                 .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                         )
                                            }
    }

private fun pedirPermisosNecesarios() {
    if (!tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION) &&
        !tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION)) {
        locationPermLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION))
    } else {
        verificarUbicacionSistema()
    }

    if (!tienePermiso(Manifest.permission.SEND_SMS)) {
        smsPermLauncher.launch(Manifest.permission.SEND_SMS)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !tienePermiso(Manifest.permission.POST_NOTIFICATIONS)) {
        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

    private fun verificarUbicacionSistema() {
        val lm = getSystemService(LOCATION_SERVICE) as android.location.LocationManager
        val activo = lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                     lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        if (!activo) {
            AlertDialog.Builder(this)
                .setTitle("Ubicación desactivada")
                .setMessage("Activa la ubicación para que SignalOne pueda enviar tus coordenadas en una emergencia.")
                .setPositiveButton("Abrir ajustes") { _, _ ->
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton("Ahora no", null).show()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (!AppState.volumenActivo) return super.onKeyDown(keyCode, event)
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimoPressVolumen > VENTANA_VOLUMEN) contadorVolumen = 0
            contadorVolumen++; ultimoPressVolumen = ahora
            if (contadorVolumen >= 5) { contadorVolumen = 0; iniciarAlerta("Volumen ×5") }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!AppState.agitarActivo) return
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val fuerza = sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH
        if (fuerza > UMBRAL_AGITE) {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimoAgite > COOLDOWN_AGITE) {
                ultimoAgite = ahora; iniciarAlerta("Agitar dispositivo")
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun iniciarAlerta(origen: String) {
        AppState.origenAlertaActual = origen
        if (tienePermiso(Manifest.permission.ACCESS_FINE_LOCATION) ||
            tienePermiso(Manifest.permission.ACCESS_COARSE_LOCATION))
            obtenerUbicacionYLanzarAlerta()
        else
            locationPermLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    private fun obtenerUbicacionYLanzarAlerta() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null)
                    lanzarAlerta("https://maps.google.com/?q=${loc.latitude},${loc.longitude}", AppState.origenAlertaActual)
                else {
                    val req = com.google.android.gms.location.CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY).setDurationMillis(5000).build()
                    fusedClient.getCurrentLocation(req, null)
                        .addOnSuccessListener { l ->
                            lanzarAlerta(if (l != null) "https://maps.google.com/?q=${l.latitude},${l.longitude}" else null, AppState.origenAlertaActual)
                        }
                        .addOnFailureListener { lanzarAlerta(null, AppState.origenAlertaActual) }
                }
            }.addOnFailureListener { lanzarAlerta(null, AppState.origenAlertaActual) }
        } catch (e: SecurityException) { lanzarAlerta(null, AppState.origenAlertaActual) }
    }

private fun lanzarAlerta(ubicacionUrl: String?, origen: String) {
    AppState.ultimaUbicacionUrl = ubicacionUrl
    AppState.registrarAlerta(origen, "⚠", "#B91C1C", ubicacionUrl)
    UserPreferences.guardarHistorial(this, AppState.historial)

    val mensaje = if (ubicacionUrl != null)
        "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. Mi ubicación:\n$ubicacionUrl"
    else
        "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. (Ubicación no disponible)"

    // SMS
    if (tienePermiso(Manifest.permission.SEND_SMS)) {
        val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            getSystemService(SmsManager::class.java)
        else
            @Suppress("DEPRECATION") SmsManager.getDefault()

        AppState.contactos.forEach { c ->
            val telefonoLimpio = c.telefono.replace(Regex("[^\\d+]"), "")
            try {
                smsManager?.sendTextMessage(telefonoLimpio, null, mensaje, null, null)
            } catch (e: Exception) {
                Toast.makeText(this, "Error SMS: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    } else {
        Toast.makeText(this, "Sin permiso SMS", Toast.LENGTH_LONG).show()
    }

    // WhatsApp
    AppState.contactos.forEach { c ->
        try {
            val numero = c.telefono.replace(Regex("[^\\d+]"), "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp no instalado, SMS fue enviado igual
        }
    }

    startActivity(Intent(this, AlertaActivaActivity::class.java))
}

    private fun tienePermiso(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

 override fun onResume() {
    super.onResume()

    AppState.contactos.clear()
    AppState.contactos.addAll(UserPreferences.cargarContactos(this))

    acelerometro?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }

    if (AppState.modoDiscretoActivo && AppState.bloqueadoActivo)
        PanicService.start(this)
    else if (!AppState.modoDiscretoActivo)
        PanicService.stop(this)

    val nombre = AppState.nombreUsuario
    b.tvNombreUsuario.text = nombre
    b.tvAvatar.text = if (nombre.isNotEmpty()) nombre[0].uppercaseChar().toString() else "U"

    if (AppState.modoDiscretoActivo) {
        val partes = mutableListOf<String>()
        if (AppState.volumenActivo) partes.add("vol ×5")
        if (AppState.agitarActivo)  partes.add("agitar")
        b.tvBannerTexto.text = "Modo discreto activo — ${partes.joinToString(" o ")} = alerta inmediata"
        b.tvInstruccion.visibility = View.VISIBLE
        val instrucciones = mutableListOf<String>()
        if (AppState.volumenActivo) instrucciones.add("botón de volumen ×5")
        if (AppState.agitarActivo)  instrucciones.add("agitar dispositivo")
        b.tvInstruccion.text = if (instrucciones.isNotEmpty()) "o ${instrucciones.joinToString(" / ")}" else ""
    } else {
        b.tvBannerTexto.text = "Modo discreto inactivo"
        b.tvInstruccion.visibility = View.INVISIBLE
    }
}

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }
}
