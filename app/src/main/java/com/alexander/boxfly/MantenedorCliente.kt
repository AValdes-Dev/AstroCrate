package com.alexander.boxfly

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.alexander.boxfly.models.DatosGlobales
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// Formulario para crear y editar Clientes en Firestore
class MantenedorCliente : AppCompatActivity() {

    // Instancia de Firestore para guardar los datos en la nube
    private val db = FirebaseFirestore.getInstance()
    // Objeto para Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var txtNombre: EditText
    private lateinit var txtRut: EditText
    private lateinit var txtDir: EditText
    private lateinit var txtComuna: EditText
    private lateinit var txtTel: EditText
    private lateinit var txtEmail: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenedor_cliente)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        txtNombre = findViewById(R.id.txtNombreCliente)
        txtRut = findViewById(R.id.txtRutCliente)
        txtDir = findViewById(R.id.txtDireccionCliente)
        txtComuna = findViewById(R.id.txtComunaCliente)
        txtTel = findViewById(R.id.txtTelefonoCliente)
        txtEmail = findViewById(R.id.txtEmailCliente)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarCliente)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarCliente)

        val posicion = intent.getIntExtra("POSICION", -1)

        if (posicion != -1 && posicion < DatosGlobales.listaClientes.size) {
            val c = DatosGlobales.listaClientes[posicion]
            txtNombre.setText(c.nombre)
            txtRut.setText(c.rut)
            txtDir.setText(c.direccion)
            txtComuna.setText(c.comuna)
            txtTel.setText(c.telefono)
            txtEmail.setText(c.email)
            btnGuardar.text = "Actualizar Cambios"
        }

        btnGuardar.setOnClickListener {
            val nom = txtNombre.text.toString().trim()
            val rut = txtRut.text.toString().trim()
            val dir = txtDir.text.toString().trim()
            val comuna = txtComuna.text.toString().trim()
            val tel = txtTel.text.toString().trim()
            val email = txtEmail.text.toString().trim()

            if (nom.isNotEmpty() && rut.isNotEmpty()) {
                btnGuardar.isEnabled = false

                if (posicion == -1) {
                    // CREAR UN NUEVO REGISTRO EN FIRESTORE
                    val clienteData = armarClienteData(nom, rut, dir, comuna, tel, email)

                    db.collection("clientes").add(clienteData)
                        .addOnSuccessListener { docCliente ->
                            Toast.makeText(this, "Cliente Guardado en Firestore", Toast.LENGTH_SHORT).show()

                            // Validación de dirección y luego cerrar la pantalla
                            ValidadorDireccion.validarYGuardar(docCliente, dir, comuna) {
                                val params = Bundle().apply {
                                    putString("tipo_entidad", "cliente")
                                }
                                firebaseAnalytics.logEvent("registro_creado", params)
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            btnGuardar.isEnabled = true
                            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    // ACTUALIZAR UN REGISTRO EXISTENTE EN FIRESTORE
                    val clienteExistente = DatosGlobales.listaClientes[posicion]
                    val nombreAntiguo = clienteExistente.nombre

                    val clienteData = armarClienteData(nom, rut, dir, comuna, tel, email)

                    val docCliente = db.collection("clientes").document(clienteExistente.id)
                    docCliente.set(clienteData, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            // SI EL NOMBRE DEL CLIENTE CAMBIÓ, ACTUALIZAR EN CASCADA LOS ENVÍOS EN FIRESTORE
                            if (nombreAntiguo != nom) {
                                db.collection("envios")
                                    .whereEqualTo("clienteNombre", nombreAntiguo)
                                    .get()
                                    .addOnSuccessListener { enviosAfectados ->
                                        for (doc in enviosAfectados) {
                                            db.collection("envios").document(doc.id)
                                                .update("clienteNombre", nom)
                                        }
                                    }
                            }

                            Toast.makeText(this, "Cliente y Envíos vinculados actualizados", Toast.LENGTH_SHORT).show()

                            ValidadorDireccion.validarYGuardar(docCliente, dir, comuna) {
                                val bundleParams = Bundle().apply {
                                    putString("tipo_entidad", "cliente")
                                }
                                firebaseAnalytics.logEvent("registro_actualizado", bundleParams)
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            btnGuardar.isEnabled = true
                            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                Toast.makeText(this, "Nombre y RUT son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }

    private fun armarClienteData(
        nom: String, rut: String, dir: String, comuna: String, tel: String, email: String
    ) = hashMapOf<String, Any>(
        "nombre" to nom,
        "rut" to rut,
        "direccion" to dir,
        "comuna" to comuna,
        "telefono" to tel,
        "email" to email
    )
}
