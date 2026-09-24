package com.alexander.boxfly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.R
import com.alexander.boxfly.models.ClienteModel

// Puente entre la lista de Clientes y la pantalla
class ClienteAdapter(
    private val lista: MutableList<ClienteModel>, // Datos almacenados en RAM
    private val onEditar: (Int) -> Unit,         // Acción para modificar
    private val onBorrar: (Int) -> Unit          // Acción para eliminar
) : RecyclerView.Adapter<ClienteAdapter.ViewHolder>() {

    // Clase para sujetar los componentes visuales del XML
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.lblNombreItem)
        val txtRut: TextView = view.findViewById(R.id.lblRutItem)
        val txtDireccion: TextView = view.findViewById(R.id.lblDireccionItem)
        val txtTelefono: TextView = view.findViewById(R.id.lblTelefonoItem)
        val btnEditar: Button = view.findViewById(R.id.btnEditarItem)
        val btnBorrar: Button = view.findViewById(R.id.btnBorrarItem)
    }

    // Crea el molde visual (item_cliente)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
        return ViewHolder(view)
    }

    // Vincula los datos reales con los TextViews y Buttons
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.txtNombre.text = item.nombre
        holder.txtRut.text = "RUT: ${item.rut}"
        holder.txtDireccion.text = "Dir: ${item.direccion}"
        holder.txtTelefono.text = "Tel: ${item.telefono}"
        
        // Configuración de clics para el CRUD
        holder.btnEditar.setOnClickListener { onEditar(position) }
        holder.btnBorrar.setOnClickListener { onBorrar(position) }
    }

    // Indica cuántos elementos hay en la lista
    override fun getItemCount(): Int = lista.size
}
