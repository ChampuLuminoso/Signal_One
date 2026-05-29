package com.signalone.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.signalone.app.databinding.ActivityRecuperarBinding

class RecuperarActivity : AppCompatActivity() {
    private lateinit var b: ActivityRecuperarBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityRecuperarBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()

        b.btnEnviar.setOnClickListener {
            val correo = b.etCorreo.text.toString().trim()
            if (correo.isEmpty()) {
                snack("Ingresa tu correo")
                return@setOnClickListener
            }
            auth.sendPasswordResetEmail(correo)
                .addOnSuccessListener {
                    snack("Correo de recuperación enviado")
                    finish()
                }
                .addOnFailureListener { e ->
                    snack("Error: ${e.localizedMessage}")
                }
        }
    }

    private fun snack(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}