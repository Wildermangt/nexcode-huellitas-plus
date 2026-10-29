package com.usb.edadperro.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Pruebas de la restricción del marketplace (Punto 11.5: solo alimentos para perros).
 */
public class ValidadorCatalogoTest {

    @Test
    public void concentradoParaPerroEsValido() {
        assertTrue(ValidadorCatalogo.validar("Dog Chow Adultos Medianos y Grandes", "Purina",
                "Alimento completo para perros adultos", "Bulto 4 kg", "concentrado",
                "perro", 79_900, 10).isEmpty());
    }

    @Test
    public void rechazaEspecieDistintaDePerro() {
        assertFalse(ValidadorCatalogo.validar("Cat Chow", "Purina", "Alimento", "1 kg",
                "concentrado", "gato", 30_000, 5).isEmpty());
    }

    @Test
    public void rechazaProductoParaGatoAunqueDigaPerro() {
        assertFalse(ValidadorCatalogo.validar("Alimento para gatitos", "Marca",
                "Croquetas para felinos", "1 kg", "concentrado", "perro", 30_000, 5).isEmpty());
    }

    @Test
    public void rechazaProductosQueNoSonAlimento() {
        assertFalse(ValidadorCatalogo.validar("Shampoo antipulgas", "Marca", "Para perros",
                "500 ml", "snacks", "perro", 20_000, 5).isEmpty());
    }

    @Test
    public void rechazaCategoriaInvalidaYPrecioCero() {
        assertFalse(ValidadorCatalogo.validar("Galletas", "Marca", "Snack", "200 g",
                "accesorios", "perro", 0, 5).isEmpty());
    }
}
