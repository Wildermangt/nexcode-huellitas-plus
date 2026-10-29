package com.usb.edadperro.ui.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Categoria;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.data.repo.TiendaRepo;
import com.usb.edadperro.databinding.FragmentProductoFormBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Ui;
import com.usb.edadperro.util.ValidadorCatalogo;

import java.util.List;

/**
 * Formulario de administración para agregar, modificar y eliminar productos.
 * Aplica la restricción del marketplace: solo alimentos para perros.
 */
public class ProductoFormFragment extends Fragment {

    public static final String ARG_ID = "productoId";

    private FragmentProductoFormBinding b;
    private Producto producto = new Producto();
    private long stockAnterior = 0;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentProductoFormBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        for (Categoria c : TiendaRepo.categoriasBase()) {
            Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filtro, b.chipsCategoria, false);
            chip.setId(View.generateViewId());
            chip.setTag(c.id);
            chip.setText(c.nombre);
            b.chipsCategoria.addView(chip);
        }
        ((Chip) b.chipsCategoria.getChildAt(0)).setChecked(true);

        b.etImagen.setOnFocusChangeListener((v, foco) -> {
            if (!foco) {
                Ui.imagenProducto(b.ivVista, Ui.texto(b.etImagen));
            }
        });
        b.btnGuardar.setOnClickListener(v -> guardar());
        b.btnEliminar.setOnClickListener(v -> eliminar());

        String id = getArguments() == null ? null : getArguments().getString(ARG_ID);
        if (id != null) {
            if (getActivity() instanceof AppCompatActivity
                    && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
                ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(R.string.editar_producto);
            }
            b.btnEliminar.setVisibility(View.VISIBLE);
            Fb.productos().document(id).get().addOnSuccessListener(d -> {
                Producto p = d.toObject(Producto.class);
                if (p != null && b != null) {
                    producto = p;
                    stockAnterior = p.stock;
                    mostrar(p);
                }
            });
        }
    }

    private void mostrar(Producto p) {
        b.etImagen.setText(p.imagenUrl);
        Ui.imagenProducto(b.ivVista, p.imagenUrl);
        b.etNombre.setText(p.nombre);
        b.etMarca.setText(p.marca);
        b.etPresentacion.setText(p.presentacion);
        b.etPrecio.setText(String.valueOf(p.precio));
        b.etStock.setText(String.valueOf(p.stock));
        b.etDescripcion.setText(p.descripcion);
        b.swActivo.setChecked(p.activo);
        for (int i = 0; i < b.chipsCategoria.getChildCount(); i++) {
            Chip c = (Chip) b.chipsCategoria.getChildAt(i);
            c.setChecked(c.getTag().equals(p.categoriaId));
        }
    }

    private static long numero(String s) {
        try {
            return Long.parseLong(s.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void guardar() {
        Chip cat = b.chipsCategoria.findViewById(b.chipsCategoria.getCheckedChipId());
        producto.nombre = Ui.texto(b.etNombre);
        producto.marca = Ui.texto(b.etMarca);
        producto.presentacion = Ui.texto(b.etPresentacion);
        producto.descripcion = Ui.texto(b.etDescripcion);
        producto.imagenUrl = Ui.texto(b.etImagen);
        producto.precio = numero(Ui.texto(b.etPrecio));
        producto.stock = numero(Ui.texto(b.etStock));
        producto.categoriaId = cat == null ? null : (String) cat.getTag();
        producto.especie = ValidadorCatalogo.ESPECIE;
        producto.activo = b.swActivo.isChecked();

        List<String> errores = ValidadorCatalogo.validar(producto.nombre, producto.marca,
                producto.descripcion, producto.presentacion, producto.categoriaId,
                producto.especie, producto.precio, producto.stock);
        if (!errores.isEmpty()) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.producto_no_valido)
                    .setMessage("• " + android.text.TextUtils.join("\n• ", errores))
                    .setPositiveButton(R.string.entendido, null)
                    .show();
            return;
        }
        b.btnGuardar.setEnabled(false);
        TiendaRepo.guardar(producto, stockAnterior)
                .addOnSuccessListener(v -> {
                    if (b == null) {
                        return;
                    }
                    Toast.makeText(requireContext(), R.string.producto_guardado, Toast.LENGTH_SHORT).show();
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .addOnFailureListener(e -> {
                    if (b != null) {
                        b.btnGuardar.setEnabled(true);
                        Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void eliminar() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.eliminar_producto_pregunta, producto.nombre))
                .setMessage(R.string.accion_irreversible)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> TiendaRepo.eliminar(producto)
                        .addOnSuccessListener(v -> {
                            if (b != null) {
                                NavHostFragment.findNavController(this).popBackStack();
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (b != null) {
                                Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                            }
                        }))
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
