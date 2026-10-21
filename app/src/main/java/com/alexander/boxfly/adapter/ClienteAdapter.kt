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
    private val lista: MutableList<ClienteModel>,
    private val onEditar: (Int) -> Unit,
    private val onBorrar: (Int) -> Unit
) : RecyclerView.Adapter<ClienteAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtNombre: TextView = view.findViewById(R.id.lblNombreItem)
        val txtRut: TextView = view.findViewById(R.id.lblRutItem)
        val txtDireccion: TextView = view.findViewById(R.id.lblDireccionItem)
        val txtTelefono: TextView = view.findViewById(R.id.lblTelefonoItem)
        val txtEmail: TextView = view.findViewById(R.id.lblEmailItem)
        val btnEditar: Button = view.findViewById(R.id.btnEditarItem)
        val btnBorrar: Button = view.findViewById(R.id.btnBorrarItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cliente, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.txtNombre.text = item.nombre
        holder.txtRut.text = "RUT: ${item.rut}"
        holder.txtDireccion.text = "Dir: ${item.direccion}"
        holder.txtTelefono.text = "Tel: ${item.telefono}"
        holder.txtEmail.text = "Email: ${item.email}"
        
        holder.btnEditar.setOnClickListener { onEditar(position) }
        holder.btnBorrar.setOnClickListener { onBorrar(position) }
    }

    override fun getItemCount(): Int = lista.size
}
