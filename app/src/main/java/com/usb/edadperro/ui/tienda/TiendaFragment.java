package com.usb.edadperro.ui.tienda;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Categoria;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.data.repo.TiendaRepo;
import com.usb.edadperro.databinding.FragmentTiendaBinding;
import com.usb.edadperro.util.Errores;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Tienda de alimentos para perros: catálogo, buscador y filtro por categoría.
 *
 * El catálogo se escucha en tiempo real, así los cambios del administrador y
 * el inventario se reflejan al instante. El buscador y el filtro se aplican en
 * el teléfono, lo que permite usarlos también sin conexión (caché de Firestore).
 */
public class TiendaFragment extends Fragment {

    private static final String TODAS = "todas";

    private FragmentTiendaBinding b;
    private ListenerRegistration escucha;
    private final List<Producto> todos = new ArrayList<>();
    private String categoria = TODAS;
    private final ProductosAdapter adapter = new ProductosAdapter(this::abrir);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentTiendaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvProductos.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        b.rvProductos.setAdapter(adapter);

        // Chips de categorías: "Todas" + las tres categorías del catálogo.
        // Al volver del detalle la vista se recrea: se marca la categoría que seguía activa.
        agregarChip(TODAS, getString(R.string.todas), TODAS.equals(categoria));
        for (Categoria c : TiendaRepo.categoriasBase()) {
            agregarChip(c.id, c.nombre, c.id.equals(categoria));
        }
        b.chipsCategorias.setOnCheckedStateChangeListener((g, ids) -> {
            if (!ids.isEmpty()) {
                Chip chip = g.findViewById(ids.get(0));
                categoria = (String) chip.getTag();
                filtrar();
            }
        });

        b.etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Sin acción
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrar();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Sin acción
            }
        });

        escucha = TiendaRepo.catalogo().addSnapshotListener((snap, e) -> {
            if (b == null) {
                return;
            }
            b.progreso.setVisibility(View.GONE);
            if (e != null) {
                mostrarVacio(Errores.mensaje(e));
                return;
            }
            todos.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Producto p = d.toObject(Producto.class);
                if (p != null) {
                    todos.add(p);
                }
            }
            Collections.sort(todos, (x, y) -> x.nombre.compareToIgnoreCase(y.nombre));
            filtrar();
        });
    }

    private void agregarChip(String id, String texto, boolean marcado) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filtro, b.chipsCategorias, false);
        chip.setId(View.generateViewId());
        chip.setTag(id);
        chip.setText(texto);
        b.chipsCategorias.addView(chip);
        chip.setChecked(marcado);
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /** Aplica el texto de búsqueda y la categoría seleccionada. */
    private void filtrar() {
        String q = normalizar(b.etBuscar.getText() == null ? "" : b.etBuscar.getText().toString().trim());
        List<Producto> visibles = new ArrayList<>();
        for (Producto p : todos) {
            boolean enCategoria = TODAS.equals(categoria) || categoria.equals(p.categoriaId);
            boolean coincide = q.isEmpty()
                    || normalizar(p.nombre + " " + p.marca + " " + p.descripcion + " " + p.presentacion)
                    .contains(q);
            if (enCategoria && coincide) {
                visibles.add(p);
            }
        }
        adapter.setDatos(visibles);
        if (todos.isEmpty()) {
            mostrarVacio(getString(R.string.catalogo_vacio));
        } else if (visibles.isEmpty()) {
            mostrarVacio(getString(R.string.sin_resultados));
        } else {
            b.tvVacio.setVisibility(View.GONE);
        }
    }

    private void mostrarVacio(String texto) {
        b.tvVacio.setText(texto);
        b.tvVacio.setVisibility(View.VISIBLE);
    }

    private void abrir(Producto p) {
        Bundle args = new Bundle();
        args.putString(ProductoDetalleFragment.ARG_ID, p.id);
        NavHostFragment.findNavController(this).navigate(R.id.productoDetalleFragment, args);
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
