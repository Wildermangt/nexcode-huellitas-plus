package com.usb.edadperro.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas del detector de urgencias del chat con IA (Punto 4, requisito obligatorio).
 */
public class DetectorUrgenciasTest {

    @Test
    public void detectaIntoxicacionConTildesYMayusculas() {
        assertTrue(DetectorUrgencias.esUrgencia("Mi perro está ENVENENADO, ¿qué hago?"));
    }

    @Test
    public void detectaIngestaDeChocolate() {
        assertTrue(DetectorUrgencias.esUrgencia("Max se comió una barra de chocolate hace una hora"));
    }

    @Test
    public void detectaConvulsionesYDificultadRespiratoria() {
        assertTrue(DetectorUrgencias.esUrgencia("tiene convulsiones"));
        assertTrue(DetectorUrgencias.esUrgencia("le cuesta respirar y tiene las encías moradas"));
    }

    @Test
    public void detectaAtropelloYSangrado() {
        assertTrue(DetectorUrgencias.esUrgencia("lo atropelló una moto"));
        assertTrue(DetectorUrgencias.esUrgencia("está vomitando sangre y no se mueve"));
        assertTrue(DetectorUrgencias.esUrgencia("vomita sangre"));
    }

    @Test
    public void preguntaNormalNoEsUrgencia() {
        assertFalse(DetectorUrgencias.esUrgencia("¿Cada cuánto debo bañar a mi perro?"));
        assertFalse(DetectorUrgencias.esUrgencia("¿Qué concentrado es bueno para un cachorro?"));
        assertFalse(DetectorUrgencias.esUrgencia("¿Los perros pueden comer chocolate?"));
        assertFalse(DetectorUrgencias.esUrgencia(""));
        assertFalse(DetectorUrgencias.esUrgencia(null));
    }

    @Test
    public void devuelveMotivosSinRepetir() {
        assertEquals(1, DetectorUrgencias.motivos("envenenado con veneno para ratas").size());
    }

    @Test
    public void manejaLaEtiquetaDeLaIa() {
        String r = "[URGENCIA] Lleva a tu perro de inmediato al veterinario.";
        assertTrue(DetectorUrgencias.respuestaMarcaUrgencia(r));
        assertEquals("Lleva a tu perro de inmediato al veterinario.",
                DetectorUrgencias.limpiarRespuesta(r));
    }
}
