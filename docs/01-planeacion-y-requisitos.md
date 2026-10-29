# HuellitasPlus · Planeación y requisitos

Optativa II – Desarrollo de Aplicaciones para Dispositivos Móviles · Universidad de San Buenaventura, Bogotá
Equipo Nexcode: Jeferson Wilderman González Tenjo (Product Owner · Developer) y José Leandro Ocampo Camacho (Scrum Master · Developer)

## 1. Problema

Los dueños de perros resuelven por separado necesidades que están relacionadas: calcular la etapa de vida de su mascota, recordar vacunas y desparasitaciones, encontrar una veterinaria de urgencias, comprar el alimento adecuado y resolver dudas de cuidado. Para cada una usan una app, un buscador o un chat distinto, la información queda dispersa y, ante una urgencia, se pierde tiempo buscando una veterinaria abierta y cercana.

## 2. Justificación

HuellitasPlus reúne esos servicios en una sola aplicación alrededor del perfil de cada perro:

- **La edad humana equivalente** orienta los cuidados según la etapa de vida (cachorro, adulto o senior).
- **Los recordatorios** de vacunas, baños y desparasitantes evitan olvidos que afectan la salud del animal.
- **El asistente de IA** responde dudas de cuidado al instante, pero advierte que no sustituye al veterinario y deriva las urgencias al mapa.
- **El mapa usa datos públicos verificables de OpenStreetMap,** de modo que cada veterinaria mostrada existe y tiene ficha pública.
- **La tienda vende solo alimento para perros,** con inventario y pago en línea.
- **Todo funciona con servicios gratuitos sin tarjeta de crédito,** lo que hace viable el proyecto para un equipo estudiantil.

## 3. Objetivo

Desarrollar una aplicación Android que ayude a los dueños de perros a cuidar a sus mascotas, integrando la calculadora de edad canina, el registro de perros, recordatorios de cuidados, un asistente de IA, la búsqueda de veterinarias, una tienda de alimentos con pagos y un chat entre usuarios.

## 4. Alcance

**Incluye:**

- Calculadora de edad canina con tres métodos (veterinario, ×7 y fórmula epigenética 2020) y recomendaciones por etapa de vida.
- Cuenta de usuario: registro, inicio y cierre de sesión, recuperación de contraseña y edición del perfil.
- Registro de varios perros por usuario (nombre, raza, edad, peso, tamaño, sexo y notas).
- Cuidados con alarmas: vacunas, baños, desparasitantes y medicamentos, con repetición periódica.
- Asistente de IA (Gemini) sobre cuidado y alimentación, personalizado con los perros del usuario, con aviso legal y detección de urgencias.
- Mapa de veterinarias reales y urgencias 24 h, búsqueda por ciudad o dirección, distancia, llamada y ruta.
- Tienda de alimentos para perros con búsqueda, filtros, inventario y administración de productos.
- Carrito, confirmación de pedido y pago con Wompi en sandbox (aprobado, rechazado y pendiente), con comprobante en PDF.
- Chat en tiempo real entre usuarios registrados, con bloqueo y reporte.

**No incluye:**

- Pagos reales: Wompi funciona en modo sandbox.
- Agendamiento de citas con las veterinarias.
- Diagnóstico veterinario: el asistente orienta, pero no reemplaza la consulta.
- Productos para otras especies.
- Versión para iOS o web.

## 5. Usuarios

| Perfil | Necesidad principal |
|---|---|
| Dueño de perro | Cuidar a su mascota, recibir orientación, encontrar veterinarias y comprar alimento |
| Administrador de la tienda | Mantener el catálogo y el inventario de alimentos |

## 6. Requisitos funcionales

La columna *Dónde se implementa* sirve como trazabilidad entre cada requisito y el código (paquete `com.usb.edadperro`).

| ID | Requisito | Prioridad | Dónde se implementa |
|---|---|---|---|
| RF-01 | Calcular la edad humana equivalente del perro según su edad y tamaño, con tres métodos | Alta | `CalculadoraEdad.java`, `ui/perros/CalculadoraFragment.java` |
| RF-02 | Mostrar la etapa de vida y recomendaciones de cuidado según la edad | Media | `util/Recomendaciones.java`, `ui/perros/TextosPerro.java` |
| RF-03 | Registrarse, iniciar y cerrar sesión, y recuperar la contraseña | Alta | `ui/auth/AuthActivity.java`, `ui/auth/RegistroActivity.java`, `data/repo/SesionRepo.java` |
| RF-04 | Consultar y editar el perfil del usuario | Media | `ui/perfil/PerfilFragment.java`, `ui/perfil/EditarPerfilFragment.java` |
| RF-05 | Registrar, editar y eliminar perros asociados al usuario | Alta | `ui/perros/PerroFormFragment.java`, `data/repo/PerroRepo.java` |
| RF-06 | Programar cuidados con alarma y repetición periódica | Media | `ui/cuidados/CuidadosFragment.java`, `CuidadoFormFragment.java`, `alarmas/`, `util/Periodicidad.java` |
| RF-07 | Conversar con un asistente de IA sobre cuidado y alimentación | Alta | `ui/ia/AsistenteFragment.java`, `ui/ia/AsistenteViewModel.java` |
| RF-08 | Detectar mensajes de urgencia y ofrecer el mapa de urgencias | Alta | `util/DetectorUrgencias.java`, `AsistenteFragment` |
| RF-09 | Buscar veterinarias reales cercanas o por ciudad, con distancia, llamada y ruta | Alta | `ui/vets/VeterinariasFragment.java`, `data/remoto/Http.java`, `util/Distancia.java` |
| RF-10 | Consultar la tienda de alimentos con búsqueda, filtros y disponibilidad | Alta | `ui/tienda/` (`TiendaFragment`, `ProductoDetalleFragment`), `data/repo/TiendaRepo.java` |
| RF-11 | Administrar productos: crear, editar y eliminar (solo administrador) | Media | `ui/admin/AdminProductosFragment.java`, `ProductoFormFragment.java`, `util/ValidadorCatalogo.java` |
| RF-12 | Agregar productos al carrito, cambiar cantidades y quitar, con subtotal, envío y total | Alta | `ui/carrito/CarritoFragment.java`, `util/CarritoCalculadora.java`, `data/repo/CarritoRepo.java` |
| RF-13 | Confirmar el pedido validando precios e inventario | Alta | `ui/carrito/CheckoutFragment.java`, `data/repo/PedidoRepo.java` |
| RF-14 | Pagar con Wompi y ver el estado del pago y el comprobante en PDF | Alta | `ui/pago/PagoWompiFragment.java`, `ui/pago/ComprobantePdf.java`, `data/remoto/PagosServicio.java` |
| RF-15 | Consultar el historial y el detalle de los pedidos | Media | `ui/pago/PedidosFragment.java`, `PedidoDetalleFragment.java` |
| RF-16 | Chatear en tiempo real con otros usuarios, y bloquearlos o reportarlos | Media | `ui/chat/UsuariosFragment.java`, `ConversacionFragment.java`, `data/repo/ChatRepo.java` |

## 7. Requisitos no funcionales

| ID | Requisito | Cómo se cumple |
|---|---|---|
| RNF-01 | **Seguridad:** cada usuario accede solo a sus datos y nadie se asigna el rol de administrador | `firestore.rules` restringe por `uid` y el rol solo se cambia en la consola |
| RNF-02 | **Protección de credenciales:** ninguna clave secreta viaja en el APK | Gemini mediante Firebase AI Logic; los secretos de Wompi solo en Vercel; la contraseña del keystore en `local.properties` |
| RNF-03 | **Privacidad:** los datos locales no se copian en respaldos y el usuario autoriza el tratamiento de datos (Ley 1581 de 2012) | `android:allowBackup="false"`; aceptación en el registro |
| RNF-04 | **Compatibilidad:** funciona desde Android 7.0 | `minSdk 24`, `targetSdk 35` |
| RNF-05 | **Comunicaciones seguras:** todo el tráfico va por HTTPS | URL `https://` para OpenStreetMap, el backend y Firebase |
| RNF-06 | **Usabilidad y accesibilidad:** navegación por barra inferior de cinco módulos e imágenes con descripción | Material 3, Navigation Component; todas las `ImageView` tienen `contentDescription` |
| RNF-07 | **Costo cero:** solo servicios gratuitos sin tarjeta de crédito | Firebase Spark, Vercel Hobby, OpenStreetMap, Wompi sandbox |
| RNF-08 | **Calidad:** la lógica de negocio tiene pruebas automatizadas | 46 pruebas JUnit en `app/src/test` |

## 8. Historias de usuario y Sprints

Las historias de usuario, sus tareas y los tres Sprints de dos semanas se gestionan en Azure DevOps, en el proyecto **Nexcode - HuellitasPlus** (https://dev.azure.com/wildermangt). Cada historia sigue el formato *Como [rol] quiero [acción] para [beneficio]*; por ejemplo, *«Como dueño quiero conocer la edad humana de mi perro»* corresponde a RF-01.

## 9. Diseño de pantallas

Los mockups de las pantallas principales están en [`02-mockups.md`](02-mockups.md).
