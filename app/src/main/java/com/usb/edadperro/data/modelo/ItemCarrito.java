package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.usb.edadperro.util.CarritoCalculadora;

import java.util.Date;

/**
 * Entidad Carrito de compras: cada documento de "carritos/{uid}/items"
 * es una línea del carrito y su id es el id del producto.
 */
public class ItemCarrito implements CarritoCalculadora.Linea {

    @DocumentId
    public String productoId;
    public String nombre;
    public String marca;
    public String presentacion;
    public String imagenUrl;
    /** Precio al momento de agregarlo; se revalida al confirmar el pedido. */
    public long precioUnitario;
    public int cantidad;
    @ServerTimestamp
    public Date agregado;

    public ItemCarrito() {
        // Requerido por Firestore
    }

    @Exclude
    @Override
    public long getPrecioUnitario() {
        return precioUnitario;
    }

    @Exclude
    @Override
    public int getCantidad() {
        return cantidad;
    }

    @Exclude
    public long subtotal() {
        return CarritoCalculadora.subtotalLinea(precioUnitario, cantidad);
    }
}
