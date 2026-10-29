package com.usb.edadperro.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Usuario;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.FragmentEditarPerfilBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Ui;

/**
 * Edición de los datos básicos del perfil. El correo no se edita aquí
 * porque es la credencial de inicio de sesión.
 */
public class EditarPerfilFragment extends Fragment {

    private FragmentEditarPerfilBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentEditarPerfilBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.limpiarErrorAlEscribir(b.tilNombre);
        Ui.limpiarErrorAlEscribir(b.tilTelefono);
        SesionRepo.cargarPerfil().addOnSuccessListener(this::mostrar);
        b.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void mostrar(Usuario u) {
        if (b == null) {
            return;
        }
        b.etNombre.setText(u.nombre);
        b.etCorreo.setText(u.email);
        b.etTelefono.setText(u.telefono);
        b.etCiudad.setText(u.ciudad);
        b.etDireccion.setText(u.direccion);
    }

    private void guardar() {
        String nombre = Ui.texto(b.etNombre);
        String telefono = Ui.texto(b.etTelefono);
        if (nombre.length() < 3) {
            b.tilNombre.setError(getString(R.string.error_nombre));
            return;
        }
        if (!telefono.isEmpty() && telefono.replaceAll("[^0-9]", "").length() < 7) {
            b.tilTelefono.setError(getString(R.string.error_telefono));
            return;
        }
        b.btnGuardar.setEnabled(false);
        SesionRepo.actualizarPerfil(nombre, telefono, Ui.texto(b.etDireccion), Ui.texto(b.etCiudad))
                .addOnSuccessListener(v -> {
                    if (b == null) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.perfil_actualizado, Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (b != null) {
                        b.btnGuardar.setEnabled(true);
                        Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
