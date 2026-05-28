package com.signalone.app.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper

object WhatsAppHelper {

    /**
     * Envía el mensaje a cada contacto por WhatsApp en secuencia.
     * Usa un delay de 2 segundos entre cada uno para dar tiempo al usuario
     * de ver la apertura. En segundo plano Android no permite abrir múltiples
     * intents simultáneos — la secuencia es la mejor solución sin API Business.
     */
    fun enviarATodos(context: Context, contactos: List<Contacto>, mensaje: String) {
        val handler = Handler(Looper.getMainLooper())
        contactos.forEachIndexed { index, contacto ->
            handler.postDelayed({
                enviar(context, contacto.telefono, mensaje)
            }, index * 2500L)  // 2.5 seg entre cada uno
        }
    }

    private fun enviar(context: Context, telefono: String, mensaje: String) {
        try {
            val numero = telefono.replace("[^0-9+]".toRegex(), "")
            val url = "https://api.whatsapp.com/send?phone=$numero&text=${Uri.encode(mensaje)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.whatsapp")   // forzar WhatsApp, no el selector
            }
            // Verificar que WhatsApp está instalado
            val pm = context.packageManager
            if (pm.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
            } else {
                // Fallback sin setPackage (abre selector)
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            }
        } catch (e: Exception) {
            // Si falla WhatsApp, el SMS ya fue enviado — no es crítico
        }
    }
}
