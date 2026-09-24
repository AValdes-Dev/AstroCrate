package com.alexander.boxfly.models

data class EnvioModel(
    val codigoSeguimiento: String,
    val clienteNombre: String,
    val paqueteTipo: String,
    val fechaEnvio: String,
    val estado: String,
    val precio: Double
)