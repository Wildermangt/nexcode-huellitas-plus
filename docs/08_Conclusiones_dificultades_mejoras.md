# HuellitasPlus — Conclusiones, dificultades y mejoras

Estado al 30 de octubre de 2026 (versión 2.0).

## Conclusiones

- La calculadora de edad canina de la Guía I se convirtió en una **plataforma de servicios
  para perros** con nueve módulos: edad del perro, cuenta, asistente con IA, veterinarias,
  tienda, carrito, pagos, mensajes y cuidados con alarmas. Se conservó el cálculo original
  (método veterinario, ×7 y fórmula epigenética de 2020).
- **Todo el stack es gratuito y sin tarjeta de crédito**: Firebase en plan Spark, Gemini
  vía Firebase AI Logic, mapas de OpenStreetMap, Wompi en sandbox y un backend en Vercel.
  Fue una restricción de diseño, no una casualidad, y condicionó varias decisiones (ver
  «Dificultades»).
- El **asistente de IA** no lleva clave en el APK (Firebase AI Logic) y detecta urgencias
  en dos capas. Una es local, con expresiones regulares, y funciona sin conexión; la otra
  pide al modelo marcar `[URGENCIA]`. Con cualquiera de las dos aparece un aviso rojo que
  lleva al mapa de urgencias 24 h, y un aviso fijo recuerda que la IA no reemplaza al
  veterinario.
- **Los pagos no se pueden alterar desde la app**: el backend recalcula el total con los
  precios de Firestore y firma el checkout con SHA-256. Las llaves de Wompi viven solo en
  Vercel. Se probaron los tres estados: aprobado, rechazado y pendiente.
- La seguridad se comprobó con herramientas: análisis estático del APK en Kali (apktool,
  jadx), revisión del almacenamiento local y pruebas de autorización entre usuarios en el
  Rules Playground, donde ningún usuario alcanza datos ajenos. Hay 46 pruebas JUnit en verde.

## Dificultades encontradas y cómo se resolvieron

| Dificultad | Solución aplicada |
|---|---|
| Firebase Cloud Functions y Storage exigen el plan de pago Blaze | Backend de pagos en Vercel (plan Hobby) e imágenes de Open Pet Food Facts (CC BY-SA), consultadas por código de barras |
| Android borra las alarmas programadas al reiniciar el teléfono | Las alarmas se guardan también en `SharedPreferences` y `ArranqueReceiver` las vuelve a programar al encender, sin conexión |
| Sin el permiso de alarmas exactas, Android puede retrasar el aviso | La app usa alarma exacta si hay permiso y, si no, una alarma con ventana de unos minutos, y se lo advierte al usuario |
| El nivel gratuito de Gemini responde a veces «alta demanda» (500/503) o 429 | `AsistenteIA` reintenta una vez tras una pausa corta. El SDK solo guarda el mensaje en el historial cuando hay respuesta, así que no se duplica |
| Restringir la clave de Firebase por aplicación Android bloqueó a Firebase AI Logic (`<empty> application blocked`) | Se dejó solo la restricción por API. La protección adecuada por aplicación es App Check (ver «Mejoras») |
| La APK del 24 de octubre no traía el módulo de cuidados | Se recompiló el 29/10 con la misma firma y se repitió la regresión en el emulador (`03_Plan_de_pruebas_y_evidencias.md`, sección D) |
| Las veterinarias tenían que ser reales y verificables | Overpass y Nominatim (OpenStreetMap): cada marcador abre su ficha pública en openstreetmap.org |

## Mejoras futuras

- **App Check con Play Integrity** al publicar en Google Play, para que solo la app
  firmada use Firestore y Gemini.
- **Wompi en producción** con un comercio real. Hoy los pagos son de prueba (sandbox).
- **Notificaciones push** del estado del pago y de los mensajes nuevos con Firebase Cloud
  Messaging.
- **Pruebas de interfaz automáticas** (Espresso) para los flujos de compra y del asistente,
  que hoy se prueban a mano.
- **Compartir el perfil del perro con el veterinario** (cuidados cumplidos, vacunas y edad)
  como PDF, reutilizando el generador de comprobantes que ya existe.
