package com.alexander.boxfly.models

//Singleton
//para evitar que los datos globales se pierdan al cambiar de pantalla
object DatosGlobales {
    // Lista de usuarios registrados (Admin siendo el principal)
    val listaUsuarios = mutableListOf<UsuarioModel>(
        UsuarioModel("admin", "admin@gmail.com", "1234")
    )

    // Variables temporales
    var nombreUsuario = "admin"
    var correoUsuario = "admin@gmail.com"
    var claveUsuario = "1234"

    val listaClientes = mutableListOf<ClienteModel>()
    val listaPaquetes = mutableListOf<PaqueteModel>()
    val listaEnvios = mutableListOf<EnvioModel>()
}
