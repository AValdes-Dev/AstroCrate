package com.alexander.boxfly

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging

// Menú principal con navegación y pruebas de diagnóstico de Firebase
class Menu : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        // Habilitar la recolección de Crashlytics en tiempo de ejecución
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)

        // Solicitar permisos de notificación (Android 13+)
        solicitarPermisoNotificaciones()

        // Obtener Token de registro de FCM para pruebas de notificaciones
        obtenerTokenFCM()

        // Navegación a las gestiones
        findViewById<Button>(R.id.btnClientes).setOnClickListener {
            startActivity(Intent(this, GestionClientes::class.java))
        }
        findViewById<Button>(R.id.btnEmpaqueteria).setOnClickListener {
            startActivity(Intent(this, GestorEmpaqueteria::class.java))
        }
        findViewById<Button>(R.id.btnEnvios).setOnClickListener {
            startActivity(Intent(this, GestorEnvios::class.java))
        }


        // Cerrar sesión activa en Firebase Auth y volver al Login
        findViewById<Button>(R.id.btnCerrar).setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Cierra la sesión en Firebase
            Toast.makeText(this, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun obtenerTokenFCM() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM_TOKEN", "Token de Registro FCM: $token")
            }
        }
    }
}
