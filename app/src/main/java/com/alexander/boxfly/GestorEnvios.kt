package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.adapter.EnvioAdapter
import com.alexander.boxfly.models.DatosGlobales

class GestorEnvios : AppCompatActivity() {

    private lateinit var adapter: EnvioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_envios)

        val rv = findViewById<RecyclerView>(R.id.rvEnvios)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = EnvioAdapter(
            DatosGlobales.listaEnvios,
            { posicion ->
                val intent = Intent(this, MantenedorEnvio::class.java)
                intent.putExtra("POSICION", posicion)
                startActivity(intent)
            },
            { posicion ->
                DatosGlobales.listaEnvios.removeAt(posicion)
                adapter.notifyDataSetChanged()
            }
        )
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAgregarEnvio).setOnClickListener {
            startActivity(Intent(this, MantenedorEnvio::class.java))
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
