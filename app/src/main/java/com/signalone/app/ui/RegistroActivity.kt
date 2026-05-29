package com.signalone.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.signalone.app.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(b.root)

        auth = FirebaseAuth.getInstance()

        b.btnCrear.setOnClickListener {
            val nombre  = b.etNombre.text.toString().trim()
            val correo  = b.etCorreo.text.toString().trim()
            val pass    = b.etPass.text.toString()
            val confirm = b.etConfirm.text.toString()

            when {
                nombre.isEmpty() || correo.isEmpty() || pass.isEmpty() ->
                    snack("Por favor completa todos los campos")
                !correo.contains("@") ->
                    snack("Ingresa un correo válido")
                pass.length < 6 ->
                    snack("La contraseña debe tener al menos 6 caracteres")
                pass != confirm ->
                    snack("Las contraseñas no coinciden")
                else -> {
                    auth.createUserWithEmailAndPassword(correo, pass)
                        .addOnSuccessListener { result ->
                            // Guardar nombre en el perfil de Firebase
                            val update = UserProfileChangeRequest.Builder()
                                .setDisplayName(nombre)
                                .build()
                            result.user?.updateProfile(update)

                            // Guardar localmente también
                            UserPreferences.guardarCuenta(this, nombre, correo, pass)
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
            }
        }

        b.btnGoogle.setOnClickListener { snack("Google Sign-In próximamente") }

        b.tvIrLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun snack(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
