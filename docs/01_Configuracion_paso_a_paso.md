# HuellitasPlus — Configuración paso a paso (todo gratis, sin tarjeta)

Orden recomendado: **Firebase → reglas → IA → probar la app → Wompi → Vercel → probar pagos.**
Tiempo estimado: 40–60 minutos.

---

## 1. Firebase (plan Spark gratuito)

1. Entra a <https://console.firebase.google.com> con tu cuenta de Google → **Agregar proyecto**
   → nombre `huellitasplus` → puedes desactivar Google Analytics → **Crear proyecto**.
2. En la página del proyecto pulsa el ícono de **Android**:
   - Nombre del paquete: **`com.usb.edadperro`** (debe ser exactamente este).
   - Apodo: HuellitasPlus → **Registrar app**.
   - Descarga **`google-services.json`** y cópialo en `EdadPerro/app/google-services.json`.
   - Los pasos de "Agregar el SDK" ya están hechos en el proyecto: solo **Siguiente** hasta terminar.
3. **Authentication** → Comenzar → pestaña *Sign-in method* → **Correo electrónico/contraseña** → Habilitar → Guardar.
4. **Firestore Database** → Crear base de datos → ubicación `southamerica-east1` (São Paulo)
   o `nam5` → **Iniciar en modo de producción** → Crear.
5. Firestore → pestaña **Reglas** → borra todo, pega el contenido de `EdadPerro/firestore.rules` → **Publicar**.
6. **AI Logic** (menú *Compilación / Build* → *AI Logic*) → **Comenzar** → elige
   **Gemini Developer API** (la opción gratuita, no pide tarjeta) → Habilitar.
   > Si el asistente responde "modelo no encontrado", en `EdadPerro/local.properties`
   > agrega `huellita.geminiModel=` con el nombre de un modelo vigente que aparezca en
   > la consola de AI Logic (por ejemplo `gemini-flash-lite-latest`, que es el valor por defecto) y vuelve a compilar.

### Crear el usuario administrador

1. Abre la app y **crea una cuenta** normal (por ejemplo la tuya).
2. Consola de Firebase → Firestore → colección `usuarios` → abre tu documento →
   cambia el campo `rol` de `cliente` a **`admin`** → Actualizar.
3. Cierra y abre la app → **Perfil → Administrar productos** → menú **⋮ → Cargar catálogo inicial**.
   Se crean las 3 categorías y 14 alimentos para perros con inventario.

Con esto ya funcionan: login, perfil, perros, calculadora, tienda, carrito, pedidos,
asistente IA, mapa de veterinarias y chat entre usuarios. **Solo falta el pago.**

---

## 2. Wompi (sandbox, gratis)

1. Regístrate en <https://comercios.wompi.co> (cuenta de comercio). El ambiente de pruebas
   queda disponible al registrarte; no se hacen cobros reales.
2. En el dashboard activa **Modo de pruebas (Sandbox)**.
3. **Desarrolladores → Llaves del API**: copia la **llave pública de pruebas** (`pub_test_...`).
4. **Desarrolladores → Secretos para integración técnica**: copia
   - el secreto de **Integridad** (`test_integrity_...`)
   - el secreto de **Eventos** (`test_events_...`)

> Los secretos **no** van en la app: solo en Vercel.

---

## 3. Cuenta de servicio de Firebase (para el backend)

Consola de Firebase → ⚙ **Configuración del proyecto** → **Cuentas de servicio** →
**Generar nueva clave privada** → se descarga un `.json`.
Guárdalo fuera del proyecto (no lo subas a GitHub ni lo envíes por chat).

---

## 4. Vercel (plan Hobby, gratis)

1. Crea una cuenta en <https://vercel.com> con tu GitHub.
2. Sube la carpeta `backend-wompi/` a un repositorio de GitHub (puede ser privado) e
   **impórtalo** en Vercel (*Add New → Project*). No necesita configuración de build.
3. En el proyecto de Vercel → **Settings → Environment Variables**, crea:

   | Variable | Valor |
   |---|---|
   | `WOMPI_PUBLIC_KEY` | `pub_test_...` |
   | `WOMPI_INTEGRITY_SECRET` | `test_integrity_...` |
   | `WOMPI_EVENTS_SECRET` | `test_events_...` |
   | `WOMPI_PRIVATE_KEY` | `prv_test_...` (para buscar el pago por referencia si el usuario sale del checkout) |
   | `WOMPI_API_URL` | `https://sandbox.wompi.co/v1` |
   | `FIREBASE_SERVICE_ACCOUNT` | el contenido completo del `.json` de la cuenta de servicio |

4. **Deployments → Redeploy**. Copia la URL, por ejemplo `https://huellitasplus-pagos.vercel.app`.
5. En Wompi → **Desarrolladores → URL de eventos** (sandbox):
   `https://huellitasplus-pagos.vercel.app/api/wompi/webhook`
6. En `EdadPerro/local.properties` agrega:
   ```
   huellita.backendUrl=https://huellitasplus-pagos.vercel.app
   ```
7. Vuelve a compilar la app (*Run ▶* o *Build → Build APK(s)*).

---

## 5. Prueba de pagos (datos sandbox de Wompi)

| Para obtener | Usa |
|---|---|
| **Aprobado** | Tarjeta `4242 4242 4242 4242`, fecha futura, CVC `123` |
| **Rechazado** | Tarjeta `4111 1111 1111 1111` |
| **Pendiente** | Daviplata: en el simulador del checkout no elegir respuesta y pulsar **Salir** (queda en PENDING; la app lo busca por la referencia) |
| Aprobado / rechazado | Nequi `3991111111` / `3992222222` · PSE: banco "que aprueba" / "que rechaza" |

Después de pagar, el pedido cambia solo de estado (webhook) y se puede descargar el comprobante PDF.

---

## Resumen de lo que queda en cada lugar

| Dato | Dónde vive | ¿Secreto? |
|---|---|---|
| `google-services.json` | `EdadPerro/app/` | No (identifica el proyecto) |
| URL del backend, modelo Gemini | `EdadPerro/local.properties` | No |
| Contraseña del keystore | `EdadPerro/local.properties` (no se sube) | **Sí** |
| Llave pública Wompi | Vercel | No |
| Secretos de integridad y eventos | Vercel | **Sí** |
| Cuenta de servicio Firebase | Vercel | **Sí** |
