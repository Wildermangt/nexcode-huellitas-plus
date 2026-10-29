package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;

/**
 * Datos mínimos de un usuario visibles para los demás (colección
 * "perfiles_publicos"). No incluye correo, teléfono ni dirección.
 */
public class PerfilPublico {

    @DocumentId
    public String id;
    public String nombre;
    public String ciudad;

    public PerfilPublico() {
        // Requerido por Firestore
    }
}
