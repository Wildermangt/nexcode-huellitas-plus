# HuellitasPlus (EdadPerro)

Aplicación Android de Nexcode Corporation para dueños de perros: calculadora de edad humana equivalente, perfil de mascotas, cuidados con alarmas, tienda con pagos Wompi, veterinarias cercanas en OpenStreetMap, chat y asistente con IA (Gemini vía Firebase AI Logic).

Optativa II – Desarrollo de Aplicaciones Móviles · Universidad de San Buenaventura, Bogotá
Docente: Ing. Jairo Armando Salcedo Aranda

| Integrante | Rol Scrum |
|---|---|
| Jeferson Wilderman González Tenjo | Product Owner · Developer |
| José Leandro Ocampo Camacho | Scrum Master · Developer |

## Tecnologías

- Java, Android SDK 35 (`minSdk 24`), Gradle
- Firebase Authentication y Cloud Firestore (reglas en `firestore.rules`)
- Gemini Developer API mediante Firebase AI Logic (sin clave de API en la app)
- osmdroid, Overpass y Nominatim (OpenStreetMap)
- OkHttp para el backend de pagos (Vercel)
- JUnit para las pruebas unitarias (`app/src/test`)

## Configuración local

Estos archivos **no se versionan** y cada integrante los crea en su equipo:

| Archivo | Contenido |
|---|---|
| `app/google-services.json` | Se descarga de la consola de Firebase (proyecto `huellitasplus-usb`) |
| `local.properties` | `sdk.dir`, `keystore.password`, `huellita.backendUrl` y, opcional, `huellita.geminiModel` |
| `edadperro.jks` | Almacén de claves para firmar la versión release |

## Compilar y probar

```
./gradlew assembleDebug        # APK de depuración
./gradlew testDebugUnitTest    # pruebas unitarias
firebase deploy --only firestore:rules
```
