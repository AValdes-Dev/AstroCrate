package com.alexander.boxfly.models

//Singleton
//para evitar que los datos globales se pierdan al cambiar de pantalla
object DatosGlobales {
    val listaClientes = mutableListOf<ClienteModel>()
    val listaPaquetes = mutableListOf<PaqueteModel>()
    val listaEnvios = mutableListOf<EnvioModel>()
}
