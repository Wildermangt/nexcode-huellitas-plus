package com.usb.edadperro.data.repo;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Conversacion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Mensajería entre usuarios registrados (Punto 9). Es independiente del
 * chat con IA: usa sus propias colecciones y no pasa por el modelo.
 *
 * Privacidad: las reglas de Firestore solo permiten leer y escribir una
 * conversación a sus dos participantes, y los demás usuarios solo ven el
 * nombre y la ciudad (perfiles_publicos), nunca el correo ni el teléfono.
 */
public final class ChatRepo {

    /** Longitud máxima de un mensaje (también la validan las reglas). */
    public static final int MAX_MENSAJE = 1000;

    private ChatRepo() {
    }

    public static Query misConversaciones() {
        return Fb.conversaciones().whereArrayContains("participantes", Fb.uid());
    }

    public static Query mensajes(String conversacionId) {
        return Fb.mensajes(conversacionId).orderBy("fecha", Query.Direction.ASCENDING).limitToLast(200);
    }

    // ---------- Bloqueo y reporte (controles de privacidad) ----------

    /** Documento que indica que el usuario actual bloqueó a otro. */
    public static com.google.firebase.firestore.DocumentReference bloqueo(String otroUid) {
        return Fb.usuarios().document(Fb.uid()).collection("bloqueados").document(otroUid);
    }

    /** Usuarios bloqueados por el usuario actual. */
    public static Query misBloqueados() {
        return Fb.usuarios().document(Fb.uid()).collection("bloqueados");
    }

    /**
     * Bloquea a un usuario: ninguno de los dos puede volver a escribir en la
     * conversación (lo hacen cumplir las reglas de Firestore).
     */
    public static Task<Void> bloquear(String otroUid, String otroNombre) {
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", otroNombre);
        m.put("fecha", FieldValue.serverTimestamp());
        return bloqueo(otroUid).set(m);
    }

    public static Task<Void> desbloquear(String otroUid) {
        return bloqueo(otroUid).delete();
    }

    /** Envía un reporte para que el administrador lo revise. */
    public static Task<Void> reportar(String otroUid, String otroNombre, String motivo) {
        Map<String, Object> r = new HashMap<>();
        r.put("reportanteId", Fb.uid());
        r.put("reportanteNombre", SesionRepo.nombre());
        r.put("reportadoId", otroUid);
        r.put("reportadoNombre", otroNombre);
        r.put("conversacionId", Conversacion.idPara(Fb.uid(), otroUid));
        r.put("motivo", motivo);
        r.put("estado", "pendiente");
        r.put("fecha", FieldValue.serverTimestamp());
        return Fb.db().collection("reportes").document().set(r);
    }

    public static Query usuarios() {
        return Fb.perfiles().orderBy("nombre").limit(100);
    }

    /**
     * Envía un mensaje. La conversación y el mensaje se escriben en el mismo
     * lote atómico, así la primera vez también se crea la conversación.
     */
    public static Task<Void> enviar(String otroUid, String otroNombre, String texto) {
        String yo = Fb.uid();
        String miNombre = SesionRepo.nombre();
        String convId = Conversacion.idPara(yo, otroUid);

        Map<String, Object> nombres = new HashMap<>();
        nombres.put(yo, miNombre);
        nombres.put(otroUid, otroNombre);

        Map<String, Object> conv = new HashMap<>();
        // Siempre en el mismo orden, para que las reglas comprueben que no cambian.
        conv.put("participantes", yo.compareTo(otroUid) < 0
                ? Arrays.asList(yo, otroUid) : Arrays.asList(otroUid, yo));
        conv.put("nombres", nombres);
        conv.put("ultimoMensaje", texto.length() > 80 ? texto.substring(0, 80) + "…" : texto);
        conv.put("ultimoRemitente", yo);
        conv.put("actualizado", FieldValue.serverTimestamp());

        Map<String, Object> msg = new HashMap<>();
        msg.put("remitenteId", yo);
        msg.put("remitenteNombre", miNombre);
        msg.put("texto", texto);
        msg.put("fecha", FieldValue.serverTimestamp());

        WriteBatch b = Fb.db().batch();
        b.set(Fb.conversaciones().document(convId), conv, SetOptions.merge());
        b.set(Fb.mensajes(convId).document(), msg);
        return b.commit();
    }
}
