package com.usb.edadperro.ui.inicio;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Cuidado;
import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.data.repo.CuidadoRepo;
import com.usb.edadperro.data.repo.PerroRepo;
import com.usb.edadperro.data.repo.SesionRepo;
import com.usb.edadperro.databinding.FragmentInicioBinding;
import com.usb.edadperro.databinding.ItemAccesoBinding;
import com.usb.edadperro.ui.perros.PerroFormFragment;
import com.usb.edadperro.ui.perros.PerrosAdapter;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Periodicidad;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Pantalla de inicio: saludo, acceso a urgencias, accesos rápidos a cada
 * módulo y lista de perros registrados.
 */
public class InicioFragment extends Fragment {

    private FragmentInicioBinding b;
    private ListenerRegistration escuchaPerros;
    private ListenerRegistration escuchaCuidados;
    private final PerrosAdapter adapter = new PerrosAdapter(this::abrirPerro);

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentInicioBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.tvSaludo.setText(getString(R.string.saludo, primerNombre(SesionRepo.nombre())));
        SesionRepo.cargarPerfil().addOnSuccessListener(u -> {
            if (b != null) {
                b.tvSaludo.setText(getString(R.string.saludo, primerNombre(u.nombre)));
            }
        });

        b.cardUrgencias.setOnClickListener(v -> ((MainActivity) requireActivity()).abrirUrgencias());
        b.btnAgregarPerro.setOnClickListener(v -> abrirPerro(null));
        b.cardCuidados.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.cuidadosFragment));
        escuchaCuidados = CuidadoRepo.misCuidados().addSnapshotListener((snap, e) -> {
            if (b != null && snap != null) {
                mostrarProximoCuidado(snap.toObjects(Cuidado.class));
            }
        });

        // Los accesos alternan los tres colores de las almohadillas del logo.
        agregarAcceso(R.drawable.ic_calculadora, R.string.acceso_calculadora, R.id.calculadoraFragment,
                R.color.verde, R.color.verde_suave);
        agregarAcceso(R.drawable.ic_tienda, R.string.acceso_tienda, R.id.tiendaFragment,
                R.color.azul, R.color.azul_suave);
        agregarAcceso(R.drawable.ic_ia, R.string.acceso_asistente, R.id.asistenteFragment,
                R.color.naranja, R.color.naranja_suave);
        agregarAcceso(R.drawable.ic_ubicacion, R.string.acceso_veterinarias, R.id.veterinariasFragment,
                R.color.azul, R.color.azul_suave);
        agregarAcceso(R.drawable.ic_chats, R.string.acceso_chats, R.id.chatsFragment,
                R.color.naranja, R.color.naranja_suave);
        agregarAcceso(R.drawable.ic_pedidos, R.string.acceso_pedidos, R.id.pedidosFragment,
                R.color.verde, R.color.verde_suave);

        b.rvPerros.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvPerros.setAdapter(adapter);
        escuchaPerros = PerroRepo.misPerros().addSnapshotListener((snap, e) -> {
            if (b == null || snap == null) {
                return;
            }
            List<Perro> perros = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                Perro p = d.toObject(Perro.class);
                if (p != null) {
                    perros.add(p);
                }
            }
            adapter.setDatos(perros);
            b.tvSinPerros.setVisibility(perros.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    /** Resumen en la tarjeta: el próximo cuidado y cuántos quedaron sin registrar. */
    private void mostrarProximoCuidado(List<Cuidado> cuidados) {
        Date ahora = new Date();
        Cuidado proximo = null;
        int vencidos = 0;
        for (Cuidado c : cuidados) {
            Periodicidad.Estado e = c.estado(ahora);
            if (e == Periodicidad.Estado.VENCIDO) {
                vencidos++;
            } else if (e != Periodicidad.Estado.COMPLETADO
                    && (proximo == null || c.proxima.before(proximo.proxima))) {
                proximo = c;
            }
        }
        b.tvCuidadosResumen.setText(proximo == null ? getString(R.string.inicio_cuidados_vacio)
                : getString(R.string.inicio_cuidados_proximo, proximo.titulo(), proximo.perroNombre,
                Periodicidad.relativo(proximo.proxima, ahora).toLowerCase(Locale.getDefault())
                        + " " + Formato.hora(proximo.proxima)));
        b.tvCuidadosVencidos.setText(getString(R.string.inicio_cuidados_vencidos, vencidos));
        b.tvCuidadosVencidos.setVisibility(vencidos > 0 ? View.VISIBLE : View.GONE);
    }

    private void agregarAcceso(int icono, int titulo, int destino, int color, int fondo) {
        ItemAccesoBinding item = ItemAccesoBinding.inflate(getLayoutInflater(), b.gridAccesos, false);
        item.icono.setImageResource(icono);
        androidx.core.widget.ImageViewCompat.setImageTintList(item.icono,
                android.content.res.ColorStateList.valueOf(requireContext().getColor(color)));
        item.icono.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(requireContext().getColor(fondo)));
        item.titulo.setText(titulo);
        item.getRoot().setOnClickListener(v -> NavHostFragment.findNavController(this).navigate(destino));
        b.gridAccesos.addView(item.getRoot());
    }

    private void abrirPerro(@Nullable Perro p) {
        Bundle args = new Bundle();
        if (p != null) {
            args.putString(PerroFormFragment.ARG_ID, p.id);
        }
        NavHostFragment.findNavController(this).navigate(R.id.perroFormFragment, args);
    }

    private static String primerNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return "";
        }
        return nombre.trim().split("\\s+")[0];
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (escuchaPerros != null) {
            escuchaPerros.remove();
        }
        if (escuchaCuidados != null) {
            escuchaCuidados.remove();
        }
        b = null;
    }
}
