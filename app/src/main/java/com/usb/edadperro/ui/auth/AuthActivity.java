package com.usb.edadperro.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.ActivityAuthBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Red;
import com.usb.edadperro.util.Ui;

/**
 * Pantalla de bienvenida con el formulario de inicio de sesión (Punto 3).
 */
public class AuthActivity extends AppCompatActivity {

    private ActivityAuthBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        Ui.limpiarErrorAlEscribir(b.tilCorreo);
        Ui.limpiarErrorAlEscribir(b.tilClave);

        b.btnEntrar.setOnClickListener(v -> entrar());
        b.etClave.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                entrar();
                return true;
            }
            return false;
        });
        b.btnRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
        b.btnOlvide.setOnClickListener(v -> recuperarClave());
    }

    private void entrar() {
        String correo = Ui.texto(b.etCorreo);
        String clave = Ui.texto(b.etClave);
        boolean ok = true;
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            b.tilCorreo.setError(getString(R.string.error_correo));
            ok = false;
        }
        if (clave.isEmpty()) {
            b.tilClave.setError(getString(R.string.error_clave_vacia));
            ok = false;
        }
        if (!ok) {
            return;
        }
        if (!Red.hayConexion(this)) {
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
            return;
        }
        Ui.ocultarTeclado(this);
        cargando(true);
        SesionRepo.iniciarSesion(correo, clave)
                .addOnSuccessListener(r -> {
                    startActivity(new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .addOnFailureListener(e -> {
                    cargando(false);
                    Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                });
    }

    private void recuperarClave() {
        String correo = Ui.texto(b.etCorreo);
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            b.tilCorreo.setError(getString(R.string.error_correo_para_recuperar));
            return;
        }
        SesionRepo.recuperarClave(correo)
                .addOnSuccessListener(r -> Snackbar.make(b.getRoot(),
                        R.string.correo_recuperacion_enviado, Snackbar.LENGTH_LONG).show())
                .addOnFailureListener(e -> Snackbar.make(b.getRoot(),
                        Errores.mensaje(e), Snackbar.LENGTH_LONG).show());
    }

    private void cargando(boolean si) {
        b.progreso.setVisibility(si ? View.VISIBLE : View.INVISIBLE);
        b.btnEntrar.setEnabled(!si);
        b.btnRegistro.setEnabled(!si);
    }
}
