package com.signalone.app.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityHistorialBinding

class HistorialActivity : AppCompatActivity() {
    private lateinit var b: ActivityHistorialBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityHistorialBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvBack.setOnClickListener { finish() }

        b.tvLimpiar.setOnClickListener {
            AppState.historial.clear()
            UserPreferences.guardarHistorial(this, AppState.historial)
            renderHistorial()
        }

        renderHistorial()
    }

    private fun renderHistorial() {
        b.llAlertas.removeAllViews()
        if (AppState.historial.isEmpty()) {
            b.llAlertas.addView(TextView(this).apply {
                text = "Sin alertas registradas"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 14f; gravity = Gravity.CENTER
                setPadding(0, 60, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            })
        } else {
            AppState.historial.forEach { b.llAlertas.addView(buildCard(it)) }
        }
    }

    private fun buildCard(a: AlertaHistorial): LinearLayout {
        val dp    = resources.displayMetrics.density
        val color = Color.parseColor(a.colorHex)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#1E293B"))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (14 * dp).toInt(); layoutParams = lp
            setPadding((14*dp).toInt(), (14*dp).toInt(), (14*dp).toInt(), (14*dp).toInt())
        }

        // Barra lateral de color
        row.addView(android.view.View(this).apply {
            setBackgroundColor(color)
            layoutParams = LinearLayout.LayoutParams((4*dp).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
        })

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = (12*dp).toInt(); layoutParams = lp
        }

        // Fila header: tipo + fecha
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        header.addView(TextView(this).apply {
            text = "${a.emoji} ${a.tipo}"
            setTextColor(Color.parseColor("#F1F5F9")); textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(this).apply {
            text = a.fecha; setTextColor(Color.parseColor("#94A3B8")); textSize = 11f
        })
        col.addView(header)

        // Contactos notificados
        col.addView(tvSecund("👥 ${a.contactosNotificados.joinToString(", ")}", 12f, dp))

        // Ubicación
        if (a.ubicacionUrl != null) {
            col.addView(TextView(this).apply {
                text = "📍 Ver ubicación en Maps →"
                setTextColor(Color.parseColor("#4ADE80")); textSize = 12f
                isClickable = true; isFocusable = true
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.topMargin = (4*dp).toInt(); layoutParams = lp
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(a.ubicacionUrl)))
                }
            })
        } else {
            col.addView(tvSecund("📍 Ubicación no disponible", 12f, dp))
        }

        row.addView(col)
        return row
    }

    private fun tvSecund(txt: String, size: Float, dp: Float) = TextView(this).apply {
        text = txt; setTextColor(Color.parseColor("#94A3B8")); textSize = size
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        lp.topMargin = (4*dp).toInt(); layoutParams = lp
    }
}
