package com.usb.edadperro.data.modelo;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entidad Conversación entre dos usuarios (colección "conversaciones").
 * El id es la unión ordenada de los dos uid, así cada pareja tiene una sola.
 */
public class Conversacion {

    @DocumentId
    public String id;
    public List<String> participantes = new ArrayList<>();
    /** uid → nombre visible, para mostrar la lista sin consultas extra. */
    public Map<String, String> nombres = new HashMap<>();
    public String ultimoMensaje;
    public String ultimoRemitente;
    public Date actualizado;

    public Conversacion() {
        // Requerido por Firestore
    }

    /** Id determinístico de la conversación entre dos usuarios. */
    public static String idPara(String uidA, String uidB) {
        return uidA.compareTo(uidB) < 0 ? uidA + "_" + uidB : uidB + "_" + uidA;
    }

    @Exclude
    public String otroUid(String miUid) {
        for (String p : participantes) {
            if (!p.equals(miUid)) {
                return p;
            }
        }
        return miUid;
    }

    @Exclude
    public String otroNombre(String miUid) {
        String n = nombres.get(otroUid(miUid));
        return n == null ? "Usuario" : n;
    }
}
