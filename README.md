# HuellitasPlus (EdadPerro)

Aplicación Android de Nexcode Corporation para dueños de perros: calculadora de edad humana equivalente, perfil de mascotas, cuidados con alarmas, tienda con pagos Wompi, veterinarias cercanas en OpenStreetMap, chat y asistente con IA (Gemini vía Firebase AI Logic).

Optativa II – Desarrollo de Aplicaciones Móviles · Universidad de San Buenaventura, Bogotá
Docente: Ing. Jairo Armando Salcedo Aranda

| Integrante | Rol Scrum |
|---|---|
| Jeferson Wilderman González Tenjo | Product Owner · Developer |
| José Leandro Ocampo Camacho | Scrum Master · Developer |

Documentación del proyecto en [`docs/`](docs/):

| Documento | Contenido |
|---|---|
| [01 Planeación y requisitos](docs/01-planeacion-y-requisitos.md) · [02 Mockups](docs/02-mockups.md) | Problema, alcance, requisitos y diseño de pantallas |
| [01 Configuración paso a paso](docs/01_Configuracion_paso_a_paso.md) | Firebase, Gemini, Wompi y compilación |
| [02 Documento técnico](docs/02_Documento_tecnico.md) | Arquitectura, tecnologías y decisiones |
| [03 Plan de pruebas y evidencias](docs/03_Plan_de_pruebas_y_evidencias.md) | Casos de uso probados y su resultado |
| [04 Cumplimiento de la rúbrica](docs/04_Cumplimiento_rubrica.md) | Criterio por criterio |
| [05 Análisis estático del APK](docs/05_Analisis_estatico_APK.md) | Kali (apktool, jadx), secretos, permisos y almacenamiento local |
| [06 Cambios de interfaz 29/10](docs/06_Cambios_interfaz_29oct.md) | Foto de la mascota, cuidados y alarmas |
| [07 Seguridad, permisos y autorización](docs/07_Seguridad_permisos_y_autorizacion.md) · [Pruebas en el Playground](docs/pruebas-autorizacion-firestore.md) | Reglas de Firestore y pruebas entre usuarios |
| [08 Conclusiones, dificultades y mejoras](docs/08_Conclusiones_dificultades_mejoras.md) | Qué se logró, qué costó y qué sigue |
| [09 Manual de usuario](docs/09_Manual_de_usuario.md) | Cómo usar cada módulo de la app |

Capturas de las pruebas funcionales en [`docs/capturas/`](docs/capturas/) (sin las tres que
muestran una dirección de entrega) y de la regresión del 29/10 en
[`docs/evidencias-29-oct/`](docs/evidencias-29-oct/).

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
