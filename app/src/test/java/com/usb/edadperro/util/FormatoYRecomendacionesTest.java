package com.usb.edadperro.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.usb.edadperro.CalculadoraEdad;

import org.junit.Test;

/**
 * Pruebas del formato de precios y de las recomendaciones por etapa de vida.
 */
public class FormatoYRecomendacionesTest {

    @Test
    public void formateaPesosColombianos() {
        assertEquals("$ 45.900", Formato.pesos(45_900));
        assertEquals("$ 1.250.000", Formato.pesos(1_250_000));
        assertEquals("$ 0", Formato.pesos(0));
    }

    @Test
    public void nombreDeProductoNoRepiteLaMarca() {
        assertEquals("Pedigree Dentastix", Formato.producto("Pedigree", "Dentastix"));
        assertEquals("Cesar Clásicos", Formato.producto("Cesar", "Cesar Clásicos"));
        assertEquals("Cesar Clásicos", Formato.producto("CESAR", "Cesar Clásicos"));
        assertEquals("Dentastix", Formato.producto("", "Dentastix"));
    }

    @Test
    public void cadaEtapaTieneRecomendaciones() {
        for (CalculadoraEdad.Etapa etapa : CalculadoraEdad.Etapa.values()) {
            assertFalse(Recomendaciones.obtener(etapa, CalculadoraEdad.Tamano.MEDIANO).isEmpty());
        }
    }

    @Test
    public void razaGrandeAgregaRecomendacionDeArticulaciones() {
        boolean menciona = false;
        for (String r : Recomendaciones.obtener(CalculadoraEdad.Etapa.ADULTO,
                CalculadoraEdad.Tamano.GRANDE)) {
            menciona |= r.contains("articulaciones");
        }
        assertTrue(menciona);
    }
}
