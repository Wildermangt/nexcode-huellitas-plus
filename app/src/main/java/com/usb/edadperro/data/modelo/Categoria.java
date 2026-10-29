package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;

/**
 * Entidad Categoría de alimentos (colección "categorias").
 * Solo existen tres: concentrado, humedo y snacks.
 */
public class Categoria {

    @DocumentId
    public String id;
    public String nombre;
    public String descripcion;
    public long orden;

    public Categoria() {
        // Requerido por Firestore
    }

    public Categoria(String id, String nombre, String descripcion, long orden) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.orden = orden;
    }
}
