package com.alexander.boxfly

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexander.boxfly.models.DatosGlobales
import com.alexander.boxfly.models.UsuarioModel

class RegistroLogIn : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_log_in)

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarUsuario)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        btnRegistrar.setOnClickListener {
            val nom = findViewById<EditText>(R.id.txtNombreRegistro).text.toString()
            val cor = findViewById<EditText>(R.id.txtCorreoRegistro).text.toString()
            val cla = findViewById<EditText>(R.id.txtClaveRegistro).text.toString()

            if (nom.isNotEmpty() && cor.isNotEmpty() && cla.isNotEmpty()) {
                // Agregar el nuevo usuario a la lista global
                val nuevoUsuario = UsuarioModel(nom, cor, cla)
                DatosGlobales.listaUsuarios.add(nuevoUsuario)

                Toast.makeText(this, "Usuario Registrado Exitosamente", Toast.LENGTH_SHORT).show()
                finish() // Volver al Login
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
