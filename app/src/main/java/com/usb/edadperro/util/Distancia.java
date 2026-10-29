package com.usb.edadperro.util;

import java.util.Locale;

/**
 * Distancia aproximada entre dos coordenadas usando la fórmula de Haversine.
 */
public final class Distancia {

    private static final double RADIO_TIERRA_KM = 6371.0;

    private Distancia() {
    }

    /** @return distancia en línea recta, en kilómetros */
    public static double km(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return RADIO_TIERRA_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** Formato legible: "850 m" o "2,4 km". */
    public static String formatear(double km) {
        if (km < 1) {
            return Math.round(km * 1000 / 10.0) * 10 + " m";
        }
        return String.format(new Locale("es", "CO"), "%.1f km", km);
    }
}
