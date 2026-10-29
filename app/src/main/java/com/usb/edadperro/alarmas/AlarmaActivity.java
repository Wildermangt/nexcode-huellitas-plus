package com.usb.edadperro.alarmas;

import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.usb.edadperro.R;
import com.usb.edadperro.databinding.ActivityAlarmaBinding;

/**
 * Pantalla completa de la alarma: aparece sobre la pantalla de bloqueo cuando
 * suena un recordatorio con el teléfono bloqueado, como un despertador.
 */
public class AlarmaActivity extends AppCompatActivity {

    private String id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ActivityAlarmaBinding b = ActivityAlarmaBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        id = getIntent().getStringExtra(Recordatorios.EXTRA_ID);
        String titulo = getIntent().getStringExtra(Recordatorios.EXTRA_TITULO);
        String texto = getIntent().getStringExtra(Recordatorios.EXTRA_TEXTO);
        if (id == null) {
            finish();
            return;
        }
        b.tvTitulo.setText(titulo);
        b.tvTexto.setText(texto);
        b.btnPosponer.setText(getString(R.string.alarma_posponer, Recordatorios.MINUTOS_POSPONER));

        b.btnHecho.setOnClickListener(v -> enviar(Recordatorios.ACCION_HECHO, titulo, texto));
        b.btnPosponer.setOnClickListener(v -> enviar(Recordatorios.ACCION_POSPONER, titulo, texto));
        b.btnDetener.setOnClickListener(v -> {
            NotificationManagerCompat.from(this).cancel(Recordatorios.idNotificacion(id));
            finish();
        });
    }

    /** Reutiliza las mismas acciones de los botones de la notificación. */
    private void enviar(String accion, String titulo, String texto) {
        try {
            Recordatorios.accion(this, accion, id, titulo, texto).send();
        } catch (android.app.PendingIntent.CanceledException e) {
            NotificationManagerCompat.from(this).cancel(Recordatorios.idNotificacion(id));
        }
        finish();
    }
}
