package com.signalone.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityRecuperarBinding

class RecuperarActivity : AppCompatActivity() {
    private lateinit var b: ActivityRecuperarBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityRecuperarBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.tvVolver.setOnClickListener { finish() }

        b.btnEnviar.setOnClickListener {
            val correoIngresado = b.etCorreo.text.toString().trim()

            if (correoIngresado.isEmpty()) {
                snack("Ingresa tu correo electrónico")
                return@setOnClickListener
            }

            val correoGuardado = UserPreferences.getCorreoGuardado(this)

            if (correoGuardado.isEmpty()) {
                snack("No hay cuenta registrada con ese correo")
            } else if (correoIngresado.lowercase() == correoGuardado) {
                // Correo coincide → mostrar confirmación
                b.layoutConfirm.visibility = View.VISIBLE
                snack("Correo verificado. En una app real se enviaría un enlace.")
            } else {
                snack("No encontramos una cuenta con ese correo")
            }
        }

        b.tvIrLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java)); finish()
        }
    }

    private fun snack(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
