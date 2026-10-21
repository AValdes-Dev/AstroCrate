package com.alexander.boxfly.models

data class PaqueteModel(
    var id: String = "",
    val tipo: String = "",
    val contenido: String = "",
    val peso: Double = 0.0,
    val esFragil: Boolean = false,
    val valorDeclarado: Double = 0.0
)
