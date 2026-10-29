package com.usb.edadperro.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

/**
 * Utilidades para verificar la conexión a internet.
 */
public final class Red {

    private Red() {
    }

    /** @return true si el dispositivo tiene una red con acceso a internet */
    public static boolean hayConexion(Context ctx) {
        ConnectivityManager cm =
                (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) {
            return false;
        }
        Network red = cm.getActiveNetwork();
        if (red == null) {
            return false;
        }
        NetworkCapabilities cap = cm.getNetworkCapabilities(red);
        return cap != null && cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
