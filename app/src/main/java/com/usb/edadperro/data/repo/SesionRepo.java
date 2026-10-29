package com.usb.edadperro.data.repo;

import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Usuario;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro, inicio y cierre de sesión, y datos del perfil (Punto 3).
 *
 * La autenticación la hace Firebase Authentication (correo y contraseña);
 * las contraseñas nunca se guardan en Firestore. La autorización se aplica
 * con las reglas de firestore.rules.
 */
public final class SesionRepo {

    /** Copia en memoria del usuario actual (incluye el rol). */
    @Nullable
    private static Usuario actual;

    private SesionRepo() {
    }

    public static boolean haySesion() {
        return Fb.auth().getCurrentUser() != null;
    }

    @Nullable
    public static Usuario actual() {
        return actual;
    }

    /** Nombre para mostrar aunque el perfil aún no se haya cargado. */
    public static String nombre() {
        if (actual != null && actual.nombre != null) {
            return actual.nombre;
        }
        FirebaseUser u = Fb.auth().getCurrentUser();
        if (u != null && u.getDisplayName() != null) {
            return u.getDisplayName();
        }
        return "Usuario";
    }

    /** Crea la cuenta, guarda el nombre y crea los documentos del perfil. */
    public static Task<Void> registrar(String nombre, String email, String clave, String ciudad) {
        return Fb.auth().createUserWithEmailAndPassword(email, clave)
                .onSuccessTask(res -> {
                    FirebaseUser u = res.getUser();
                    UserProfileChangeRequest cambio = new UserProfileChangeRequest.Builder()
                            .setDisplayName(nombre).build();
                    return u.updateProfile(cambio).onSuccessTask(v -> {
                        Map<String, Object> privado = new HashMap<>();
                        privado.put("nombre", nombre);
                        privado.put("email", email);
                        privado.put("ciudad", ciudad);
                        privado.put("telefono", "");
                        privado.put("direccion", "");
                        privado.put("rol", Usuario.ROL_CLIENTE);
                        privado.put("creado", FieldValue.serverTimestamp());

                        Map<String, Object> publico = new HashMap<>();
                        publico.put("nombre", nombre);
                        publico.put("ciudad", ciudad);

                        WriteBatch b = Fb.db().batch();
                        b.set(Fb.usuarios().document(u.getUid()), privado);
                        b.set(Fb.perfiles().document(u.getUid()), publico);
                        return b.commit();
                    });
                });
    }

    public static Task<Void> iniciarSesion(String email, String clave) {
        return Fb.auth().signInWithEmailAndPassword(email, clave).onSuccessTask(r -> Tasks.forResult(null));
    }

    public static Task<Void> recuperarClave(String email) {
        return Fb.auth().sendPasswordResetEmail(email);
    }

    public static void cerrarSesion() {
        actual = null;
        Fb.auth().signOut();
    }

    /** Lee el perfil del usuario actual y lo deja en memoria. */
    public static Task<Usuario> cargarPerfil() {
        String uid = Fb.uid();
        if (uid == null) {
            return Tasks.forException(new IllegalStateException("No hay sesión iniciada"));
        }
        return Fb.usuarios().document(uid).get().continueWith(t -> {
            Usuario u = t.getResult().toObject(Usuario.class);
            if (u == null) {
                // Perfil incompleto (p. ej. cuenta creada desde la consola): se crea uno básico.
                u = new Usuario();
                u.id = uid;
                u.nombre = nombre();
                u.email = Fb.auth().getCurrentUser().getEmail();
            }
            actual = u;
            return u;
        });
    }

    /** Actualiza los datos básicos del perfil (privados y públicos). */
    public static Task<Void> actualizarPerfil(String nombre, String telefono, String direccion,
                                              String ciudad) {
        String uid = Fb.uid();
        FirebaseUser u = Fb.auth().getCurrentUser();
        if (uid == null || u == null) {
            return Tasks.forException(new IllegalStateException("No hay sesión iniciada"));
        }
        Map<String, Object> privado = new HashMap<>();
        privado.put("nombre", nombre);
        privado.put("telefono", telefono);
        privado.put("direccion", direccion);
        privado.put("ciudad", ciudad);
        privado.put("email", u.getEmail());

        Map<String, Object> publico = new HashMap<>();
        publico.put("nombre", nombre);
        publico.put("ciudad", ciudad);

        WriteBatch b = Fb.db().batch();
        b.set(Fb.usuarios().document(uid), privado, SetOptions.merge());
        b.set(Fb.perfiles().document(uid), publico, SetOptions.merge());
        return b.commit()
                .onSuccessTask(v -> u.updateProfile(new UserProfileChangeRequest.Builder()
                        .setDisplayName(nombre).build()))
                .addOnSuccessListener(v -> {
                    if (actual != null) {
                        actual.nombre = nombre;
                        actual.telefono = telefono;
                        actual.direccion = direccion;
                        actual.ciudad = ciudad;
                    }
                });
    }
}
