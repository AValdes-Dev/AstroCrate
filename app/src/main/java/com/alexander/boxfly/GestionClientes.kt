package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.adapter.ClienteAdapter
import com.alexander.boxfly.models.ClienteModel
import com.alexander.boxfly.models.DatosGlobales
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore

// Pantalla principal de la gestión de Clientes
class GestionClientes : AppCompatActivity() {

    private lateinit var adapter: ClienteAdapter
    // Instancia de la base de datos Firestore
    private val db = FirebaseFirestore.getInstance()
    // Objeto para registrar los eventos de Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_clientes)

        // Inicializar Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Configurar el RecyclerView para listar
        val rv = findViewById<RecyclerView>(R.id.rvClientes)
        rv.layoutManager = LinearLayoutManager(this)

        // Adaptador con acciones de Editar y Eliminar
        adapter = ClienteAdapter(
            DatosGlobales.listaClientes,
            { posicion ->
                // Abrir pantalla para editar el cliente seleccionado
                val intent = Intent(this, MantenedorCliente::class.java)
                intent.putExtra("POSICION", posicion)
                startActivity(intent)
            },
            { posicion ->
                // Confirmación previa antes de eliminar en Firestore
                confirmarEliminacion(posicion)
            }
        )
        rv.adapter = adapter

        // Botón para ir a crear nuevo cliente
        findViewById<Button>(R.id.btnAgregarCliente).setOnClickListener {
            startActivity(Intent(this, MantenedorCliente::class.java))
        }

        // Botón para volver al menú
        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }

    // Al volver a la pantalla se recargan los datos desde Firestore
    override fun onResume() {
        super.onResume()
        cargarClientes()
    }

    // Consultar todos los clientes guardados en la nube
    private fun cargarClientes() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                DatosGlobales.listaClientes.clear()
                for (document in result) {
                    val cliente = document.toObject(ClienteModel::class.java)
                    cliente.id = document.id // Guardamos el ID del documento en Firestore
                    DatosGlobales.listaClientes.add(cliente)
                }
                adapter.notifyDataSetChanged()

                // Registrar evento de Analytics exigido por la pauta
                val params = Bundle().apply {
                    putString("tipo_entidad", "cliente")
                }
                firebaseAnalytics.logEvent("listado_consultado", params)

                if (DatosGlobales.listaClientes.isEmpty()) {
                    Toast.makeText(this, "No existen clientes registrados", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar clientes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Diálogo de alerta para confirmar si desea eliminar el cliente
    private fun confirmarEliminacion(posicion: Int) {
        val cliente = DatosGlobales.listaClientes[posicion]
        AlertDialog.Builder(this)
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Está seguro de que desea eliminar a ${cliente.nombre}?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (cliente.id.isNotEmpty()) {
                    // Borrar registro en Firestore por su ID
                    db.collection("clientes").document(cliente.id).delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Cliente eliminado", Toast.LENGTH_SHORT).show()
                            
                            // Registrar evento de eliminación en Analytics
                            val params = Bundle().apply {
                                putString("tipo_entidad", "cliente")
                            }
                            firebaseAnalytics.logEvent("registro_eliminado", params)

                            // Actualizar el listado
                            cargarClientes()
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
