package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.usb.edadperro.util.CarritoCalculadora;

/**
 * Entidad Detalle de pedido (subcolección "pedidos/{id}/detalles").
 * Guarda una copia del producto con el precio validado al confirmar.
 */
public class DetallePedido implements CarritoCalculadora.Linea {

    @DocumentId
    public String productoId;
    public String nombre;
    public String marca;
    public String presentacion;
    public String imagenUrl;
    public long precioUnitario;
    public int cantidad;
    public long subtotal;

    public DetallePedido() {
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
}
