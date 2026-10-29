package com.usb.edadperro.data.modelo;

/**
 * Veterinaria obtenida de OpenStreetMap (no se guarda en Firestore).
 */
public class Veterinaria {

    /** Identificador en OSM, ej. "node/123456". */
    public String osmId;
    public String nombre;
    public double lat;
    public double lon;
    public String telefono;
    public String direccion;
    public String horario;
    public String sitioWeb;
    /** true solo si OSM indica emergency=yes u horario 24/7 (dato verificado). */
    public boolean urgencias;
    public double distanciaKm;

    public String urlOsm() {
        return "https://www.openstreetmap.org/" + osmId;
    }
}
