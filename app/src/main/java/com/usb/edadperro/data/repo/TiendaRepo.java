package com.usb.edadperro.data.repo;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Categoria;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.util.ValidadorCatalogo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Catálogo del marketplace e interfaz de administración (Punto 6).
 */
public final class TiendaRepo {

    private TiendaRepo() {
    }

    /** Productos visibles para los clientes (solo alimentos para perros activos). */
    public static Query catalogo() {
        return Fb.productos()
                .whereEqualTo("especie", ValidadorCatalogo.ESPECIE)
                .whereEqualTo("activo", true);
    }

    /** Todos los productos (pantalla de administración). */
    public static Query todos() {
        return Fb.productos().orderBy("nombre");
    }

    public static Query categorias() {
        return Fb.categorias().orderBy("orden");
    }

    /**
     * Crea o modifica un producto. Si cambió el stock, registra el movimiento
     * de inventario en la misma escritura atómica.
     *
     * @param stockAnterior stock antes de editar (0 para productos nuevos)
     */
    public static Task<Void> guardar(Producto p, long stockAnterior) {
        List<String> errores = ValidadorCatalogo.validar(p.nombre, p.marca, p.descripcion,
                p.presentacion, p.categoriaId, p.especie, p.precio, p.stock);
        if (!errores.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException(errores.get(0)));
        }
        DocumentReference ref = p.id == null ? Fb.productos().document() : Fb.productos().document(p.id);
        Map<String, Object> datos = new HashMap<>();
        datos.put("nombre", p.nombre.trim());
        datos.put("marca", p.marca.trim());
        datos.put("descripcion", p.descripcion == null ? "" : p.descripcion.trim());
        datos.put("presentacion", p.presentacion.trim());
        datos.put("precio", p.precio);
        datos.put("stock", p.stock);
        datos.put("categoriaId", p.categoriaId);
        datos.put("categoriaNombre", nombreCategoria(p.categoriaId));
        datos.put("imagenUrl", p.imagenUrl == null ? "" : p.imagenUrl.trim());
        datos.put("especie", ValidadorCatalogo.ESPECIE);
        datos.put("activo", p.activo);
        datos.put("actualizado", FieldValue.serverTimestamp());

        WriteBatch b = Fb.db().batch();
        b.set(ref, datos);
        long diferencia = p.stock - stockAnterior;
        if (diferencia != 0) {
            b.set(Fb.movimientos().document(),
                    movimiento(ref.getId(), p.nombre, diferencia, "ajuste_admin", null));
        }
        return b.commit();
    }

    public static Task<Void> eliminar(Producto p) {
        WriteBatch b = Fb.db().batch();
        b.delete(Fb.productos().document(p.id));
        if (p.stock > 0) {
            b.set(Fb.movimientos().document(),
                    movimiento(p.id, p.nombre, -p.stock, "producto_eliminado", null));
        }
        return b.commit();
    }

    /** Documento de la colección inventario_movimientos. */
    static Map<String, Object> movimiento(String productoId, String nombre, long cantidad,
                                          String motivo, String pedidoId) {
        Map<String, Object> m = new HashMap<>();
        m.put("productoId", productoId);
        m.put("productoNombre", nombre);
        m.put("cantidad", cantidad);
        m.put("motivo", motivo);
        m.put("pedidoId", pedidoId);
        m.put("usuarioId", Fb.uid());
        m.put("fecha", FieldValue.serverTimestamp());
        return m;
    }

    public static String nombreCategoria(String id) {
        if ("concentrado".equals(id)) {
            return "Concentrados";
        }
        if ("humedo".equals(id)) {
            return "Alimento húmedo";
        }
        if ("snacks".equals(id)) {
            return "Snacks";
        }
        return "";
    }

    /** Las tres categorías del catálogo. */
    public static List<Categoria> categoriasBase() {
        return Arrays.asList(
                new Categoria("concentrado", "Concentrados", "Alimento seco completo para cada etapa", 1),
                new Categoria("humedo", "Alimento húmedo", "Sobres, latas y bandejas", 2),
                new Categoria("snacks", "Snacks", "Premios y galletas comestibles caninas", 3));
    }

    /**
     * Carga el catálogo inicial (categorías y productos). Solo el administrador
     * puede ejecutarlo; las reglas de Firestore lo exigen.
     */
    public static Task<Void> cargarCatalogoInicial() {
        WriteBatch b = Fb.db().batch();
        for (Categoria c : categoriasBase()) {
            Map<String, Object> m = new HashMap<>();
            m.put("nombre", c.nombre);
            m.put("descripcion", c.descripcion);
            m.put("orden", c.orden);
            b.set(Fb.categorias().document(c.id), m);
        }
        for (Object[] p : CatalogoInicial.PRODUCTOS) {
            DocumentReference ref = Fb.productos().document((String) p[0]);
            Map<String, Object> m = new HashMap<>();
            m.put("categoriaId", p[1]);
            m.put("categoriaNombre", nombreCategoria((String) p[1]));
            m.put("nombre", p[2]);
            m.put("marca", p[3]);
            m.put("presentacion", p[4]);
            m.put("precio", p[5]);
            m.put("stock", p[6]);
            m.put("descripcion", p[7]);
            m.put("imagenUrl", CatalogoInicial.URL_IMAGENES + p[8]);
            m.put("especie", ValidadorCatalogo.ESPECIE);
            m.put("activo", true);
            m.put("actualizado", FieldValue.serverTimestamp());
            b.set(ref, m);
            b.set(Fb.movimientos().document(),
                    movimiento(ref.getId(), (String) p[2], (Long) p[6], "inventario_inicial", null));
        }
        return b.commit();
    }
}
