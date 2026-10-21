package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics

class MainActivity : AppCompatActivity() {

    // Objetos para Firebase
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Habilitar recolección de Crashlytics desde el inicio
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)

        // Inicializar Firebase
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        auth = FirebaseAuth.getInstance()

        // Valores
        val txtCorreo = findViewById<EditText>(R.id.txtCorreo)
        val txtClave = findViewById<EditText>(R.id.txtClave)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        // Función del botón ingresar con Firebase Auth
        btnIngresar.setOnClickListener {
            val mail = txtCorreo.text.toString().trim()
            val pass = txtClave.text.toString().trim()

            if (mail.isNotEmpty() && pass.isNotEmpty()) {
                auth.signInWithEmailAndPassword(mail, pass)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Registrar evento de Analytics al entrar
                            val params = Bundle()
                            params.putString(FirebaseAnalytics.Param.METHOD, "firebase_email")
                            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, params)

                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, Menu::class.java))
                            finish()
                        } else {
                            Toast.makeText(
                                this,
                                "Error de autenticación: ${task.exception?.localizedMessage}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Por favor complete correo y contraseña", Toast.LENGTH_SHORT).show()
            }
        }

        btnRegistrar.setOnClickListener {
            startActivity(Intent(this, RegistroLogIn::class.java))
        }
    }

    // Persistencia de sesión: Si el usuario ya inició sesión previamente y no cerró sesión, entra directo al Menú
    override fun onStart() {
        super.onStart()
        val usuarioActual = auth.currentUser
        if (usuarioActual != null) {
            startActivity(Intent(this, Menu::class.java))
            finish()
        }
    }
}
