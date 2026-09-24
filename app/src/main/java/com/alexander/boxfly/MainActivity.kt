package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexander.boxfly.models.DatosGlobales
import com.google.firebase.analytics.FirebaseAnalytics


class MainActivity : AppCompatActivity() {

    // Objeto para Analytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Analytics
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // Valores
        val txtUsuario = findViewById<EditText>(R.id.txtUsuario)
        val txtCorreo = findViewById<EditText>(R.id.txtCorreo)
        val txtClave = findViewById<EditText>(R.id.txtClave)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        //función del boton ingresar
        btnIngresar.setOnClickListener {
            val user = txtUsuario.text.toString()
            val mail = txtCorreo.text.toString()
            val pass = txtClave.text.toString()

            // Buscar si el usuario existe en nuestra lista global
            val usuarioEncontrado = DatosGlobales.listaUsuarios.find { 
                it.nombre == user && it.correo == mail && it.clave == pass 
            }

            if (usuarioEncontrado != null) {
                // Registrar evento de Analytics al entrar
                val params = Bundle()
                params.putString(FirebaseAnalytics.Param.METHOD, "local")
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, params)

                startActivity(Intent(this, Menu::class.java))
                finish()
            } else {
                Toast.makeText(this, "Datos incorrectos", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroLogIn::class.java))
        }
    }
}
