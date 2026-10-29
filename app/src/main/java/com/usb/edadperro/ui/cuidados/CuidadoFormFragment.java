package com.usb.edadperro.ui.cuidados;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.firestore.DocumentSnapshot;
import com.usb.edadperro.R;
import com.usb.edadperro.alarmas.Recordatorios;
import com.usb.edadperro.data.modelo.Cuidado;
import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.data.repo.CuidadoRepo;
import com.usb.edadperro.data.repo.PerroRepo;
import com.usb.edadperro.databinding.FragmentCuidadoFormBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Periodicidad;
import com.usb.edadperro.util.Ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * Registro y edición de un cuidado con su alarma: perro, tipo, fecha y hora,
 * aviso anticipado y cada cuánto se repite. Al editar muestra el historial.
 */
public class CuidadoFormFragment extends Fragment {

    public static final String ARG_ID = "cuidadoId";

    /** Opciones de aviso anticipado, en minutos. */
    private static final int[] AVISOS = {0, 60, 1440};
    private static final int CADA_MAXIMO = 365;

    private FragmentCuidadoFormBinding b;
    private Cuidado cuidado = new Cuidado();
    private final Calendar cuando = Calendar.getInstance();
    /** Fecha con la que se abrió (al editar no se exige que una fecha sin cambios sea futura). */
    private Date fechaOriginal;
    /** true mientras se muestran los datos guardados, para no aplicar las sugerencias del tipo. */
    private boolean mostrando;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentCuidadoFormBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.limpiarErrorAlEscribir(b.tilNombre);
        Ui.limpiarErrorAlEscribir(b.tilCada);

        for (Cuidado.Tipo t : Cuidado.Tipo.values()) {
            agregarChip(b.chipsTipo, t.name(), t.etiqueta);
        }
        for (int minutos : AVISOS) {
            agregarChip(b.chipsAviso, minutos, Periodicidad.describirAviso(minutos));
        }
        for (Periodicidad.Unidad u : Periodicidad.Unidad.values()) {
            agregarChip(b.chipsUnidad, u.name(),
                    Character.toUpperCase(u.plural.charAt(0)) + u.plural.substring(1));
        }
        marcar(b.chipsAviso, 0);

        // Por defecto: mañana a las 9:00 a. m.
        cuando.add(Calendar.DATE, 1);
        cuando.set(Calendar.HOUR_OF_DAY, 9);
        cuando.set(Calendar.MINUTE, 0);
        cuando.set(Calendar.SECOND, 0);
        cuando.set(Calendar.MILLISECOND, 0);

        b.chipsTipo.setOnCheckedStateChangeListener((g, ids) -> alCambiarTipo());
        b.chipsAviso.setOnCheckedStateChangeListener((g, ids) -> actualizarResumen());
        b.chipsUnidad.setOnCheckedStateChangeListener((g, ids) -> actualizarResumen());
        b.swRepetir.setOnCheckedChangeListener((v, si) -> {
            b.grupoRepetir.setVisibility(si ? View.VISIBLE : View.GONE);
            actualizarResumen();
        });
        b.etCada.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Sin acción
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarResumen();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Sin acción
            }
        });
        b.btnFecha.setOnClickListener(v -> elegirFecha());
        b.btnHora.setOnClickListener(v -> elegirHora());
        b.btnGuardar.setOnClickListener(v -> guardar());
        b.btnHecho.setOnClickListener(v -> CuidadosFragment.confirmarHecho(this, cuidado,
                () -> NavHostFragment.findNavController(this).popBackStack()));
        b.btnEliminar.setOnClickListener(v -> confirmarEliminar());

        cargarPerros();
        String id = getArguments() == null ? null : getArguments().getString(ARG_ID);
        if (id != null) {
            titulo(R.string.titulo_editar_cuidado);
            b.btnHecho.setVisibility(View.VISIBLE);
            b.btnEliminar.setVisibility(View.VISIBLE);
            CuidadoRepo.cuidado(id).get().addOnSuccessListener(d -> {
                Cuidado c = d.toObject(Cuidado.class);
                if (c != null && b != null) {
                    cuidado = c;
                    mostrar(c);
                }
            });
        } else {
            marcar(b.chipsTipo, Cuidado.Tipo.VACUNA.name());
        }
        mostrarCuando();
    }

    private void titulo(int res) {
        if (getActivity() instanceof AppCompatActivity
                && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(res);
        }
    }

    private void agregarChip(ChipGroup grupo, Object tag, String texto) {
        Chip chip = (Chip) getLayoutInflater().inflate(R.layout.chip_filtro, grupo, false);
        chip.setId(View.generateViewId());
        chip.setTag(tag);
        chip.setText(texto);
        grupo.addView(chip);
    }

    private static void marcar(ChipGroup grupo, Object tag) {
        for (int i = 0; i < grupo.getChildCount(); i++) {
            Chip c = (Chip) grupo.getChildAt(i);
            if (c.getTag().equals(tag)) {
                c.setChecked(true);
                return;
            }
        }
    }

    @Nullable
    private static Object elegido(ChipGroup grupo) {
        Chip c = grupo.findViewById(grupo.getCheckedChipId());
        return c == null ? null : c.getTag();
    }

    private Cuidado.Tipo tipo() {
        Object t = elegido(b.chipsTipo);
        return t == null ? Cuidado.Tipo.OTRO : Cuidado.Tipo.valueOf((String) t);
    }

    private Periodicidad.Unidad unidad() {
        return Periodicidad.unidad((String) elegido(b.chipsUnidad));
    }

    private int aviso() {
        Object a = elegido(b.chipsAviso);
        return a == null ? 0 : (Integer) a;
    }

    /** @return cada cuánto se repite, 0 si no se repite, o -1 si el número no es válido */
    private int cada() {
        if (!b.swRepetir.isChecked()) {
            return 0;
        }
        try {
            int n = Integer.parseInt(Ui.texto(b.etCada));
            return n >= 1 && n <= CADA_MAXIMO ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void cargarPerros() {
        PerroRepo.misPerros().get().addOnSuccessListener(snap -> {
            if (b == null) {
                return;
            }
            List<Perro> perros = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Perro p = d.toObject(Perro.class);
                if (p != null) {
                    perros.add(p);
                }
            }
            for (Perro p : perros) {
                agregarChip(b.chipsPerro, p.id, p.nombre);
            }
            b.tvSinPerros.setVisibility(perros.isEmpty() ? View.VISIBLE : View.GONE);
            if (cuidado.perroId != null) {
                marcar(b.chipsPerro, cuidado.perroId);
            } else if (!perros.isEmpty()) {
                marcar(b.chipsPerro, perros.get(0).id);
            }
        });
    }

    /** Al crear un cuidado, el tipo sugiere la repetición habitual (vacuna: cada año…). */
    private void alCambiarTipo() {
        Cuidado.Tipo t = tipo();
        b.tilNombre.setHelperText(ayudaNombre(t));
        if (!mostrando && cuidado.id == null) {
            b.swRepetir.setChecked(t.cadaSugerido > 0);
            if (t.cadaSugerido > 0) {
                b.etCada.setText(String.valueOf(t.cadaSugerido));
                marcar(b.chipsUnidad, t.unidadSugerida.name());
            }
        }
        actualizarResumen();
    }

    private String ayudaNombre(Cuidado.Tipo t) {
        switch (t) {
            case VACUNA:
                return getString(R.string.ayuda_vacuna);
            case DESPARASITANTE:
                return getString(R.string.ayuda_desparasitante);
            case MEDICAMENTO:
                return getString(R.string.ayuda_medicamento);
            case BANO:
            case PELUQUERIA:
                return getString(R.string.ayuda_opcional);
            default:
                return getString(R.string.ayuda_otro);
        }
    }

    private void mostrar(Cuidado c) {
        mostrando = true;
        marcar(b.chipsPerro, c.perroId);
        marcar(b.chipsTipo, c.tipo);
        b.etNombre.setText(c.nombre);
        b.etNotas.setText(c.notas);
        if (c.proxima != null) {
            cuando.setTime(c.proxima);
            fechaOriginal = c.proxima;
        }
        marcar(b.chipsAviso, c.avisoMinutos);
        b.swRepetir.setChecked(c.cada > 0);
        if (c.cada > 0) {
            b.etCada.setText(String.valueOf(c.cada));
            marcar(b.chipsUnidad, c.unidadEnum().name());
        }
        b.btnHecho.setVisibility(c.activo ? View.VISIBLE : View.GONE);
        mostrarHistorial(c);
        mostrarCuando();
        mostrando = false;
    }

    private void mostrarHistorial(Cuidado c) {
        b.cardHistorial.setVisibility(View.VISIBLE);
        if (c.historial == null || c.historial.isEmpty()) {
            b.tvHistorial.setText(R.string.cuidado_sin_historial);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = c.historial.size() - 1; i >= 0; i--) {
            sb.append("• ").append(Formato.fechaHora(c.historial.get(i)));
            if (i > 0) {
                sb.append('\n');
            }
        }
        b.tvHistorial.setText(sb);
    }

    // ---------- Fecha y hora ----------

    private void elegirFecha() {
        // El selector trabaja con medianoche UTC del día elegido.
        Calendar utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        utc.clear();
        utc.set(cuando.get(Calendar.YEAR), cuando.get(Calendar.MONTH), cuando.get(Calendar.DAY_OF_MONTH));
        Calendar local = Calendar.getInstance();
        Calendar hoy = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        hoy.clear();
        hoy.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH));
        MaterialDatePicker<Long> dp = MaterialDatePicker.Builder.datePicker()
                .setTitleText(R.string.cuidado_elige_fecha)
                .setSelection(utc.getTimeInMillis())
                .setCalendarConstraints(new CalendarConstraints.Builder()
                        .setValidator(DateValidatorPointForward.from(hoy.getTimeInMillis()))
                        .build())
                .build();
        dp.addOnPositiveButtonClickListener(sel -> {
            Calendar elegido = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            elegido.setTimeInMillis(sel);
            cuando.set(elegido.get(Calendar.YEAR), elegido.get(Calendar.MONTH),
                    elegido.get(Calendar.DAY_OF_MONTH));
            mostrarCuando();
        });
        dp.show(getChildFragmentManager(), "fecha");
    }

    private void elegirHora() {
        MaterialTimePicker tp = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(cuando.get(Calendar.HOUR_OF_DAY))
                .setMinute(cuando.get(Calendar.MINUTE))
                .setTitleText(R.string.cuidado_elige_hora)
                .build();
        tp.addOnPositiveButtonClickListener(v -> {
            cuando.set(Calendar.HOUR_OF_DAY, tp.getHour());
            cuando.set(Calendar.MINUTE, tp.getMinute());
            cuando.set(Calendar.SECOND, 0);
            cuando.set(Calendar.MILLISECOND, 0);
            mostrarCuando();
        });
        tp.show(getChildFragmentManager(), "hora");
    }

    private void mostrarCuando() {
        b.btnFecha.setText(Formato.fecha(cuando.getTime()));
        b.btnHora.setText(Formato.hora(cuando.getTime()));
        actualizarResumen();
    }

    /** "Sonará el 24 oct 2026 a las 8:00 a. m. (y 1 día antes) · Cada 1 año" */
    private void actualizarResumen() {
        Date d = cuando.getTime();
        StringBuilder sb = new StringBuilder(getString(R.string.cuidado_sonara,
                Formato.fecha(d), Formato.hora(d)));
        if (aviso() > 0) {
            sb.append(getString(R.string.cuidado_y_antes, Periodicidad.describirAviso(aviso())));
        }
        int n = cada();
        sb.append(" · ").append(n < 0 ? getString(R.string.cuidado_repeticion_invalida)
                : Periodicidad.describir(n, unidad()));
        b.tvResumen.setText(sb);
    }

    // ---------- Guardar y eliminar ----------

    private void guardar() {
        Object perroId = elegido(b.chipsPerro);
        Cuidado.Tipo tipo = tipo();
        String nombre = Ui.texto(b.etNombre);
        int cada = cada();
        Date proxima = cuando.getTime();

        if (perroId == null) {
            Snackbar.make(b.getRoot(), R.string.cuidado_sin_perros, Snackbar.LENGTH_LONG).show();
            return;
        }
        boolean ok = true;
        boolean nombreOpcional = tipo == Cuidado.Tipo.BANO || tipo == Cuidado.Tipo.PELUQUERIA;
        if (!nombreOpcional && nombre.length() < 2) {
            b.tilNombre.setError(getString(R.string.error_nombre_cuidado));
            ok = false;
        }
        if (cada < 0) {
            b.tilCada.setError(getString(R.string.error_cada, CADA_MAXIMO));
            ok = false;
        }
        boolean fechaCambio = fechaOriginal == null || fechaOriginal.getTime() != proxima.getTime();
        if (fechaCambio && !proxima.after(new Date())) {
            Snackbar.make(b.getRoot(), R.string.error_fecha_pasada, Snackbar.LENGTH_LONG).show();
            ok = false;
        }
        if (!ok) {
            return;
        }
        Chip chipPerro = b.chipsPerro.findViewById(b.chipsPerro.getCheckedChipId());
        cuidado.perroId = (String) perroId;
        cuidado.perroNombre = chipPerro.getText().toString();
        cuidado.tipo = tipo.name();
        cuidado.nombre = nombre;
        cuidado.notas = Ui.texto(b.etNotas);
        cuidado.proxima = proxima;
        cuidado.cada = cada;
        cuidado.unidad = unidad().name();
        cuidado.avisoMinutos = aviso();
        // Un cuidado completado vuelve a quedar activo si se le da una fecha futura.
        cuidado.activo = cuidado.activo || proxima.after(new Date());

        b.btnGuardar.setEnabled(false);
        // Firestore guarda primero en la caché local, así funciona también sin conexión.
        CuidadoRepo.guardar(cuidado).addOnFailureListener(e -> {
            if (b != null) {
                b.btnGuardar.setEnabled(true);
                Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
            }
        });
        Recordatorios.programar(requireContext(), cuidado);
        Toast.makeText(requireContext(), getString(R.string.cuidado_programado,
                Formato.fechaHora(proxima)), Toast.LENGTH_LONG).show();
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void confirmarEliminar() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.eliminar_cuidado_pregunta, cuidado.titulo()))
                .setMessage(R.string.eliminar_cuidado_detalle)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    Recordatorios.cancelar(requireContext(), cuidado.id);
                    CuidadoRepo.eliminar(cuidado.id);
                    NavHostFragment.findNavController(this).popBackStack();
                })
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b = null;
    }
}
