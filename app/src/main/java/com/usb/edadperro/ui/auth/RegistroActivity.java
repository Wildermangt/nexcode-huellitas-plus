package com.usb.edadperro.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.ActivityRegistroBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Red;
import com.usb.edadperro.util.Ui;

/**
 * Registro de usuarios con nombre, correo y contraseña (Punto 3).
 */
public class RegistroActivity extends AppCompatActivity {

    private static final int MIN_CLAVE = 6;

    private ActivityRegistroBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegistroBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());

        Ui.limpiarErrorAlEscribir(b.tilNombre);
        Ui.limpiarErrorAlEscribir(b.tilCorreo);
        Ui.limpiarErrorAlEscribir(b.tilCiudad);
        Ui.limpiarErrorAlEscribir(b.tilClave);
        Ui.limpiarErrorAlEscribir(b.tilClave2);

        b.btnCrear.setOnClickListener(v -> registrar());
    }

    private void registrar() {
        String nombre = Ui.texto(b.etNombre);
        String correo = Ui.texto(b.etCorreo);
        String ciudad = Ui.texto(b.etCiudad);
        String clave = b.etClave.getText() == null ? "" : b.etClave.getText().toString();
        String clave2 = b.etClave2.getText() == null ? "" : b.etClave2.getText().toString();

        boolean ok = true;
        if (nombre.length() < 3) {
            b.tilNombre.setError(getString(R.string.error_nombre));
            ok = false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(correo).matches()) {
            b.tilCorreo.setError(getString(R.string.error_correo));
            ok = false;
        }
        if (ciudad.length() < 3) {
            b.tilCiudad.setError(getString(R.string.error_ciudad));
            ok = false;
        }
        if (clave.length() < MIN_CLAVE) {
            b.tilClave.setError(getString(R.string.error_clave_corta));
            ok = false;
        }
        if (!clave.equals(clave2)) {
            b.tilClave2.setError(getString(R.string.error_claves_distintas));
            ok = false;
        }
        if (!b.cbDatos.isChecked()) {
            Snackbar.make(b.getRoot(), R.string.error_acepto_datos, Snackbar.LENGTH_LONG).show();
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
        SesionRepo.registrar(nombre, correo, clave, ciudad)
                .addOnSuccessListener(v -> {
                    startActivity(new Intent(this, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    finish();
                })
                .addOnFailureListener(e -> {
                    cargando(false);
                    Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                });
    }

    private void cargando(boolean si) {
        b.progreso.setVisibility(si ? View.VISIBLE : View.INVISIBLE);
        b.btnCrear.setEnabled(!si);
    }
}
