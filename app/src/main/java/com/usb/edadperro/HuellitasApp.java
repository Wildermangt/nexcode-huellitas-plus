package com.usb.edadperro;

import android.app.Application;

import com.usb.edadperro.alarmas.Recordatorios;
import com.usb.edadperro.data.remoto.Http;

import org.osmdroid.config.Configuration;

import java.io.File;

/**
 * Clase Application: configuración global al arrancar la app.
 */
public class HuellitasApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // osmdroid (mapas de OpenStreetMap): User-Agent propio, como exige la
        // política de uso de los servidores de teselas, y caché dentro de la app
        // para no necesitar permisos de almacenamiento.
        Configuration.getInstance().setUserAgentValue(Http.USER_AGENT);
        File base = new File(getCacheDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(base);
        Configuration.getInstance().setOsmdroidTileCache(new File(base, "tiles"));
        // Canal de notificaciones con sonido de alarma para los cuidados.
        Recordatorios.crearCanal(this);
    }
}
