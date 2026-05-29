package com.signalone.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityModoDiscretoBinding

class ModoDiscretoActivity : AppCompatActivity() {
    private lateinit var b: ActivityModoDiscretoBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityModoDiscretoBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvBack.setOnClickListener { finish() }

        // Cargar estado persistido
        val (vol, agitar, bloqueado) = UserPreferences.cargarModoDiscreto(this)
        AppState.volumenActivo   = vol
        AppState.agitarActivo    = agitar
        AppState.bloqueadoActivo = bloqueado

        b.swVolumen.isChecked   = vol
        b.swAgitar.isChecked    = agitar
        b.swBloqueada.isChecked = bloqueado

        b.swVolumen.setOnCheckedChangeListener { _, v ->
            AppState.volumenActivo = v
            persistir()
            Toast.makeText(this,
                if (v) "✅ Botón de volumen ×5 activado" else "⭕ Botón de volumen desactivado",
                Toast.LENGTH_SHORT).show()
        }
        b.swAgitar.setOnCheckedChangeListener { _, v ->
            AppState.agitarActivo = v
            persistir()
            Toast.makeText(this,
                if (v) "✅ Agitar dispositivo activado" else "⭕ Agitar desactivado",
                Toast.LENGTH_SHORT).show()
        }
        b.swBloqueada.setOnCheckedChangeListener { _, v ->
            AppState.bloqueadoActivo = v
            persistir()
            Toast.makeText(this,
                if (v) "✅ Pantalla bloqueada activada" else "⭕ Pantalla bloqueada desactivada",
                Toast.LENGTH_SHORT).show()
        }
    }

private fun persistir() {
    UserPreferences.guardarModoDiscreto(
        this,
        AppState.volumenActivo,
        AppState.agitarActivo,
        AppState.bloqueadoActivo
    )
    // Iniciar o detener el servicio según el estado
    if (AppState.agitarActivo || AppState.bloqueadoActivo) {
        PanicService.start(this)
    } else {
        PanicService.stop(this)
    }
}
}
