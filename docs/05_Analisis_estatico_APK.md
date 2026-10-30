# HuellitasPlus — Análisis estático del APK (Kali Linux)

Prueba de seguridad de caja blanca sobre la APK de publicación, realizada el 29 de
octubre de 2026 en un laboratorio aislado.

## Entorno y método

| Componente | Detalle |
|---|---|
| Atacante | Kali Linux 2026.2 (VirtualBox), red Host-Only |
| APK analizada | `APK/HuellitasPlus-v2.0-2026-10-29.apk` (16 MB) |
| Herramientas | `apktool` (manifiesto y recursos), `jadx`, `unzip` + `strings`/`grep` sobre `classes*.dex` |

Procedimiento: se decompiló la APK con apktool, se revisó el `AndroidManifest.xml`
(banderas, permisos y componentes exportados), y se buscaron secretos (claves de API,
URLs de backend, tokens) tanto en los recursos (`resources.arsc`, `strings.xml`) como en
el bytecode (`classes*.dex`).

## Resultado general

| Control | Resultado |
|---|---|
| Firma | Esquema **v2** válido · huella SHA-256 `e8:b4:86:6e…bd:d9:fd` (la misma de la v1.0) |
| `android:allowBackup` | **false** ✔ (la corrección del 28/10 viaja en la APK) |
| `android:debuggable` | ausente ✔ |
| Tráfico en claro | sin `usesCleartextTraffic`; con `targetSdk 35` el sistema bloquea HTTP en claro por defecto ✔ |
| minSdk / targetSdk | 24 / 35 |

## Permisos

Todos quedan justificados por una funcionalidad concreta: `INTERNET`;
`ACCESS_FINE_LOCATION` y `ACCESS_COARSE_LOCATION` (mapa de veterinarias);
`SCHEDULE_EXACT_ALARM` y `RECEIVE_BOOT_COMPLETED` (alarmas de cuidados y su
reprogramación al reiniciar); `POST_NOTIFICATIONS` (avisos). No se piden permisos de
contactos, SMS, almacenamiento amplio ni `QUERY_ALL_PACKAGES`.

## Componentes exportados

Solo los esperados: la actividad de inicio (`IntroActivity`, launcher), el receptor de
arranque `ArranqueReceiver` (necesita exportarse para recibir `BOOT_COMPLETED`) y los
componentes estándar de Firebase Auth, Google Play Services y `profileinstaller`. No hay
servicios ni *content providers* propios expuestos.

## Búsqueda de secretos

| Qué se buscó | Resultado |
|---|---|
| Claves `AIza…` | **Una sola**, en `resources.arsc`: la clave de Firebase (`AIzaSyBpO…MEAY`). Es la clave de `google-services.json`, **pública por diseño** |
| Clave de Gemini | No hay ninguna clave incrustada. El agente usa Firebase AI Logic (no una clave en el APK) |
| Backend de pagos | `BuildConfig.BACKEND_URL = https://huellitasplus-pagos.vercel.app` — endpoint público; las llaves de Wompi viven solo en Vercel ✔ |
| Llaves de Wompi (`pub_…`, `prv_…`) | No aparecen en el APK ✔ |

## Hallazgos y recomendaciones

| ID | Hallazgo | Severidad | Recomendación |
|---|---|---|---|
| APK-01 | La clave de API de Firebase viaja en los recursos (inevitable en apps Firebase, pero debe restringirse) | BAJA | En Google Cloud Console, restringir la clave por **aplicación Android** (paquete `com.usb.edadperro` + huella SHA-256 de firma) y por **API** (solo las que usa el proyecto) |
| APK-02 | La URL del backend de pagos es visible en el APK | INFORMATIVA | Es correcto que lo sea; el backend ya valida la sesión (token de Firebase) y recalcula el total con precios de Firestore antes de firmar el checkout |

**Conclusión:** la APK de publicación aplica configuraciones seguras por defecto
(sin respaldo, sin depuración, sin tráfico en claro), no filtra secretos de pago ni la
clave de Gemini, y sus permisos y componentes exportados están justificados. El único
punto de mejora, de severidad baja, es restringir la clave de Firebase en la consola de
Google Cloud.
