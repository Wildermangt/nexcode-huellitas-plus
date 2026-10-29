package com.usb.edadperro.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Pruebas del cálculo del carrito (Punto 11.6: el carrito calcula correctamente los valores).
 */
public class CarritoCalculadoraTest {

    /** Línea de carrito simple para las pruebas. */
    private static CarritoCalculadora.Linea linea(final long precio, final int cantidad) {
        return new CarritoCalculadora.Linea() {
            @Override
            public long getPrecioUnitario() {
                return precio;
            }

            @Override
            public int getCantidad() {
                return cantidad;
            }
        };
    }

    @Test
    public void subtotalDeLineaEsPrecioPorCantidad() {
        assertEquals(91_800, CarritoCalculadora.subtotalLinea(45_900, 2));
    }

    @Test
    public void carritoConEnvioCobrado() {
        List<CarritoCalculadora.Linea> lineas = Arrays.asList(linea(45_900, 2), linea(6_500, 3));
        CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(lineas);
        assertEquals(111_300, r.subtotal);
        assertEquals(CarritoCalculadora.COSTO_ENVIO, r.envio);
        assertEquals(120_300, r.total);
        assertEquals(5, r.unidades);
        assertEquals(38_700, r.faltaParaEnvioGratis());
    }

    @Test
    public void envioGratisDesdeElMinimo() {
        CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(
                Collections.singletonList(linea(150_000, 1)));
        assertEquals(0, r.envio);
        assertEquals(150_000, r.total);
        assertEquals(0, r.faltaParaEnvioGratis());
    }

    @Test
    public void carritoVacioNoCobraEnvio() {
        CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(
                Collections.<CarritoCalculadora.Linea>emptyList());
        assertEquals(0, r.subtotal);
        assertEquals(0, r.envio);
        assertEquals(0, r.total);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cantidadNegativaNoSePermite() {
        CarritoCalculadora.subtotalLinea(10_000, -1);
    }

    @Test
    public void cantidadSeLimitaAlInventario() {
        assertEquals(3, CarritoCalculadora.limitarCantidad(5, 3));
        assertEquals(1, CarritoCalculadora.limitarCantidad(0, 10));
        assertEquals(CarritoCalculadora.MAX_POR_PRODUCTO, CarritoCalculadora.limitarCantidad(99, 500));
        assertEquals(0, CarritoCalculadora.limitarCantidad(2, 0));
    }
}
