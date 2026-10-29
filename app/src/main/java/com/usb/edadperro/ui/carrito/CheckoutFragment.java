package com.usb.edadperro.ui.carrito;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.ItemCarrito;
import com.usb.edadperro.data.modelo.Usuario;
import com.usb.edadperro.data.repo.CarritoRepo;
import com.usb.edadperro.data.repo.PedidoRepo;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.FragmentCheckoutBinding;
import com.usb.edadperro.ui.pago.PedidoDetalleFragment;
import com.usb.edadperro.util.CarritoCalculadora;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Red;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.List;

/**
 * Checkout: dirección de entrega, resumen y confirmación del pedido.
 * Al confirmar se crea el pedido (con precios y existencias validados en una
 * transacción) y se pasa a la pantalla de pago.
 */
public class CheckoutFragment extends Fragment {

    private FragmentCheckoutBinding b;
    private final CarritoAdapter adapter = new CarritoAdapter(null);
    private final List<ItemCarrito> items = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentCheckoutBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvItems.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvItems.setAdapter(adapter);
        b.filaSubtotal.etiqueta.setText(R.string.subtotal);
        b.filaEnvio.etiqueta.setText(R.string.envio);
        b.filaTotal.etiqueta.setText(R.string.total);
        b.filaTotal.valor.setTextSize(18);
        Ui.limpiarErrorAlEscribir(b.tilDireccion);
        Ui.limpiarErrorAlEscribir(b.tilCiudad);
        Ui.limpiarErrorAlEscribir(b.tilTelefono);

        // Datos de entrega guardados en el perfil.
        Usuario u = SesionRepo.actual();
        if (u != null) {
            b.etDireccion.setText(u.direccion);
            b.etCiudad.setText(u.ciudad);
            b.etTelefono.setText(u.telefono);
        }

        CarritoRepo.items().get().addOnSuccessListener(snap -> {
            if (b == null) {
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
            CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(items);
            b.filaSubtotal.valor.setText(Formato.pesos(r.subtotal));
            b.filaEnvio.valor.setText(r.envio == 0 ? getString(R.string.gratis) : Formato.pesos(r.envio));
            b.filaTotal.valor.setText(Formato.pesos(r.total));
            b.btnConfirmar.setEnabled(!items.isEmpty());
        });

        b.btnConfirmar.setOnClickListener(v -> validarYConfirmar());
    }

    private void validarYConfirmar() {
        String direccion = Ui.texto(b.etDireccion);
        String ciudad = Ui.texto(b.etCiudad);
        String telefono = Ui.texto(b.etTelefono);
        boolean ok = true;
        if (direccion.length() < 6) {
            b.tilDireccion.setError(getString(R.string.error_direccion));
            ok = false;
        }
        if (ciudad.length() < 3) {
            b.tilCiudad.setError(getString(R.string.error_ciudad));
            ok = false;
        }
        if (telefono.replaceAll("[^0-9]", "").length() < 7) {
            b.tilTelefono.setError(getString(R.string.error_telefono));
            ok = false;
        }
        if (!ok) {
            return;
        }
        CarritoCalculadora.Resumen r = CarritoCalculadora.calcular(items);
        // Confirmación explícita antes de iniciar el pago.
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.confirmar_pedido_pregunta)
                .setMessage(getString(R.string.confirmar_pedido_detalle, r.unidades,
                        Formato.pesos(r.total), direccion + ", " + ciudad))
                .setNegativeButton(R.string.revisar, null)
                .setPositiveButton(R.string.confirmar, (d, w) -> confirmar(direccion, ciudad, telefono))
                .show();
    }

    private void confirmar(String direccion, String ciudad, String telefono) {
        if (!Red.hayConexion(requireContext())) {
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
            return;
        }
        PedidoRepo.Entrega e = new PedidoRepo.Entrega();
        e.direccion = direccion;
        e.ciudad = ciudad;
        e.telefono = telefono;
        e.notas = Ui.texto(b.etNotas);

        if (b.cbGuardarDireccion.isChecked()) {
            SesionRepo.actualizarPerfil(SesionRepo.nombre(), telefono, direccion, ciudad);
        }

        cargando(true);
        PedidoRepo.confirmar(items, e)
                .addOnSuccessListener(pedidoId -> {
                    if (b == null) {
                        return;
                    }
                    Bundle args = new Bundle();
                    args.putString(PedidoDetalleFragment.ARG_ID, pedidoId);
                    args.putBoolean(PedidoDetalleFragment.ARG_NUEVO, true);
                    NavOptions opciones = new NavOptions.Builder()
                            .setPopUpTo(R.id.carritoFragment, true).build();
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.pedidoDetalleFragment, args, opciones);
                })
                .addOnFailureListener(err -> {
                    if (b == null) {
                        return;
                    }
                    cargando(false);
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.no_se_confirmo)
                            .setMessage(Errores.mensaje(err))
                            .setPositiveButton(R.string.volver_al_carrito, (d, w) ->
                                    NavHostFragment.findNavController(this).popBackStack())
                            .show();
                });
    }

    private void cargando(boolean si) {
        b.progreso.setVisibility(si ? View.VISIBLE : View.INVISIBLE);
        b.btnConfirmar.setEnabled(!si);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
