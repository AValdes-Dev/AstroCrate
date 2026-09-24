package com.alexander.boxfly

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.alexander.boxfly.models.PaqueteModel
import com.alexander.boxfly.models.DatosGlobales

class MantenedorPaquete : AppCompatActivity() {

    private val opcionesTipo = arrayOf("Sobre", "Caja Chica", "Caja Grande", "Carga Irregular")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mantenedor_paquete)

        val spnTipo = findViewById<Spinner>(R.id.spnTipoPaquete)
        val txtCont = findViewById<EditText>(R.id.txtContenido)
        val txtPeso = findViewById<EditText>(R.id.txtPeso)
        val swFragil = findViewById<SwitchCompat>(R.id.swFragil)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPaquete)
        val btnCancelar = findViewById<Button>(R.id.btnCancelarPaquete)

        // Configurar ComboBox (Spinner) con texto BLANCO
        val adapterTipo = ArrayAdapter(this, R.layout.spinner_item, opcionesTipo)
        adapterTipo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnTipo.adapter = adapterTipo

        val posicion = intent.getIntExtra("POSICION", -1)

        if (posicion != -1) {
            val p = DatosGlobales.listaPaquetes[posicion]
            val index = opcionesTipo.indexOf(p.tipo)
            if (index != -1) spnTipo.setSelection(index)
            
            txtCont.setText(p.contenido)
            txtPeso.setText(p.peso.toString())
            swFragil.isChecked = p.esFragil
            btnGuardar.text = "Actualizar Paquete"
        }

        btnGuardar.setOnClickListener {
            val tipo = spnTipo.selectedItem.toString()
            val cont = txtCont.text.toString()
            val peso = txtPeso.text.toString().toDoubleOrNull() ?: 0.0

            if (cont.isNotEmpty()) {
                if (posicion == -1) {
                    val nuevo = PaqueteModel(DatosGlobales.listaPaquetes.size + 1, tipo, cont, peso, swFragil.isChecked)
                    DatosGlobales.listaPaquetes.add(nuevo)
                    Toast.makeText(this, "Paquete Registrado", Toast.LENGTH_SHORT).show()
                } else {
                    val editado = PaqueteModel(DatosGlobales.listaPaquetes[posicion].id, tipo, cont, peso, swFragil.isChecked)
                    DatosGlobales.listaPaquetes[posicion] = editado
                    Toast.makeText(this, "Paquete Actualizado", Toast.LENGTH_SHORT).show()
                }
                finish()
            } else {
                Toast.makeText(this, "La descripción es obligatoria", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener { finish() }
    }
}
