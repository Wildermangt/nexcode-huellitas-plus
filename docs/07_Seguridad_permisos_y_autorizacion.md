# HuellitasPlus — Seguridad: permisos, autorización y agente de IA

## 1. Permisos declarados y justificación

El `AndroidManifest.xml` declara 10 permisos. Ninguno de almacenamiento externo.

| Permiso | Para qué se usa | ¿Peligroso? |
|---|---|---|
| `INTERNET` | Firebase, mapas (OpenStreetMap) y Open Pet Food Facts. | No |
| `ACCESS_NETWORK_STATE` | Comprobar la conexión. | No |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | Mostrar veterinarias/tiendas cercanas en el mapa. | Sí (en tiempo de ejecución) |
| `POST_NOTIFICATIONS` | Recordatorios de cuidado de la mascota. | Sí (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Programar los recordatorios a una hora exacta. | Sí (especial) |
| `RECEIVE_BOOT_COMPLETED` | Reprogramar los recordatorios tras reiniciar el teléfono. | No |
| `USE_FULL_SCREEN_INTENT` | Mostrar el recordatorio a pantalla completa. | No |
| `VIBRATE` / `WAKE_LOCK` | Aviso del recordatorio. | No |

Los permisos peligrosos (ubicación y notificaciones) se solicitan en tiempo de ejecución y
la app funciona sin ellos, solo con las funciones que dependen de cada uno desactivadas.

## 2. Control de acceso y autorización

- **Autenticación**: Firebase Auth con correo y contraseña.
- **Reglas de Firestore** (`firestore.rules`): los datos de cada mascota y usuario se
  restringen por `uid`; hay rol de administrador para la gestión global.

### Plan de pruebas de autorización entre usuarios

| # | Caso | Resultado | Captura |
|---|---|---|---|
| CTRL | El dueño lee su propia mascota | ✅ Permitido | `hp-auth-CTRL-permitido.png` |
| A1 | Usuario A intenta leer las mascotas del usuario B | ❌ Denegado (`uid` distinto) | `hp-auth-A1-denegado.png` |
| A2 | Usuario sin sesión intenta leer/escribir | ❌ Denegado (`request.auth != null`) | `hp-auth-A2-denegado.png` |
| A3 | Usuario B intenta leer una conversación en la que no participa | ❌ Denegado (no es participante) | `hp-auth-A3-denegado.png` |
| A4 | Usuario intenta crearse con `rol: "admin"` | ❌ Denegado (nadie se asigna admin desde la app) | `hp-auth-A4-denegado.png` |

**Ejecutado el 29/10/2026 en *Firestore → Rules → Playground*: todos los casos quedaron
denegados y el control (el dueño lee su propio perro, un participante real lee su chat)
permitido.** A3 se probó contra una conversación real. Pasos y resultados completos en
`docs/pruebas-autorizacion-firestore.md`.

> Capturas en `docs/evidencias-29-oct/`. Si aún no se han descargado, ejecutar de
> nuevo cada caso en el Playground del proyecto `huellitasplus-usb` y guardarlas con los
> nombres de la tabla.

## 3. Agente de IA (AsistenteIA)

- **Modelo**: Gemini a través de **Firebase AI Logic**, por lo que **la clave no viaja en
  el APK** (la gestiona Firebase). Es la forma recomendada y la ventaja de HP frente a CG.
- **Pruebas**: documentadas en `PLAN_DE_PRUEBAS_AGENTE_VOZ.md`.
- **Manejo de errores**: `util/Errores.java` centraliza los mensajes; el asistente usa
  try/catch y responde con un mensaje claro cuando falla la red o el modelo.

### Aviso de IA

La pantalla del asistente (`fragment_asistente.xml`) muestra de forma permanente, arriba, el
aviso `@string/aviso_ia`: *"🤖 Estás hablando con un asistente de inteligencia artificial. Da
orientación general y no sustituye la valoración de un médico veterinario."* Así se cumple el
criterio "se informa al usuario cuando interactúa con un sistema de IA".

### Manejo de errores del agente (criterio sección 8)

El agente reintenta ante fallos transitorios y, cuando no puede responder, muestra un mensaje
claro. Los mensajes están centralizados en `util/Errores.java` (`Errores.mensaje(Throwable)`),
que `AsistenteViewModel.alFallar` usa para pintar el error en el chat.

| Situación | Excepción | Qué hace la app |
|---|---|---|
| Servidor del modelo caído | `ServerException` | Reintenta (hasta `REINTENTOS`) y, si persiste, muestra mensaje |
| Cuota agotada | `QuotaExceededException` | Reintenta y luego informa que el servicio no está disponible |
| Tiempo de espera agotado | `RequestTimeoutException` | Reintenta; si falla, pide reintentar |
| Sin conexión | `UnknownHostException` / `ConnectException` / `SocketTimeoutException` | Mensaje `SIN_CONEXION`: "No hay conexión a internet…" |
| Error inesperado | cualquier otro | Mensaje genérico: "Ocurrió un error inesperado. Inténtalo de nuevo." |

Los escenarios de conversación (bienvenida, tono, conocimiento, restricciones y casos límite)
están en `PLAN_DE_PRUEBAS_AGENTE_VOZ.md`.

## 3.b Restricción de la clave de API de Firebase (30/10/2026)

La "Android key (auto created by Firebase)" del proyecto `huellitasplus-usb` se restringió en
Google Cloud Console a **Apps de Android**, con el paquete `com.usb.edadperro` y las dos huellas
SHA-1 (debug `35:90:BD:…:5C:6F` y release `7F:F6:31:…:65:43`). La restricción por API se mantuvo
sin cambios (25 APIs, incluida **"Firebase AI Logic API"**). Captura:
`key-restriccion-huellitas.png`.

**IMPORTANTE — la restricción por app NO es compatible con este asistente (revertida).** El
asistente (`AsistenteIA.java`) usa `GenerativeBackend.googleAI()` (Gemini Developer API), que
llama a `generativelanguage.googleapis.com` **sin** enviar la identidad de la app (paquete y
huella llegan vacíos). Con la restricción "Apps de Android" activa, Google rechaza la petición
con `requests from this android client application <empty> are blocked`
(`API_KEY_ANDROID_APP_BLOCKED`), que fue justo el aviso de "uso activo" que mostró la consola.
Verificado en dispositivo el 30/10: el asistente respondía "no pude responder en este momento".

Por eso **se quitó la restricción de aplicación** de la Android key y se dejó solo la
restricción por API (25 APIs, incluida *Firebase AI Logic API*). La protección correcta para
una clave de cliente Android que usa Firebase AI Logic es **Firebase App Check** (Play
Integrity), no la restricción por paquete/SHA; queda como mejora pendiente.

> Aclaración: se revisó el código de HuellitasPlus (app y backend) y **no existe ninguna llamada
> directa a Gemini ni clave `AIza` embebida**; el tráfico a `generativelanguage` proviene del
> propio SDK de Firebase AI Logic (backend `googleAI`).

## 4. Almacenamiento y comunicaciones

- `allowBackup="false"`.
- Revisar que `SharedPreferences` no guarde datos sensibles (solo preferencias de UI).
- Todas las URL son HTTPS.

## 5. Análisis estático

Ver `DOCUMENTACION/05_Analisis_estatico_APK.md`.
