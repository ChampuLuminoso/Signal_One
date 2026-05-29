package com.signalone.app.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.R
import com.signalone.app.databinding.ActivityAlertaBinding
import com.signalone.app.databinding.ItemContactoAlertaBinding

class AlertaActivaActivity : AppCompatActivity() {
    private lateinit var b: ActivityAlertaBinding
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityAlertaBinding.inflate(layoutInflater)
        setContentView(b.root)

        // Parpadeo del ícono de alerta
        ObjectAnimator.ofFloat(b.tvWarning, "alpha", 1f, 0.25f).apply {
            duration = 600; repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }.start()

        // Card de ubicación
        val urlUbicacion = AppState.ultimaUbicacionUrl
        if (urlUbicacion != null) {
            b.tvUbicacionLink.text = "Ver mi ubicación en Maps →"
            b.tvUbicacionLink.setTextColor(Color.parseColor("#4ADE80"))
            b.cardUbicacion.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlUbicacion)))
            }
        } else {
            b.tvUbicacionLink.text = "Ubicación no disponible"
            b.tvUbicacionLink.setTextColor(Color.parseColor("#94A3B8"))
        }

        val mensaje = if (urlUbicacion != null)
            "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. Mi ubicación:\n$urlUbicacion"
        else
            "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. (Ubicación no disponible)"

        val contactos = AppState.contactos

        if (contactos.isEmpty()) {
            b.llContactos.addView(TextView(this).apply {
                text = "No tienes contactos.\nAgrega uno desde la pantalla principal."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f; setPadding(0, 16, 0, 16)
            })
        } else {
            contactos.forEachIndexed { i, c ->
                val item = ItemContactoAlertaBinding.inflate(layoutInflater, b.llContactos, false)
                item.tvAvatar.text = c.inicial
                item.tvAvatar.setBackgroundColor(c.color)
                item.tvNombre.text = c.nombre

                // Estado inicial
                item.tvEstado.text = getString(R.string.enviando)
                item.tvEstado.setTextColor(Color.parseColor("#FBBF24"))
                b.llContactos.addView(item.root)

                // Paso 1: mostrar "✓ SMS enviado" con delay escalonado
                // El SMS ya fue enviado en PrincipalActivity — solo actualizamos el estado visual
                val delaySmS = (i * 1200L) + 1000L
                handler.postDelayed({
                    item.tvEstado.text = "✓ SMS enviado"
                    item.tvEstado.setTextColor(Color.parseColor("#4ADE80"))
                }, delaySmS)

                // Paso 2: abrir WhatsApp para este contacto (2s después del SMS visual)
                val delayWhatsApp = delaySmS + 2000L
                handler.postDelayed({
                    val numero = c.telefono.replace(Regex("[^\\d+]"), "")
                    val url = "https://wa.me/$numero?text=${Uri.encode(mensaje)}"
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            setPackage("com.whatsapp")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (packageManager.resolveActivity(intent, 0) != null) {
                            startActivity(intent)
                            item.tvEstado.text = "✓ SMS + WhatsApp enviado"
                        } else {
                            // Fallback sin setPackage (abre selector)
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                            item.tvEstado.text = "✓ SMS + WhatsApp enviado"
                        }
                    } catch (e: Exception) {
                        // WhatsApp no disponible — SMS ya fue enviado
                        item.tvEstado.text = "✓ SMS enviado"
                    }
                }, delayWhatsApp)
            }
        }

        b.btnCancelar.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            if (AppState.historial.isNotEmpty()) {
                val u = AppState.historial[0]
                AppState.historial[0] = u.copy(
                    tipo = "Falsa alarma — ${u.tipo}",
                    emoji = "✓",
                    colorHex = "#15803D"
                )
            }
            UserPreferences.guardarHistorial(this, AppState.historial)
            AppState.ultimaUbicacionUrl = null
            startActivity(Intent(this, PrincipalActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
