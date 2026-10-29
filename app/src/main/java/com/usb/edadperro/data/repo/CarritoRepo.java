package com.usb.edadperro.data.repo;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.ItemCarrito;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.util.CarritoCalculadora;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carrito de compras del usuario (Punto 7). Se guarda en Firestore para que
 * no se pierda al cerrar la app y funcione sin conexión (caché local).
 */
public final class CarritoRepo {

    private CarritoRepo() {
    }

    public static Query items() {
        return Fb.carrito(Fb.uid()).orderBy("agregado");
    }

    /**
     * Agrega unidades de un producto. La cantidad total se limita al inventario
     * disponible leído en la misma transacción.
     *
     * @return cantidad final del producto en el carrito
     */
    public static Task<Integer> agregar(Producto producto, int cantidad) {
        String uid = Fb.uid();
        DocumentReference refItem = Fb.carrito(uid).document(producto.id);
        DocumentReference refProd = Fb.productos().document(producto.id);
        return Fb.db().runTransaction(tx -> {
            DocumentSnapshot prod = tx.get(refProd);
            DocumentSnapshot item = tx.get(refItem);
            Producto p = prod.toObject(Producto.class);
            if (p == null || !p.disponible()) {
                throw new FirebaseFirestoreException("El producto ya no está disponible.",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            long actual = item.exists() && item.getLong("cantidad") != null ? item.getLong("cantidad") : 0;
            int nueva = CarritoCalculadora.limitarCantidad((int) actual + cantidad, p.stock);
            if (nueva <= actual) {
                throw new FirebaseFirestoreException("Ya tienes en el carrito todas las unidades disponibles ("
                        + nueva + ").", FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            Map<String, Object> m = new HashMap<>();
            m.put("nombre", p.nombre);
            m.put("marca", p.marca);
            m.put("presentacion", p.presentacion);
            m.put("imagenUrl", p.imagenUrl);
            m.put("precioUnitario", p.precio);
            m.put("cantidad", nueva);
            if (!item.exists()) {
                m.put("agregado", FieldValue.serverTimestamp());
            }
            tx.set(refItem, m, com.google.firebase.firestore.SetOptions.merge());
            return nueva;
        });
    }

    public static Task<Void> cambiarCantidad(ItemCarrito item, int nueva) {
        if (nueva < 1) {
            return eliminar(item);
        }
        return Fb.carrito(Fb.uid()).document(item.productoId).update("cantidad", nueva);
    }

    public static Task<Void> eliminar(ItemCarrito item) {
        return Fb.carrito(Fb.uid()).document(item.productoId).delete();
    }

    /** Agrega de nuevo los productos de un pedido (p. ej. tras un pago rechazado). */
    public static Task<Void> agregarVarios(List<com.usb.edadperro.data.modelo.DetallePedido> detalles) {
        List<Task<Integer>> tareas = new ArrayList<>();
        for (com.usb.edadperro.data.modelo.DetallePedido d : detalles) {
            Producto p = new Producto();
            p.id = d.productoId;
            tareas.add(agregar(p, d.cantidad));
        }
        // Si algún producto ya no existe se ignora; se agregan los demás.
        return Tasks.whenAllComplete(tareas).continueWith(t -> null);
    }

    /**
     * Revalida el carrito contra el catálogo actual: actualiza precios que
     * cambiaron, ajusta cantidades al inventario y elimina productos agotados.
     *
     * @return mensajes de los cambios aplicados (vacío si no hubo cambios)
     */
    public static Task<List<String>> revalidar(List<ItemCarrito> items) {
        if (items.isEmpty()) {
            return Tasks.forResult(new ArrayList<>());
        }
        List<Task<DocumentSnapshot>> lecturas = new ArrayList<>();
        for (ItemCarrito it : items) {
            lecturas.add(Fb.productos().document(it.productoId).get());
        }
        return Tasks.whenAllSuccess(lecturas).continueWithTask(t -> {
            List<String> cambios = new ArrayList<>();
            WriteBatch b = Fb.db().batch();
            String uid = Fb.uid();
            for (int i = 0; i < items.size(); i++) {
                ItemCarrito it = items.get(i);
                Producto p = ((DocumentSnapshot) t.getResult().get(i)).toObject(Producto.class);
                DocumentReference ref = Fb.carrito(uid).document(it.productoId);
                if (p == null || !p.disponible()) {
                    b.delete(ref);
                    cambios.add(it.nombre + " se agotó y se quitó del carrito.");
                    continue;
                }
                if (p.precio != it.precioUnitario) {
                    b.update(ref, "precioUnitario", p.precio);
                    cambios.add("El precio de " + p.nombre + " cambió.");
                }
                int ajustada = CarritoCalculadora.limitarCantidad(it.cantidad, p.stock);
                if (ajustada != it.cantidad) {
                    b.update(ref, "cantidad", ajustada);
                    cambios.add("Solo quedan " + p.stock + " unidades de " + p.nombre + ".");
                }
            }
            return b.commit().continueWith(x -> cambios);
        });
    }
}
