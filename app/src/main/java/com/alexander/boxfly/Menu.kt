package com.alexander.boxfly

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Menu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Botones funcionales
        findViewById<Button>(R.id.btnClientes).setOnClickListener {
            startActivity(Intent(this, GestionClientes::class.java))
        }
        findViewById<Button>(R.id.btnEmpaqueteria).setOnClickListener {
            startActivity(Intent(this, GestorEmpaqueteria::class.java))
        }
        findViewById<Button>(R.id.btnEnvios).setOnClickListener {
            startActivity(Intent(this, GestorEnvios::class.java))
        }
        findViewById<Button>(R.id.btnCerrar).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}