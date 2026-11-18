package com.alexander.boxfly

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import com.alexander.boxfly.models.EnvioModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream

// Etiqueta QR de cada envío: se genera en el teléfono (ZXing) y se lee con el escáner de Google
// (Code Scanner de Play Services), que no pide permiso de cámara y funciona sin internet.
// El QR contiene "ASTROCRATE:<código>" para no confundirlo con QR de otras empresas.
object EtiquetaQR {

    private const val PREFIJO = "ASTROCRATE:"

    fun contenido(codigoSeguimiento: String) = "$PREFIJO$codigoSeguimiento"

    // Extrae el código de seguimiento de un QR leído (acepta también el código pelado, ej. "AC-12345")
    fun leerCodigo(valorQr: String): String? {
        val valor = valorQr.trim()
        return when {
            valor.startsWith(PREFIJO) -> valor.removePrefix(PREFIJO).trim().ifEmpty { null }
            Regex("^AC-\\d+$").matches(valor) -> valor
            else -> null
        }
    }

    fun generar(texto: String, tamano: Int = 720): Bitmap {
        val matriz = QRCodeWriter().encode(texto, BarcodeFormat.QR_CODE, tamano, tamano, mapOf(EncodeHintType.MARGIN to 1))
        val pixeles = IntArray(tamano * tamano) { i ->
            if (matriz[i % tamano, i / tamano]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixeles, tamano, tamano, Bitmap.Config.ARGB_8888)
    }

    // Muestra la etiqueta del envío con opción de compartirla (imprimir, WhatsApp, correo, etc.)
    fun mostrar(activity: Activity, envio: EnvioModel) {
        val qr = generar(contenido(envio.codigoSeguimiento))
        val imagen = ImageView(activity).apply {
            setImageBitmap(qr)
            adjustViewBounds = true
            val margen = (16 * activity.resources.displayMetrics.density).toInt()
            setPadding(margen, margen, margen, margen)
        }
        AlertDialog.Builder(activity)
            .setTitle("Etiqueta ${envio.codigoSeguimiento}")
            .setMessage("Cliente: ${envio.clienteNombre}\nPaquete: ${envio.paqueteTipo}")
            .setView(imagen)
            .setPositiveButton("Compartir") { _, _ -> compartir(activity, envio, qr) }
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun compartir(activity: Activity, envio: EnvioModel, qr: Bitmap) {
        try {
            val carpeta = File(activity.cacheDir, "etiquetas").apply { mkdirs() }
            val archivo = File(carpeta, "etiqueta_${envio.codigoSeguimiento}.png")
            FileOutputStream(archivo).use { qr.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", archivo)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Etiqueta AstroCrate ${envio.codigoSeguimiento} - ${envio.clienteNombre}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, "Compartir etiqueta"))
        } catch (e: Exception) {
            Toast.makeText(activity, "No se pudo compartir la etiqueta", Toast.LENGTH_SHORT).show()
        }
    }

    // Abre el escáner de Google y entrega el código de seguimiento leído
    fun escanear(activity: Activity, onCodigo: (String) -> Unit) {
        val opciones = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(activity, opciones).startScan()
            .addOnSuccessListener { barcode ->
                val codigo = leerCodigo(barcode.rawValue.orEmpty())
                if (codigo == null) {
                    Toast.makeText(activity, "Ese QR no es una etiqueta de AstroCrate", Toast.LENGTH_SHORT).show()
                } else {
                    onCodigo(codigo)
                }
            }
            .addOnFailureListener {
                Toast.makeText(activity, "No se pudo abrir el escáner. Revisa que Google Play Services esté actualizado.", Toast.LENGTH_LONG).show()
            }
    }
}
