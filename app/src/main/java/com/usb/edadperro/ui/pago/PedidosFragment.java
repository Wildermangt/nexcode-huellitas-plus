package com.usb.edadperro.ui.pago;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Pedido;
import com.usb.edadperro.data.repo.PedidoRepo;
import com.usb.edadperro.databinding.FragmentListaBinding;
import com.usb.edadperro.ui.comun.FilaAdapter;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Historial de pedidos del usuario con su estado de pago (Punto 7).
 */
public class PedidosFragment extends Fragment {

    private FragmentListaBinding b;
    private ListenerRegistration escucha;
    private final FilaAdapter adapter = new FilaAdapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentListaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvLista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvLista.setAdapter(adapter);

        escucha = PedidoRepo.misPedidos().addSnapshotListener((snap, e) -> {
            if (b == null) {
                return;
            }
            b.progreso.setVisibility(View.GONE);
            if (e != null) {
                vacio(Errores.mensaje(e));
                return;
            }
            List<Pedido> pedidos = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Pedido p = d.toObject(Pedido.class);
                if (p != null) {
                    pedidos.add(p);
                }
            }
            // Más recientes primero (los recién creados aún sin hora del servidor van arriba).
            Collections.sort(pedidos, (x, y) -> {
                if (x.creado == null) {
                    return -1;
                }
                if (y.creado == null) {
                    return 1;
                }
                return y.creado.compareTo(x.creado);
            });
            List<FilaAdapter.Fila> filas = new ArrayList<>();
            for (Pedido p : pedidos) {
                filas.add(fila(p));
            }
            adapter.setDatos(filas);
            if (filas.isEmpty()) {
                vacio(getString(R.string.sin_pedidos));
            } else {
                b.tvVacio.setVisibility(View.GONE);
            }
        });
    }

    private FilaAdapter.Fila fila(Pedido p) {
        EstadoUi e = EstadoUi.de(p.estado);
        FilaAdapter.Fila f = new FilaAdapter.Fila();
        f.icono = R.drawable.ic_pedidos;
        f.titulo = getString(R.string.pedido_numero, p.id.substring(0, 8).toUpperCase());
        f.subtitulo = Formato.fecha(p.creado) + " · " + getString(R.string.unidades, p.unidades);
        f.derecha = Formato.pesos(p.total);
        f.etiqueta = p.estadoLegible();
        f.colorEtiqueta = e.color;
        f.fondoEtiqueta = e.fondo;
        f.alTocar = () -> {
            Bundle args = new Bundle();
            args.putString(PedidoDetalleFragment.ARG_ID, p.id);
            NavHostFragment.findNavController(this).navigate(R.id.pedidoDetalleFragment, args);
        };
        return f;
    }

    private void vacio(String texto) {
        b.tvVacio.setText(texto);
        b.tvVacio.setVisibility(View.VISIBLE);
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
