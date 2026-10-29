package com.usb.edadperro.ui.tienda;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Producto;
import com.usb.edadperro.data.repo.CarritoRepo;
import com.usb.edadperro.databinding.FragmentProductoDetalleBinding;
import com.usb.edadperro.util.CarritoCalculadora;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Ui;

/**
 * Detalle de un producto y botón para agregarlo al carrito.
 */
public class ProductoDetalleFragment extends Fragment {

    public static final String ARG_ID = "productoId";

    private FragmentProductoDetalleBinding b;
    private ListenerRegistration escucha;
    private Producto producto;
    private int cantidad = 1;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentProductoDetalleBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        String id = requireArguments().getString(ARG_ID);
        escucha = Fb.productos().document(id).addSnapshotListener((d, e) -> {
            if (b == null || d == null) {
                return;
            }
            Producto p = d.toObject(Producto.class);
            if (p == null) {
                Snackbar.make(b.getRoot(), R.string.producto_no_disponible, Snackbar.LENGTH_LONG).show();
                b.btnAgregar.setEnabled(false);
                return;
            }
            producto = p;
            mostrar(p);
        });

        b.btnMenos.setOnClickListener(v -> cambiarCantidad(-1));
        b.btnMas.setOnClickListener(v -> cambiarCantidad(+1));
        b.btnAgregar.setOnClickListener(v -> agregar());
    }

    private void mostrar(Producto p) {
        if (getActivity() instanceof AppCompatActivity
                && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(p.marca);
        }
        Ui.imagenProducto(b.ivProducto, p.imagenUrl);
        b.tvCategoria.setText(p.categoriaNombre);
        b.tvMarca.setText(p.marca);
        b.tvNombre.setText(p.nombre);
        b.tvPresentacion.setText(p.presentacion);
        b.tvPrecio.setText(Formato.pesos(p.precio));
        b.tvDescripcion.setText(p.descripcion);
        ProductosAdapter.mostrarStock(b.tvStock, p.stock);
        cantidad = CarritoCalculadora.limitarCantidad(cantidad, p.stock);
        actualizarCantidad();
    }

    private void cambiarCantidad(int delta) {
        if (producto == null) {
            return;
        }
        cantidad = CarritoCalculadora.limitarCantidad(cantidad + delta, producto.stock);
        actualizarCantidad();
    }

    private void actualizarCantidad() {
        boolean hay = producto != null && producto.disponible();
        b.tvCantidad.setText(String.valueOf(hay ? cantidad : 0));
        b.btnAgregar.setEnabled(hay);
        b.btnAgregar.setText(hay ? R.string.agregar_al_carrito : R.string.agotado);
        b.btnMenos.setEnabled(hay && cantidad > 1);
        b.btnMas.setEnabled(hay && cantidad < Math.min(producto.stock, CarritoCalculadora.MAX_POR_PRODUCTO));
    }

    private void agregar() {
        b.btnAgregar.setEnabled(false);
        CarritoRepo.agregar(producto, cantidad)
                .addOnSuccessListener(total -> {
                    if (b == null) {
                        return;
                    }
                    b.btnAgregar.setEnabled(true);
                    Snackbar.make(b.getRoot(), getString(R.string.agregado_al_carrito, total),
                                    Snackbar.LENGTH_LONG)
                            .setAction(R.string.ver_carrito, v ->
                                    NavHostFragment.findNavController(this).navigate(R.id.carritoFragment))
                            .show();
                })
                .addOnFailureListener(e -> {
                    if (b == null) {
                        return;
                    }
                    b.btnAgregar.setEnabled(true);
                    Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                });
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
