package com.alexander.boxfly.models

data class ClienteModel(
    var id: String = "",
    val nombre: String = "",
    val rut: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val email: String = ""
)
