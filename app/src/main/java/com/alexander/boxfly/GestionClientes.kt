package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.adapter.ClienteAdapter
import com.alexander.boxfly.models.DatosGlobales

class GestionClientes : AppCompatActivity() {

    private lateinit var adapter: ClienteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_clientes)

        val rv = findViewById<RecyclerView>(R.id.rvClientes)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ClienteAdapter(
            DatosGlobales.listaClientes,
            { posicion ->
                // Acción para EDITAR
                val intent = Intent(this, MantenedorCliente::class.java)
                intent.putExtra("POSICION", posicion) // Enviamos la posición para editar
                startActivity(intent)
            },
            { posicion ->
                // Acción para BORRAR
                DatosGlobales.listaClientes.removeAt(posicion)
                adapter.notifyDataSetChanged()
            }
        )
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAgregarCliente).setOnClickListener {
            startActivity(Intent(this, MantenedorCliente::class.java))
        }

        findViewById<Button>(R.id.btnVolver).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
    }
}
