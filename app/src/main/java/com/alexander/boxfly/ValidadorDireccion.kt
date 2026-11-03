package com.alexander.boxfly

import android.util.Log
import com.google.firebase.firestore.DocumentReference
import org.json.JSONObject

// Valida la dirección del cliente con el geocodificador de Google Maps (vía Apps Script)
// y guarda el resultado en el mismo documento de Firestore.
object ValidadorDireccion {

    private const val TAG = "ValidadorDireccion"

    fun validarYGuardar(docCliente: DocumentReference, direccion: String, comuna: String) {
        if (direccion.isEmpty()) return

        ServicioAppsScript.llamar("validarDireccion", {
            JSONObject().put("direccion", direccion).put("comuna", comuna)
        }) { respuesta, error ->
            if (respuesta == null) {
                Log.w(TAG, "No se pudo validar la dirección: $error")
                return@llamar
            }

            val resultado = mutableMapOf<String, Any>("addressValidity" to respuesta.optBoolean("valida"))
            if (respuesta.has("direccionNormalizada")) {
                resultado["direccionNormalizada"] = respuesta.optString("direccionNormalizada")
                resultado["lat"] = respuesta.optDouble("lat")
                resultado["lng"] = respuesta.optDouble("lng")
            }
            docCliente.update(resultado)
                .addOnFailureListener { e -> Log.e(TAG, "No se pudo guardar la validación", e) }
        }
    }
}
