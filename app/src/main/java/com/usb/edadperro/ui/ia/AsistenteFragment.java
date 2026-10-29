package com.usb.edadperro.ui.ia;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.chip.Chip;
import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.databinding.FragmentAsistenteBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Red;
import com.usb.edadperro.util.Ui;

/**
 * Chat con el asistente de IA sobre cuidado y bienestar de los perros.
 *
 * Requisito obligatorio de la guía: cuando se identifica una posible urgencia,
 * se muestra un banner rojo que recomienda atención profesional inmediata y
 * lleva al mapa de veterinarias de urgencias.
 */
public class AsistenteFragment extends Fragment {

    private FragmentAsistenteBinding b;
    private AsistenteViewModel vm;
    private final MensajesIAAdapter adapter = new MensajesIAAdapter();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentAsistenteBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        // El ViewModel se asocia a la actividad para conservar la conversación entre pestañas.
        vm = new ViewModelProvider(requireActivity()).get(AsistenteViewModel.class);

        LinearLayoutManager lm = new LinearLayoutManager(requireContext());
        lm.setStackFromEnd(true);
        b.rvMensajes.setLayoutManager(lm);
        b.rvMensajes.setAdapter(adapter);

        vm.mensajes().observe(getViewLifecycleOwner(), lista -> {
            adapter.setDatos(lista);
            b.rvMensajes.scrollToPosition(Math.max(0, lista.size() - 1));
            b.sugerencias.setVisibility(lista.size() <= 1 ? View.VISIBLE : View.GONE);
        });
        vm.esperando().observe(getViewLifecycleOwner(), espera -> b.btnEnviar.setEnabled(!espera));
        vm.urgencia().observe(getViewLifecycleOwner(), motivo -> {
            b.cardUrgencia.setVisibility(motivo == null ? View.GONE : View.VISIBLE);
            if (motivo != null) {
                b.tvUrgenciaMotivo.setText(motivo);
            }
        });

        b.btnEnviar.setOnClickListener(v -> enviar(Ui.texto(b.etPregunta)));
        b.btnVerUrgencias.setOnClickListener(v -> ((MainActivity) requireActivity()).abrirUrgencias());
        b.btnCerrarUrgencia.setOnClickListener(v -> vm.cerrarUrgencia());

        for (int res : new int[]{R.string.sugerencia_1, R.string.sugerencia_2,
                R.string.sugerencia_3, R.string.sugerencia_4}) {
            Chip chip = new Chip(requireContext());
            chip.setText(res);
            chip.setOnClickListener(v -> enviar(chip.getText().toString()));
            b.chipsSugerencias.addView(chip);
        }
    }

    private void enviar(String texto) {
        if (texto.isEmpty()) {
            return;
        }
        if (!Red.hayConexion(requireContext())) {
            // La detección local de urgencias funciona aún sin conexión.
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
        }
        b.etPregunta.setText("");
        vm.enviar(texto);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
