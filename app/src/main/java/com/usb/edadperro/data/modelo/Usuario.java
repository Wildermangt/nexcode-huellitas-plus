package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entidad Usuario (colección "usuarios"). Datos privados: solo el propio
 * usuario puede leerlos. El nombre visible para otros está en "perfiles_publicos".
 */
public class Usuario {

    public static final String ROL_CLIENTE = "cliente";
    public static final String ROL_ADMIN = "admin";

    @DocumentId
    public String id;
    public String nombre;
    public String email;
    public String telefono;
    public String direccion;
    public String ciudad;
    /** "cliente" o "admin". El rol admin solo se asigna desde la consola de Firebase. */
    public String rol = ROL_CLIENTE;
    @ServerTimestamp
    public Date creado;

    public Usuario() {
        // Requerido por Firestore
    }

    @Exclude
    public boolean esAdmin() {
        return ROL_ADMIN.equals(rol);
    }
}
