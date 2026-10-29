package com.usb.edadperro.data.repo;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.DetallePedido;
import com.usb.edadperro.data.modelo.ItemCarrito;
import com.usb.edadperro.data.modelo.Pedido;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.util.CarritoCalculadora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Confirmación de pedidos e historial (Puntos 7 y 8).
 */
public final class PedidoRepo {

    private PedidoRepo() {
    }

    /** Datos de entrega capturados en el checkout. */
    public static final class Entrega {
        public String direccion;
        public String ciudad;
        public String telefono;
        public String notas;
    }

    /**
     * Confirma el pedido en una sola transacción de Firestore:
     * 1. Relee cada producto y valida existencias y precio actual.
     * 2. Crea el pedido y sus detalles con los valores validados.
     * 3. Descuenta el inventario y registra el movimiento.
     * 4. Vacía el carrito.
     * Si cualquier validación falla, no se escribe nada.
     *
     * @return id del pedido creado
     */
    public static Task<String> confirmar(List<ItemCarrito> items, Entrega entrega) {
        String uid = Fb.uid();
        String nombre = SesionRepo.nombre();
        return Fb.db().runTransaction(tx -> {
            // ---- 1. Lecturas y validación (todas las lecturas van antes de escribir) ----
            List<Producto> productos = new ArrayList<>();
            List<String> problemas = new ArrayList<>();
            for (ItemCarrito it : items) {
                DocumentSnapshot s = tx.get(Fb.productos().document(it.productoId));
                Producto p = s.toObject(Producto.class);
                productos.add(p);
                if (p == null || !p.activo) {
                    problemas.add(it.nombre + " ya no está disponible");
                } else if (p.stock < it.cantidad) {
                    problemas.add("Solo quedan " + p.stock + " unidades de " + p.nombre);
                } else if (p.precio != it.precioUnitario) {
                    problemas.add("El precio de " + p.nombre + " cambió");
                }
            }
            if (items.isEmpty()) {
                problemas.add("El carrito está vacío");
            }
            if (!problemas.isEmpty()) {
                throw new FirebaseFirestoreException(
                        "No se pudo confirmar: " + android.text.TextUtils.join("; ", problemas)
                                + ". Revisa tu carrito.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }

            // ---- 2. Totales con los precios validados ----
            CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(items);
            DocumentReference refPedido = Fb.pedidos().document();
            Map<String, Object> pedido = new HashMap<>();
            pedido.put("usuarioId", uid);
            pedido.put("usuarioNombre", nombre);
            pedido.put("estado", Pedido.PENDIENTE_PAGO);
            pedido.put("subtotal", r.subtotal);
            pedido.put("envio", r.envio);
            pedido.put("total", r.total);
            pedido.put("unidades", r.unidades);
            pedido.put("direccion", entrega.direccion);
            pedido.put("ciudad", entrega.ciudad);
            pedido.put("telefono", entrega.telefono);
            pedido.put("notas", entrega.notas);
            pedido.put("creado", FieldValue.serverTimestamp());
            pedido.put("actualizado", FieldValue.serverTimestamp());
            tx.set(refPedido, pedido);

            // ---- 3. Detalles, inventario y carrito ----
            for (int i = 0; i < items.size(); i++) {
                ItemCarrito it = items.get(i);
                Producto p = productos.get(i);
                Map<String, Object> d = new HashMap<>();
                d.put("nombre", p.nombre);
                d.put("marca", p.marca);
                d.put("presentacion", p.presentacion);
                d.put("imagenUrl", p.imagenUrl);
                d.put("precioUnitario", p.precio);
                d.put("cantidad", it.cantidad);
                d.put("subtotal", CarritoCalculadora.subtotalLinea(p.precio, it.cantidad));
                tx.set(Fb.detalles(refPedido.getId()).document(p.id), d);

                tx.update(Fb.productos().document(p.id),
                        "stock", p.stock - it.cantidad,
                        "actualizado", FieldValue.serverTimestamp());
                tx.set(Fb.movimientos().document(),
                        TiendaRepo.movimiento(p.id, p.nombre, -it.cantidad, "venta", refPedido.getId()));
                tx.delete(Fb.carrito(uid).document(it.productoId));
            }
            return refPedido.getId();
        });
    }

    /** Pedidos del usuario (se ordenan en la app para no requerir índices compuestos). */
    public static Query misPedidos() {
        return Fb.pedidos().whereEqualTo("usuarioId", Fb.uid());
    }

    public static DocumentReference pedido(String id) {
        return Fb.pedidos().document(id);
    }

    public static Query detalles(String pedidoId) {
        return Fb.detalles(pedidoId);
    }

    /** Pagos asociados a un pedido (intentos con Wompi). */
    public static Query pagos(String pedidoId) {
        return Fb.pagos().whereEqualTo("pedidoId", pedidoId).whereEqualTo("usuarioId", Fb.uid());
    }

    /** Detalles convertidos a objetos. */
    public static List<DetallePedido> aDetalles(List<DocumentSnapshot> docs) {
        List<DetallePedido> lista = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            DetallePedido dp = d.toObject(DetallePedido.class);
            if (dp != null) {
                lista.add(dp);
            }
        }
        return lista;
    }
}
