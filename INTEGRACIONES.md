# Integraciones gratuitas — AstroCrate

Proyecto Firebase: `boxfly-95dc6`, **plan Spark (gratis)**.

Las extensiones oficiales (Trigger Email, Validate Address, Export to Drive) exigen plan Blaze con tarjeta.
Aquí se reemplazan por un **Google Apps Script** gratuito (`apps-script/Code.gs`) que corre con tu cuenta Google.
Firestore sigue siendo el registro de todo.

| Requisito de la tarea | Cómo se resuelve | Dónde queda en Firestore |
|---|---|---|
| Trigger Email from Firestore | La app crea el correo en `mail` y Apps Script lo envía con Gmail (`MailApp`) | `mail/{id}` con `to`, `message`, `delivery.state` |
| Validate Address in Firestore | Apps Script usa el geocodificador de Google Maps (`Maps.newGeocoder`) | `clientes/{id}.addressValidity`, `direccionNormalizada`, `lat`, `lng` |
| Export files to Google Drive | La app comprime la foto del paquete y Apps Script la guarda en tu carpeta de Drive (`DriveApp`) | `paquetes/{id}.fotoDriveUrl` |

Límites gratis de Apps Script (cuenta Gmail): ~100 correos/día, ~1.000 geocodificaciones/día, 90 min de ejecución/día.

## 1. Crear el Apps Script

1. Abrir <https://script.google.com> → **Nuevo proyecto** → nombre `AstroCrate`.
2. Borrar el contenido de `Code.gs` y pegar el de `apps-script/Code.gs`.
3. Crear una carpeta en Google Drive (ej. `AstroCrate - Fotos paquetes`) y copiar su ID desde la URL:
   `https://drive.google.com/drive/folders/<ESTE_ES_EL_ID>`.
4. En Apps Script: **Configuración del proyecto (engranaje) → Propiedades del script → Agregar**:
   - `TOKEN` → una clave larga inventada (ej. generada en <https://www.random.org/strings/>).
   - `DRIVE_FOLDER_ID` → el ID de la carpeta.
5. **Implementar → Nueva implementación → tipo: Aplicación web**:
   - Ejecutar como: **Yo**.
   - Quién tiene acceso: **Cualquier usuario**.
6. Autorizar los permisos que pide Google (Gmail, Drive, Maps). Aparece "Google no verificó esta app":
   **Configuración avanzada → Ir a AstroCrate**. Es normal en scripts propios.
7. Copiar la **URL de la aplicación web** (termina en `/exec`).

> Si cambias el código del script: **Implementar → Gestionar implementaciones → Editar → Nueva versión**.
> La URL se mantiene.

## 2. Configurar la app

En `local.properties` (raíz del proyecto, **no se sube a git**):

```properties
APPS_SCRIPT_URL=https://script.google.com/macros/s/XXXXXXXX/exec
APPS_SCRIPT_TOKEN=la-misma-clave-que-pusiste-en-TOKEN
```

Luego en Android Studio: **Sync Gradle** y compilar. Los valores llegan al código como
`BuildConfig.APPS_SCRIPT_URL` / `BuildConfig.APPS_SCRIPT_TOKEN`.

## 3. Reglas de Firestore

`firestore.rules` restringe `clientes`, `paquetes`, `envios` a usuarios autenticados y en `mail`
solo permite crear correos y anotar su resultado. Desplegar (gratis en Spark):

- Consola: **Firestore → Reglas** → pegar el contenido de `firestore.rules` → Publicar; o
- CLI: `npm i -g firebase-tools && firebase login && firebase deploy --only firestore:rules`.

## 4. Probar

1. Crear un cliente con email real y dirección real (ej. `Av. Libertador Bernardo O'Higgins 1449`, comuna `Santiago`).
   Al volver a la lista debe decir "(validada)" (puede tardar unos segundos; reabrir la lista).
2. Crear un envío para ese cliente → llega un correo. En Firestore, `mail/{id}.delivery.state = SUCCESS`.
3. Cambiar el estado del envío → llega otro correo.
4. Crear un paquete con foto → la foto aparece en la carpeta de Drive y `fotoDriveUrl` en Firestore.

## Archivos relacionados

- `apps-script/Code.gs` — servidor gratuito (correo, dirección, Drive).
- `ServicioAppsScript.kt` — cliente HTTP hacia el script.
- `NotificadorCorreo.kt` — correo al cliente desde `MantenedorEnvio`.
- `ValidadorDireccion.kt` — validación desde `MantenedorCliente`.
- `MantenedorPaquete.kt` — botón "Adjuntar Foto" y subida a Drive.

## Seguridad

El `TOKEN` va dentro del APK: evita que cualquiera use tu script, pero alguien que descompile la app
podría obtenerlo. Suficiente para un proyecto académico; para producción usar Cloud Functions con
Firebase Auth (requiere Blaze).

---

# Mensajería e innovaciones (sin costo, sin Blaze)

| Función | Cómo funciona | Dónde queda registro |
|---|---|---|
| **Aviso por SMS / WhatsApp** | Al crear un envío o cambiar su estado, la app ofrece avisar al cliente. Abre la app de SMS o WhatsApp con el mensaje listo (estado + enlace a Google Maps). También hay un botón "💬 Avisar" en cada envío. | `mensajes/{id}` con `canal`, `telefono`, `texto`, `codigoSeguimiento`, `creado` |
| **Etiqueta QR del envío** | Botón "🔳 Etiqueta QR" en cada envío: genera el QR en el teléfono (ZXing) y permite compartirlo/imprimirlo. "📷 Escanear etiqueta QR" en Gestión de Envíos lo lee con el escáner de Google y abre el envío. | Evento Analytics `etiqueta_qr_escaneada` |
| **Escaneo inteligente de etiquetas (IA en el teléfono)** | En el formulario de cliente, "📷 Escanear etiqueta o documento" usa el escáner de documentos de Google y OCR de ML Kit para autocompletar nombre, RUT, dirección, comuna, teléfono y correo. Solo rellena campos vacíos. | Evento Analytics `etiqueta_escaneada` |

Notas:
- No se usa el permiso `SEND_SMS`: Google Play lo rechaza en apps que no son de mensajería. El usuario confirma el envío en su app de SMS/WhatsApp.
- El escáner QR y el de documentos son de Google Play Services: **no piden permiso de cámara** y funcionan sin internet (la primera vez descargan el módulo).
- Teléfonos chilenos se normalizan a `+569XXXXXXXX` (acepta `912345678`, `+56 9 1234 5678`, etc.).
- Publicar las reglas nuevas de `firestore.rules` (colección `mensajes`).

Archivos: `NotificadorMensaje.kt`, `EtiquetaQR.kt`, `LectorEtiqueta.kt`, pruebas en `app/src/test/.../InnovacionesTest.kt`.
