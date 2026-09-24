package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.adapter.PaqueteAdapter
import com.alexander.boxfly.models.DatosGlobales

class GestorEmpaqueteria : AppCompatActivity() {

    private lateinit var adapter: PaqueteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_empaqueteria)

        val rv = findViewById<RecyclerView>(R.id.rvEmpaqueteria)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = PaqueteAdapter(
            DatosGlobales.listaPaquetes,
            { posicion ->
                val intent = Intent(this, MantenedorPaquete::class.java)
                intent.putExtra("POSICION", posicion)
                startActivity(intent)
            },
            { posicion ->
                DatosGlobales.listaPaquetes.removeAt(posicion)
                adapter.notifyDataSetChanged()
            }
        )
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAgregarPaquete).setOnClickListener {
            startActivity(Intent(this, MantenedorPaquete::class.java))
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
