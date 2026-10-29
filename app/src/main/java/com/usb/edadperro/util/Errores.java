package com.usb.edadperro.util;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.ai.type.QuotaExceededException;
import com.google.firebase.ai.type.ServerException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Traduce las excepciones técnicas a mensajes claros para el usuario
 * (Punto 11.10: mensajes adecuados ante errores o falta de conexión).
 */
public final class Errores {

    public static final String SIN_CONEXION =
            "No hay conexión a internet. Revisa tu red e inténtalo de nuevo.";
    private static final String GENERICO = "Ocurrió un error inesperado. Inténtalo de nuevo.";

    private Errores() {
    }

    /** Error con un mensaje ya pensado para el usuario (se muestra tal cual). */
    public static class Amigable extends IOException {
        public Amigable(String mensaje) {
            super(mensaje);
        }

        public Amigable(String mensaje, Throwable causa) {
            super(mensaje, causa);
        }
    }

    public static String mensaje(Throwable e) {
        if (e == null) {
            return GENERICO;
        }
        // 1. Mensajes amigables y errores de Firebase, en cualquier nivel de la cadena.
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof Amigable) {
                return t.getMessage();
            }
            String firebase = deFirebase(t);
            if (firebase != null) {
                return firebase;
            }
            String m = t.getMessage();
            if (m != null && m.contains("genai config not found")) {
                return "El asistente de IA aún no está activado en Firebase (AI Logic). "
                        + "Inténtalo más tarde.";
            }
            if (m != null && m.contains("no longer available")) {
                return "El modelo de IA configurado ya no está disponible (huellita.geminiModel).";
            }
            if (t instanceof QuotaExceededException
                    || (t instanceof ServerException && m != null && m.contains("high demand"))) {
                return "El asistente está recibiendo muchas consultas. Inténtalo de nuevo en unos segundos.";
            }
        }
        // 2. Problemas de red.
        for (Throwable t = e; t != null; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof FirebaseNetworkException || t instanceof UnknownHostException
                    || t instanceof ConnectException || t instanceof NoRouteToHostException) {
                return SIN_CONEXION;
            }
            if (t instanceof SocketTimeoutException) {
                return "El servidor tardó demasiado en responder. Inténtalo de nuevo.";
            }
        }
        if (e instanceof IOException) {
            return SIN_CONEXION;
        }
        // 3. Mensaje de la excepción si es legible; si no, uno genérico.
        String m = e.getMessage();
        return m == null || m.isEmpty() || m.contains("Exception") ? GENERICO : m;
    }

    private static String deFirebase(Throwable t) {
        if (t instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es muy débil: usa al menos 6 caracteres.";
        }
        if (t instanceof FirebaseAuthUserCollisionException) {
            return "Ya existe una cuenta con ese correo.";
        }
        if (t instanceof FirebaseAuthInvalidUserException) {
            return "No existe una cuenta con ese correo o fue deshabilitada.";
        }
        if (t instanceof FirebaseAuthInvalidCredentialsException) {
            return "Correo o contraseña incorrectos.";
        }
        if (t instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException fe = (FirebaseFirestoreException) t;
            switch (fe.getCode()) {
                case PERMISSION_DENIED:
                    return "No tienes permiso para realizar esta acción.";
                case UNAVAILABLE:
                    return SIN_CONEXION;
                case FAILED_PRECONDITION:
                case ABORTED:
                    return fe.getMessage();
                default:
                    return null;
            }
        }
        return null;
    }
}
