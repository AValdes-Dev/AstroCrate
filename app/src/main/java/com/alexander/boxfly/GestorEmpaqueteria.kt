package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.adapter.PaqueteAdapter
import com.alexander.boxfly.models.DatosGlobales
import com.alexander.boxfly.models.PaqueteModel
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore

// Pantalla principal de la gestión de Paquetes
class GestorEmpaqueteria : AppCompatActivity() {

    private lateinit var adapter: PaqueteAdapter
    // Instancia de Firestore
    private val db = FirebaseFirestore.getInstance()
    // Objeto para Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empaqueteria)

        // Inicializar Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        val rv = findViewById<RecyclerView>(R.id.rvEmpaqueteria)
        rv.layoutManager = LinearLayoutManager(this)

        // Configurar adaptador con acciones
        adapter = PaqueteAdapter(
            DatosGlobales.listaPaquetes,
            { posicion ->
                // Abrir pantalla de edición
                val intent = Intent(this, MantenedorPaquete::class.java)
                intent.putExtra("POSICION", posicion)
                startActivity(intent)
            },
            { posicion ->
                // Pedir confirmación antes de eliminar
                confirmarEliminacion(posicion)
            }
        )
        rv.adapter = adapter

        // Botón para agregar un nuevo paquete
        findViewById<Button>(R.id.btnAgregarPaquete).setOnClickListener {
            startActivity(Intent(this, MantenedorPaquete::class.java))
        }

        // Botón volver
        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }

    // Recargar datos desde Firestore cada vez que la pantalla vuelve a primer plano
    override fun onResume() {
        super.onResume()
        cargarPaquetes()
    }

    // Obtener todos los paquetes almacenados en la colección "paquetes"
    private fun cargarPaquetes() {
        db.collection("paquetes")
            .get()
            .addOnSuccessListener { result ->
                DatosGlobales.listaPaquetes.clear()
                for (document in result) {
                    val paquete = document.toObject(PaqueteModel::class.java)
                    paquete.id = document.id // Asignar ID generado por Firestore
                    DatosGlobales.listaPaquetes.add(paquete)
                }
                adapter.notifyDataSetChanged()

                // Registrar evento de consulta en Analytics
                val params = Bundle().apply {
                    putString("tipo_entidad", "paquete")
                }
                firebaseAnalytics.logEvent("listado_consultado", params)

                if (DatosGlobales.listaPaquetes.isEmpty()) {
                    Toast.makeText(this, "No existen paquetes registrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar paquetes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Confirmación mediante diálogo antes de eliminar en la nube
    private fun confirmarEliminacion(posicion: Int) {
        val paquete = DatosGlobales.listaPaquetes[posicion]
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Está seguro de eliminar el paquete '${paquete.contenido}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (paquete.id.isNotEmpty()) {
                    // Borrar documento de la colección de Firestore
                    db.collection("paquetes").document(paquete.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Paquete eliminado", Toast.LENGTH_SHORT).show()

                            // Registrar evento de eliminación
                            val params = Bundle().apply {
                                putString("tipo_entidad", "paquete")
                            }
                            firebaseAnalytics.logEvent("registro_eliminado", params)

                            cargarPaquetes()
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
