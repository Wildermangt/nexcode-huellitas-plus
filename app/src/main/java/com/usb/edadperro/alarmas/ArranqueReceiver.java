package com.usb.edadperro.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Android borra las alarmas al reiniciar el teléfono, al actualizar la app o al
 * cambiar la hora; aquí se vuelven a programar desde la copia local.
 */
public class ArranqueReceiver extends BroadcastReceiver {

    /** Solo se atienden los avisos del sistema declarados en el manifiesto. */
    private static final Set<String> ACCIONES = new HashSet<>(Arrays.asList(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            // AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED (Android 12+)
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"));

    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (intent == null || !ACCIONES.contains(intent.getAction())) {
            return;
        }
        Recordatorios.crearCanal(ctx);
        Recordatorios.reprogramarGuardadas(ctx);
    }
}
