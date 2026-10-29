package com.usb.edadperro.ui.perfil;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Usuario;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.FragmentPerfilBinding;
import com.usb.edadperro.databinding.ItemFilaBinding;

/**
 * Perfil: datos básicos, accesos a perros, pedidos y mensajes, panel de
 * administración (solo rol admin) y cierre de sesión.
 */
public class PerfilFragment extends Fragment {

    private FragmentPerfilBinding b;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentPerfilBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.btnEditar.setOnClickListener(v -> ir(R.id.editarPerfilFragment));
        b.btnSalir.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cerrar_sesion_pregunta)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.cerrar_sesion, (d, w) ->
                        ((MainActivity) requireActivity()).cerrarSesion())
                .show());

        if (SesionRepo.actual() != null) {
            mostrar(SesionRepo.actual());
        }
        SesionRepo.cargarPerfil().addOnSuccessListener(u -> {
            if (b != null) {
                mostrar(u);
            }
        });
    }

    private void mostrar(Usuario u) {
        String nombre = u.nombre == null ? SesionRepo.nombre() : u.nombre;
        b.tvInicial.setText(nombre.isEmpty() ? "?" : nombre.substring(0, 1).toUpperCase());
        b.tvNombre.setText(nombre);
        b.tvCorreo.setText(u.email);
        StringBuilder datos = new StringBuilder();
        if (u.ciudad != null && !u.ciudad.isEmpty()) {
            datos.append(u.ciudad);
        }
        if (u.telefono != null && !u.telefono.isEmpty()) {
            datos.append(datos.length() > 0 ? " · " : "").append(u.telefono);
        }
        b.tvDatos.setText(datos);
        b.tvDatos.setVisibility(datos.length() == 0 ? View.GONE : View.VISIBLE);
        b.tvRol.setVisibility(u.esAdmin() ? View.VISIBLE : View.GONE);

        b.opciones.removeAllViews();
        opcion(R.drawable.ic_huella, R.string.mis_perros, R.string.mis_perros_detalle, R.id.inicioFragment);
        opcion(R.drawable.ic_pedidos, R.string.mis_pedidos, R.string.mis_pedidos_detalle, R.id.pedidosFragment);
        opcion(R.drawable.ic_chats, R.string.mensajes, R.string.mensajes_detalle, R.id.chatsFragment);
        if (u.esAdmin()) {
            opcion(R.drawable.ic_admin, R.string.administrar_productos,
                    R.string.administrar_productos_detalle, R.id.adminProductosFragment);
        }
    }

    private void opcion(int icono, int titulo, int detalle, int destino) {
        ItemFilaBinding f = ItemFilaBinding.inflate(getLayoutInflater(), b.opciones, true);
        f.ivIcono.setImageResource(icono);
        f.tvTitulo.setText(titulo);
        f.tvSubtitulo.setText(detalle);
        f.tvDerecha.setVisibility(View.GONE);
        f.getRoot().setOnClickListener(v -> ir(destino));
    }

    private void ir(int destino) {
        NavHostFragment.findNavController(this).navigate(destino);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
