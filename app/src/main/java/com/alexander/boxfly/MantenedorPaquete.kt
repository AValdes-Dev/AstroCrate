package com.alexander.boxfly

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.alexander.boxfly.models.DatosGlobales
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.io.ByteArrayOutputStream

// Formulario para crear y editar Paquetes en Firestore
class MantenedorPaquete : AppCompatActivity() {

    private val opcionesTipo = arrayOf("Sobre", "Caja Chica", "Caja Grande", "Carga Irregular")
    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()
    // Objeto para registrar eventos en Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    // Foto elegida en esta pantalla (aún no subida) y URL en Drive de la foto ya guardada
    private var fotoSeleccionada: Uri? = null
    private var fotoUrlActual = ""
    private lateinit var lblFoto: TextView

    // Selector de imágenes del sistema (no requiere permisos de almacenamiento)
    private val seleccionarFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            fotoSeleccionada = uri
            lblFoto.text = "Foto lista para subir"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenedor_paquete)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val spnTipo = findViewById<Spinner>(R.id.spnTipoPaquete)
        val txtCont = findViewById<EditText>(R.id.txtContenido)
        val txtPeso = findViewById<EditText>(R.id.txtPeso)
        val txtValor = findViewById<EditText>(R.id.txtValorDeclarado)
        val swFragil = findViewById<SwitchCompat>(R.id.swFragil)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPaquete)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarPaquete)
        val btnFoto = findViewById<Button>(R.id.btnFotoPaquete)
        lblFoto = findViewById(R.id.lblFotoPaquete)

        btnFoto.setOnClickListener {
            seleccionarFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Cargar las opciones del selector (Spinner)
        val adapterTipo = ArrayAdapter(this, R.layout.spinner_item, opcionesTipo)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnTipo.adapter = adapterTipo

        val posicion = intent.getIntExtra("POSICION", -1)

        // Si venimos a editar, llenamos los campos con los valores actuales
        if (posicion != -1 && posicion < DatosGlobales.listaPaquetes.size) {
            val p = DatosGlobales.listaPaquetes[posicion]
            val index = opcionesTipo.indexOf(p.tipo)
            if (index != -1) spnTipo.setSelection(index)

            txtCont.setText(p.contenido)
            txtPeso.setText(p.peso.toString())
            txtValor.setText(p.valorDeclarado.toString())
            swFragil.isChecked = p.esFragil
            fotoUrlActual = p.fotoDriveUrl
            if (fotoUrlActual.isNotEmpty()) lblFoto.text = "Foto adjunta"
            btnGuardar.text = "Actualizar Paquete"
        }

        btnGuardar.setOnClickListener {
            val tipo = spnTipo.selectedItem.toString()
            val cont = txtCont.text.toString().trim()
            val peso = txtPeso.text.toString().toDoubleOrNull() ?: 0.0
            val valor = txtValor.text.toString().toDoubleOrNull() ?: 0.0

            if (cont.isNotEmpty() && peso > 0) {
                btnGuardar.isEnabled = false

                val paqueteData = hashMapOf<String, Any>(
                    "tipo" to tipo,
                    "contenido" to cont,
                    "peso" to peso,
                    "esFragil" to swFragil.isChecked,
                    "valorDeclarado" to valor
                )

                // El ID se conoce antes de guardar para usarlo en el nombre de la foto en Drive
                val docRef = if (posicion == -1) {
                    db.collection("paquetes").document()
                } else {
                    db.collection("paquetes").document(DatosGlobales.listaPaquetes[posicion].id)
                }

                subirFotoSiCorresponde(docRef, onError = { btnGuardar.isEnabled = true }) { fotoUrl ->
                    paqueteData["fotoDriveUrl"] = fotoUrl

                    if (posicion == -1) {
                        // CREAR NUEVO PAQUETE EN FIRESTORE
                        docRef.set(paqueteData)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Paquete Registrado en Firestore", Toast.LENGTH_SHORT).show()

                                // Evento Analytics: registro_creado
                                val params = Bundle().apply {
                                    putString("tipo_entidad", "paquete")
                                }
                                firebaseAnalytics.logEvent("registro_creado", params)

                                finish()
                            }
                            .addOnFailureListener { e ->
                                btnGuardar.isEnabled = true
                                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        // ACTUALIZAR PAQUETE EXISTENTE EN FIRESTORE
                        val paqueteExistente = DatosGlobales.listaPaquetes[posicion]
                        val descripcionAntigua = "${paqueteExistente.tipo} - ${paqueteExistente.contenido}"
                        val descripcionNueva = "$tipo - $cont"

                        docRef.set(paqueteData)
                            .addOnSuccessListener {
                                // SI LA DESCRIPCIÓN CAMBIÓ, ACTUALIZAR EN CASCADA TODOS LOS ENVÍOS ASOCIADOS
                                if (descripcionAntigua != descripcionNueva) {
                                    db.collection("envios")
                                        .whereEqualTo("paqueteTipo", descripcionAntigua)
                                        .get()
                                        .addOnSuccessListener { enviosAfectados ->
                                            for (doc in enviosAfectados) {
                                                db.collection("envios").document(doc.id)
                                                    .update("paqueteTipo", descripcionNueva)
                                            }
                                        }
                                }

                                Toast.makeText(this, "Paquete y Envíos vinculados actualizados", Toast.LENGTH_SHORT).show()

                                // Evento Analytics: registro_actualizado
                                val params = Bundle().apply {
                                    putString("tipo_entidad", "paquete")
                                }
                                firebaseAnalytics.logEvent("registro_actualizado", params)

                                finish()
                            }
                            .addOnFailureListener { e ->
                                btnGuardar.isEnabled = true
                                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    }
                }
            } else {
                Toast.makeText(this, "Descripción y peso mayor a 0 son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }

    // Si hay foto nueva la sube a Google Drive (vía Apps Script) y entrega su URL; si no, entrega la URL actual
    private fun subirFotoSiCorresponde(docRef: DocumentReference, onError: () -> Unit, onListo: (String) -> Unit) {
        val uri = fotoSeleccionada
        if (uri == null) {
            onListo(fotoUrlActual)
            return
        }

        lblFoto.text = "Subiendo foto a Drive..."
        ServicioAppsScript.llamar("subirFoto", {
            JSONObject()
                .put("nombre", "paquete_${docRef.id}_${System.currentTimeMillis()}.jpg")
                .put("mimeType", "image/jpeg")
                .put("base64", comprimirFoto(uri))
        }) { respuesta, error ->
            if (respuesta != null) {
                fotoUrlActual = respuesta.optString("url")
                fotoSeleccionada = null
                lblFoto.text = "Foto adjunta"
                onListo(fotoUrlActual)
            } else {
                lblFoto.text = "Foto lista para subir"
                onError()
                Toast.makeText(this, "Error al subir la foto: $error", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Reduce la foto a máx. 1280 px y JPEG 80% para que la subida sea liviana; devuelve Base64
    private fun comprimirFoto(uri: Uri): String {
        val original = contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) }
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        val escala = minOf(1f, LADO_MAXIMO_FOTO.toFloat() / maxOf(original.width, original.height))
        val foto = if (escala < 1f) {
            Bitmap.createScaledBitmap(original, (original.width * escala).toInt(), (original.height * escala).toInt(), true)
        } else {
            original
        }
        val salida = ByteArrayOutputStream()
        foto.compress(Bitmap.CompressFormat.JPEG, 80, salida)
        return Base64.encodeToString(salida.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val LADO_MAXIMO_FOTO = 1280
    }
}
