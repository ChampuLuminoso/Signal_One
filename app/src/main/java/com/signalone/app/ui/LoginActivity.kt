package com.signalone.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.signalone.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()

        b.btnIniciar.setOnClickListener {
            val correo = b.etCorreo.text.toString().trim()
            val pass   = b.etPass.text.toString()

            if (correo.isEmpty() || pass.isEmpty()) {
                snack("Completa todos los campos")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(correo, pass)
                .addOnSuccessListener { result ->
                    val nombre = result.user?.displayName
                        ?: UserPreferences.getNombreGuardado(this)
                    AppState.nombreUsuario = nombre.split(" ").first()
                    UserPreferences.guardarSesionActiva(this, true)
                    startActivity(
                        Intent(this, PrincipalActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                .addOnFailureListener { e ->
                    snack("Error: ${e.localizedMessage}")
                }
        }

        b.tvOlvide.setOnClickListener {
            startActivity(Intent(this, RecuperarActivity::class.java))
        }

        b.btnGoogle.setOnClickListener {
            snack("Google Sign-In próximamente")
        }

        b.tvIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
            finish()
        }
    }

    private fun snack(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}