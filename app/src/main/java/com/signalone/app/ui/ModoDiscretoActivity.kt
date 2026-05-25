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

        // Cargar estado actual (por defecto apagados)
        b.swVolumen.isChecked   = AppState.volumenActivo
        b.swAgitar.isChecked    = AppState.agitarActivo
        b.swBloqueada.isChecked = AppState.bloqueadoActivo

        b.swVolumen.setOnCheckedChangeListener { _, v ->
            AppState.volumenActivo = v
            Toast.makeText(this,
                if (v) "✅ Botón de volumen ×5 activado" else "⭕ Botón de volumen desactivado",
                Toast.LENGTH_SHORT).show()
        }
        b.swAgitar.setOnCheckedChangeListener { _, v ->
            AppState.agitarActivo = v
            Toast.makeText(this,
                if (v) "✅ Agitar dispositivo activado" else "⭕ Agitar desactivado",
                Toast.LENGTH_SHORT).show()
        }
        b.swBloqueada.setOnCheckedChangeListener { _, v ->
            AppState.bloqueadoActivo = v
            Toast.makeText(this,
                if (v) "✅ Pantalla bloqueada activada" else "⭕ Pantalla bloqueada desactivada",
                Toast.LENGTH_SHORT).show()
        }
    }
}
