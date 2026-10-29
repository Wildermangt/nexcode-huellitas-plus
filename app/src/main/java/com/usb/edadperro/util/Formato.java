package com.usb.edadperro.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Formatos de presentación usados en toda la app (pesos colombianos y fechas).
 */
public final class Formato {

    private static final Locale ES_CO = new Locale("es", "CO");

    private Formato() {
    }

    /** 45900 → "$ 45.900" */
    public static String pesos(long valor) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(ES_CO);
        s.setGroupingSeparator('.');
        DecimalFormat df = new DecimalFormat("#,##0", s);
        return "$ " + df.format(valor);
    }

    /**
     * "Pedigree" + "Dentastix" → "Pedigree Dentastix", sin repetir la marca cuando
     * el nombre ya empieza por ella ("Cesar" + "Cesar Clásicos" → "Cesar Clásicos").
     */
    public static String producto(String marca, String nombre) {
        if (marca == null || marca.trim().isEmpty()) {
            return nombre;
        }
        if (nombre == null || nombre.toLowerCase(ES_CO).startsWith(marca.trim().toLowerCase(ES_CO))) {
            return nombre == null ? marca.trim() : nombre;
        }
        return marca.trim() + " " + nombre;
    }

    /** Fecha y hora corta: "21/10/2026 7:45 p. m." */
    public static String fechaHora(Date fecha) {
        if (fecha == null) {
            return "";
        }
        return new SimpleDateFormat("dd/MM/yyyy h:mm a", ES_CO).format(fecha);
    }

    /** Solo la hora: "7:45 p. m." */
    public static String hora(Date fecha) {
        if (fecha == null) {
            return "";
        }
        return new SimpleDateFormat("h:mm a", ES_CO).format(fecha);
    }

    /** Solo la fecha: "21 oct 2026" */
    public static String fecha(Date fecha) {
        if (fecha == null) {
            return "";
        }
        return new SimpleDateFormat("d MMM yyyy", ES_CO).format(fecha);
    }
}
