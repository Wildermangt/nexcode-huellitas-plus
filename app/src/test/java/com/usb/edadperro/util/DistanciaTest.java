package com.usb.edadperro.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pruebas del cálculo de distancia a las veterinarias (Haversine).
 */
public class DistanciaTest {

    @Test
    public void mismaUbicacionEsCero() {
        assertEquals(0.0, Distancia.km(4.6097, -74.0817, 4.6097, -74.0817), 0.0001);
    }

    @Test
    public void bogotaAMedellinAproximadamente240Km() {
        // Plaza de Bolívar (Bogotá) a Parque Berrío (Medellín): ~240 km en línea recta
        double km = Distancia.km(4.5981, -74.0760, 6.2502, -75.5683);
        assertEquals(245, km, 10);
    }

    @Test
    public void formateaMetrosYKilometros() {
        assertEquals("850 m", Distancia.formatear(0.8496));
        assertEquals("2,4 km", Distancia.formatear(2.43));
    }
}
