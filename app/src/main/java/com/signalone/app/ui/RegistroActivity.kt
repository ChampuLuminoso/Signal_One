package com.signalone.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityRegistroBinding

class RegistroActivity : AppCompatActivity() {
    private lateinit var b: ActivityRegistroBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityRegistroBinding.inflate(layoutInflater)
        setContentView(b.root)

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
                    // Guardar cuenta en SharedPreferences
                    UserPreferences.guardarCuenta(this, nombre, correo, pass)

                    // Cargar nombre en AppState para la sesión actual
                    AppState.nombreUsuario = nombre.split(" ").first()

                    snack("¡Cuenta creada exitosamente!")

                    // Ir a la pantalla principal
                    startActivity(
                        Intent(this, PrincipalActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
            }
        }

        b.btnGoogle.setOnClickListener { snack("Google OAuth no configurado en demo") }

        b.tvIrLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun snack(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
