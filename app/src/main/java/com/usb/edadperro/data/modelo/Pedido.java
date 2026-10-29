package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entidad Pedido (colección "pedidos"). Los detalles están en la
 * subcolección "pedidos/{id}/detalles".
 *
 * Los estados de pago los actualiza únicamente el backend (Wompi → Vercel),
 * nunca la app, para que no puedan falsificarse.
 */
public class Pedido {

    public static final String PENDIENTE_PAGO = "PENDIENTE_PAGO";
    public static final String PAGO_EN_PROCESO = "PAGO_EN_PROCESO";
    public static final String PAGADO = "PAGADO";
    public static final String PAGO_RECHAZADO = "PAGO_RECHAZADO";

    @DocumentId
    public String id;
    public String usuarioId;
    public String usuarioNombre;
    public String estado = PENDIENTE_PAGO;
    public long subtotal;
    public long envio;
    public long total;
    public int unidades;
    public String direccion;
    public String ciudad;
    public String telefono;
    public String notas;
    /** Referencia del último intento de pago en Wompi. */
    public String pagoReferencia;
    public String pagoTransaccionId;
    @ServerTimestamp
    public Date creado;
    @ServerTimestamp
    public Date actualizado;

    public Pedido() {
        // Requerido por Firestore
    }

    @Exclude
    public String estadoLegible() {
        if (estado == null) {
            return "";
        }
        switch (estado) {
            case PAGADO:
                return "Pagado";
            case PAGO_RECHAZADO:
                return "Pago rechazado";
            case PAGO_EN_PROCESO:
                return "Pago pendiente";
            default:
                return "Pendiente de pago";
        }
    }

    @Exclude
    public boolean puedePagarse() {
        return PENDIENTE_PAGO.equals(estado);
    }
}
