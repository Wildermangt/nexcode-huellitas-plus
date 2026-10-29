package com.usb.edadperro.alarmas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.repo.CuidadoRepo;

/**
 * Recibe la alarma programada (la muestra) y los botones de la notificación
 * («Hecho» y «Posponer 10 min»).
 */
public class AlarmaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {
        String accion = intent.getAction();
        String id = intent.getStringExtra(Recordatorios.EXTRA_ID);
        String titulo = intent.getStringExtra(Recordatorios.EXTRA_TITULO);
        String texto = intent.getStringExtra(Recordatorios.EXTRA_TEXTO);
        if (accion == null || id == null) {
            return;
        }
        switch (accion) {
            case Recordatorios.ACCION_SONAR:
                Recordatorios.olvidar(ctx, intent.getStringExtra(Recordatorios.EXTRA_CLAVE));
                Recordatorios.mostrar(ctx, id, titulo, texto);
                break;
            case Recordatorios.ACCION_POSPONER:
                Recordatorios.posponer(ctx, id, titulo, texto);
                Toast.makeText(ctx, ctx.getString(R.string.alarma_pospuesta, Recordatorios.MINUTOS_POSPONER),
                        Toast.LENGTH_SHORT).show();
                break;
            case Recordatorios.ACCION_HECHO:
                marcarHecho(ctx, id);
                break;
            default:
                break;
        }
    }

    private void marcarHecho(Context ctx, String id) {
        NotificationManagerCompat.from(ctx).cancel(Recordatorios.idNotificacion(id));
        if (Fb.uid() == null) {
            Recordatorios.cancelar(ctx, id);
            return;
        }
        Context app = ctx.getApplicationContext();
        PendingResult pendiente = goAsync();
        CuidadoRepo.marcarHecho(id).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                Recordatorios.alMarcarHecho(app, t.getResult());
                Toast.makeText(app, R.string.cuidado_registrado, Toast.LENGTH_SHORT).show();
            } else {
                Recordatorios.cancelar(app, id);
            }
            pendiente.finish();
        });
    }
}
