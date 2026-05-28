package com.signalone.app.ui

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.signalone.app.R
import kotlin.math.sqrt

class PanicService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var acelerometro: Sensor? = null
    private var ultimoAgite = 0L
    private val UMBRAL_AGITE = 12f
    private val COOLDOWN_AGITE = 4000L

    companion object {
        const val CHANNEL_ID   = "signalone_panic"
        const val NOTIF_ID     = 1001
        const val ACTION_PANIC = "com.signalone.app.PANIC_FROM_SERVICE"

        fun start(context: Context) {
            val intent = Intent(context, PanicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(intent)
            else
                context.startService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PanicService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()
        startForeground(NOTIF_ID, buildNotificacion())

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        acelerometro  = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        acelerometro?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Detectar agite en segundo plano ────────────────────────────────────
    override fun onSensorChanged(event: SensorEvent?) {
        if (!AppState.agitarActivo) return
        if (event?.sensor?.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
        val fuerza = sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH
        if (fuerza > UMBRAL_AGITE) {
            val ahora = SystemClock.elapsedRealtime()
            if (ahora - ultimoAgite > COOLDOWN_AGITE) {
                ultimoAgite = ahora
                activarPanicoDesdeServicio("Agitar (pantalla bloqueada)")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ── Activar pánico desde segundo plano ─────────────────────────────────
    fun activarPanicoDesdeServicio(origen: String) {
        AppState.origenAlertaActual = origen
        vibrar()
        obtenerUbicacionYEnviar()
    }

    private fun vibrar() {
        val vib = getSystemService(VIBRATOR_SERVICE) as? Vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            vib.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400), -1))
        else
            @Suppress("DEPRECATION") vib.vibrate(longArrayOf(0, 400, 200, 400), -1)
    }

    private fun obtenerUbicacionYEnviar() {
        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(this)
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                val url = if (loc != null)
                    "https://maps.google.com/?q=${loc.latitude},${loc.longitude}" else null
                enviarAlertas(url)
            }.addOnFailureListener { enviarAlertas(null) }
        } catch (e: SecurityException) { enviarAlertas(null) }
    }

    private fun enviarAlertas(ubicacionUrl: String?) {
        AppState.ultimaUbicacionUrl = ubicacionUrl
        AppState.registrarAlerta(AppState.origenAlertaActual, "⚠", "#B91C1C", ubicacionUrl)

        val mensaje = if (ubicacionUrl != null)
            "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. Mi ubicación:\n$ubicacionUrl"
        else
            "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. (Ubicación no disponible)"

        // SMS
        try {
            val smsManager = android.telephony.SmsManager.getDefault()
            AppState.contactos.forEach { c ->
                try { smsManager.sendTextMessage(c.telefono, null, mensaje, null, null) }
                catch (e: Exception) {}
            }
        } catch (e: Exception) {}

        // WhatsApp a cada contacto
        AppState.contactos.forEach { c ->
            enviarWhatsApp(c.telefono, mensaje)
        }

        // Abrir AlertaActivaActivity desde background
        val intent = Intent(this, AlertaActivaActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun enviarWhatsApp(telefono: String, mensaje: String) {
        try {
            val numero = telefono.replace("[^0-9+]".toRegex(), "")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$numero&text=${android.net.Uri.encode(mensaje)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {}
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                CHANNEL_ID, "SignalOne Activo",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Monitoreo discreto activo" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
        }
    }

    private fun buildNotificacion(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, PrincipalActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SignalOne activo")
            .setContentText("Monitoreo discreto en segundo plano")
            .setSmallIcon(R.drawable.ic_shield)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
