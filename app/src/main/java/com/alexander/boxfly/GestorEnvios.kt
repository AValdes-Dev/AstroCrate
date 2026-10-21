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
            }
        )
        rv.adapter = adapter

        // Botón para crear un nuevo envío
        findViewById<Button>(R.id.btnAgregarEnvio).setOnClickListener {
            startActivity(Intent(this, MantenedorEnvio::class.java))
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
