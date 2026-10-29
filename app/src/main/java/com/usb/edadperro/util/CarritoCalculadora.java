package com.usb.edadperro.util;

import java.util.List;

/**
 * Cálculo de valores del carrito de compras (subtotales, envío y total).
 *
 * No depende de Android ni de Firebase, por lo que se prueba con JUnit.
 * Todos los valores se manejan en pesos colombianos enteros (COP).
 */
public final class CarritoCalculadora {

    /** A partir de este subtotal el envío es gratis. */
    public static final long ENVIO_GRATIS_DESDE = 150_000L;
    /** Costo fijo del envío cuando el subtotal no alcanza el mínimo. */
    public static final long COSTO_ENVIO = 9_000L;
    /** Cantidad máxima de unidades de un mismo producto por pedido. */
    public static final int MAX_POR_PRODUCTO = 20;

    private CarritoCalculadora() {
    }

    /** Una línea del carrito: precio unitario y cantidad. */
    public interface Linea {
        long getPrecioUnitario();

        int getCantidad();
    }

    /** Resultado del cálculo de un carrito. */
    public static final class Resumen {
        public final long subtotal;
        public final long envio;
        public final long total;
        public final int unidades;

        Resumen(long subtotal, long envio, int unidades) {
            this.subtotal = subtotal;
            this.envio = envio;
            this.total = subtotal + envio;
            this.unidades = unidades;
        }

        /** @return cuánto falta para obtener envío gratis (0 si ya aplica) */
        public long faltaParaEnvioGratis() {
            return envio == 0 ? 0 : ENVIO_GRATIS_DESDE - subtotal;
        }
    }

    /**
     * @return precio unitario por cantidad
     * @throws IllegalArgumentException si el precio o la cantidad son negativos
     */
    public static long subtotalLinea(long precioUnitario, int cantidad) {
        if (precioUnitario < 0 || cantidad < 0) {
            throw new IllegalArgumentException("Precio y cantidad deben ser positivos");
        }
        return precioUnitario * cantidad;
    }

    /** Costo de envío según el subtotal. Un carrito vacío no paga envío. */
    public static long envio(long subtotal) {
        if (subtotal <= 0 || subtotal >= ENVIO_GRATIS_DESDE) {
            return 0;
        }
        return COSTO_ENVIO;
    }

    /** Calcula subtotal, envío, total y número de unidades de una lista de líneas. */
    public static Resumen calcular(List<? extends Linea> lineas) {
        long subtotal = 0;
        int unidades = 0;
        if (lineas != null) {
            for (Linea l : lineas) {
                subtotal += subtotalLinea(l.getPrecioUnitario(), l.getCantidad());
                unidades += l.getCantidad();
            }
        }
        return new Resumen(subtotal, envio(subtotal), unidades);
    }

    /**
     * Ajusta una cantidad al rango permitido [1, min(stock, MAX_POR_PRODUCTO)].
     *
     * @return la cantidad válida, o 0 si no hay existencias
     */
    public static int limitarCantidad(int deseada, long stock) {
        long maximo = Math.min(stock, MAX_POR_PRODUCTO);
        if (maximo <= 0) {
            return 0;
        }
        if (deseada < 1) {
            return 1;
        }
        return (int) Math.min(deseada, maximo);
    }
}
