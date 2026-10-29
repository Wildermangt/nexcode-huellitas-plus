package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.usb.edadperro.CalculadoraEdad;

import java.util.Date;

/**
 * Entidad Perro registrado (subcolección "usuarios/{uid}/perros").
 */
public class Perro {

    @DocumentId
    public String id;
    public String nombre;
    public String raza;
    /** Edad en años (admite decimales, ej. 0.5). */
    public double edad;
    /** Nombre del enum {@link CalculadoraEdad.Tamano}. */
    public String tamano = CalculadoraEdad.Tamano.MEDIANO.name();
    public double peso;
    /** "Macho" o "Hembra". */
    public String sexo;
    public String notas;
    @ServerTimestamp
    public Date creado;

    public Perro() {
        // Requerido por Firestore
    }

    @Exclude
    public CalculadoraEdad.Tamano tamanoEnum() {
        try {
            return CalculadoraEdad.Tamano.valueOf(tamano);
        } catch (Exception e) {
            return CalculadoraEdad.Tamano.MEDIANO;
        }
    }

    @Exclude
    public double edadHumana() {
        return CalculadoraEdad.calcularVeterinario(edad, tamanoEnum());
    }

    @Exclude
    public CalculadoraEdad.Etapa etapa() {
        return CalculadoraEdad.obtenerEtapa(edad, tamanoEnum());
    }
}
