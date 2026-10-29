package com.usb.edadperro.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Hilos de trabajo para las llamadas HTTP (Overpass, Nominatim, backend de pagos),
 * que no pueden ejecutarse en el hilo principal.
 */
public final class Hilos {

    private static final ExecutorService FONDO = Executors.newFixedThreadPool(3);
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    private Hilos() {
    }

    public static void fondo(Runnable r) {
        FONDO.execute(r);
    }

    public static void principal(Runnable r) {
        PRINCIPAL.post(r);
    }

    public static void principal(Runnable r, long esperaMs) {
        PRINCIPAL.postDelayed(r, esperaMs);
    }
}
