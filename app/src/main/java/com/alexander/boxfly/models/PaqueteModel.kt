package com.alexander.boxfly.models

data class PaqueteModel(
    val id: Int,
    val tipo: String,
    val contenido: String,
    val peso: Double,
    val esFragil: Boolean
)