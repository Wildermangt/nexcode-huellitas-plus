# HuellitasPlus — Plan de pruebas y evidencias (Punto 11)

## A. Pruebas automáticas (ya ejecutadas)

| Suite | Pruebas | Qué demuestra | Punto |
|---|---|---|---|
| `CalculadoraEdadTest` | 7 | Cálculo veterinario, ×7, epigenético y etapas | 11.1 |
| `FormatoYRecomendacionesTest` | 4 | Recomendaciones para cada etapa; precios en COP; nombre de producto sin repetir la marca | 11.1 |
| `CarritoCalculadoraTest` | 6 | Subtotales, envío, total, envío gratis, límites por stock | 11.6 |
| `ValidadorCatalogoTest` | 5 | Rechaza otras especies, accesorios, categorías inválidas | 11.5 |
| `CatalogoInicialTest` | 2 | Los 14 productos iniciales son alimentos para perros válidos | 11.5 |
| `DetectorUrgenciasTest` | 7 | Detecta urgencias (con y sin tildes) y no da falsos positivos | 4 |
| `OsmServicioTest` | 4 | Respuestas **reales** de OpenStreetMap (Overpass y su respaldo Nominatim): ids verificables, distancias ordenadas y urgencias solo con dato verificado | 11.4 |
| `DistanciaTest` | 3 | Haversine y formato de distancia | 5 |
| `backend-wompi` (`npm test`) | 5 | Firma de integridad, checksum de eventos, estados, URL del checkout y último intento por referencia | 11.7 |

**Resultado:** 38/38 pruebas de la app y 5/5 del backend en verde.
Ejecutar: Android Studio → clic derecho en `app/src/test` → *Run 'All Tests'*
(o `gradlew testDebugUnitTest`). Captura: `07-pruebas-unitarias.png`.

## B. Pruebas funcionales en el dispositivo

| # | Caso (guía) | Pasos | Resultado esperado | Captura |
|---|---|---|---|---|
| 1 | Calculadora de edad | Inicio → Edad del perro → 5 años, Mediano → Calcular. Luego vacío, `-3` y `40` | 39,0 años humanos · Adulto + recomendaciones. Errores: "Debe ingresar…", "mayor que cero", "no puede superar 30" | 10–13 |
| 2 | Registro e inicio de sesión | Crear cuenta → cerrar sesión → entrar con clave errada → entrar bien | Cuenta creada; "Correo o contraseña incorrectos"; ingreso correcto | 03–06 |
| 3 | Chat con IA | Asistente → "¿Cada cuánto bañar a mi perro?" | Respuesta en español con aviso de no sustituir al veterinario | 20 |
| 3b | Urgencia en el chat | "Mi perro comió chocolate y está temblando" | Banner rojo **Posible urgencia** → botón abre el mapa con urgencias 24 h | 21–22 |
| 4 | Mapa verificable | Veterinarias → permitir ubicación. Tocar **Fuente** en una veterinaria | Marcadores reales; la ficha abre en openstreetmap.org con el mismo nombre y dirección | 23–25 |
| 4b | Sin permiso de ubicación | Negar permiso → buscar "Medellín" | Mapa centrado en Medellín con sus veterinarias | 26 |
| 5 | Catálogo solo para perros | Tienda → filtros y buscador. Admin: crear "Alimento para gatos" | Solo alimentos caninos; el admin recibe "El producto parece ser para otra especie" | 27–30 |
| 6 | Carrito | Agregar 2× Medium Adult + 3× Dentastix; sumar, restar, quitar | Subtotales y total correctos; envío gratis desde $150.000 | 31–32 |
| 7 | Pago (3 estados) | Pagar con 4242… (aprobado), 4111… (rechazado), Daviplata saliendo del simulador sin responder (pendiente) | Pedido **Pagado**, **Pago rechazado** (stock devuelto) y **Pago pendiente**; comprobante PDF | 33–38 |
| 8 | Mensajes entre usuarios | Dos cuentas (emulador + teléfono): Mensajes → Nuevo chat → enviar | Llegan en tiempo real, con remitente, fecha y hora | 39–40 |
| 9 | Almacenamiento | Consola Firebase → Firestore: ver `usuarios`, `perros`, `pedidos`, `pagos`, `conversaciones`… | Documentos creados por las pruebas anteriores | 41 |
| 10 | Errores y sin conexión | Activar modo avión: abrir la app, preguntar a la IA, buscar veterinarias, pagar | Barra "Sin conexión" y mensajes claros; la tienda sigue visible desde la caché | 42–43 |

## C. Lista de capturas numeradas (para el manual)

| N.º | Archivo | Qué mostrar |
|---|---|---|
| 01 | `01-intro.png` | Intro animada con el logo |
| 02 | `02-icono.png` | Ícono de HuellitasPlus en el teléfono |
| 03 | `03-bienvenida-login.png` | Bienvenida e inicio de sesión |
| 04 | `04-registro.png` | Registro con autorización de datos |
| 05 | `05-error-login.png` | Credenciales incorrectas |
| 06 | `06-inicio.png` | Pantalla de inicio rediseñada |
| 07 | `07-pruebas-unitarias.png` | JUnit en verde |
| 08 | `08-perfil.png` | Perfil y opciones |
| 09 | `09-registrar-perro.png` | Formulario de perro con recomendaciones |
| 10 | `10-calculo.png` | Calculadora con resultado |
| 11–13 | `11..13-validaciones.png` | Campo vacío, negativo, mayor a 30 |
| 20 | `20-ia-respuesta.png` | Respuesta del asistente |
| 21 | `21-ia-urgencia.png` | Banner de urgencia |
| 22 | `22-mapa-urgencias.png` | Mapa filtrado por urgencias |
| 23 | `23-permiso-ubicacion.png` | Solicitud de permiso |
| 24 | `24-mapa.png` | Mapa con veterinarias |
| 25 | `25-ficha-osm.png` | Ficha de la veterinaria en openstreetmap.org (prueba de que es real) |
| 26 | `26-busqueda-ciudad.png` | Búsqueda por ciudad |
| 27 | `27-tienda.png` | Catálogo |
| 28 | `28-filtro-busqueda.png` | Filtro y buscador |
| 29 | `29-detalle-producto.png` | Detalle con inventario |
| 30 | `30-admin.png` | Administración de productos y validación |
| 31 | `31-carrito.png` | Carrito con totales |
| 32 | `32-checkout.png` | Dirección y confirmación |
| 33 | `33-wompi.png` | Checkout de Wompi sandbox |
| 34–36 | `34..36-pago-*.png` | Aprobado, rechazado, pendiente |
| 37 | `37-comprobante.png` | Comprobante PDF |
| 38 | `38-pedidos.png` | Historial de pedidos |
| 39–40 | `39..40-chat.png` | Chat entre dos usuarios |
| 41 | `41-firestore.png` | Colecciones en la consola de Firebase |
| 42–43 | `42..43-sin-conexion.png` | Mensajes sin conexión |

## D. Prueba de regresión con la APK recompilada (29 de octubre de 2026)

La APK del 24 de octubre no incluía el módulo de cuidados y alarmas. Se recompiló la versión de publicación (`APK/HuellitasPlus-v2.0-2026-10-29.apk`, misma firma `e8:b4:86:6e…`) y se probó en el emulador Pixel 6 (API 35) con la cuenta de prueba de Camila. 46/46 pruebas JUnit en verde. Capturas en `docs/evidencias-29-oct/`.

| Caso | Resultado obtenido | Estado | Capturas |
|---|---|---|---|
| Inicio de sesión | Clave errada → «Correo o contraseña incorrectos.»; clave correcta → Inicio con los perros de la cuenta | ✅ | 01–03 |
| Calculadora | Vacío, −3 y 40 muestran sus errores; 5 años Mediano → 39.0 años humanos · Adulto | ✅ | 04–07 |
| Cuidados (nuevo) | Pide permiso de notificaciones; se crea «Baño · Prueba de alarma» para las 11:30; sobrevive a cerrar la app a la fuerza | ✅ | 08–11 |
| Alarma (nueva) | Suena a las 11:32:55 (sin permiso de alarmas exactas Android la agenda con ventana de 3 min, como advierte la app) con **Hecho** y **Posponer 10 min**; «Hecho» lo marca Completado y queda en el historial | ✅ | 12–14 |
| Eliminar cuidado | Pide confirmación y cancela sus alarmas (no queda ninguna pendiente en `dumpsys alarm`) | ✅ | — |
| Tienda y carrito | Solo alimentos para perros; 1 × Beggin' $ 15.900 + envío $ 9.000 = $ 24.900 y «Te faltan $ 134.100 para el envío gratis» | ✅ | 15, 16 |
| Sin conexión | Barra «Sin conexión…» y la tienda se ve desde la caché | ✅ | 17 |

Observaciones: el resultado de la calculadora se muestra con punto (39.0) aunque el resto de la app usa coma decimal; el botón del carrito de la barra superior no tiene descripción para lectores de pantalla. Los datos de prueba (cuidado y carrito) se borraron al terminar.
