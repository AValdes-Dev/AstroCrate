package com.alexander.boxfly

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ConversorMoneda {

    val opcionesMoneda = arrayOf("CLP ($)", "USD ($)", "EUR (€)")
    private val ejecutor = Executors.newSingleThreadExecutor()
    private val hiloPrincipal = Handler(Looper.getMainLooper())

    // Llama a la API externa global en la nube (open.er-api.com) para convertir Pesos Chilenos (CLP) a USD/EUR en vivo
    fun obtenerConversionEnVivo(montoClp: Double, monedaDestino: String, onResultado: (montoConvertido: String) -> Unit) {
        if (monedaDestino.contains("CLP")) {
            val montoFormateado = String.format("%.0f", montoClp)
            onResultado("$ $montoFormateado CLP")
            return
        }

        ejecutor.execute {
            try {
                // Consulta HTTP a la API REST de tipo de cambio en la nube
                val url = URL("https://open.er-api.com/v6/latest/CLP")
                val conexion = url.openConnection() as HttpURLConnection
                conexion.requestMethod = "GET"
                conexion.setRequestProperty("User-Agent", "Mozilla/5.0")
                conexion.connectTimeout = 5000
                conexion.readTimeout = 5000

                val respuestaTexto = conexion.inputStream.bufferedReader().use { it.readText() }
                conexion.disconnect()

                val json = JSONObject(respuestaTexto)
                val rates = json.getJSONObject("rates")

                val tasa = if (monedaDestino.contains("USD")) {
                    rates.getDouble("USD")
                } else {
                    rates.getDouble("EUR")
                }

                // Cálculo real de la conversión de CLP a la moneda destino
                val resultadoCalculado = montoClp * tasa
                val simbolo = if (monedaDestino.contains("USD")) "$ " else "€ "
                val codigo = if (monedaDestino.contains("USD")) "USD" else "EUR"
                val textoFinal = String.format("%s%.2f %s", simbolo, resultadoCalculado, codigo)

                hiloPrincipal.post { onResultado(textoFinal) }
            } catch (e: Exception) {
                // Si no hay respuesta o falla la red, realiza la conversión estimada local
                val tasaEstimada = if (monedaDestino.contains("USD")) 0.00102 else 0.00091
                val resultadoCalculado = montoClp * tasaEstimada
                val codigo = if (monedaDestino.contains("USD")) "USD" else "EUR"
                val textoFinal = String.format("%.2f %s", resultadoCalculado, codigo)
                hiloPrincipal.post { onResultado(textoFinal) }
            }
        }
    }
}