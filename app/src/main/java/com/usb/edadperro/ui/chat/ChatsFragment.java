package com.usb.edadperro.ui.chat;

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
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.Conversacion;
import com.usb.edadperro.data.repo.ChatRepo;
import com.usb.edadperro.databinding.FragmentListaBinding;
import com.usb.edadperro.ui.comun.FilaAdapter;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Historial de conversaciones con otros usuarios (Punto 9).
 */
public class ChatsFragment extends Fragment {

    private FragmentListaBinding b;
    private ListenerRegistration escucha;
    private ListenerRegistration escuchaBloqueos;
    private final FilaAdapter adapter = new FilaAdapter();
    /** Usuarios bloqueados por mí: sus conversaciones se marcan como "Bloqueado". */
    private final java.util.Set<String> bloqueados = new java.util.HashSet<>();
    private final List<Conversacion> convs = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentListaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvLista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvLista.setAdapter(adapter);
        b.tvEncabezado.setText(R.string.chats_encabezado);
        b.tvEncabezado.setVisibility(View.VISIBLE);
        b.fab.setText(R.string.nuevo_chat);
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.usuariosFragment));

        String yo = Fb.uid();
        escucha = ChatRepo.misConversaciones().addSnapshotListener((snap, e) -> {
            if (b == null) {
                return;
            }
            b.progreso.setVisibility(View.GONE);
            if (e != null) {
                vacio(Errores.mensaje(e));
                return;
            }
            convs.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Conversacion c = d.toObject(Conversacion.class);
                if (c != null) {
                    convs.add(c);
                }
            }
            Collections.sort(convs, (x, y) -> {
                if (x.actualizado == null) {
                    return -1;
                }
                if (y.actualizado == null) {
                    return 1;
                }
                return y.actualizado.compareTo(x.actualizado);
            });
            mostrar();
        });
        escuchaBloqueos = ChatRepo.misBloqueados().addSnapshotListener((snap, e) -> {
            if (b == null || snap == null) {
                return;
            }
            bloqueados.clear();
            for (DocumentSnapshot d : snap.getDocuments()) {
                bloqueados.add(d.getId());
            }
            mostrar();
        });
    }

    private void mostrar() {
        String yo = Fb.uid();
        List<FilaAdapter.Fila> filas = new ArrayList<>();
        for (Conversacion c : convs) {
            FilaAdapter.Fila f = new FilaAdapter.Fila();
            f.titulo = c.otroNombre(yo);
            String prefijo = yo.equals(c.ultimoRemitente) ? getString(R.string.tu) + ": " : "";
            f.subtitulo = prefijo + (c.ultimoMensaje == null ? "" : c.ultimoMensaje);
            f.derecha = Formato.hora(c.actualizado);
            if (bloqueados.contains(c.otroUid(yo))) {
                f.etiqueta = getString(R.string.bloqueado);
                f.colorEtiqueta = R.color.urgencia;
                f.fondoEtiqueta = R.color.urgencia_suave;
            }
            f.alTocar = () -> abrir(c.otroUid(yo), c.otroNombre(yo));
            filas.add(f);
        }
        adapter.setDatos(filas);
        if (filas.isEmpty()) {
            vacio(getString(R.string.sin_chats));
        } else {
            b.tvVacio.setVisibility(View.GONE);
        }
    }

    private void abrir(String uid, String nombre) {
        Bundle args = new Bundle();
        args.putString(ConversacionFragment.ARG_UID, uid);
        args.putString(ConversacionFragment.ARG_NOMBRE, nombre);
        NavHostFragment.findNavController(this).navigate(R.id.conversacionFragment, args);
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
        if (escuchaBloqueos != null) {
            escuchaBloqueos.remove();
        }
        b = null;
    }
}
