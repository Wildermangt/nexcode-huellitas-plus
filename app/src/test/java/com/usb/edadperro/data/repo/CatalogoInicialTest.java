package com.usb.edadperro.data.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.usb.edadperro.util.ValidadorCatalogo;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Punto 11.5: el catálogo presenta exclusivamente alimentos para perros.
 * Cada producto del catálogo inicial debe pasar el validador del marketplace.
 */
public class CatalogoInicialTest {

    @Test
    public void todosLosProductosSonAlimentosParaPerro() {
        for (Object[] p : CatalogoInicial.PRODUCTOS) {
            List<String> errores = ValidadorCatalogo.validar((String) p[2], (String) p[3], (String) p[7],
                    (String) p[4], (String) p[1], ValidadorCatalogo.ESPECIE, (Long) p[5], (Long) p[6]);
            assertTrue(p[2] + " no es válido: " + errores, errores.isEmpty());
        }
    }

    @Test
    public void hayProductosEnLasTresCategoriasYSinIdsRepetidos() {
        Set<String> categorias = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (Object[] p : CatalogoInicial.PRODUCTOS) {
            categorias.add((String) p[1]);
            assertTrue("Id repetido: " + p[0], ids.add((String) p[0]));
            assertTrue(((String) p[8]).endsWith(".jpg"));
        }
        assertEquals(new HashSet<>(ValidadorCatalogo.CATEGORIAS), categorias);
        assertEquals(14, CatalogoInicial.PRODUCTOS.length);
    }
}
