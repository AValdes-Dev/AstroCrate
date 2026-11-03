package com.alexander.boxfly

import android.text.TextUtils
import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject

// Registra cada correo en la colección "mail" de Firestore y lo envía con el Gmail del Apps Script.
// El resultado queda en "delivery.state" (SUCCESS / ERROR), igual que con la extensión Trigger Email.
object NotificadorCorreo {

    private const val COLECCION_MAIL = "mail"
    private const val TAG = "NotificadorCorreo"

    // Busca el email del cliente por su nombre (los envíos guardan solo el nombre) y encola el aviso
    fun notificarEstadoEnvio(
        db: FirebaseFirestore,
        clienteNombre: String,
        codigoSeguimiento: String,
        paqueteTipo: String,
        estado: String,
        fechaEnvio: String,
        esNuevo: Boolean
    ) {
        db.collection("clientes")
            .whereEqualTo("nombre", clienteNombre)
            .limit(1)
            .get()
            .addOnSuccessListener { resultado ->
                val email = resultado.documents.firstOrNull()?.getString("email")?.trim().orEmpty()
                if (email.isEmpty()) {
                    Log.w(TAG, "Cliente '$clienteNombre' sin email, no se envía notificación")
                    return@addOnSuccessListener
                }

                val asunto = if (esNuevo) {
                    "AstroCrate: tu envío $codigoSeguimiento fue registrado"
                } else {
                    "AstroCrate: tu envío $codigoSeguimiento ahora está \"$estado\""
                }

                // Escapar datos ingresados por el usuario antes de insertarlos en el HTML
                val html = """
                    <h2>Hola ${TextUtils.htmlEncode(clienteNombre)},</h2>
                    <p>${if (esNuevo) "Registramos un nuevo envío a tu nombre." else "El estado de tu envío cambió."}</p>
                    <ul>
                        <li><b>Código de seguimiento:</b> ${TextUtils.htmlEncode(codigoSeguimiento)}</li>
                        <li><b>Paquete:</b> ${TextUtils.htmlEncode(paqueteTipo)}</li>
                        <li><b>Fecha de envío:</b> ${TextUtils.htmlEncode(fechaEnvio)}</li>
                        <li><b>Estado:</b> ${TextUtils.htmlEncode(estado)}</li>
                    </ul>
                    <p>Gracias por confiar en AstroCrate.</p>
                """.trimIndent()

                val texto = "Envío $codigoSeguimiento ($paqueteTipo) - Estado: $estado - Fecha: $fechaEnvio"

                val correo = hashMapOf(
                    "to" to email,
                    "message" to hashMapOf(
                        "subject" to asunto,
                        "text" to texto,
                        "html" to html
                    )
                )

                db.collection(COLECCION_MAIL).add(correo)
                    .addOnSuccessListener { docMail ->
                        ServicioAppsScript.llamar("enviarCorreo", {
                            JSONObject()
                                .put("to", email)
                                .put("subject", asunto)
                                .put("text", texto)
                                .put("html", html)
                        }) { _, error ->
                            if (error != null) Log.e(TAG, "No se pudo enviar el correo: $error")
                            docMail.update(
                                mapOf(
                                    "delivery" to mapOf(
                                        "state" to if (error == null) "SUCCESS" else "ERROR",
                                        "error" to error,
                                        "endTime" to FieldValue.serverTimestamp()
                                    )
                                )
                            )
                        }
                    }
                    .addOnFailureListener { e -> Log.e(TAG, "No se pudo registrar el correo", e) }
            }
            .addOnFailureListener { e -> Log.e(TAG, "No se pudo buscar el email del cliente", e) }
    }
}
