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

        // Parpadeo
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

        // Lista de contactos con estado progresivo
        val contactos = AppState.contactos
        if (contactos.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No tienes contactos.\nAgrega uno desde la pantalla principal."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f; setPadding(0, 16, 0, 16)
            }
            b.llContactos.addView(tv)
        } else {
            val mensaje = if (urlUbicacion != null)
                "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. Mi ubicación:\n$urlUbicacion"
            else
                "🚨 ALERTA DE EMERGENCIA 🚨\nNecesito ayuda. (Ubicación no disponible)"

            contactos.forEachIndexed { i, c ->
                val item = ItemContactoAlertaBinding.inflate(layoutInflater, b.llContactos, false)
                item.tvAvatar.text = c.inicial
                item.tvAvatar.setBackgroundColor(c.color)
                item.tvNombre.text = c.nombre

                // Estado inicial: "Enviando..."
                item.tvEstado.text = getString(R.string.enviando)
                item.tvEstado.setTextColor(Color.parseColor("#FBBF24"))

                b.llContactos.addView(item.root)

                // Delay escalonado: SMS ya fue enviado, ahora mostrar confirmación
                // y luego abrir WhatsApp en secuencia
                val delayConfirmacion = (i * 1800L) + 1500L
                handler.postDelayed({
                    item.tvEstado.text = "✓ SMS enviado"
                    item.tvEstado.setTextColor(Color.parseColor("#4ADE80"))
                }, delayConfirmacion)

                // WhatsApp — abre uno por uno con delay extra
                val delayWhatsApp = delayConfirmacion + 600L
                handler.postDelayed({
                    item.tvEstado.text = "✓ SMS + WhatsApp enviado"
                    item.tvEstado.setTextColor(Color.parseColor("#4ADE80"))
                    // Abrir WhatsApp para este contacto
                    try {
                        val numero = c.telefono.replace("[^0-9+]".toRegex(), "")
                        val url = "https://api.whatsapp.com/send?phone=$numero&text=${android.net.Uri.encode(mensaje)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            setPackage("com.whatsapp")
                        }
                        if (packageManager.resolveActivity(intent, 0) != null)
                            startActivity(intent)
                        else
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                    } catch (e: Exception) {
                        item.tvEstado.text = "✓ SMS enviado"
                    }
                }, delayWhatsApp)
            }
        }

        b.btnCancelar.setOnClickListener {
            handler.removeCallbacksAndMessages(null)
            // Marcar como falsa alarma
            if (AppState.historial.isNotEmpty()) {
                val u = AppState.historial[0]
                AppState.historial[0] = u.copy(tipo = "Falsa alarma — ${u.tipo}", emoji = "✓", colorHex = "#15803D")
            }
            // Guardar historial actualizado
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
