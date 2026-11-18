package com.alexander.boxfly

// Interpreta el texto leído por OCR (ML Kit) desde una etiqueta, boleta o documento
// y extrae los datos del cliente. Es lógica pura para poder probarla sin teléfono.
object LectorEtiqueta {

    data class DatosLeidos(
        val nombre: String? = null,
        val rut: String? = null,
        val direccion: String? = null,
        val comuna: String? = null,
        val telefono: String? = null,
        val email: String? = null
    ) {
        val cantidadCampos: Int
            get() = listOf(nombre, rut, direccion, comuna, telefono, email).count { !it.isNullOrBlank() }
    }

    private val regexEmail = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val regexRut = Regex("\\b(\\d{1,2}\\.?\\d{3}\\.?\\d{3})\\s*-\\s*([\\dkK])\\b")
    private val regexTelefono = Regex("(\\+?56\\s?)?(9[\\s.-]?\\d{4}[\\s.-]?\\d{4})")
    private val regexDireccion = Regex(
        "^(av(enida)?\\.?|calle|pasaje|psje\\.?|camino|los|las|el|la)?\\s*[A-Za-zÁÉÍÓÚÑáéíóúñ'. ]{3,}\\s+#?\\d{1,5}\\b.*",
        RegexOption.IGNORE_CASE
    )
    // Etiquetas típicas que preceden al dato ("Nombre: Juan Pérez")
    private val regexEtiqueta = Regex("^(nombre|destinatario|cliente|para|direcci[oó]n|domicilio|comuna|ciudad)\\s*:\\s*(.+)$", RegexOption.IGNORE_CASE)

    fun interpretar(texto: String): DatosLeidos {
        val lineas = texto.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val email = regexEmail.find(texto)?.value?.lowercase()
        val rut = regexRut.find(texto)?.let { formatearRut(it.groupValues[1], it.groupValues[2]) }
        val telefono = regexTelefono.find(texto)?.groupValues?.get(2)?.filter { it.isDigit() }?.let { "+56$it" }

        var nombre: String? = null
        var direccion: String? = null
        var comuna: String? = null

        for (linea in lineas) {
            val etiquetada = regexEtiqueta.find(linea) ?: continue
            val clave = etiquetada.groupValues[1].lowercase()
            val valor = etiquetada.groupValues[2].trim()
            when {
                clave.startsWith("direc") || clave.startsWith("domic") -> direccion = direccion ?: valor
                clave.startsWith("comuna") || clave.startsWith("ciudad") -> comuna = comuna ?: valor
                else -> nombre = nombre ?: valor
            }
        }

        // Sin etiquetas: la dirección es la primera línea con calle + número
        if (direccion == null) {
            direccion = lineas.firstOrNull { linea ->
                regexDireccion.matches(linea) && !regexRut.containsMatchIn(linea) &&
                    !regexTelefono.containsMatchIn(linea) && !regexEmail.containsMatchIn(linea)
            }
        }

        // Comuna sin etiqueta: línea inmediatamente después de la dirección si es solo texto corto
        if (comuna == null && direccion != null) {
            val idx = lineas.indexOfFirst { it.contains(direccion) }
            val siguiente = lineas.getOrNull(idx + 1)
            if (siguiente != null && siguiente.length <= 30 && siguiente.none { it.isDigit() } && !siguiente.contains(":")) {
                comuna = siguiente
            }
        }

        // Nombre sin etiqueta: primera línea con 2 a 4 palabras, solo letras
        if (nombre == null) {
            nombre = lineas.firstOrNull { linea ->
                val palabras = linea.split(Regex("\\s+"))
                palabras.size in 2..4 && linea.all { it.isLetter() || it == ' ' || it == '\'' } &&
                    linea != comuna && palabras.all { it.first().isUpperCase() }
            }
        }

        return DatosLeidos(nombre, rut, direccion, comuna, telefono, email)
    }

    // Deja el RUT como 12.345.678-9
    private fun formatearRut(cuerpo: String, dv: String): String {
        val numeros = cuerpo.filter { it.isDigit() }
        val conPuntos = numeros.reversed().chunked(3).joinToString(".").reversed()
        return "$conPuntos-${dv.uppercase()}"
    }
}
