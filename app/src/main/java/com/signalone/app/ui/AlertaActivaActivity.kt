package com.signalone.app.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.R
import com.signalone.app.databinding.ActivityAlertaBinding
import com.signalone.app.databinding.ItemContactoAlertaBinding

class AlertaActivaActivity : AppCompatActivity() {
    private lateinit var b: ActivityAlertaBinding

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

        // ── Card de ubicación ──────────────────────────────────────────────
        val urlUbicacion = AppState.ultimaUbicacionUrl
        if (urlUbicacion != null) {
            // Mostrar link tappable en el card que ya existe en el XML
            b.tvUbicacionLink.text = "Ver mi ubicación en Maps →"
            b.tvUbicacionLink.setTextColor(Color.parseColor("#4ADE80"))
            b.cardUbicacion.setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlUbicacion)))
            }
        } else {
            // Sin ubicación disponible
            b.tvUbicacionLink.text = "Ubicación no disponible"
            b.tvUbicacionLink.setTextColor(Color.parseColor("#94A3B8"))
        }

        // ── Lista de contactos ─────────────────────────────────────────────
        val contactos = AppState.contactos
        if (contactos.isEmpty()) {
            val tv = TextView(this).apply {
                text = "No tienes contactos de confianza.\nAgrega uno desde la pantalla principal."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 13f
                setPadding(0, 16, 0, 16)
            }
            b.llContactos.addView(tv)
        } else {
            contactos.forEachIndexed { i, c ->
                val item = ItemContactoAlertaBinding.inflate(layoutInflater, b.llContactos, false)
                item.tvAvatar.text = c.inicial
                item.tvAvatar.setBackgroundColor(c.color)
                item.tvNombre.text = c.nombre
                val esUltimo = i == contactos.size - 1
                if (esUltimo) {
                    item.tvEstado.text = getString(R.string.enviando)
                    item.tvEstado.setTextColor(Color.parseColor("#FBBF24"))
                    item.tvEstado.postDelayed({
                        item.tvEstado.text = getString(R.string.enviado)
                        item.tvEstado.setTextColor(Color.parseColor("#4ADE80"))
                    }, 2000)
                } else {
                    item.tvEstado.text = getString(R.string.enviado)
                    item.tvEstado.setTextColor(Color.parseColor("#4ADE80"))
                }
                b.llContactos.addView(item.root)
            }
        }

        b.btnCancelar.setOnClickListener {
            // Marcar la última alerta como falsa alarma en el historial
            if (AppState.historial.isNotEmpty()) {
                val ultima = AppState.historial[0]
                AppState.historial[0] = ultima.copy(tipo = "Falsa alarma — ${ultima.tipo}", emoji = "✓", colorHex = "#15803D")
            }
            AppState.ultimaUbicacionUrl = null
            startActivity(
                Intent(this, PrincipalActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
    }
}
