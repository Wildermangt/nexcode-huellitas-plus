package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entidad Producto (colección "productos"). El campo stock es el inventario
 * disponible; cada cambio queda registrado en "inventario_movimientos".
 */
public class Producto {

    @DocumentId
    public String id;
    public String nombre;
    public String marca;
    public String descripcion;
    /** Presentación comercial, ej. "Bulto 8 kg" o "Sobre 100 g". */
    public String presentacion;
    /** Precio en pesos colombianos. */
    public long precio;
    /** Unidades disponibles (inventario). */
    public long stock;
    public String categoriaId;
    public String categoriaNombre;
    public String imagenUrl;
    /** Siempre "perro": lo exigen el validador y las reglas de Firestore. */
    public String especie = "perro";
    public boolean activo = true;
    @ServerTimestamp
    public Date actualizado;

    public Producto() {
        // Requerido por Firestore
    }

    @Exclude
    public boolean disponible() {
        return activo && stock > 0;
    }
}
