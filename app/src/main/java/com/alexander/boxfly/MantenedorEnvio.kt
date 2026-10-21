package com.alexander.boxfly

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexander.boxfly.models.ClienteModel
import com.alexander.boxfly.models.DatosGlobales
import com.alexander.boxfly.models.PaqueteModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

// Formulario para generar o actualizar envíos asociando Clientes y Paquetes de Firestore
class MantenedorEnvio : AppCompatActivity() {

    private val opcionesEstado = arrayOf("En sucursal", "En tránsito", "En ruta de entrega", "Entregado", "Devuelto")
    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()
    // Objeto para Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private val nombresClientes = mutableListOf<String>()
    private val infoPaquetes = mutableListOf<String>()
    private lateinit var adapterCli: ArrayAdapter<String>
    private lateinit var adapterPaq: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenedor_envio)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val txtSeg = findViewById<EditText>(R.id.txtSeguimiento)
        val spnCli = findViewById<Spinner>(R.id.spnCliente)
        val spnPaq = findViewById<Spinner>(R.id.spnPaquete)
        val txtFec = findViewById<EditText>(R.id.txtFecha)
        val spnEst = findViewById<Spinner>(R.id.spnEstado)
        val txtPre = findViewById<EditText>(R.id.txtPrecio)
        val btnGenerar = findViewById<Button>(R.id.btnGenerarCodigo)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarEnvio)
        val btnCancelar = findViewById<Button>(R.id.btnVolverEnvio)

        // Generar un código aleatorio para la guía de envío
        btnGenerar.setOnClickListener {
            val randomNum = (10000..99999).random()
            txtSeg.setText("AC-$randomNum")
            Toast.makeText(this, "Código generado", Toast.LENGTH_SHORT).show()
        }

        // Configuración para abrir el selector de fecha (DatePickerDialog)
        txtFec.isFocusable = false
        txtFec.setOnClickListener {
            val locale = Locale("es")
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                txtFec.setText("$day/${month + 1}/$year")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Configurar adaptadores para los selectores (Spinners)
        adapterCli = ArrayAdapter(this, R.layout.spinner_item, nombresClientes)
        adapterCli.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnCli.adapter = adapterCli

        adapterPaq = ArrayAdapter(this, R.layout.spinner_item, infoPaquetes)
        adapterPaq.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnPaq.adapter = adapterPaq

        val adapterEst = ArrayAdapter(this, R.layout.spinner_item, opcionesEstado)
        adapterEst.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnEst.adapter = adapterEst

        val posicion = intent.getIntExtra("POSICION", -1)

        // Cargar desde Firestore los clientes y paquetes disponibles para seleccionar
        cargarSpinnersFirestore {
            if (posicion != -1 && posicion < DatosGlobales.listaEnvios.size) {
                val e = DatosGlobales.listaEnvios[posicion]
                txtSeg.setText(e.codigoSeguimiento)
                txtFec.setText(e.fechaEnvio)
                txtPre.setText(e.precio.toString())

                // Buscar el cliente en el Spinner o agregar el guardado previamente
                val idxCli = nombresClientes.indexOf(e.clienteNombre)
                if (idxCli != -1) {
                    spnCli.setSelection(idxCli)
                } else if (e.clienteNombre.isNotEmpty()) {
                    nombresClientes.add(0, e.clienteNombre)
                    adapterCli.notifyDataSetChanged()
                    spnCli.setSelection(0)
                }

                // Buscar coincidencia exacta o parcial del paquete
                var idxPaq = infoPaquetes.indexOf(e.paqueteTipo)
                if (idxPaq == -1 && e.paqueteTipo.contains(" - ")) {
                    val tipoActual = e.paqueteTipo.split(" - ").firstOrNull() ?: ""
                    idxPaq = infoPaquetes.indexOfFirst { it.startsWith(tipoActual) }
                }

                if (idxPaq != -1) {
                    spnPaq.setSelection(idxPaq)
                } else if (e.paqueteTipo.isNotEmpty()) {
                    infoPaquetes.add(0, e.paqueteTipo)
                    adapterPaq.notifyDataSetChanged()
                    spnPaq.setSelection(0)
                }

                val idxEst = opcionesEstado.indexOf(e.estado)
                if (idxEst != -1) spnEst.setSelection(idxEst)

                btnFinalizar.text = "Actualizar Envío"
            }
        }

        btnFinalizar.setOnClickListener {
            val seg = txtSeg.text.toString().trim()
            val fec = txtFec.text.toString().trim()
            val cli = spnCli.selectedItem?.toString() ?: ""
            val paq = spnPaq.selectedItem?.toString() ?: ""
            val est = spnEst.selectedItem?.toString() ?: ""
            val pre = txtPre.text.toString().toDoubleOrNull() ?: 0.0

            if (nombresClientes.isEmpty() || infoPaquetes.isEmpty()) {
                Toast.makeText(this, "Debe registrar clientes y paquetes primero", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (seg.isNotEmpty() && pre > 0) {
                btnFinalizar.isEnabled = false

                val envioData = hashMapOf(
                    "codigoSeguimiento" to seg,
                    "clienteNombre" to cli,
                    "paqueteTipo" to paq,
                    "fechaEnvio" to fec,
                    "estado" to est,
                    "precio" to pre
                )

                if (posicion == -1) {
                    // CREAR ENVÍO EN FIRESTORE
                    db.collection("envios").add(envioData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Envío Registrado en Firestore", Toast.LENGTH_SHORT).show()

                            // Evento Analytics: registro_creado
                            val params = Bundle().apply {
                                putString("tipo_entidad", "envio")
                            }
                            firebaseAnalytics.logEvent("registro_creado", params)

                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnFinalizar.isEnabled = true
                            Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    // ACTUALIZAR ENVÍO EN FIRESTORE
                    val envioExistente = DatosGlobales.listaEnvios[posicion]
                    db.collection("envios").document(envioExistente.id).set(envioData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Envío Actualizado", Toast.LENGTH_SHORT).show()

                            // Evento Analytics: registro_actualizado
                            val params = Bundle().apply {
                                putString("tipo_entidad", "envio")
                            }
                            firebaseAnalytics.logEvent("registro_actualizado", params)

                            finish()
                        }
                        .addOnFailureListener { e ->
                            btnFinalizar.isEnabled = true
                            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                Toast.makeText(this, "Código de seguimiento y precio mayor a 0 son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }

    // Consulta en tiempo real a las colecciones de clientes y paquetes para llenar los spinners
    private fun cargarSpinnersFirestore(onComplete: () -> Unit) {
        db.collection("clientes").get().addOnSuccessListener { resCli ->
            nombresClientes.clear()
            for (doc in resCli) {
                val c = doc.toObject(ClienteModel::class.java)
                if (c.nombre.isNotEmpty()) nombresClientes.add(c.nombre)
            }
            if (nombresClientes.isEmpty()) nombresClientes.add("Sin clientes en Firestore")
            adapterCli.notifyDataSetChanged()

            db.collection("paquetes").get().addOnSuccessListener { resPaq ->
                infoPaquetes.clear()
                for (doc in resPaq) {
                    val p = doc.toObject(PaqueteModel::class.java)
                    if (p.contenido.isNotEmpty()) infoPaquetes.add("${p.tipo} - ${p.contenido}")
                }
                if (infoPaquetes.isEmpty()) infoPaquetes.add("Sin paquetes en Firestore")
                adapterPaq.notifyDataSetChanged()

                onComplete()
            }
        }
    }
}
