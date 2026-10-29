package com.usb.edadperro.ui.pago;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.DetallePedido;
import com.usb.edadperro.data.modelo.Pago;
import com.usb.edadperro.data.modelo.Pedido;
import com.usb.edadperro.data.remoto.PagosServicio;
import com.usb.edadperro.data.repo.CarritoRepo;
import com.usb.edadperro.data.repo.PedidoRepo;
import com.usb.edadperro.databinding.FilaValorBinding;
import com.usb.edadperro.databinding.FragmentPedidoDetalleBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Hilos;
import com.usb.edadperro.util.Red;

import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de la compra, estado del pago y comprobante de la transacción.
 *
 * El estado se escucha en tiempo real: cuando el webhook de Wompi actualiza el
 * pedido en Firestore (aprobado, rechazado o pendiente), la pantalla cambia sola.
 */
public class PedidoDetalleFragment extends Fragment {

    public static final String ARG_ID = "pedidoId";
    public static final String ARG_NUEVO = "nuevo";
    /** El usuario salió del checkout: se busca en Wompi si dejó un pago iniciado. */
    public static final String ARG_VERIFICAR = "verificar";
    /** Id de transacción que devuelve Wompi al terminar el checkout. */
    public static final String ARG_TRANSACCION = "transaccionId";

    private FragmentPedidoDetalleBinding b;
    private ListenerRegistration escuchaPedido;
    private ListenerRegistration escuchaPagos;
    private String pedidoId;
    private Pedido pedido;
    private Pago ultimoPago;
    private final List<DetallePedido> detalles = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentPedidoDetalleBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        pedidoId = requireArguments().getString(ARG_ID);
        if (requireArguments().getBoolean(ARG_NUEVO)) {
            Snackbar.make(b.getRoot(), R.string.pedido_creado, Snackbar.LENGTH_LONG).show();
        }

        b.btnPagar.setOnClickListener(v -> pagar());
        b.btnVerificar.setOnClickListener(v ->
                verificar(ultimoPago == null ? null : ultimoPago.transaccionId, true));
        b.btnRecomprar.setOnClickListener(v -> volverAComprar());
        b.btnPdf.setOnClickListener(v -> {
            if (pedido != null) {
                ComprobantePdf.compartir(requireContext(), pedido, detalles, ultimoPago);
            }
        });

        escuchaPedido = PedidoRepo.pedido(pedidoId).addSnapshotListener((d, e) -> {
            if (b == null || d == null) {
                return;
            }
            pedido = d.toObject(Pedido.class);
            if (pedido != null) {
                mostrarPedido();
            }
        });
        escuchaPagos = PedidoRepo.pagos(pedidoId).addSnapshotListener((snap, e) -> {
            if (b == null || snap == null) {
                return;
            }
            ultimoPago = null;
            for (DocumentSnapshot d : snap.getDocuments()) {
                Pago p = d.toObject(Pago.class);
                if (p != null && (ultimoPago == null || masReciente(p, ultimoPago))) {
                    ultimoPago = p;
                }
            }
            mostrarPago();
        });
        PedidoRepo.detalles(pedidoId).get().addOnSuccessListener(snap -> {
            if (b == null) {
                return;
            }
            detalles.clear();
            detalles.addAll(PedidoRepo.aDetalles(snap.getDocuments()));
            mostrarDetalles();
        });

        // Si venimos del checkout de Wompi, se consulta de inmediato el resultado.
        String transaccion = requireArguments().getString(ARG_TRANSACCION);
        if (state == null && (transaccion != null || requireArguments().getBoolean(ARG_VERIFICAR))) {
            verificar(transaccion, false);
        }
    }

    private static boolean masReciente(Pago a, Pago b) {
        if (a.creado == null) {
            return true; // escritura local aún sin hora del servidor
        }
        return b.creado != null && a.creado.after(b.creado);
    }

    private void mostrarPedido() {
        EstadoUi e = EstadoUi.de(pedido.estado);
        b.cardEstado.setCardBackgroundColor(ContextCompat.getColor(requireContext(), e.fondo));
        b.ivEstado.setImageResource(e.icono);
        b.ivEstado.setColorFilter(ContextCompat.getColor(requireContext(), e.color));
        b.tvEstado.setText(e.titulo);
        b.tvEstado.setTextColor(ContextCompat.getColor(requireContext(), e.color));
        b.tvEstadoDetalle.setText(e.detalle);

        boolean pendientePago = pedido.puedePagarse();
        b.btnPagar.setVisibility(pendientePago ? View.VISIBLE : View.GONE);
        b.tvSandbox.setVisibility(pendientePago ? View.VISIBLE : View.GONE);
        b.btnVerificar.setVisibility(Pedido.PAGO_EN_PROCESO.equals(pedido.estado) ? View.VISIBLE : View.GONE);
        b.btnRecomprar.setVisibility(Pedido.PAGO_RECHAZADO.equals(pedido.estado) ? View.VISIBLE : View.GONE);

        b.datosPedido.removeAllViews();
        fila(b.datosPedido, R.string.numero_pedido, pedido.id.substring(0, 8).toUpperCase());
        fila(b.datosPedido, R.string.fecha, Formato.fechaHora(pedido.creado));
        fila(b.datosPedido, R.string.cliente, pedido.usuarioNombre);
        fila(b.datosPedido, R.string.entrega, pedido.direccion + ", " + pedido.ciudad);
        fila(b.datosPedido, R.string.telefono, pedido.telefono);
        fila(b.datosPedido, R.string.estado, pedido.estadoLegible());

        b.totales.removeAllViews();
        fila(b.totales, R.string.subtotal, Formato.pesos(pedido.subtotal));
        fila(b.totales, R.string.envio, pedido.envio == 0 ? getString(R.string.gratis) : Formato.pesos(pedido.envio));
        FilaValorBinding total = fila(b.totales, R.string.total, Formato.pesos(pedido.total));
        total.valor.setTextSize(17);
    }

    private void mostrarDetalles() {
        b.listaDetalles.removeAllViews();
        for (DetallePedido d : detalles) {
            FilaValorBinding f = FilaValorBinding.inflate(getLayoutInflater(), b.listaDetalles, true);
            f.etiqueta.setText(d.cantidad + " x " + Formato.producto(d.marca, d.nombre) + " (" + d.presentacion + ")");
            f.valor.setText(Formato.pesos(d.subtotal));
        }
    }

    private void mostrarPago() {
        b.datosPago.removeAllViews();
        if (ultimoPago == null) {
            fila(b.datosPago, R.string.proveedor, getString(R.string.sin_intentos_pago));
            return;
        }
        fila(b.datosPago, R.string.proveedor, "Wompi (sandbox)");
        fila(b.datosPago, R.string.referencia, ultimoPago.referencia);
        if (ultimoPago.transaccionId != null) {
            fila(b.datosPago, R.string.id_transaccion, ultimoPago.transaccionId);
        }
        fila(b.datosPago, R.string.estado_pago, ultimoPago.estado);
        if (ultimoPago.metodo != null) {
            fila(b.datosPago, R.string.metodo, ultimoPago.metodo);
        }
        fila(b.datosPago, R.string.monto, Formato.pesos(ultimoPago.montoPesos()));
        fila(b.datosPago, R.string.fecha, Formato.fechaHora(ultimoPago.actualizado));
    }

    private FilaValorBinding fila(LinearLayout contenedor, int etiqueta, String valor) {
        FilaValorBinding f = FilaValorBinding.inflate(getLayoutInflater(), contenedor, true);
        f.etiqueta.setText(etiqueta);
        f.valor.setText(valor == null ? "-" : valor);
        return f;
    }

    private void pagar() {
        if (!Red.hayConexion(requireContext())) {
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
            return;
        }
        if (!PagosServicio.configurado()) {
            Snackbar.make(b.getRoot(), R.string.pagos_no_configurados, Snackbar.LENGTH_LONG).show();
            return;
        }
        Bundle args = new Bundle();
        args.putString(PagoWompiFragment.ARG_PEDIDO, pedidoId);
        NavHostFragment.findNavController(this).navigate(R.id.pagoWompiFragment, args);
    }

    /**
     * Pide al backend que consulte en Wompi el estado real de la transacción.
     * Sin transaccionId, el backend la busca por la referencia del último intento de pago.
     *
     * @param manual true si el usuario pulsó "Verificar" (se le avisa si no hay pago iniciado)
     */
    private void verificar(@Nullable String transaccionId, boolean manual) {
        FirebaseUser u = Fb.auth().getCurrentUser();
        if (u == null) {
            return;
        }
        b.progreso.setVisibility(View.VISIBLE);
        u.getIdToken(false).addOnSuccessListener(token -> Hilos.fondo(() -> {
            try {
                PagosServicio.Verificacion v = PagosServicio.verificar(pedidoId, transaccionId, token.getToken());
                Hilos.principal(() -> {
                    if (b != null) {
                        b.progreso.setVisibility(View.GONE);
                        if (PagosServicio.SIN_TRANSACCION.equals(v.estado)) {
                            if (manual) {
                                Snackbar.make(b.getRoot(), R.string.sin_transaccion, Snackbar.LENGTH_LONG).show();
                            }
                        } else if (v.mensaje != null && !v.mensaje.isEmpty()) {
                            Snackbar.make(b.getRoot(), v.mensaje, Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
            } catch (Exception e) {
                Hilos.principal(() -> {
                    if (b != null) {
                        b.progreso.setVisibility(View.GONE);
                        Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        })).addOnFailureListener(e -> {
            if (b != null) {
                b.progreso.setVisibility(View.GONE);
                Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    /** Tras un pago rechazado, vuelve a poner los productos en el carrito. */
    private void volverAComprar() {
        b.btnRecomprar.setEnabled(false);
        CarritoRepo.agregarVarios(detalles).addOnCompleteListener(t -> {
            if (b != null) {
                NavHostFragment.findNavController(this).navigate(R.id.carritoFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (escuchaPedido != null) {
            escuchaPedido.remove();
        }
        if (escuchaPagos != null) {
            escuchaPagos.remove();
        }
        b = null;
    }
}
