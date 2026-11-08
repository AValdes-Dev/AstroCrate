package com.alexander.boxfly.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.R
import com.alexander.boxfly.models.PaqueteModel
import com.bumptech.glide.Glide

// Puente entre la lista de Paquetes y la pantalla
class PaqueteAdapter(
    private val lista: MutableList<PaqueteModel>, // Datos en memoria
    private val onEditar: (Int) -> Unit,          // Función para editar
    private val onBorrar: (Int) -> Unit           // Función para borrar
) : RecyclerView.Adapter<PaqueteAdapter.ViewHolder>() {

    // Referencias a los objetos visuales del item_paquete
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.imgFotoPaqueteItem)
        val txtTipo: TextView = view.findViewById(R.id.lblTipoPaqueteItem)
        val txtContenido: TextView = view.findViewById(R.id.lblContenidoItem)
        val txtPeso: TextView = view.findViewById(R.id.lblPesoItem)
        val txtFragil: TextView = view.findViewById(R.id.lblFragilItem)
        val btnVerFotoDrive: Button = view.findViewById(R.id.btnVerFotoDrive)
        val btnEditar: Button = view.findViewById(R.id.btnEditarPaquete)
        val btnBorrar: Button = view.findViewById(R.id.btnBorrarPaquete)
    }

    // Infla el molde visual
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_paquete, parent, false)
        return ViewHolder(view)
    }

    // Llena los datos y aplica la lógica visual (como el color frágil y foto en Drive)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.txtTipo.text = "Tipo: ${item.tipo}"
        holder.txtContenido.text = "Cont: ${item.contenido}"
        holder.txtPeso.text = "Peso: ${item.peso} Kg"
        
        // Mostrar alerta roja solo si es un objeto frágil
        holder.txtFragil.visibility = if (item.esFragil) View.VISIBLE else View.GONE
        
        // Cargar imagen de Google Drive en la tarjeta y habilitar botón si existe URL
        if (item.fotoDriveUrl.isNotEmpty()) {
            holder.imgFoto.visibility = View.VISIBLE
            holder.btnVerFotoDrive.visibility = View.VISIBLE

            val urlThumbnail = obtenerUrlThumbnailDrive(item.fotoDriveUrl)
            Glide.with(holder.itemView.context)
                .load(urlThumbnail)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .centerCrop()
                .into(holder.imgFoto)

            val abrirDriveListener = View.OnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.fotoDriveUrl))
                intent.setPackage("com.android.chrome")
                try {
                    holder.itemView.context.startActivity(intent)
                } catch (_: Exception) {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(item.fotoDriveUrl))
                    holder.itemView.context.startActivity(Intent.createChooser(fallback, "Abrir foto"))
                }
            }
            holder.btnVerFotoDrive.setOnClickListener(abrirDriveListener)
            holder.imgFoto.setOnClickListener(abrirDriveListener)
        } else {
            holder.imgFoto.visibility = View.GONE
            holder.btnVerFotoDrive.visibility = View.GONE
        }

        holder.btnEditar.setOnClickListener { onEditar(position) }
        holder.btnBorrar.setOnClickListener { onBorrar(position) }
    }

    private fun obtenerUrlThumbnailDrive(urlOriginal: String): String {
        return if (urlOriginal.contains("/d/")) {
            val fileId = urlOriginal.substringAfter("/d/").substringBefore("/")
            "https://drive.google.com/uc?export=view&id=$fileId"
        } else {
            urlOriginal
        }
    }

    // Cantidad de paquetes registrados
    override fun getItemCount(): Int = lista.size
}
