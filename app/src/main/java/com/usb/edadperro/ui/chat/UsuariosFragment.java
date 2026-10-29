package com.usb.edadperro.ui.chat;

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

import com.google.firebase.firestore.DocumentSnapshot;
import com.usb.edadperro.R;
import com.usb.edadperro.data.Fb;
import com.usb.edadperro.data.modelo.PerfilPublico;
import com.usb.edadperro.data.repo.ChatRepo;
import com.usb.edadperro.databinding.FragmentListaBinding;
import com.usb.edadperro.ui.comun.FilaAdapter;
import com.usb.edadperro.util.Errores;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de usuarios registrados para iniciar una conversación.
 * Solo se muestra el nombre y la ciudad (perfil público).
 */
public class UsuariosFragment extends Fragment {

    private FragmentListaBinding b;
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
        b.tvEncabezado.setText(R.string.usuarios_encabezado);
        b.tvEncabezado.setVisibility(View.VISIBLE);

        String yo = Fb.uid();
        ChatRepo.usuarios().get()
                .addOnSuccessListener(snap -> {
                    if (b == null) {
                        return;
                    }
                    b.progreso.setVisibility(View.GONE);
                    List<FilaAdapter.Fila> filas = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        PerfilPublico p = d.toObject(PerfilPublico.class);
                        if (p == null || p.id.equals(yo)) {
                            continue;
                        }
                        FilaAdapter.Fila f = new FilaAdapter.Fila();
                        f.titulo = p.nombre;
                        f.subtitulo = p.ciudad;
                        f.alTocar = () -> abrir(p);
                        filas.add(f);
                    }
                    adapter.setDatos(filas);
                    if (filas.isEmpty()) {
                        b.tvVacio.setText(R.string.sin_usuarios);
                        b.tvVacio.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (b != null) {
                        b.progreso.setVisibility(View.GONE);
                        b.tvVacio.setText(Errores.mensaje(e));
                        b.tvVacio.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void abrir(PerfilPublico p) {
        Bundle args = new Bundle();
        args.putString(ConversacionFragment.ARG_UID, p.id);
        args.putString(ConversacionFragment.ARG_NOMBRE, p.nombre);
        // Al volver desde la conversación se regresa a la lista de chats.
        NavOptions op = new NavOptions.Builder().setPopUpTo(R.id.usuariosFragment, true).build();
        NavHostFragment.findNavController(this).navigate(R.id.conversacionFragment, args, op);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
