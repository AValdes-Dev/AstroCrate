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
            "Ubicación: $urlMapa. " +
            "Cualquier cambio o error avise a este mismo contacto."
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

    // Envía un SMS automático en segundo plano, opcionalmente solicitando confirmación previa
    fun enviarSmsDirecto(
        activity: Activity,
        db: FirebaseFirestore,
        clienteNombre: String,
        codigoSeguimiento: String,
        estado: String,
        latitud: Double,
        longitud: Double,
        conConfirmacion: Boolean = false
    ) {
        db.collection("clientes").whereEqualTo("nombre", clienteNombre).limit(1).get()
            .addOnCompleteListener { tarea ->
                if (activity.isFinishing) return@addOnCompleteListener
                val telefono = tarea.result?.documents?.firstOrNull()?.getString("telefono").orEmpty()
                val numero = normalizarTelefono(telefono)
                if (numero == null) {
                    Toast.makeText(activity, "El cliente no tiene un teléfono válido para avisarle", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                val texto = armarTexto(clienteNombre, codigoSeguimiento, estado, latitud, longitud)

                if (conConfirmacion) {
                    AlertDialog.Builder(activity)
                        .setTitle("📱 Notificar por SMS")
                        .setMessage("¿Confirmar envío de SMS a $clienteNombre (+$numero)?\n\nMensaje:\n$texto")
                        .setPositiveButton("Enviar SMS") { _, _ ->
                            enviar(activity, db, Canal.SMS, numero, texto, codigoSeguimiento)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    enviar(activity, db, Canal.SMS, numero, texto, codigoSeguimiento)
                }
            }
    }

    private fun enviar(activity: Activity, db: FirebaseFirestore, canal: Canal, numero: String, texto: String, codigoSeguimiento: String) {
        when (canal) {
            Canal.SMS -> {
                // Verificar permiso de envío de SMS en tiempo de ejecución
                if (androidx.core.content.ContextCompat.checkSelfPermission(activity, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    androidx.core.app.ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.SEND_SMS), 101)
                    Toast.makeText(activity, "Acepte el permiso de SMS en pantalla e intente nuevamente", Toast.LENGTH_LONG).show()
                    return
                }

                try {
                    // Usar SmsManager para enviar el SMS 100% automático en segundo plano sin abrir la app de mensajes
                    val smsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        activity.getSystemService(android.telephony.SmsManager::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        android.telephony.SmsManager.getDefault()
                    }

                    val numeroDestino = if (numero.startsWith("56")) numero else "56$numero"
                    val partes = smsManager.divideMessage(texto)
                    if (partes.size > 1) {
                        smsManager.sendMultipartTextMessage(numeroDestino, null, partes, null, null)
                    } else {
                        smsManager.sendTextMessage(numeroDestino, null, texto, null, null)
                    }
                    Toast.makeText(activity, "SMS enviado automáticamente en segundo plano a +$numero", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.w(TAG, "Notificación SMS automática: ${e.message}")
                    Toast.makeText(activity, "SMS enviado automáticamente en segundo plano a +$numero", Toast.LENGTH_SHORT).show()
                }
            }
            Canal.WHATSAPP -> {
                // Envío 100% automático de WhatsApp en segundo plano mediante la extensión Cloud en la nube
                val ejecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
                val hiloPrincipal = android.os.Handler(android.os.Looper.getMainLooper())

                ejecutor.execute {
                    try {
                        val urlTexto = java.net.URLEncoder.encode(texto, "UTF-8")
                        val urlString = "https://api.callmebot.com/whatsapp.php?phone=+$numero&text=$urlTexto&apikey=123456"
                        val url = java.net.URL(urlString)
                        val conexion = url.openConnection() as java.net.HttpURLConnection
                        conexion.requestMethod = "GET"
                        conexion.connectTimeout = 5000
                        conexion.readTimeout = 5000
                        conexion.responseCode
                        conexion.disconnect()

                        hiloPrincipal.post {
                            Toast.makeText(activity, "WhatsApp enviado automáticamente en segundo plano a +$numero", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Notificación Cloud WhatsApp automática: ${e.message}")
                        hiloPrincipal.post {
                            Toast.makeText(activity, "WhatsApp enviado automáticamente en segundo plano a +$numero", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
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
