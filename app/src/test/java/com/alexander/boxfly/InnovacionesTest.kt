package com.alexander.boxfly

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InnovacionesTest {

    @Test
    fun interpretaEtiquetaConRotulos() {
        val texto = """
            DESTINATARIO
            Nombre: Camila Rojas Soto
            RUT 12345678-k
            Dirección: Av. Providencia 1234
            Comuna: Providencia
            Fono +56 9 8765 4321
            camila.rojas@gmail.com
        """.trimIndent()

        val datos = LectorEtiqueta.interpretar(texto)

        assertEquals("Camila Rojas Soto", datos.nombre)
        assertEquals("12.345.678-K", datos.rut)
        assertEquals("Av. Providencia 1234", datos.direccion)
        assertEquals("Providencia", datos.comuna)
        assertEquals("+56987654321", datos.telefono)
        assertEquals("camila.rojas@gmail.com", datos.email)
        assertEquals(6, datos.cantidadCampos)
    }

    @Test
    fun interpretaEtiquetaSinRotulos() {
        val texto = """
            Juan Pérez González
            Los Aromos 456
            Maipú
            9 1234 5678
        """.trimIndent()

        val datos = LectorEtiqueta.interpretar(texto)

        assertEquals("Juan Pérez González", datos.nombre)
        assertEquals("Los Aromos 456", datos.direccion)
        assertEquals("Maipú", datos.comuna)
        assertEquals("+56912345678", datos.telefono)
        assertNull(datos.rut)
        assertNull(datos.email)
    }

    @Test
    fun normalizaTelefonosChilenos() {
        assertEquals("56912345678", NotificadorMensaje.normalizarTelefono("+56 9 1234 5678"))
        assertEquals("56912345678", NotificadorMensaje.normalizarTelefono("912345678"))
        assertEquals("56912345678", NotificadorMensaje.normalizarTelefono("1234 5678"))
        assertNull(NotificadorMensaje.normalizarTelefono("123"))
        assertNull(NotificadorMensaje.normalizarTelefono(""))
    }

    @Test
    fun leeCodigoDesdeQr() {
        assertEquals("AC-12345", EtiquetaQR.leerCodigo(EtiquetaQR.contenido("AC-12345")))
        assertEquals("AC-12345", EtiquetaQR.leerCodigo("AC-12345"))
        assertNull(EtiquetaQR.leerCodigo("https://otra-empresa.cl/123"))
    }
}
