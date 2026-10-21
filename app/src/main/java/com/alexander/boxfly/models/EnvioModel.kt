package com.alexander.boxfly.models

data class EnvioModel(
    var id: String = "",
    val codigoSeguimiento: String = "",
    val clienteNombre: String = "",
    val paqueteTipo: String = "",
    val fechaEnvio: String = "",
    val estado: String = "",
    val precio: Double = 0.0
)
