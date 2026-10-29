package com.usb.edadperro.alarmas;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Cuidado;
import com.usb.edadperro.util.Formato;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Alarmas locales de los cuidados (AlarmManager + notificación con sonido de alarma).
 *
 * Cada cuidado activo tiene hasta tres alarmas, identificadas por una «clave»:
 * <pre>
 * {id}            a la hora programada
 * {id}#antes      aviso anticipado (1 hora o 1 día antes)
 * {id}#pospuesta  cuando el usuario pospone la alarma 10 minutos
 * </pre>
 * Las alarmas programadas se guardan también en SharedPreferences, porque Android
 * las borra al reiniciar el teléfono y hay que volver a programarlas sin conexión.
 * No usa servicios de pago: todo ocurre en el dispositivo.
 */
public final class Recordatorios {

    public static final String CANAL = "alarmas_cuidados";
    public static final int MINUTOS_POSPONER = 10;

    static final String ACCION_SONAR = "com.usb.edadperro.alarma.SONAR";
    static final String ACCION_HECHO = "com.usb.edadperro.alarma.HECHO";
    static final String ACCION_POSPONER = "com.usb.edadperro.alarma.POSPONER";
    static final String EXTRA_CLAVE = "clave";
    static final String EXTRA_ID = "cuidadoId";
    static final String EXTRA_TITULO = "titulo";
    static final String EXTRA_TEXTO = "texto";

    private static final String ANTES = "#antes";
    private static final String POSPUESTA = "#pospuesta";
    private static final String PREFS = "recordatorios";

    private Recordatorios() {
    }

    // ---------- Permisos ----------

    /** Canal con sonido de alarma y vibración (Android 8+). Se crea al iniciar la app. */
    public static void crearCanal(Context ctx) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel canal = new NotificationChannel(CANAL,
                ctx.getString(R.string.canal_alarmas), NotificationManager.IMPORTANCE_HIGH);
        canal.setDescription(ctx.getString(R.string.canal_alarmas_detalle));
        canal.setSound(sonido(), new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        canal.enableVibration(true);
        canal.setVibrationPattern(VIBRACION);
        canal.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ctx.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }

    private static final long[] VIBRACION = {0, 800, 400, 800, 400, 800};

    private static Uri sonido() {
        Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        return u != null ? u : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
    }

    public static boolean puedeNotificar(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(ctx).areNotificationsEnabled();
    }

    /** En Android 12+ el usuario puede negar las alarmas exactas; sin ellas suenan con unos minutos de retraso. */
    public static boolean alarmasExactasPermitidas(Context ctx) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
                || ctx.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    /** Pantalla de ajustes donde se permiten las alarmas exactas (Android 12+). */
    public static Intent ajustesAlarmasExactas(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    Uri.parse("package:" + ctx.getPackageName()));
        }
        return ajustesNotificaciones(ctx);
    }

    public static Intent ajustesNotificaciones(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.getPackageName());
        }
        return new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + ctx.getPackageName()));
    }

    // ---------- Programación ----------

    /** Programa (o reprograma) las alarmas de un cuidado según su fecha y aviso. */
    public static void programar(Context ctx, Cuidado c) {
        cancelarClave(ctx, c.id);
        cancelarClave(ctx, c.id + ANTES);
        if (!c.activo || c.proxima == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        long hora = c.proxima.getTime();
        String titulo = titulo(c);
        String cuando = Formato.fechaHora(c.proxima);
        String notas = c.notas == null || c.notas.isEmpty() ? "" : "\n" + c.notas;
        if (hora > ahora) {
            programarEn(ctx, c.id, c.id, hora, titulo,
                    ctx.getString(R.string.alarma_es_hora, cuando) + notas);
        }
        long antes = hora - c.avisoMinutos * 60_000L;
        if (c.avisoMinutos > 0 && antes > ahora) {
            programarEn(ctx, c.id + ANTES, c.id, antes, titulo,
                    ctx.getString(R.string.alarma_aviso_previo, cuando) + notas);
        }
    }

    /**
     * Deja programadas exactamente las alarmas de esta lista: programa las vigentes
     * y cancela las de cuidados que ya no existen (por ejemplo, eliminados en otro equipo).
     */
    public static void sincronizar(Context ctx, List<Cuidado> cuidados) {
        Set<String> ids = new HashSet<>();
        for (Cuidado c : cuidados) {
            ids.add(c.id);
            programar(ctx, c);
        }
        for (String clave : new ArrayList<>(prefs(ctx).getAll().keySet())) {
            if (!ids.contains(idDe(clave))) {
                cancelarClave(ctx, clave);
            }
        }
    }

    /** Después de marcar el cuidado como hecho: quita la alarma sonando y programa la siguiente. */
    public static void alMarcarHecho(Context ctx, Cuidado c) {
        cancelar(ctx, c.id);
        programar(ctx, c);
    }

    /** Cancela todas las alarmas y la notificación de un cuidado. */
    public static void cancelar(Context ctx, String id) {
        cancelarClave(ctx, id);
        cancelarClave(ctx, id + ANTES);
        cancelarClave(ctx, id + POSPUESTA);
        NotificationManagerCompat.from(ctx).cancel(idNotificacion(id));
    }

    /** Al cerrar sesión: las alarmas son de la cuenta, no del teléfono. */
    public static void cancelarTodas(Context ctx) {
        for (String clave : new ArrayList<>(prefs(ctx).getAll().keySet())) {
            cancelarClave(ctx, clave);
            NotificationManagerCompat.from(ctx).cancel(idNotificacion(idDe(clave)));
        }
    }

    static void posponer(Context ctx, String id, String titulo, String texto) {
        NotificationManagerCompat.from(ctx).cancel(idNotificacion(id));
        programarEn(ctx, id + POSPUESTA, id,
                System.currentTimeMillis() + MINUTOS_POSPONER * 60_000L, titulo, texto);
    }

    /**
     * Tras reiniciar el teléfono o cambiar la hora: vuelve a programar las alarmas
     * guardadas y muestra de inmediato las que debieron sonar con el teléfono apagado.
     */
    static void reprogramarGuardadas(Context ctx) {
        long ahora = System.currentTimeMillis();
        for (Map.Entry<String, ?> e : prefs(ctx).getAll().entrySet()) {
            try {
                JSONObject j = new JSONObject(String.valueOf(e.getValue()));
                String id = j.getString("id");
                String titulo = j.getString("titulo");
                String texto = j.getString("texto");
                if (j.getLong("t") > ahora) {
                    programarEn(ctx, e.getKey(), id, j.getLong("t"), titulo, texto);
                } else {
                    prefs(ctx).edit().remove(e.getKey()).apply();
                    mostrar(ctx, id, titulo, texto);
                }
            } catch (JSONException ex) {
                prefs(ctx).edit().remove(e.getKey()).apply();
            }
        }
    }

    /** La alarma ya sonó: se olvida para no repetirla al reiniciar. */
    static void olvidar(Context ctx, String clave) {
        prefs(ctx).edit().remove(clave).apply();
    }

    private static void programarEn(Context ctx, String clave, String id, long cuando,
                                    String titulo, String texto) {
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0,
                intentoAlarma(ctx, clave, id, titulo, texto),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = ctx.getSystemService(AlarmManager.class);
        try {
            if (alarmasExactasPermitidas(ctx)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi);
            }
        } catch (SecurityException e) {
            // El permiso de alarmas exactas se retiró justo ahora.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cuando, pi);
        }
        try {
            JSONObject j = new JSONObject();
            j.put("t", cuando);
            j.put("id", id);
            j.put("titulo", titulo);
            j.put("texto", texto);
            prefs(ctx).edit().putString(clave, j.toString()).apply();
        } catch (JSONException e) {
            // No ocurre con valores de texto y número.
        }
    }

    private static void cancelarClave(Context ctx, String clave) {
        PendingIntent pi = PendingIntent.getBroadcast(ctx, 0,
                intentoAlarma(ctx, clave, idDe(clave), null, null),
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        if (pi != null) {
            ctx.getSystemService(AlarmManager.class).cancel(pi);
            pi.cancel();
        }
        prefs(ctx).edit().remove(clave).apply();
    }

    /** La URI con la clave hace único cada PendingIntent (los extras no cuentan para eso). */
    private static Intent intentoAlarma(Context ctx, String clave, String id, String titulo, String texto) {
        return new Intent(ctx, AlarmaReceiver.class)
                .setAction(ACCION_SONAR)
                .setData(Uri.parse("huellitas://alarma/" + Uri.encode(clave)))
                .putExtra(EXTRA_CLAVE, clave)
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITULO, titulo)
                .putExtra(EXTRA_TEXTO, texto);
    }

    // ---------- Notificación ----------

    /** Muestra la alarma: notificación que suena hasta que el usuario la atiende. */
    static void mostrar(Context ctx, String id, String titulo, String texto) {
        if (!puedeNotificar(ctx)) {
            return;
        }
        int nid = idNotificacion(id);
        PendingIntent abrir = PendingIntent.getActivity(ctx, nid,
                new Intent(ctx, MainActivity.class)
                        .putExtra(MainActivity.ARG_ABRIR_CUIDADOS, true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent pantalla = PendingIntent.getActivity(ctx, nid,
                new Intent(ctx, AlarmaActivity.class)
                        .putExtra(EXTRA_ID, id)
                        .putExtra(EXTRA_TITULO, titulo)
                        .putExtra(EXTRA_TEXTO, texto)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(ctx, CANAL)
                .setSmallIcon(R.drawable.ic_alarma)
                .setColor(ContextCompat.getColor(ctx, R.color.verde))
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSound(sonido(), android.media.AudioManager.STREAM_ALARM)
                .setVibrate(VIBRACION)
                .setAutoCancel(true)
                .setContentIntent(abrir)
                .setFullScreenIntent(pantalla, true)
                .addAction(R.drawable.ic_aprobado, ctx.getString(R.string.alarma_hecho),
                        accion(ctx, ACCION_HECHO, id, titulo, texto))
                .addAction(R.drawable.ic_pendiente,
                        ctx.getString(R.string.alarma_posponer, MINUTOS_POSPONER),
                        accion(ctx, ACCION_POSPONER, id, titulo, texto));
        Notification n = nb.build();
        // Insistente: el sonido se repite hasta que el usuario toque la alarma, como un despertador.
        n.flags |= Notification.FLAG_INSISTENT;
        try {
            NotificationManagerCompat.from(ctx).notify(nid, n);
        } catch (SecurityException e) {
            // Permiso de notificaciones retirado: no hay forma de avisar.
        }
    }

    static PendingIntent accion(Context ctx, String accion, String id, String titulo, String texto) {
        Intent i = new Intent(ctx, AlarmaReceiver.class)
                .setAction(accion)
                .setData(Uri.parse("huellitas://accion/" + Uri.encode(id)))
                .putExtra(EXTRA_ID, id)
                .putExtra(EXTRA_TITULO, titulo)
                .putExtra(EXTRA_TEXTO, texto);
        return PendingIntent.getBroadcast(ctx, 0, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    static int idNotificacion(String id) {
        return ("cuidado:" + id).hashCode();
    }

    private static String titulo(Cuidado c) {
        return c.perroNombre == null || c.perroNombre.isEmpty()
                ? c.titulo() : c.perroNombre + ": " + c.titulo();
    }

    private static String idDe(String clave) {
        int i = clave.indexOf('#');
        return i < 0 ? clave : clave.substring(0, i);
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
