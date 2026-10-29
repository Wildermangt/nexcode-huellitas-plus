package com.usb.edadperro;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Pruebas unitarias de la lógica de cálculo.
 * Se ejecutan con: Clic derecho sobre el archivo > Run 'CalculadoraEdadTest'
 *
 * Autores: Jeferson Wilderman González Tenjo - José Leandro Ocampo Camacho
 */
public class CalculadoraEdadTest {

    /** Margen de tolerancia para comparar números decimales. */
    private static final double DELTA = 0.01;

    @Test
    public void primerAnoEquivaleA15AnosHumanos() {
        assertEquals(15.0,
                CalculadoraEdad.calcularVeterinario(1, CalculadoraEdad.Tamano.MEDIANO), DELTA);
    }

    @Test
    public void segundoAnoEquivaleA24AnosHumanos() {
        assertEquals(24.0,
                CalculadoraEdad.calcularVeterinario(2, CalculadoraEdad.Tamano.MEDIANO), DELTA);
    }

    @Test
    public void medioAnoEsProporcional() {
        assertEquals(7.5,
                CalculadoraEdad.calcularVeterinario(0.5, CalculadoraEdad.Tamano.MEDIANO), DELTA);
    }

    @Test
    public void perroGrandeEnvejeceMasRapidoQuePerroPequeno() {
        double grande = CalculadoraEdad.calcularVeterinario(10, CalculadoraEdad.Tamano.GRANDE);
        double pequeno = CalculadoraEdad.calcularVeterinario(10, CalculadoraEdad.Tamano.PEQUENO);
        // 24 + 8*6 = 72 para el grande, 24 + 8*4 = 56 para el pequeño
        assertEquals(72.0, grande, DELTA);
        assertEquals(56.0, pequeno, DELTA);
    }

    @Test
    public void metodoTradicionalMultiplicaPorSiete() {
        assertEquals(35.0, CalculadoraEdad.calcularTradicional(5), DELTA);
    }

    @Test
    public void formulaEpigeneticaDevuelveValorEsperado() {
        // 16 * ln(5) + 31 = 56.75
        assertEquals(56.75, CalculadoraEdad.calcularEpigenetico(5), DELTA);
    }

    @Test
    public void etapaDeVidaSeClasificaCorrectamente() {
        assertEquals(CalculadoraEdad.Etapa.CACHORRO,
                CalculadoraEdad.obtenerEtapa(0.5, CalculadoraEdad.Tamano.MEDIANO));
        assertEquals(CalculadoraEdad.Etapa.JOVEN,
                CalculadoraEdad.obtenerEtapa(2, CalculadoraEdad.Tamano.MEDIANO));
        assertEquals(CalculadoraEdad.Etapa.ADULTO,
                CalculadoraEdad.obtenerEtapa(5, CalculadoraEdad.Tamano.MEDIANO));
        assertEquals(CalculadoraEdad.Etapa.SENIOR,
                CalculadoraEdad.obtenerEtapa(10, CalculadoraEdad.Tamano.MEDIANO));
    }
}
