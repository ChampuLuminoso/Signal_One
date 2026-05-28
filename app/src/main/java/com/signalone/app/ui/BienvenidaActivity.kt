package com.signalone.app.ui

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityBienvenidaBinding

class BienvenidaActivity : AppCompatActivity() {
    private lateinit var b: ActivityBienvenidaBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityBienvenidaBinding.inflate(layoutInflater)
        setContentView(b.root)

        // ── Cargar todos los datos persistidos al arrancar ─────────────────
        cargarDatosPersistidos()

        val logo = SpannableString("SignalOne")
        logo.setSpan(ForegroundColorSpan(Color.parseColor("#B91C1C")), 6, 9, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        b.tvLogo.text = logo

        b.btnComenzar.setOnClickListener { startActivity(Intent(this, RegistroActivity::class.java)) }
        b.tvLoginLink.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
    }

    private fun cargarDatosPersistidos() {
        // Contactos guardados (si existen, reemplazar los defaults)
        val contactosGuardados = UserPreferences.cargarContactos(this)
        if (contactosGuardados.isNotEmpty()) {
            AppState.contactos.clear()
            AppState.contactos.addAll(contactosGuardados)
        }

        // Historial
        val historialGuardado = UserPreferences.cargarHistorial(this)
        AppState.historial.clear()
        AppState.historial.addAll(historialGuardado)

        // Modo discreto
        val (vol, agitar, bloqueado) = UserPreferences.cargarModoDiscreto(this)
        AppState.volumenActivo   = vol
        AppState.agitarActivo    = agitar
        AppState.bloqueadoActivo = bloqueado
    }
}
