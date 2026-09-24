package com.alexander.boxfly.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.R
import com.alexander.boxfly.models.PaqueteModel

// Puente entre la lista de Paquetes y la pantalla
class PaqueteAdapter(
    private val lista: MutableList<PaqueteModel>, // Datos en memoria
    private val onEditar: (Int) -> Unit,          // Función para editar
    private val onBorrar: (Int) -> Unit           // Función para borrar
) : RecyclerView.Adapter<PaqueteAdapter.ViewHolder>() {

    // Referencias a los objetos visuales del item_paquete
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTipo: TextView = view.findViewById(R.id.lblTipoPaqueteItem)
        val txtContenido: TextView = view.findViewById(R.id.lblContenidoItem)
        val txtPeso: TextView = view.findViewById(R.id.lblPesoItem)
        val txtFragil: TextView = view.findViewById(R.id.lblFragilItem)
        val btnEditar: Button = view.findViewById(R.id.btnEditarPaquete)
        val btnBorrar: Button = view.findViewById(R.id.btnBorrarPaquete)
    }

    // Infla el molde visual
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paquete, parent, false)
        return ViewHolder(view)
    }

    // Llena los datos y aplica la lógica visual (como el color frágil)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.txtTipo.text = "Tipo: ${item.tipo}"
        holder.txtContenido.text = "Cont: ${item.contenido}"
        holder.txtPeso.text = "Peso: ${item.peso} Kg"
        
        // Mostrar alerta roja solo si es un objeto frágil
        holder.txtFragil.visibility = if (item.esFragil) View.VISIBLE else View.GONE
        
        holder.btnEditar.setOnClickListener { onEditar(position) }
        holder.btnBorrar.setOnClickListener { onBorrar(position) }
    }

    // Cantidad de paquetes registrados
    override fun getItemCount(): Int = lista.size
}
