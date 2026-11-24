package com.alexander.boxfly

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ConversorMoneda {

    val opcionesMoneda = arrayOf(
        "CLP ($)",
        "USD ($)",
        "EUR (€)",
        "ARS ($)",
        "BRL (R$)",
        "MXN ($)",
        "COP ($)"
    )
    private val ejecutor = Executors.newSingleThreadExecutor()
    private val hiloPrincipal = Handler(Looper.getMainLooper())

    // Llama a la API externa global en la nube (open.er-api.com) para convertir Pesos Chilenos (CLP) a monedas internacionales en vivo
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

                val (codigo, simbolo, tasaDefault) = when {
                    monedaDestino.contains("USD") -> Triple("USD", "$ ", 0.00102)
                    monedaDestino.contains("EUR") -> Triple("EUR", "€ ", 0.00091)
                    monedaDestino.contains("ARS") -> Triple("ARS", "$ ", 1.05000)
                    monedaDestino.contains("BRL") -> Triple("BRL", "R$ ", 0.00580)
                    monedaDestino.contains("MXN") -> Triple("MXN", "$ ", 0.02050)
                    monedaDestino.contains("COP") -> Triple("COP", "$ ", 4.40000)
                    else -> Triple("USD", "$ ", 0.00102)
                }

                val tasa = rates.optDouble(codigo, tasaDefault)
                val resultadoCalculado = montoClp * tasa
                val textoFinal = if (codigo == "ARS" || codigo == "COP") {
                    String.format("%s%.0f %s", simbolo, resultadoCalculado, codigo)
                } else {
                    String.format("%s%.2f %s", simbolo, resultadoCalculado, codigo)
                }

                hiloPrincipal.post { onResultado(textoFinal) }
            } catch (e: Exception) {
                // Si no hay respuesta o falla la red, realiza la conversión estimada local
                val (codigo, simbolo, tasaEstimada) = when {
                    monedaDestino.contains("USD") -> Triple("USD", "$ ", 0.00102)
                    monedaDestino.contains("EUR") -> Triple("EUR", "€ ", 0.00091)
                    monedaDestino.contains("ARS") -> Triple("ARS", "$ ", 1.05000)
                    monedaDestino.contains("BRL") -> Triple("BRL", "R$ ", 0.00580)
                    monedaDestino.contains("MXN") -> Triple("MXN", "$ ", 0.02050)
                    monedaDestino.contains("COP") -> Triple("COP", "$ ", 4.40000)
                    else -> Triple("USD", "$ ", 0.00102)
                }
                val resultadoCalculado = montoClp * tasaEstimada
                val textoFinal = String.format("%s%.2f %s", simbolo, resultadoCalculado, codigo)
                hiloPrincipal.post { onResultado(textoFinal) }
            }
        }
    }
}