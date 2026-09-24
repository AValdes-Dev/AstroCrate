package com.alexander.boxfly

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.alexander.boxfly.models.EnvioModel
import com.alexander.boxfly.models.DatosGlobales
import java.util.Calendar
import java.util.Locale

class MantenedorEnvio : AppCompatActivity() {

    private val opcionesEstado = arrayOf("En sucursal", "En tránsito", "En ruta de entrega", "Entregado", "Devuelto")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenedor_envio)

        val txtSeg = findViewById<EditText>(R.id.txtSeguimiento)
        val spnCli = findViewById<Spinner>(R.id.spnCliente)
        val spnPaq = findViewById<Spinner>(R.id.spnPaquete)
        val txtFec = findViewById<EditText>(R.id.txtFecha)
        val spnEst = findViewById<Spinner>(R.id.spnEstado)
        val txtPre = findViewById<EditText>(R.id.txtPrecio)
        val btnGenerar = findViewById<Button>(R.id.btnGenerarCodigo)
        val btnFinalizar = findViewById<Button>(R.id.btnFinalizarEnvio)
        val btnCancelar = findViewById<Button>(R.id.btnVolverEnvio)

        // Lógica para Generar Codigo automatico para las cajas
        btnGenerar.setOnClickListener {
            val randomNum = (10000..99999).random()
            txtSeg.setText("AC-$randomNum")
            Toast.makeText(this, "Código generado", Toast.LENGTH_SHORT).show()
        }

        // Hacer que el calendario se ponga en español
        txtFec.isFocusable = false
        txtFec.setOnClickListener {
            val locale = Locale("es")
            Locale.setDefault(locale)
            val config = resources.configuration
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)

            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                txtFec.setText("$day/${month + 1}/$year")
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // Preparar listas de Clientes y Paquetes registrados
        val nombresClientes = DatosGlobales.listaClientes.map { it.nombre }.toMutableList()
        val infoPaquetes = DatosGlobales.listaPaquetes.map { "${it.tipo} - ${it.contenido}" }.toMutableList()

        if (nombresClientes.isEmpty()) nombresClientes.add("Sin Clientes registrados")
        if (infoPaquetes.isEmpty()) infoPaquetes.add("Sin Paquetes registrados")

        // Configurar ComboBoxes (Spinners) para que tengan texto blanco
        val adapterCli = ArrayAdapter(this, R.layout.spinner_item, nombresClientes)
        adapterCli.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnCli.adapter = adapterCli

        val adapterPaq = ArrayAdapter(this, R.layout.spinner_item, infoPaquetes)
        adapterPaq.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnPaq.adapter = adapterPaq

        val adapterEst = ArrayAdapter(this, R.layout.spinner_item, opcionesEstado)
        adapterEst.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnEst.adapter = adapterEst

        val posicion = intent.getIntExtra("POSICION", -1)

        // Lógica para EDITAR
        if (posicion != -1) {
            val e = DatosGlobales.listaEnvios[posicion]
            txtSeg.setText(e.codigoSeguimiento)
            txtFec.setText(e.fechaEnvio)
            txtPre.setText(e.precio.toString())
            
            // Buscar y seleccionar en los spinners
            val idxCli = nombresClientes.indexOf(e.clienteNombre)
            if (idxCli != -1) spnCli.setSelection(idxCli)

            val idxPaq = infoPaquetes.indexOf(e.paqueteTipo)
            if (idxPaq != -1) spnPaq.setSelection(idxPaq)

            val idxEst = opcionesEstado.indexOf(e.estado)
            if (idxEst != -1) spnEst.setSelection(idxEst)
            
            btnFinalizar.text = "Actualizar Envío"
        }

        btnFinalizar.setOnClickListener {
            val seg = txtSeg.text.toString()
            val fec = txtFec.text.toString()
            val cli = spnCli.selectedItem.toString()
            val paq = spnPaq.selectedItem.toString()
            val est = spnEst.selectedItem.toString()
            val pre = txtPre.text.toString().toDoubleOrNull() ?: 0.0

            // No permitir guardar si no hay clientes o paquetes reales
            if (DatosGlobales.listaClientes.isEmpty() || DatosGlobales.listaPaquetes.isEmpty()) {
                Toast.makeText(this, "Debe registrar cliente y paquete primero", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (seg.isNotEmpty() && pre > 0) {
                if (posicion == -1) {
                    // CREAR
                    val nuevo = EnvioModel(seg, cli, paq, fec, est, pre)
                    DatosGlobales.listaEnvios.add(nuevo)
                    Toast.makeText(this, "Envío Generado con Éxito", Toast.LENGTH_SHORT).show()
                } else {
                    // ACTUALIZAR
                    val editado = EnvioModel(seg, cli, paq, fec, est, pre)
                    DatosGlobales.listaEnvios[posicion] = editado
                    Toast.makeText(this, "Envío Actualizado", Toast.LENGTH_SHORT).show()
                }
                finish()
            } else {
                Toast.makeText(this, "Código y Precio son obligatorios", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }
}
