# HuellitasPlus — Cumplimiento de la rúbrica (50 puntos)

Leyenda:
- ✅ **Verificado:** se probó en el emulador contra el proyecto real de Firebase.
- 🟡 **Implementado:** el código está y compila con pruebas en verde, pero falta demostrarlo en el dispositivo.
- ⏳ **Depende de cuentas externas:** Wompi y Vercel.

| # | Requisito | Pts | Estado | Cómo se demuestra en la sustentación |
|---|---|---|---|---|
| 1.1 | Conserva y calcula la equivalencia edad canina/humana | 2 | ✅ | Inicio → Edad del perro → 5 años, Mediano → 39,0 años · Adulto |
| 1.2 | Registra nombre, edad y características del perro | 1 | ✅ | Inicio → Mis perros → Agregar (nombre, raza, edad, peso, tamaño, sexo, notas) |
| 1.3 | Navegación funcional entre módulos | 1 | ✅ | Barra inferior (5 módulos) y accesos rápidos de Inicio |
| 1.4 | Nombre, logo, ícono y pantalla de bienvenida | 1 | ✅ | Ícono en el lanzador → splash → video de intro → bienvenida |
| 2.1 | Registrar usuarios e iniciar/cerrar sesión | 2 | ✅ registro, inicio y cierre (probado en el Motorola) | Crear cuenta → Perfil → Cerrar sesión → Entrar |
| 2.2 | Consultar/actualizar perfil y asociar perros | 1 | ✅ consulta y edición (teléfono y dirección guardados en Firestore) | Perfil → Editar perfil; los perros se guardan en `usuarios/{uid}/perros` |
| 3.1 | El chat recibe consultas y muestra respuestas de IA | 3 | ✅ | Asistente → pregunta → respuesta de Gemini (`gemini-flash-lite-latest`), personalizada con los perros del usuario. Captura 20 |
| 3.2 | Orientación sobre cuidado y alimentación | 2 | ✅ | Sugerencias: alimento de cachorro, baño, vacunas, dientes |
| 3.3 | Advierte que no sustituye al veterinario y deriva urgencias | 2 | ✅ aviso y banner de urgencia → mapa | Aviso fijo + "mi perro comió chocolate" → banner rojo → mapa de urgencias. Capturas 21–22 |
| 4.1 | Permiso de ubicación y búsqueda por ciudad o dirección | 2 | ✅ | Veterinarias → permitir/negar → buscar "Medellín" |
| 4.2 | Veterinarias reales con dirección y contacto verificables | 2 | ✅ | Botón **Fuente** → ficha pública en openstreetmap.org |
| 4.3 | Distancia/ruta y urgencias solo si están verificadas | 2 | ✅ | Distancia en cada tarjeta, **Cómo llegar**, filtro "Urgencias 24 h" (solo `emergency=yes` u horario 24/7) |
| 5.1 | Catálogo exclusivamente de alimentos para perros | 2 | ✅ (JUnit + reglas) | Tienda; el admin intenta crear "alimento para gatos" y la app lo rechaza |
| 5.2 | Imagen, nombre, marca, presentación, descripción y precio | 2 | ✅ | Tienda → detalle de producto |
| 5.3 | Búsqueda, filtros y disponibilidad de inventario | 2 | ✅ | Buscar "royal", chips de categoría, "N disponibles" o "Agotado" |
| 5.4 | Administración: altas, cambios y eliminaciones | 2 | ✅ altas, cambios y eliminaciones (producto de prueba creado, editado y borrado) | Perfil → Administrar productos → **+**, tocar para editar, mantener presionado para eliminar |
| 6.1 | Agregar, modificar cantidades y eliminar del carrito | 2 | ✅ agregar, cantidades y quitar (con confirmación) | Detalle → Agregar; carrito con **+** / **−** / borrar |
| 6.2 | Subtotales, total y envío | 2 | ✅ | Envío de $9.000, gratis desde $150.000 |
| 6.3 | Valida precios e inventario antes de confirmar | 2 | ✅ | Transacción de Firestore al confirmar; el backend también recalcula el total |
| 6.4 | Pasarela/sandbox y estados de la transacción | 3 | ✅ | Webhook verificado: Wompi marcó un pedido como PAGADO sin abrir la app. Backend en https://huellitasplus-pagos.vercel.app. Probado: 4242 aprobado, 4111 rechazado (stock devuelto), Daviplata pendiente, Nequi aprobado; comprobante PDF. Capturas 33–38 |
| 7.1 | Iniciar conversaciones y enviar/recibir mensajes | 2 | ✅ | Probado entre el teléfono (Camila) y el emulador (Wilderman): los mensajes llegan en tiempo real. Capturas 39–40 |
| 7.2 | Remitente, fecha/hora e historial | 1 | ✅ | Cada burbuja muestra nombre y fecha/hora; lista de conversaciones |
| 7.3 | Controles de privacidad, bloqueo o reporte | 1 | ✅ bloquear, desbloquear y reportar (guardados en Firestore) | Conversación → ⋮ → Bloquear / Reportar. Las reglas impiden escribir a quien te bloqueó; los demás solo ven tu nombre y ciudad |
| 8.1 | Almacena usuarios, perros, productos, pedidos y mensajes | 2 | ✅ | Consola de Firebase → Firestore |
| 8.2 | Autenticación, permisos, protección de datos, sin claves expuestas | 2 | ✅ | Reglas publicadas; secretos de Wompi solo en Vercel; contraseña del keystore fuera del código |
| 8.3 | Código, APK, documentación, capturas y evidencias | 2 | ✅ | `APK/HuellitasPlus-v2.0.apk`, `DOCUMENTACION/`, capturas en `DOCUMENTACION/capturas/` (las 37 del plan, de la 01 a la 43), ZIP `HuellitasPlus_GuiaII_CorteIII.zip`, manual de instalación y uso en `MANUAL/Manual_HuellitasPlus.pdf` |

**Estado: los 26 requisitos verificados en el dispositivo (50/50).**
