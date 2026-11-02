# HuellitasPlus — Retrospectiva del sprint

Sprint del 28 de octubre al 2 de noviembre de 2026 (versión 2.0).
Formato: qué se hizo, qué salió bien, qué mejorar y acciones con responsable.

## Qué se hizo

| Fecha | Entrega | Dónde quedó |
|---|---|---|
| 28/10 | Proyecto Gradle, módulo `app` con firma y `BuildConfig` leídos de `local.properties` | `main` |
| 28/10 | Código Java de las pantallas, Firebase y el asistente de IA (Gemini vía Firebase AI Logic) | `main` |
| 28/10 | 46 pruebas unitarias JUnit | `main` |
| 28/10 | Reglas de seguridad de Firestore y configuración de Firebase | `main` |
| 28/10 | Planeación, requisitos trazados al código y mockups de las pantallas | `feature/planeacion-y-diseno` → `main` |
| 30/10 | Foto de la mascota con recorte y vista previa | `feature/foto-mascota` → PR #1, fusionado el 02/11 |
| 30/10 | Documentación técnica, plan de pruebas, seguridad, conclusiones, manual en PDF y capturas | `main` |
| 30/10 | Botón «Hablar con Leandro»: llamada de voz con el agente de Retell AI | `feature/agente-leandro`, **sin fusionar** |

## Qué salió bien

- **Todo el stack siguió siendo gratuito**: Firebase Spark, Gemini por Firebase AI Logic,
  OpenStreetMap, Wompi en sandbox y el backend de pagos en Vercel.
- **Los secretos no viajan en el APK**: el asistente usa Firebase AI Logic sin clave propia
  y las llaves de Wompi viven solo en Vercel; la firma del APK y la configuración local se
  leen de `local.properties`, que está fuera de git.
- **La seguridad se probó con herramientas**, no solo se declaró: análisis estático del APK
  (apktool, jadx) y pruebas de autorización entre usuarios en el Rules Playground.
- **Ramas cortas y con propósito** (`feature/foto-mascota`, `feature/planeacion-y-diseno`) y
  la primera integración por pull request (#1) revisada antes de llegar a `main`.
- La documentación quedó enlazada desde el README y con evidencias (capturas y manual).

## Qué mejorar

- **La integración del agente de voz no se cerró en el sprint.** El botón «Hablar con
  Leandro» (Retell AI, con un servidor en Cloudflare Workers que crea la llamada) está en
  `feature/agente-leandro` y todavía no tiene PR ni pruebas en `main`.
- **La clave de API de Retell llegó inválida** porque se copió desde una página que el
  navegador había traducido automáticamente: la traducción alteró el texto de la clave.
  Costó tiempo encontrar la causa porque el error era un 401 genérico.
- **La firma del APK** dependió de archivos locales que no todos teníamos; al compilar en
  otro equipo la APK salió con otra firma y no se pudo instalar encima de la anterior.
- **Faltó `google-services.json`** en el clon nuevo (con razón está fuera de git) y la
  compilación falló sin un mensaje claro de qué hacer.
- **El antivirus AVG intercepta HTTPS** en uno de los equipos y rompió `git`, Gradle y npm
  hasta configurar su certificado.
- Varias entregas se concentraron en el 28 y el 30 de octubre: los commits grandes
  hicieron más lenta la revisión.

## Acciones

| # | Acción | Responsable | Para |
|---|---|---|---|
| 1 | Abrir el PR de `feature/agente-leandro`, con pruebas en teléfono físico, y revisarlo antes de fusionar | Leo / revisa Jeferson | Próximo sprint |
| 2 | Copiar claves solo desde la consola original con la traducción del navegador desactivada, y validarlas con una llamada de prueba antes de configurarlas | Equipo | Desde ya |
| 3 | Documentar en `docs/01_Configuracion_paso_a_paso.md` qué archivos locales hacen falta (`local.properties`, `google-services.json`, keystore) y de dónde se obtienen, sin incluir su contenido | Jeferson | Próximo sprint |
| 4 | Guardar el keystore de firma en un lugar compartido y seguro del equipo para que todas las APK salgan con la misma firma | Jeferson | Próximo sprint |
| 5 | Anotar en la guía de configuración cómo hacer que `git`, Gradle y npm confíen en el certificado del antivirus sin desactivarlo | Leo | Próximo sprint |
| 6 | Integrar con PR más pequeños y frecuentes, uno por funcionalidad | Equipo | Desde ya |
| 7 | Retomar las mejoras pendientes: App Check con Play Integrity y pruebas de interfaz con Espresso | Equipo | Backlog |
