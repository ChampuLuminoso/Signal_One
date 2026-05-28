package com.signalone.app.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock

/**
 * Detecta cambios de volumen cuando la app está en segundo plano.
 * Registrado dinámicamente en PanicService.
 */
class VolumeReceiver : BroadcastReceiver() {

    private var contador = 0
    private var ultimoPress = 0L
    private val VENTANA = 2000L

    override fun onReceive(context: Context, intent: Intent) {
        if (!AppState.volumenActivo) return
        if (intent.action != "android.media.VOLUME_CHANGED_ACTION") return

        val ahora = SystemClock.elapsedRealtime()
        if (ahora - ultimoPress > VENTANA) contador = 0
        contador++
        ultimoPress = ahora

        if (contador >= 5) {
            contador = 0
            // Delegar al servicio
            (context as? PanicService)?.activarPanicoDesdeServicio("Volumen ×5 (bloqueado)")
                ?: run {
                    // Si lo recibe fuera del servicio, arrancar alerta directamente
                    AppState.origenAlertaActual = "Volumen ×5 (bloqueado)"
                    val launchIntent = Intent(context, AlertaActivaActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    context.startActivity(launchIntent)
                }
        }
    }
}
