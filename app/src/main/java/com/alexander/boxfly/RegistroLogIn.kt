package com.alexander.boxfly

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegistroLogIn : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_log_in)

        auth = FirebaseAuth.getInstance()

        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarUsuario)
        val btnVolver = findViewById<Button>(R.id.btnVolverLogin)

        btnRegistrar.setOnClickListener {
            val cor = findViewById<EditText>(R.id.txtCorreoRegistro).text.toString().trim()
            val cla = findViewById<EditText>(R.id.txtClaveRegistro).text.toString().trim()

            if (cor.isNotEmpty() && cla.isNotEmpty()) {
                if (cla.length < 6) {
                    Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                auth.createUserWithEmailAndPassword(cor, cla)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Cerrar sesión automática de Firebase para requerir inicio de sesión manual
                            auth.signOut()
                            Toast.makeText(this, "Usuario Registrado Exitosamente. Por favor inicie sesión.", Toast.LENGTH_SHORT).show()
                            finish() // Volver a la pantalla de Login (MainActivity)
                        } else {
                            Toast.makeText(
                                this,
                                "Error al registrar: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor, complete correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnVolver.setOnClickListener { finish() }
    }
}
