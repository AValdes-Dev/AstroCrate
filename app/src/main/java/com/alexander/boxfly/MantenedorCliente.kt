package com.alexander.boxfly

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexander.boxfly.models.ClienteModel
import com.alexander.boxfly.models.DatosGlobales

class MantenedorCliente : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenedor_cliente)

        val txtNombre = findViewById<EditText>(R.id.txtNombreCliente)
        val txtRut = findViewById<EditText>(R.id.txtRutCliente)
        val txtDir = findViewById<EditText>(R.id.txtDireccionCliente)
        val txtTel = findViewById<EditText>(R.id.txtTelefonoCliente)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarCliente)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarCliente)

        // Verificamos si venimos a EDITAR (recibimos la posición)
        val posicion = intent.getIntExtra("POSICION", -1)

        if (posicion != -1) {
            // Llenamos los campos con los datos actuales
            val c = DatosGlobales.listaClientes[posicion]
            txtNombre.setText(c.nombre)
            txtRut.setText(c.rut)
            txtDir.setText(c.direccion)
            txtTel.setText(c.telefono)
            btnGuardar.text = "Actualizar Cambios"
        }

        btnGuardar.setOnClickListener {
            val nom = txtNombre.text.toString()
            val rut = txtRut.text.toString()
            val dir = txtDir.text.toString()
            val tel = txtTel.text.toString()

            if (nom.isNotEmpty() && rut.isNotEmpty()) {
                if (posicion == -1) {
                    // CREAR NUEVO
                    val nuevo = ClienteModel(DatosGlobales.listaClientes.size + 1, nom, rut, dir, tel)
                    DatosGlobales.listaClientes.add(nuevo)
                    Toast.makeText(this, "Cliente Guardado", Toast.LENGTH_SHORT).show()
                } else {
                    // ACTUALIZAR EXISTENTE
                    val editado = ClienteModel(DatosGlobales.listaClientes[posicion].id, nom, rut, dir, tel)
                    DatosGlobales.listaClientes[posicion] = editado
                    Toast.makeText(this, "Cliente Actualizado", Toast.LENGTH_SHORT).show()
                }
                finish()
            } else {
                Toast.makeText(this, "Faltan datos obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }
}
