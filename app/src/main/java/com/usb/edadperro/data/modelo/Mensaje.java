package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entidad Mensaje (subcolección "conversaciones/{id}/mensajes").
 */
public class Mensaje {

    @DocumentId
    public String id;
    public String remitenteId;
    public String remitenteNombre;
    public String texto;
    /** Hora del servidor, no del teléfono, para ordenar de forma confiable. */
    @ServerTimestamp
    public Date fecha;

    public Mensaje() {
        // Requerido por Firestore
    }
}
