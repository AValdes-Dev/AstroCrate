package com.alexander.boxfly.models

data class ClienteModel(
    var id: String = "",
    val nombre: String = "",
    val rut: String = "",
    val direccion: String = "",
    val comuna: String = "",
    val telefono: String = "",
    val email: String = "",
    // Resultado de ValidadorDireccion (null = aún no validada)
    val addressValidity: Boolean? = null
)
