package com.alexander.boxfly

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

// Cliente HTTP del Google Apps Script (apps-script/Code.gs).
// Reemplaza gratis a las extensiones de Firebase: correo, validación de dirección y fotos a Drive.
object ServicioAppsScript {

    private val ejecutor = Executors.newSingleThreadExecutor()
    private val hiloPrincipal = Handler(Looper.getMainLooper())

    // construirDatos corre en segundo plano (sirve para comprimir fotos sin trabar la pantalla).
    // onResultado vuelve al hilo principal con la respuesta JSON, o con el error en texto.
    fun llamar(
        accion: String,
        construirDatos: () -> JSONObject,
        onResultado: (respuesta: JSONObject?, error: String?) -> Unit
    ) {
        if (BuildConfig.APPS_SCRIPT_URL.isEmpty()) {
            onResultado(null, "Apps Script no configurado (falta APPS_SCRIPT_URL en local.properties)")
            return
        }

        ejecutor.execute {
            var respuesta: JSONObject? = null
            var error: String? = null
            try {
                val datos = construirDatos()
                    .put("token", BuildConfig.APPS_SCRIPT_TOKEN)
                    .put("accion", accion)

                // Apps Script responde con un redirect 302 que HttpURLConnection sigue solo
                val conexion = URL(BuildConfig.APPS_SCRIPT_URL).openConnection() as HttpURLConnection
                conexion.requestMethod = "POST"
                conexion.doOutput = true
                conexion.connectTimeout = 15000
                conexion.readTimeout = 60000
                conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conexion.outputStream.use { it.write(datos.toString().toByteArray(Charsets.UTF_8)) }

                val cuerpo = conexion.inputStream.bufferedReader().use { it.readText() }
                conexion.disconnect()

                val json = JSONObject(cuerpo)
                if (json.optBoolean("ok")) respuesta = json else error = json.optString("error", "Error desconocido")
            } catch (e: Exception) {
                error = e.message ?: e.javaClass.simpleName
            }
            hiloPrincipal.post { onResultado(respuesta, error) }
        }
    }
}
