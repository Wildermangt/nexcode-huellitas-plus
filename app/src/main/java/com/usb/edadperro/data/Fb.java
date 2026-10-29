package com.usb.edadperro.data;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Punto único de acceso a Firebase y a las colecciones de Firestore.
 *
 * Estructura de la base de datos (Punto 10):
 * <pre>
 * usuarios/{uid}                      Usuarios (datos privados)
 *   ├─ perros/{perroId}               Perros registrados
 *   └─ cuidados/{cuidadoId}           Vacunas, medicamentos, baños… con recordatorio
 * perfiles_publicos/{uid}             Nombre visible en el chat
 * categorias/{categoriaId}            Categorías de alimentos
 * productos/{productoId}              Productos (campo stock = inventario)
 * inventario_movimientos/{id}         Historial de entradas y salidas
 * carritos/{uid}/items/{productoId}   Carrito de compras
 * pedidos/{pedidoId}                  Pedidos
 *   └─ detalles/{productoId}          Detalles de pedido
 * pagos/{referencia}                  Pagos (solo los escribe el backend)
 * conversaciones/{uidA_uidB}          Conversaciones
 *   └─ mensajes/{mensajeId}           Mensajes
 * </pre>
 */
public final class Fb {

    private Fb() {
    }

    /** @return true si google-services.json está presente y Firebase arrancó. */
    public static boolean configurado(Context ctx) {
        return !FirebaseApp.getApps(ctx).isEmpty();
    }

    public static FirebaseAuth auth() {
        return FirebaseAuth.getInstance();
    }

    public static FirebaseFirestore db() {
        return FirebaseFirestore.getInstance();
    }

    /** @return uid del usuario con sesión iniciada, o null */
    public static String uid() {
        FirebaseUser u = auth().getCurrentUser();
        return u == null ? null : u.getUid();
    }

    public static CollectionReference usuarios() {
        return db().collection("usuarios");
    }

    public static CollectionReference perfiles() {
        return db().collection("perfiles_publicos");
    }

    public static CollectionReference perros(String uid) {
        return usuarios().document(uid).collection("perros");
    }

    public static CollectionReference cuidados(String uid) {
        return usuarios().document(uid).collection("cuidados");
    }

    public static CollectionReference categorias() {
        return db().collection("categorias");
    }

    public static CollectionReference productos() {
        return db().collection("productos");
    }

    public static CollectionReference movimientos() {
        return db().collection("inventario_movimientos");
    }

    public static CollectionReference carrito(String uid) {
        return db().collection("carritos").document(uid).collection("items");
    }

    public static CollectionReference pedidos() {
        return db().collection("pedidos");
    }

    public static CollectionReference detalles(String pedidoId) {
        return pedidos().document(pedidoId).collection("detalles");
    }

    public static CollectionReference pagos() {
        return db().collection("pagos");
    }

    public static CollectionReference conversaciones() {
        return db().collection("conversaciones");
    }

    public static CollectionReference mensajes(String conversacionId) {
        return conversaciones().document(conversacionId).collection("mensajes");
    }
}
