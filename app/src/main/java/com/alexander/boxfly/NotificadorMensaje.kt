package com.alexander.boxfly

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Mensajería al cliente por SMS o WhatsApp.
// Se usa un Intent hacia la app de mensajes del teléfono: no requiere el permiso SEND_SMS
// (Google Play lo restringe a apps de SMS por defecto) y no tiene costo para el proyecto.
// Cada aviso queda registrado en la colección "mensajes" de Firestore.
object NotificadorMensaje {

    private const val COLECCION_MENSAJES = "mensajes"
    private const val TAG = "NotificadorMensaje"

    enum class Canal(val etiqueta: String) { SMS("sms"), WHATSAPP("whatsapp") }

    // Arma el texto del aviso de un envío
    fun armarTexto(clienteNombre: String, codigoSeguimiento: String, estado: String, latitud: Double, longitud: Double): String {
        val primerNombre = clienteNombre.trim().split(" ").firstOrNull().orEmpty()
        val urlMapa = "https://www.google.com/maps/search/?api=1&query=$latitud,$longitud"
        return "Hola $primerNombre, tu envío AstroCrate $codigoSeguimiento está \"$estado\". " +
            "Ubicación: $urlMapa"
    }

    // Normaliza un teléfono chileno a formato internacional sin "+" (ej. 56912345678)
    fun normalizarTelefono(telefono: String): String? {
        val digitos = telefono.filter { it.isDigit() }
        return when {
            digitos.length == 11 && digitos.startsWith("56") -> digitos
            digitos.length == 9 -> "56$digitos"                 // 912345678
            digitos.length == 8 -> "569$digitos"                // 12345678 (sin el 9)
            digitos.length in 10..15 -> digitos                 // otro país con código
            else -> null
        }
    }

    // Busca el teléfono del cliente y muestra el diálogo para elegir canal.
    // onTerminado se llama siempre al cerrar el diálogo (enviado, omitido o sin teléfono).
    fun ofrecerAviso(
        activity: Activity,
        db: FirebaseFirestore,
        clienteNombre: String,
        codigoSeguimiento: String,
        estado: String,
        latitud: Double,
        longitud: Double,
        onTerminado: () -> Unit = {}
    ) {
        db.collection("clientes").whereEqualTo("nombre", clienteNombre).limit(1).get()
            .addOnCompleteListener { tarea ->
                if (activity.isFinishing) return@addOnCompleteListener
                val telefono = tarea.result?.documents?.firstOrNull()?.getString("telefono").orEmpty()
                val numero = normalizarTelefono(telefono)
                if (numero == null) {
                    Toast.makeText(activity, "El cliente no tiene un teléfono válido para avisarle", Toast.LENGTH_SHORT).show()
                    onTerminado()
                    return@addOnCompleteListener
                }

                val texto = armarTexto(clienteNombre, codigoSeguimiento, estado, latitud, longitud)
                AlertDialog.Builder(activity)
                    .setTitle("Avisar al cliente")
                    .setMessage("¿Enviar este mensaje a $clienteNombre (+$numero)?\n\n$texto")
                    .setPositiveButton("WhatsApp") { _, _ ->
                        enviar(activity, db, Canal.WHATSAPP, numero, texto, codigoSeguimiento)
                        onTerminado()
                    }
                    .setNeutralButton("SMS") { _, _ ->
                        enviar(activity, db, Canal.SMS, numero, texto, codigoSeguimiento)
                        onTerminado()
                    }
                    .setNegativeButton("Omitir") { _, _ -> onTerminado() }
                    .setOnCancelListener { onTerminado() }
                    .show()
            }
    }

    private fun enviar(activity: Activity, db: FirebaseFirestore, canal: Canal, numero: String, texto: String, codigoSeguimiento: String) {
        val intent = when (canal) {
            Canal.SMS -> Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:+$numero")).putExtra("sms_body", texto)
            Canal.WHATSAPP -> Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=${Uri.encode(texto)}"))
        }
        try {
            activity.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, "No hay una app disponible para ${canal.etiqueta}", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection(COLECCION_MENSAJES).add(
            hashMapOf(
                "canal" to canal.etiqueta,
                "telefono" to "+$numero",
                "texto" to texto,
                "codigoSeguimiento" to codigoSeguimiento,
                "creado" to FieldValue.serverTimestamp()
            )
        ).addOnFailureListener { e -> Log.e(TAG, "No se pudo registrar el mensaje", e) }
    }
}
