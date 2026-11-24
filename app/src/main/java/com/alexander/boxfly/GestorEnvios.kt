package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.adapter.EnvioAdapter
import com.alexander.boxfly.models.DatosGlobales
import com.alexander.boxfly.models.EnvioModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore

// Pantalla principal de la gestión de Envíos
class GestorEnvios : AppCompatActivity() {

    private lateinit var adapter: EnvioAdapter
    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()
    // Objeto para registrar eventos en Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_envios)

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val rv = findViewById<RecyclerView>(R.id.rvEnvios)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = EnvioAdapter(
            DatosGlobales.listaEnvios,
            { posicion ->
                // Abrir pantalla de edición
                val intent = Intent(this, MantenedorEnvio::class.java)
                intent.putExtra("POSICION", posicion)
                startActivity(intent)
            },
            { posicion ->
                // Diálogo de confirmación antes de eliminar
                confirmarEliminacion(posicion)
            },
            { posicion ->
                // Etiqueta QR del envío para pegar en el paquete
                DatosGlobales.listaEnvios.getOrNull(posicion)?.let { EtiquetaQR.mostrar(this, it) }
            },
            { posicion ->
                // Aviso por SMS al cliente con confirmación previa
                DatosGlobales.listaEnvios.getOrNull(posicion)?.let { e ->
                    NotificadorMensaje.enviarSmsDirecto(this, db, e.clienteNombre, e.codigoSeguimiento, e.estado, e.latitud, e.longitud, conConfirmacion = true)
                }
            }
        )
        rv.adapter = adapter

        // Configurar Selector Global de Monedas para Envíos
        val spnMonedaGlobal = findViewById<android.widget.Spinner>(R.id.spnMonedaGlobalEnvio)
        val adapterMoneda = android.widget.ArrayAdapter(this, R.layout.spinner_item, ConversorMoneda.opcionesMoneda)
        adapterMoneda.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnMonedaGlobal.adapter = adapterMoneda

        spnMonedaGlobal.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                adapter.monedaGlobal = ConversorMoneda.opcionesMoneda[position]
                adapter.notifyDataSetChanged()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Botón para crear un nuevo envío
        findViewById<Button>(R.id.btnAgregarEnvio).setOnClickListener {
            startActivity(Intent(this, MantenedorEnvio::class.java))
        }

        // Escanear la etiqueta QR de un paquete para abrir su envío directamente
        findViewById<Button>(R.id.btnEscanearQr).setOnClickListener {
            EtiquetaQR.escanear(this) { codigo -> abrirEnvioPorCodigo(codigo) }
        }

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        cargarEnvios()
    }

    // Cargar los envíos desde la colección de Firestore
    private fun cargarEnvios() {
        db.collection("envios")
            .get()
            .addOnSuccessListener { result ->
                DatosGlobales.listaEnvios.clear()
                for (document in result) {
                    val envio = document.toObject(EnvioModel::class.java)
                    envio.id = document.id
                    DatosGlobales.listaEnvios.add(envio)
                }
                adapter.notifyDataSetChanged()

                // Evento en Analytics: listado_consultado
                val params = Bundle().apply {
                    putString("tipo_entidad", "envio")
                }
                firebaseAnalytics.logEvent("listado_consultado", params)

                if (DatosGlobales.listaEnvios.isEmpty()) {
                    Toast.makeText(this, "No existen envíos registrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar envíos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Busca el envío por código en Firestore y ofrece el cambio rápido de estado con el QR
    private fun abrirEnvioPorCodigo(codigo: String) {
        db.collection("envios").whereEqualTo("codigoSeguimiento", codigo).limit(1).get()
            .addOnSuccessListener { resultado ->
                val doc = resultado.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(this, "No existe un envío con código $codigo", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val envio = doc.toObject(EnvioModel::class.java) ?: return@addOnSuccessListener
                envio.id = doc.id

                firebaseAnalytics.logEvent("etiqueta_qr_escaneada", Bundle())

                // Si el paquete YA está entregado, no ofrece cambiar a 'Entregado' nuevamente
                if (envio.estado == "Entregado" || envio.estado == "Devuelto") {
                    AlertDialog.Builder(this)
                        .setTitle("✅ Envío ${envio.codigoSeguimiento} - ${envio.estado.uppercase()}")
                        .setMessage("Cliente: ${envio.clienteNombre}\nPaquete: ${envio.paqueteTipo}\nFecha: ${envio.fechaEnvio}\n\nEste paquete ya completó su ciclo de entrega.")
                        .setPositiveButton("📍 Ver en Google Maps") { _, _ ->
                            val lat = if (envio.latitud != 0.0) envio.latitud else -33.4372
                            val lng = if (envio.longitud != 0.0) envio.longitud else -70.6506
                            val uriMapa = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(Ubicacion+Envio+${envio.codigoSeguimiento})")
                            startActivity(Intent(Intent.ACTION_VIEW, uriMapa))
                        }
                        .setNeutralButton("Ver Formulario") { _, _ ->
                            var posicion = DatosGlobales.listaEnvios.indexOfFirst { it.id == doc.id }
                            if (posicion == -1) {
                                DatosGlobales.listaEnvios.add(envio)
                                posicion = DatosGlobales.listaEnvios.lastIndex
                            }
                            startActivity(Intent(this, MantenedorEnvio::class.java).putExtra("POSICION", posicion))
                        }
                        .setNegativeButton("Cerrar", null)
                        .show()
                    return@addOnSuccessListener
                }

                // Determinar el siguiente estado lógico en el ciclo activo de entrega
                val siguienteEstado = if (envio.estado == "En sucursal") "En tránsito" else "Entregado"

                // Mostrar Formulario Rápido de Cambio de Estado mediante el QR
                AlertDialog.Builder(this)
                    .setTitle("📦 Escáner QR: ${envio.codigoSeguimiento}")
                    .setMessage("Cliente: ${envio.clienteNombre}\nPaquete: ${envio.paqueteTipo}\n\nEstado Actual: ${envio.estado}\nSiguiente Estado: $siguienteEstado")
                    .setPositiveButton("Avanzar a '$siguienteEstado'") { _, _ ->
                        // Consultar ubicación oficial del cliente en Firestore
                        db.collection("clientes").whereEqualTo("nombre", envio.clienteNombre).limit(1).get()
                            .addOnCompleteListener { taskCli ->
                                val docCli = taskCli.result?.documents?.firstOrNull()
                                val clientLat = docCli?.getDouble("lat") ?: -33.4289
                                val clientLng = docCli?.getDouble("lng") ?: -70.6208

                                // Calcular coordenadas según el nuevo estado
                                val (latFinal, lngFinal) = when (siguienteEstado) {
                                    "En sucursal" -> Pair(-33.4372, -70.6506)
                                    "En tránsito", "En ruta de entrega" -> Pair(-33.4255, -70.6120)
                                    "Entregado" -> Pair(clientLat, clientLng)
                                    else -> Pair(clientLat, clientLng)
                                }

                                // Actualizar el estado en Firestore automáticamente
                                db.collection("envios").document(envio.id).update(
                                    mapOf(
                                        "estado" to siguienteEstado,
                                        "latitud" to latFinal,
                                        "longitud" to lngFinal
                                    )
                                ).addOnSuccessListener {
                                    Toast.makeText(this, "Estado actualizado automáticamente a '$siguienteEstado'", Toast.LENGTH_SHORT).show()
                                    // Notificar por correo al cliente con el nuevo estado y mapa
                                    NotificadorCorreo.notificarEstadoEnvio(db, envio.clienteNombre, envio.codigoSeguimiento, envio.paqueteTipo, siguienteEstado, envio.fechaEnvio, false, latFinal, lngFinal)
                                    cargarEnvios()
                                }
                            }
                    }
                    .setNeutralButton("Ver Formulario Completo") { _, _ ->
                        var posicion = DatosGlobales.listaEnvios.indexOfFirst { it.id == doc.id }
                        if (posicion == -1) {
                            DatosGlobales.listaEnvios.add(envio)
                            posicion = DatosGlobales.listaEnvios.lastIndex
                        }
                        startActivity(Intent(this, MantenedorEnvio::class.java).putExtra("POSICION", posicion))
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo buscar el envío. Revisa tu conexión.", Toast.LENGTH_LONG).show()
            }
    }

    // Eliminar envío en Firestore previa confirmación
    private fun confirmarEliminacion(posicion: Int) {
        val envio = DatosGlobales.listaEnvios[posicion]
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Está seguro de eliminar el envío con código ${envio.codigoSeguimiento}?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (envio.id.isNotEmpty()) {
                    db.collection("envios").document(envio.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Envío eliminado", Toast.LENGTH_SHORT).show()

                            // Evento en Analytics: registro_eliminado
                            val params = Bundle().apply {
                                putString("tipo_entidad", "envio")
                            }
                            firebaseAnalytics.logEvent("registro_eliminado", params)

                            cargarEnvios()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al borrar: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
