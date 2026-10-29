package com.usb.edadperro.data.remoto;

import com.usb.edadperro.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Cliente HTTP compartido. Envía un User-Agent propio, como lo exigen
 * las políticas de uso de Nominatim y Overpass (OpenStreetMap).
 */
public final class Http {

    public static final String USER_AGENT =
            "HuellitasPlus/" + BuildConfig.VERSION_NAME + " (" + BuildConfig.APPLICATION_ID + ")";

    private static OkHttpClient cliente;

    private Http() {
    }

    public static synchronized OkHttpClient cliente() {
        if (cliente == null) {
            cliente = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(40, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request r = chain.request().newBuilder()
                                .header("User-Agent", USER_AGENT)
                                .build();
                        return chain.proceed(r);
                    })
                    .build();
        }
        return cliente;
    }
}
