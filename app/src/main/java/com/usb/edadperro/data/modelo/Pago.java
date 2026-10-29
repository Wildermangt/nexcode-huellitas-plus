package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entidad Pago (colección "pagos", id = referencia enviada a Wompi).
 * Solo el backend escribe en esta colección.
 */
public class Pago {

    public static final String INICIADO = "INICIADO";
    public static final String APROBADO = "APPROVED";
    public static final String RECHAZADO = "DECLINED";
    public static final String PENDIENTE = "PENDING";
    public static final String ANULADO = "VOIDED";
    public static final String ERROR = "ERROR";

    @DocumentId
    public String referencia;
    public String pedidoId;
    public String usuarioId;
    public String proveedor;
    public String transaccionId;
    public String estado;
    /** Monto en centavos, como lo maneja Wompi. */
    public long montoCentavos;
    public String moneda;
    public String metodo;
    public String mensaje;
    @ServerTimestamp
    public Date creado;
    @ServerTimestamp
    public Date actualizado;

    public Pago() {
        // Requerido por Firestore
    }

    @Exclude
    public long montoPesos() {
        return montoCentavos / 100;
    }
}
