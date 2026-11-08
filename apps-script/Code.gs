/**
 * AstroCrate — servicios gratuitos con Google Apps Script (reemplaza las extensiones de Firebase).
 *
 * Acciones (POST JSON a la URL de la Web App):
 *   { token, accion: "enviarCorreo",     to, subject, text, html }
 *   { token, accion: "validarDireccion", direccion, comuna }
 *   { token, accion: "subirFoto",        nombre, mimeType, base64 }
 *
 * Propiedades del script (Configuración del proyecto → Propiedades del script):
 *   TOKEN            → clave compartida con la app (APPS_SCRIPT_TOKEN en local.properties)
 *   DRIVE_FOLDER_ID  → ID de la carpeta de Drive donde se guardan las fotos de paquetes
 */

function doPost(e) {
  try {
    var datos = JSON.parse(e.postData.contents);
    var props = PropertiesService.getScriptProperties();

    if (!datos.token || datos.token !== props.getProperty('TOKEN')) {
      return responder({ ok: false, error: 'No autorizado' });
    }

    switch (datos.accion) {
      case 'enviarCorreo':
        return responder(enviarCorreo(datos));
      case 'validarDireccion':
        return responder(validarDireccion(datos));
      case 'subirFoto':
        return responder(subirFoto(datos, props.getProperty('DRIVE_FOLDER_ID')));
      default:
        return responder({ ok: false, error: 'Acción desconocida' });
    }
  } catch (err) {
    return responder({ ok: false, error: String(err) });
  }
}

// Trigger Email: envía con el Gmail de la cuenta dueña del script (100 correos/día en cuentas gratis)
function enviarCorreo(datos) {
  MailApp.sendEmail({
    to: datos.to,
    subject: datos.subject,
    body: datos.text || '',
    htmlBody: datos.html || datos.text || '',
    name: 'AstroCrate'
  });
  return { ok: true };
}

// Validate Address: geocodificador de Google Maps incluido en Apps Script (sin API key ni pago)
function validarDireccion(datos) {
  var consulta = [datos.direccion, datos.comuna, 'Chile'].filter(String).join(', ');
  var respuesta = Maps.newGeocoder().setRegion('cl').setLanguage('es').geocode(consulta);

  if (respuesta.status !== 'OK' || !respuesta.results.length) {
    return { ok: true, valida: false };
  }

  var r = respuesta.results[0];
  // partial_match = Google no encontró la dirección exacta, solo algo parecido.
  // Además debe resolver a una dirección con número, no solo a la calle o la comuna.
  var tiposDireccion = ['street_address', 'premise', 'subpremise', 'route', 'establishment'];
  var esDireccionExacta = r.types.some(function (t) { return tiposDireccion.indexOf(t) !== -1; });
  var valida = !r.partial_match && esDireccionExacta;
  return {
    ok: true,
    valida: valida,
    direccionNormalizada: r.formatted_address,
    lat: r.geometry.location.lat,
    lng: r.geometry.location.lng
  };
}

// Export to Drive: guarda la foto del paquete en la carpeta configurada y la hace pública por enlace
function subirFoto(datos, carpetaId) {
  if (!carpetaId) return { ok: false, error: 'Falta DRIVE_FOLDER_ID en las propiedades del script' };

  var blob = Utilities.newBlob(Utilities.base64Decode(datos.base64), datos.mimeType || 'image/jpeg', datos.nombre);
  var archivo = DriveApp.getFolderById(carpetaId).createFile(blob);

  // Otorga permiso público por enlace para que la app y el correo puedan mostrar la foto
  try {
    archivo.setSharing(DriveApp.Access.ANYONE_WITH_LINK, DriveApp.Permission.VIEW);
  } catch (e) {}

  return { ok: true, fileId: archivo.getId(), url: archivo.getUrl() };
}

function responder(objeto) {
  return ContentService.createTextOutput(JSON.stringify(objeto)).setMimeType(ContentService.MimeType.JSON);
}
