package com.usb.edadperro.data.repo;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Perro;

import java.util.HashMap;
import java.util.Map;

/**
 * Perros registrados por el usuario (Puntos 1 y 3).
 */
public final class PerroRepo {

    private PerroRepo() {
    }

    public static Query misPerros() {
        return Fb.perros(Fb.uid()).orderBy("nombre");
    }

    public static DocumentReference perro(String id) {
        return Fb.perros(Fb.uid()).document(id);
    }

    public static Task<Void> guardar(Perro p) {
        DocumentReference ref = p.id == null ? Fb.perros(Fb.uid()).document() : perro(p.id);
        Map<String, Object> m = new HashMap<>();
        m.put("nombre", p.nombre);
        m.put("raza", p.raza);
        m.put("edad", p.edad);
        m.put("tamano", p.tamano);
        m.put("peso", p.peso);
        m.put("sexo", p.sexo);
        m.put("notas", p.notas);
        m.put("fotoBase64", p.fotoBase64);
        m.put("creado", p.creado == null ? FieldValue.serverTimestamp() : p.creado);
        return ref.set(m);
    }

    public static Task<Void> eliminar(String id) {
        return perro(id).delete();
    }
}
