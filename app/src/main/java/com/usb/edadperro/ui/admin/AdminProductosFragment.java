package com.usb.edadperro.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.data.repo.TiendaRepo;
import com.usb.edadperro.databinding.FragmentListaBinding;
import com.usb.edadperro.ui.comun.FilaAdapter;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;

import java.util.ArrayList;
import java.util.List;

/**
 * Interfaz de administración del marketplace (Punto 6): lista todos los
 * productos con su inventario y permite agregar, modificar y eliminar.
 * Solo los usuarios con rol "admin" llegan aquí, y las reglas de Firestore
 * bloquean cualquier escritura de otro usuario.
 */
public class AdminProductosFragment extends Fragment {

    private FragmentListaBinding b;
    private ListenerRegistration escucha;
    private final FilaAdapter adapter = new FilaAdapter();
    private int totalProductos = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentListaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvLista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvLista.setAdapter(adapter);
        b.tvEncabezado.setText(R.string.admin_encabezado);
        b.tvEncabezado.setVisibility(View.VISIBLE);
        b.fab.setText(R.string.nuevo_producto);
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> editar(null));

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                menu.add(Menu.NONE, 1, Menu.NONE, R.string.cargar_catalogo_inicial);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == 1) {
                    confirmarCatalogoInicial();
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        escucha = TiendaRepo.todos().addSnapshotListener((snap, e) -> {
            if (b == null) {
                return;
            }
            b.progreso.setVisibility(View.GONE);
            if (e != null) {
                b.tvVacio.setText(Errores.mensaje(e));
                b.tvVacio.setVisibility(View.VISIBLE);
                return;
            }
            List<FilaAdapter.Fila> filas = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Producto p = d.toObject(Producto.class);
                if (p != null) {
                    filas.add(fila(p));
                }
            }
            totalProductos = filas.size();
            adapter.setDatos(filas);
            if (filas.isEmpty()) {
                b.tvVacio.setText(R.string.admin_vacio);
                b.tvVacio.setVisibility(View.VISIBLE);
            } else {
                b.tvVacio.setVisibility(View.GONE);
            }
        });
    }

    private FilaAdapter.Fila fila(Producto p) {
        FilaAdapter.Fila f = new FilaAdapter.Fila();
        f.imagenUrl = p.imagenUrl == null ? "" : p.imagenUrl;
        f.titulo = p.marca + " · " + p.nombre;
        f.subtitulo = p.categoriaNombre + " · " + p.presentacion;
        f.derecha = Formato.pesos(p.precio);
        f.etiqueta = !p.activo ? getString(R.string.inactivo)
                : getString(R.string.stock_n, p.stock);
        boolean bajo = p.stock <= 5;
        f.colorEtiqueta = !p.activo ? R.color.texto_secundario : bajo ? R.color.naranja : R.color.verde;
        f.fondoEtiqueta = !p.activo ? R.color.borde : bajo ? R.color.naranja_suave : R.color.verde_suave;
        f.alTocar = () -> editar(p);
        f.alMantener = () -> confirmarEliminar(p);
        return f;
    }

    private void editar(@Nullable Producto p) {
        Bundle args = new Bundle();
        if (p != null) {
            args.putString(ProductoFormFragment.ARG_ID, p.id);
        }
        NavHostFragment.findNavController(this).navigate(R.id.productoFormFragment, args);
    }

    private void confirmarEliminar(Producto p) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.eliminar_producto_pregunta, p.nombre))
                .setMessage(R.string.accion_irreversible)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> TiendaRepo.eliminar(p)
                        .addOnFailureListener(e -> Snackbar.make(b.getRoot(), Errores.mensaje(e),
                                Snackbar.LENGTH_LONG).show()))
                .show();
    }

    private void confirmarCatalogoInicial() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.cargar_catalogo_inicial)
                .setMessage(totalProductos > 0 ? R.string.catalogo_inicial_reemplaza
                        : R.string.catalogo_inicial_detalle)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.cargar, (d, w) -> TiendaRepo.cargarCatalogoInicial()
                        .addOnSuccessListener(v -> Snackbar.make(b.getRoot(), R.string.catalogo_cargado,
                                Snackbar.LENGTH_LONG).show())
                        .addOnFailureListener(e -> Snackbar.make(b.getRoot(), Errores.mensaje(e),
                                Snackbar.LENGTH_LONG).show()))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (escucha != null) {
            escucha.remove();
        }
        b = null;
    }
}
