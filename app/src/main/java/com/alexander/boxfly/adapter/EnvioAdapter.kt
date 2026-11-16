package com.alexander.boxfly.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.alexander.boxfly.ConversorMoneda
import com.alexander.boxfly.R
import com.alexander.boxfly.models.EnvioModel

// Puente entre la lista de Envíos y la pantalla (Núcleo de AstroCrate)
class EnvioAdapter(
    private val lista: MutableList<EnvioModel>, // Envíos generados
    private val onEditar: (Int) -> Unit,        // Modificar guía
    private val onBorrar: (Int) -> Unit         // Eliminar registro
) : RecyclerView.Adapter<EnvioAdapter.ViewHolder>() {

    // Moneda global para convertir los costos de toda la lista
    var monedaGlobal: String = "CLP ($)"

    // Clase interna que sujeta los datos del item_envio
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtSeguimiento: TextView = view.findViewById(R.id.lblSeguimientoItem)
        val txtCliente: TextView = view.findViewById(R.id.lblClienteEnvioItem)
        val txtInfoPaquete: TextView = view.findViewById(R.id.lblInfoPaqueteEnvio)
        val txtFecha: TextView = view.findViewById(R.id.lblFechaItem)
        val txtEstado: TextView = view.findViewById(R.id.lblEstadoItem)
        val txtPrecio: TextView = view.findViewById(R.id.lblPrecioItem)
        val btnVerMapa: Button = view.findViewById(R.id.btnVerMapaEnvio)
        val btnEditar: Button = view.findViewById(R.id.btnEditarEnvio)
        val btnBorrar: Button = view.findViewById(R.id.btnBorrarEnvio)
    }

    // Busca y prepara el molde visual
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_envio, parent, false)
        return ViewHolder(view)
    }

    // Muestra toda la información ligada (Seguimiento, Cliente, Pack, Precio, Ubicación en Mapa)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = lista[position]
        holder.txtSeguimiento.text = item.codigoSeguimiento
        holder.txtCliente.text = "Cliente: ${item.clienteNombre}"
        holder.txtInfoPaquete.text = "Pack: ${item.paqueteTipo}"
        holder.txtFecha.text = "Fecha: ${item.fechaEnvio}"
        holder.txtEstado.text = "Estado: ${item.estado}"

        // Mostrar precio convertido en tiempo real según la divisa global seleccionada
        ConversorMoneda.obtenerConversionEnVivo(item.precio, monedaGlobal) { precioConvertido ->
            holder.txtPrecio.text = "Total: $precioConvertido"
        }
        
        // Abrir ubicación simulada en Google Maps al tocar el botón de mapa
        holder.btnVerMapa.setOnClickListener {
            val lat = if (item.latitud != 0.0) item.latitud else -33.4372
            val lng = if (item.longitud != 0.0) item.longitud else -70.6506
            val uriMapa = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Ubicacion+Envio+${item.codigoSeguimiento})")
            val intentMapa = Intent(Intent.ACTION_VIEW, uriMapa)
            intentMapa.setPackage("com.google.android.apps.maps")
            try {
                holder.itemView.context.startActivity(intentMapa)
            } catch (e: Exception) {
                // Si la app nativa de Maps no abre, abrir en navegador web
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                holder.itemView.context.startActivity(webIntent)
            }
        }

        // Acciones de gestión para cada fila
        holder.btnEditar.setOnClickListener { onEditar(position) }
        holder.btnBorrar.setOnClickListener { onBorrar(position) }
    }

    // Total de envíos en la lista
    override fun getItemCount(): Int = lista.size
}
