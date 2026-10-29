package com.usb.edadperro.data.repo;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Source;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Cuidado;
import com.usb.edadperro.util.Periodicidad;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cuidados periódicos de los perros (vacunas, medicamentos, baños…) y su historial.
 */
public final class CuidadoRepo {

    /** Máximo de fechas que se guardan en el historial de cada cuidado. */
    static final int MAX_HISTORIAL = 60;

    private CuidadoRepo() {
    }

    public static CollectionReference misCuidados() {
        return Fb.cuidados(Fb.uid());
    }

    public static DocumentReference cuidado(String id) {
        return misCuidados().document(id);
    }

    /** Crea o actualiza el cuidado y deja en c.id el id del documento. */
    public static Task<Void> guardar(Cuidado c) {
        DocumentReference ref = c.id == null ? misCuidados().document() : cuidado(c.id);
        c.id = ref.getId();
        Map<String, Object> m = new HashMap<>();
        m.put("perroId", c.perroId);
        m.put("perroNombre", c.perroNombre);
        m.put("tipo", c.tipo);
        m.put("nombre", c.nombre);
        m.put("notas", c.notas);
        m.put("proxima", c.proxima);
        m.put("cada", c.cada);
        m.put("unidad", c.unidad);
        m.put("avisoMinutos", c.avisoMinutos);
        m.put("activo", c.activo);
        m.put("historial", c.historial == null ? new ArrayList<>() : c.historial);
        m.put("creado", c.creado == null ? FieldValue.serverTimestamp() : c.creado);
        return ref.set(m);
    }

    public static Task<Void> eliminar(String id) {
        return cuidado(id).delete();
    }

    /**
     * Registra que el cuidado se hizo ahora: lo agrega al historial y calcula la
     * siguiente fecha (o lo desactiva si no se repite). Modifica y guarda c.
     */
    public static Task<Void> marcarHecho(Cuidado c) {
        Date ahora = new Date();
        List<Date> historial = c.historial == null ? new ArrayList<>() : new ArrayList<>(c.historial);
        historial.add(ahora);
        while (historial.size() > MAX_HISTORIAL) {
            historial.remove(0);
        }
        c.historial = historial;
        Date siguiente = Periodicidad.siguienteTrasHecho(c.proxima, ahora, c.cada, c.unidadEnum());
        if (siguiente == null) {
            c.activo = false;
        } else {
            c.proxima = siguiente;
            c.activo = true;
        }
        return guardar(c);
    }

    /**
     * Igual que {@link #marcarHecho(Cuidado)} pero a partir del id (desde la notificación).
     * Lee primero la caché local y no espera la confirmación del servidor, para que
     * funcione sin conexión (Firestore aplica la escritura en la caché al instante).
     *
     * @return el cuidado ya actualizado
     */
    public static Task<Cuidado> marcarHecho(String id) {
        DocumentReference ref = cuidado(id);
        return ref.get(Source.CACHE)
                .continueWithTask(t -> t.isSuccessful() && t.getResult().exists() ? t : ref.get())
                .continueWithTask(t -> {
                    DocumentSnapshot d = t.getResult();
                    Cuidado c = d == null ? null : d.toObject(Cuidado.class);
                    if (c == null) {
                        return Tasks.forException(new IllegalStateException("El cuidado ya no existe"));
                    }
                    marcarHecho(c);
                    return Tasks.forResult(c);
                });
    }
}
