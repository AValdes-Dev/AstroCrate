package com.alexander.boxfly

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.alexander.boxfly.models.DatosGlobales
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore

// Formulario para crear y editar Paquetes en Firestore
class MantenedorPaquete : AppCompatActivity() {

    private val opcionesTipo = arrayOf("Sobre", "Caja Chica", "Caja Grande", "Carga Irregular")
    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()
    // Objeto para registrar eventos en Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

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
            btnGuardar.text = "Actualizar Paquete"
        }

        btnGuardar.setOnClickListener {
            val tipo = spnTipo.selectedItem.toString()
            val cont = txtCont.text.toString().trim()
            val peso = txtPeso.text.toString().toDoubleOrNull() ?: 0.0
            val valor = txtValor.text.toString().toDoubleOrNull() ?: 0.0

            if (cont.isNotEmpty() && peso > 0) {
                btnGuardar.isEnabled = false

                val paqueteData = hashMapOf(
                    "tipo" to tipo,
                    "contenido" to cont,
                    "peso" to peso,
                    "esFragil" to swFragil.isChecked,
                    "valorDeclarado" to valor
                )

                if (posicion == -1) {
                    // CREAR NUEVO PAQUETE EN FIRESTORE
                    db.collection("paquetes").add(paqueteData)
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

                    db.collection("paquetes").document(paqueteExistente.id).set(paqueteData)
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
            } else {
                Toast.makeText(this, "Descripción y peso mayor a 0 son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }
}
