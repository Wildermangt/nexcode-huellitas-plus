# HuellitasPlus — Cambios de interfaz (29 de octubre de 2026)

Dos cambios solicitados, implementados sobre la versión 2.0 y verificados en un
teléfono físico (Motorola moto g power, Android) con la cuenta de administrador.
Evidencias en `docs/evidencias-29-oct/`.

## 1. Inicio: «Mis perros» por encima de «Servicios»

En la pantalla de inicio se intercambió el orden de las dos secciones: ahora
**«Mis perros»** aparece antes que **«Servicios»** (los accesos a los módulos).

- Archivo: `app/src/main/res/layout/fragment_inicio.xml` (se movió el bloque de la lista
  de perros —encabezado, mensaje «sin perros» y `rvPerros`— delante del `TextView` de
  módulos y el `GridLayout gridAccesos`).
- No cambió ningún identificador, así que `InicioFragment` sigue funcionando igual.
- Evidencia: `18-inicio-mis-perros-arriba.png`.

## 2. Foto 1:1 por mascota

Al registrar o editar un perro se puede **agregar una foto de la mascota**, que se
recorta automáticamente a formato cuadrado **1:1** y se muestra tanto en el formulario
como en la tarjeta de «Mis perros».

**Cómo se guarda (sin costo).** El proyecto no usa Firebase Storage (exige plan de pago).
En su lugar, la foto se recorta al cuadrado centrado, se reduce a 512×512, se comprime a
JPEG y se guarda **en Base64 dentro del propio documento del perro en Firestore** (campo
`fotoBase64`). Una foto así pesa ~40–90 KB, muy por debajo del límite de 1 MiB por
documento, y funciona también sin conexión gracias a la caché de Firestore.

**Archivos modificados/creados:**

| Archivo | Cambio |
|---|---|
| `util/FotoMascota.java` | **Nuevo.** Recorta a 1:1 centrado, reduce a 512×512, comprime a JPEG y codifica en Base64; y el inverso (Base64 → Bitmap) |
| `data/modelo/Perro.java` | Campo `fotoBase64` |
| `data/repo/PerroRepo.java` | Guarda `fotoBase64` en Firestore |
| `ui/perros/PerroFormFragment.java` | Selector de imagen (`GetContent`, sin permisos de almacenamiento), recorte y vista previa |
| `res/layout/fragment_perro_form.xml` | Recuadro 1:1 (`ivFoto`) y botón «Agregar/Cambiar foto» (`btnFoto`) |
| `ui/perros/PerrosAdapter.java` + `res/layout/item_perro.xml` | La tarjeta muestra la foto 1:1 o el ícono de huella si no hay |
| `res/values/strings.xml` | Textos: foto de la mascota, agregar/cambiar foto, error |

**Verificación en el teléfono:** se registró un perro «Foto Prueba» con una foto
apaisada; la vista previa quedó cuadrada (1:1) y la miniatura apareció en «Mis perros»,
mientras los perros sin foto conservan el ícono de huella. El perro de prueba se eliminó
al terminar. Evidencias: `19-form-perro-con-foto.png`, `20-foto-preview-1a1.png`,
`21-mis-perros-con-foto.png`.

**Permisos:** no se añadió ningún permiso. El selector usa el *photo picker* del sistema
(`ActivityResultContracts.GetContent`), que no requiere permiso de almacenamiento.

Las 46 pruebas unitarias siguen en verde y `assembleRelease` compila. La APK
actualizada es `APK/HuellitasPlus-v2.0-2026-10-29.apk` (misma firma de siempre).
