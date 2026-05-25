package com.signalone.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.signalone.app.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        b = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnIniciar.setOnClickListener {
            val correo = b.etCorreo.text.toString().trim()
            val pass   = b.etPass.text.toString()

            if (correo.isEmpty() || pass.isEmpty()) {
                snack("Completa todos los campos")
                return@setOnClickListener
            }

            when (val resultado = UserPreferences.validarLogin(this, correo, pass)) {
                is UserPreferences.LoginResult.Exitoso -> {
                    // Cargar nombre en sesión y entrar
                    AppState.nombreUsuario = resultado.nombre.split(" ").first()
                    snack("¡Bienvenido, ${resultado.nombre}!")
                    startActivity(
                        Intent(this, PrincipalActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                }
                is UserPreferences.LoginResult.CorreoInvalido ->
                    snack("Correo no encontrado. ¿Ya tienes cuenta?")
                is UserPreferences.LoginResult.PassInvalida ->
                    snack("Contraseña incorrecta")
                is UserPreferences.LoginResult.SinCuenta ->
                    snack("No hay cuenta registrada. ¡Regístrate primero!")
            }
        }

        b.tvOlvide.setOnClickListener   { startActivity(Intent(this, RecuperarActivity::class.java)) }
        b.btnGoogle.setOnClickListener  { snack("Google OAuth no configurado en demo") }
        b.tvIrRegistro.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java)); finish()
        }
    }

    private fun snack(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
