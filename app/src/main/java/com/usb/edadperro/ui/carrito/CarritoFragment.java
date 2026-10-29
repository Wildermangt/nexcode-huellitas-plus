package com.usb.edadperro.ui.carrito;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.ItemCarrito;
import com.usb.edadperro.data.repo.CarritoRepo;
import com.usb.edadperro.databinding.FilaValorBinding;
import com.usb.edadperro.databinding.FragmentCarritoBinding;
import com.usb.edadperro.util.CarritoCalculadora;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Red;

import java.util.ArrayList;
import java.util.List;

/**
 * Carrito: cantidades, eliminación, subtotales, envío y total.
 * Antes de continuar al checkout se revalidan precios y existencias.
 */
public class CarritoFragment extends Fragment implements CarritoAdapter.Acciones {

    private FragmentCarritoBinding b;
    private ListenerRegistration escucha;
    private final CarritoAdapter adapter = new CarritoAdapter(this);
    private final List<ItemCarrito> items = new ArrayList<>();
    private boolean revalidado = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentCarritoBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvItems.setAdapter(adapter);
        b.filaSubtotal.etiqueta.setText(R.string.subtotal);
        b.filaEnvio.etiqueta.setText(R.string.envio);
        b.filaTotal.etiqueta.setText(R.string.total);
        b.filaTotal.etiqueta.setTextSize(17);
        b.filaTotal.valor.setTextSize(19);

        b.btnIrTienda.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.tiendaFragment));
        b.btnContinuar.setOnClickListener(v -> continuar());

        escucha = CarritoRepo.items().addSnapshotListener((snap, e) -> {
            if (b == null) {
                return;
            }
            if (e != null) {
                Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                return;
            }
            items.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                ItemCarrito it = d.toObject(ItemCarrito.class);
                if (it != null) {
                    items.add(it);
                }
            }
            adapter.setDatos(items);
            mostrarResumen();
            // Al abrir el carrito se comprueba una vez que los precios sigan vigentes.
            if (!revalidado && !items.isEmpty() && Red.hayConexion(requireContext())) {
                revalidado = true;
                revalidar(false);
            }
        });
    }

    private void mostrarResumen() {
        boolean vacio = items.isEmpty();
        b.vacio.setVisibility(vacio ? View.VISIBLE : View.GONE);
        b.resumen.setVisibility(vacio ? View.GONE : View.VISIBLE);
        CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(items);
        valor(b.filaSubtotal, Formato.pesos(r.subtotal) + "  (" + r.unidades + " u.)");
        valor(b.filaEnvio, r.envio == 0 ? getString(R.string.gratis) : Formato.pesos(r.envio));
        valor(b.filaTotal, Formato.pesos(r.total));
        if (r.faltaParaEnvioGratis() > 0) {
            b.tvEnvioGratis.setText(getString(R.string.falta_envio_gratis, Formato.pesos(r.faltaParaEnvioGratis())));
            b.tvEnvioGratis.setVisibility(View.VISIBLE);
        } else {
            b.tvEnvioGratis.setVisibility(View.GONE);
        }
    }

    private static void valor(FilaValorBinding fila, String texto) {
        fila.valor.setText(texto);
    }

    @Override
    public void cambiarCantidad(ItemCarrito item, int nueva) {
        if (nueva < 1) {
            eliminar(item);
            return;
        }
        CarritoRepo.cambiarCantidad(item, nueva);
    }

    @Override
    public void eliminar(ItemCarrito item) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.quitar_del_carrito)
                .setMessage(Formato.producto(item.marca, item.nombre))
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.quitar, (d, w) -> CarritoRepo.eliminar(item))
                .show();
    }

    /** Revalida precios y existencias; si todo sigue igual y se pidió, abre el checkout. */
    private void revalidar(boolean luegoContinuar) {
        b.btnContinuar.setEnabled(false);
        CarritoRepo.revalidar(new ArrayList<>(items))
                .addOnSuccessListener(cambios -> {
                    if (b == null) {
                        return;
                    }
                    b.btnContinuar.setEnabled(true);
                    if (!cambios.isEmpty()) {
                        new MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.carrito_actualizado)
                                .setMessage(android.text.TextUtils.join("\n", cambios))
                                .setPositiveButton(R.string.entendido, null)
                                .show();
                    } else if (luegoContinuar) {
                        NavHostFragment.findNavController(this).navigate(R.id.checkoutFragment);
                    }
                })
                .addOnFailureListener(e -> {
                    if (b == null) {
                        return;
                    }
                    b.btnContinuar.setEnabled(true);
                    Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                });
    }

    private void continuar() {
        if (!Red.hayConexion(requireContext())) {
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
            return;
        }
        revalidar(true);
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
