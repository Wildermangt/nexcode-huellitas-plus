package com.usb.edadperro.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.util.Calendar;
import java.util.Date;

/**
 * Pruebas de las fechas de los cuidados periódicos y sus recordatorios.
 */
public class PeriodicidadTest {

    private static Date fecha(int anio, int mes, int dia, int hora, int minuto) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(anio, mes - 1, dia, hora, minuto, 0);
        return c.getTime();
    }

    @Test
    public void sinRepeticionNoHayProximaFecha() {
        Date p = fecha(2026, 10, 24, 8, 0);
        assertNull(Periodicidad.siguienteTrasHecho(p, p, 0, Periodicidad.Unidad.DIAS));
    }

    @Test
    public void vacunaAnualSeCuentaDesdeElDiaAplicado() {
        // Programada el 24 oct a las 9:00, aplicada con retraso el 7 nov.
        Date sig = Periodicidad.siguienteTrasHecho(fecha(2026, 10, 24, 9, 0),
                fecha(2026, 11, 7, 15, 30), 1, Periodicidad.Unidad.ANIOS);
        assertEquals(fecha(2027, 11, 7, 9, 0), sig);
    }

    @Test
    public void desparasitanteCada3MesesRespetaFinDeMes() {
        Date sig = Periodicidad.siguienteTrasHecho(fecha(2026, 11, 30, 10, 0),
                fecha(2026, 11, 30, 10, 5), 3, Periodicidad.Unidad.MESES);
        assertEquals(fecha(2027, 2, 28, 10, 0), sig);
    }

    @Test
    public void medicamentoCada8HorasConservaElHorario() {
        // Toma de las 8:00 marcada a las 17:00: ya pasó también la de las 16:00.
        Date sig = Periodicidad.siguienteTrasHecho(fecha(2026, 10, 24, 8, 0),
                fecha(2026, 10, 24, 17, 0), 8, Periodicidad.Unidad.HORAS);
        assertEquals(fecha(2026, 10, 25, 0, 0), sig);
    }

    @Test
    public void hechoPorAdelantadoAvanzaUnPeriodo() {
        // Baño programado para mañana pero hecho hoy: el siguiente es en 2 semanas desde mañana.
        Date sig = Periodicidad.siguienteTrasHecho(fecha(2026, 10, 25, 10, 0),
                fecha(2026, 10, 24, 18, 0), 2, Periodicidad.Unidad.SEMANAS);
        assertEquals(fecha(2026, 11, 8, 10, 0), sig);
    }

    @Test
    public void estadosSegunLaFecha() {
        Date ahora = fecha(2026, 10, 24, 12, 0);
        assertEquals(Periodicidad.Estado.VENCIDO,
                Periodicidad.estado(fecha(2026, 10, 24, 8, 0), true, ahora));
        assertEquals(Periodicidad.Estado.HOY,
                Periodicidad.estado(fecha(2026, 10, 24, 20, 0), true, ahora));
        assertEquals(Periodicidad.Estado.PRONTO,
                Periodicidad.estado(fecha(2026, 10, 31, 8, 0), true, ahora));
        assertEquals(Periodicidad.Estado.PROGRAMADO,
                Periodicidad.estado(fecha(2026, 11, 1, 8, 0), true, ahora));
        assertEquals(Periodicidad.Estado.COMPLETADO,
                Periodicidad.estado(fecha(2026, 11, 1, 8, 0), false, ahora));
    }

    @Test
    public void textoRelativo() {
        Date ahora = fecha(2026, 10, 24, 23, 0);
        assertEquals("Hoy", Periodicidad.relativo(fecha(2026, 10, 24, 6, 0), ahora));
        assertEquals("Mañana", Periodicidad.relativo(fecha(2026, 10, 25, 1, 0), ahora));
        assertEquals("Hace 3 días", Periodicidad.relativo(fecha(2026, 10, 21, 8, 0), ahora));
        assertEquals("En 10 días", Periodicidad.relativo(fecha(2026, 11, 3, 8, 0), ahora));
        assertEquals("En 1 año", Periodicidad.relativo(fecha(2027, 10, 24, 8, 0), ahora));
    }

    @Test
    public void describeRepeticionYAviso() {
        assertEquals("No se repite", Periodicidad.describir(0, Periodicidad.Unidad.DIAS));
        assertEquals("Cada día", Periodicidad.describir(1, Periodicidad.Unidad.DIAS));
        assertEquals("Cada 8 horas", Periodicidad.describir(8, Periodicidad.Unidad.HORAS));
        assertEquals("Cada 3 meses", Periodicidad.describir(3, Periodicidad.Unidad.MESES));
        assertEquals("A la hora", Periodicidad.describirAviso(0));
        assertEquals("1 hora antes", Periodicidad.describirAviso(60));
        assertEquals("1 día antes", Periodicidad.describirAviso(1440));
    }
}
