package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import com.usb.edadperro.util.Periodicidad;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cuidado periódico de un perro con recordatorio (subcolección "usuarios/{uid}/cuidados"):
 * vacunas, desparasitantes, medicamentos, baños, cortes de pelo u otros.
 */
public class Cuidado {

    /** Tipos de cuidado con su repetición sugerida al crearlos. */
    public enum Tipo {
        VACUNA("Vacuna", 1, Periodicidad.Unidad.ANIOS),
        DESPARASITANTE("Desparasitante", 3, Periodicidad.Unidad.MESES),
        MEDICAMENTO("Medicamento", 8, Periodicidad.Unidad.HORAS),
        BANO("Baño", 2, Periodicidad.Unidad.SEMANAS),
        PELUQUERIA("Corte de pelo", 6, Periodicidad.Unidad.SEMANAS),
        OTRO("Otro", 0, Periodicidad.Unidad.DIAS);

        public final String etiqueta;
        public final int cadaSugerido;
        public final Periodicidad.Unidad unidadSugerida;

        Tipo(String etiqueta, int cadaSugerido, Periodicidad.Unidad unidadSugerida) {
            this.etiqueta = etiqueta;
            this.cadaSugerido = cadaSugerido;
            this.unidadSugerida = unidadSugerida;
        }
    }

    @DocumentId
    public String id;
    public String perroId;
    /** Copia del nombre para mostrarlo en la lista y en la alarma sin otra consulta. */
    public String perroNombre;
    /** Nombre del enum {@link Tipo}. */
    public String tipo = Tipo.VACUNA.name();
    /** Ej. "Rabia", "Drontal Plus", "Amoxicilina 250 mg". */
    public String nombre;
    public String notas;
    /** Fecha y hora de la próxima aplicación (cuando suena la alarma). */
    public Date proxima;
    /** Cada cuántas unidades se repite; 0 = no se repite. */
    public int cada;
    /** Nombre del enum {@link Periodicidad.Unidad}. */
    public String unidad = Periodicidad.Unidad.DIAS.name();
    /** Aviso adicional antes de la hora (0 = solo a la hora). */
    public int avisoMinutos;
    /** false cuando un cuidado que no se repite ya se hizo. */
    public boolean activo = true;
    /** Fechas en que se marcó como hecho (control de aplicaciones). */
    public List<Date> historial = new ArrayList<>();
    @ServerTimestamp
    public Date creado;

    public Cuidado() {
        // Requerido por Firestore
    }

    @Exclude
    public Tipo tipoEnum() {
        try {
            return Tipo.valueOf(tipo);
        } catch (Exception e) {
            return Tipo.OTRO;
        }
    }

    @Exclude
    public Periodicidad.Unidad unidadEnum() {
        return Periodicidad.unidad(unidad);
    }

    @Exclude
    public Periodicidad.Estado estado(Date ahora) {
        return Periodicidad.estado(proxima, activo, ahora);
    }

    /** "Vacuna · Rabia" o solo "Baño" si no tiene nombre. */
    @Exclude
    public String titulo() {
        String t = tipoEnum().etiqueta;
        return nombre == null || nombre.isEmpty() ? t : t + " · " + nombre;
    }

    /** @return la última vez que se hizo, o null */
    @Exclude
    public Date ultimaVez() {
        return historial == null || historial.isEmpty() ? null : historial.get(historial.size() - 1);
    }
}
