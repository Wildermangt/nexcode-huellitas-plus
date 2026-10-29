package com.usb.edadperro.ui.perros;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.usb.edadperro.CalculadoraEdad;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.data.repo.PerroRepo;
import com.usb.edadperro.databinding.FragmentCalculadoraBinding;
import com.usb.edadperro.util.Recomendaciones;
import com.usb.edadperro.util.Ui;

import java.util.Locale;

/**
 * Calculadora de la edad del perro vs. la edad humana.
 *
 * Conserva la lógica y las validaciones de la versión 1 (campo vacío, texto no
 * numérico, edad menor o igual a cero y edad mayor a 30 años) y agrega las
 * recomendaciones según la etapa de vida y el atajo para usar un perro registrado.
 */
public class CalculadoraFragment extends Fragment {

    private FragmentCalculadoraBinding b;
    private double ultimaEdad;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentCalculadoraBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.btnCalcular.setOnClickListener(v -> calcular());
        b.btnLimpiar.setOnClickListener(v -> limpiar());
        b.etEdad.setOnEditorActionListener((v, id, e) -> {
            calcular();
            return true;
        });
        Ui.limpiarErrorAlEscribir(b.tilEdad);
        b.btnGuardarPerro.setOnClickListener(v -> guardarComoPerro());
        cargarPerros();
    }

    /** Muestra un chip por cada perro registrado para llenar el formulario con sus datos. */
    private void cargarPerros() {
        PerroRepo.misPerros().get().addOnSuccessListener(snap -> {
            if (b == null || snap.isEmpty()) {
                return;
            }
            b.tvUsarPerro.setVisibility(View.VISIBLE);
            for (com.google.firebase.firestore.DocumentSnapshot d : snap.getDocuments()) {
                Perro p = d.toObject(Perro.class);
                if (p == null) {
                    continue;
                }
                Chip chip = new Chip(requireContext());
                chip.setText(p.nombre);
                chip.setCheckable(true);
                chip.setOnClickListener(v -> {
                    b.etEdad.setText(formatearEdad(p.edad));
                    marcarTamano(p.tamanoEnum());
                    calcular();
                });
                b.chipsPerros.addView(chip);
            }
        });
    }

    /** Lee la edad ingresada, la valida y muestra el resultado del cálculo. */
    private void calcular() {
        String texto = Ui.texto(b.etEdad);

        // ----------- VALIDACIÓN 1: el campo no puede estar vacío -----------
        if (texto.isEmpty()) {
            mostrarError(getString(R.string.error_vacio));
            return;
        }
        // Se acepta la coma como separador decimal (teclado en español).
        texto = texto.replace(',', '.');

        // ----------- VALIDACIÓN 2: el texto debe ser un número válido -----------
        double edad;
        try {
            edad = Double.parseDouble(texto);
        } catch (NumberFormatException e) {
            mostrarError(getString(R.string.error_no_numerico));
            return;
        }
        // ----------- VALIDACIÓN 3: la edad debe ser mayor que cero -----------
        if (edad <= 0) {
            mostrarError(getString(R.string.error_mayor_cero));
            return;
        }
        // ----------- VALIDACIÓN 4: la edad debe ser realista -----------
        if (edad > CalculadoraEdad.EDAD_MAXIMA) {
            mostrarError(getString(R.string.error_maximo, (int) CalculadoraEdad.EDAD_MAXIMA));
            return;
        }

        b.tilEdad.setError(null);
        Ui.ocultarTeclado(requireActivity());
        ultimaEdad = edad;

        CalculadoraEdad.Tamano tamano = tamanoSeleccionado();
        double edadHumana = CalculadoraEdad.calcularVeterinario(edad, tamano);
        double tradicional = CalculadoraEdad.calcularTradicional(edad);
        double epigenetico = CalculadoraEdad.calcularEpigenetico(edad);
        CalculadoraEdad.Etapa etapa = CalculadoraEdad.obtenerEtapa(edad, tamano);

        b.tvResultado.setText(String.format(Locale.getDefault(), "%.1f", edadHumana));
        b.tvDetalle.setText(getString(R.string.detalle_resultado, formatearEdad(edad),
                String.format(Locale.getDefault(), "%.1f", edadHumana)));
        b.tvEtapa.setText(getString(R.string.etapa_prefijo, TextosPerro.etapa(etapa)));
        b.tvTradicional.setText(getString(R.string.comparacion_tradicional,
                String.format(Locale.getDefault(), "%.1f", tradicional)));
        b.tvEpigenetico.setText(getString(R.string.comparacion_epigenetico,
                String.format(Locale.getDefault(), "%.1f", epigenetico)));

        TextosPerro.llenarVinetas(requireContext(), b.listaRecomendaciones,
                Recomendaciones.obtener(etapa, tamano),
                ContextCompat.getColor(requireContext(), R.color.texto_principal));

        b.cardResultado.setVisibility(View.VISIBLE);
        b.cardRecomendaciones.setVisibility(View.VISIBLE);
    }

    private void limpiar() {
        b.etEdad.setText("");
        b.tilEdad.setError(null);
        b.rgTamano.check(R.id.rbMediano);
        b.chipsPerros.clearCheck();
        b.cardResultado.setVisibility(View.GONE);
        b.cardRecomendaciones.setVisibility(View.GONE);
        b.etEdad.requestFocus();
    }

    private void mostrarError(String mensaje) {
        b.tilEdad.setError(mensaje);
        b.etEdad.requestFocus();
        b.cardResultado.setVisibility(View.GONE);
        b.cardRecomendaciones.setVisibility(View.GONE);
    }

    /** Abre el formulario de perro con la edad y el tamaño calculados. */
    private void guardarComoPerro() {
        Bundle args = new Bundle();
        args.putDouble(PerroFormFragment.ARG_EDAD, ultimaEdad);
        args.putString(PerroFormFragment.ARG_TAMANO, tamanoSeleccionado().name());
        NavHostFragment.findNavController(this).navigate(R.id.perroFormFragment, args);
    }

    private CalculadoraEdad.Tamano tamanoSeleccionado() {
        int id = b.rgTamano.getCheckedRadioButtonId();
        if (id == R.id.rbPequeno) {
            return CalculadoraEdad.Tamano.PEQUENO;
        }
        if (id == R.id.rbGrande) {
            return CalculadoraEdad.Tamano.GRANDE;
        }
        return CalculadoraEdad.Tamano.MEDIANO;
    }

    private void marcarTamano(CalculadoraEdad.Tamano t) {
        b.rgTamano.check(t == CalculadoraEdad.Tamano.PEQUENO ? R.id.rbPequeno
                : t == CalculadoraEdad.Tamano.GRANDE ? R.id.rbGrande : R.id.rbMediano);
    }

    /** Muestra la edad sin decimales cuando es un número entero. */
    static String formatearEdad(double edad) {
        if (edad == Math.floor(edad)) {
            return String.format(Locale.getDefault(), "%d", (long) edad);
        }
        return String.format(Locale.getDefault(), "%.1f", edad);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
