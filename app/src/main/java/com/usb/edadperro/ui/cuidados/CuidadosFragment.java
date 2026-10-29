package com.usb.edadperro.ui.cuidados;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.usb.edadperro.R;
import com.usb.edadperro.alarmas.Recordatorios;
import com.usb.edadperro.data.modelo.Cuidado;
import com.usb.edadperro.data.repo.CuidadoRepo;
import com.usb.edadperro.databinding.FragmentListaBinding;
import com.usb.edadperro.ui.comun.FilaAdapter;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Formato;
import com.usb.edadperro.util.Periodicidad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Control de vacunas, desparasitantes, medicamentos, baños y cortes de pelo,
 * ordenados por la próxima fecha, con su estado (pendiente, hoy, próximo).
 */
public class CuidadosFragment extends Fragment {

    private FragmentListaBinding b;
    private ListenerRegistration escucha;
    private final FilaAdapter adapter = new FilaAdapter();

    private final ActivityResultLauncher<String> pedirNotificaciones = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), concedido -> {
                actualizarAviso();
                if (!concedido && b != null) {
                    Snackbar.make(b.getRoot(), R.string.aviso_notificaciones_negadas, Snackbar.LENGTH_LONG)
                            .setAction(R.string.ajustes, v -> abrirAjustes(
                                    Recordatorios.ajustesNotificaciones(requireContext())))
                            .show();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentListaBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        b.rvLista.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvLista.setAdapter(adapter);
        b.fab.setText(R.string.nuevo_cuidado);
        b.fab.setVisibility(View.VISIBLE);
        b.fab.setOnClickListener(v -> abrir(null));
        b.tvEncabezado.setVisibility(View.VISIBLE);

        // Las alarmas necesitan el permiso de notificaciones (Android 13+).
        if (state == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && !Recordatorios.puedeNotificar(requireContext())) {
            pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }

        escucha = CuidadoRepo.misCuidados().addSnapshotListener((snap, e) -> {
            if (b == null) {
                return;
            }
            b.progreso.setVisibility(View.GONE);
            if (e != null) {
                vacio(Errores.mensaje(e));
                return;
            }
            List<Cuidado> cuidados = new ArrayList<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                Cuidado c = d.toObject(Cuidado.class);
                if (c != null) {
                    cuidados.add(c);
                }
            }
            ordenar(cuidados);
            Date ahora = new Date();
            List<FilaAdapter.Fila> filas = new ArrayList<>();
            for (Cuidado c : cuidados) {
                filas.add(fila(c, ahora));
            }
            adapter.setDatos(filas);
            if (filas.isEmpty()) {
                vacio(getString(R.string.sin_cuidados));
            } else {
                b.tvVacio.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Al volver de los ajustes del sistema el permiso pudo cambiar.
        actualizarAviso();
    }

    /** Pendientes primero por fecha más cercana; los completados al final. */
    static void ordenar(List<Cuidado> cuidados) {
        Collections.sort(cuidados, (x, y) -> {
            if (x.activo != y.activo) {
                return x.activo ? -1 : 1;
            }
            if (x.proxima == null || y.proxima == null) {
                return x.proxima == null ? 1 : -1;
            }
            return x.activo ? x.proxima.compareTo(y.proxima) : y.proxima.compareTo(x.proxima);
        });
    }

    private FilaAdapter.Fila fila(Cuidado c, Date ahora) {
        FilaAdapter.Fila f = new FilaAdapter.Fila();
        f.icono = icono(c.tipoEnum());
        f.titulo = c.titulo();
        String perro = c.perroNombre == null ? "" : c.perroNombre + " · ";
        Periodicidad.Estado estado = c.estado(ahora);
        if (estado == Periodicidad.Estado.COMPLETADO) {
            f.subtitulo = perro + getString(R.string.cuidado_hecho_el, Formato.fechaHora(c.ultimaVez()));
        } else {
            f.subtitulo = perro + Formato.fechaHora(c.proxima) + "\n"
                    + Periodicidad.describir(c.cada, c.unidadEnum());
            f.derecha = estado == Periodicidad.Estado.HOY
                    ? Formato.hora(c.proxima) : Periodicidad.relativo(c.proxima, ahora);
        }
        switch (estado) {
            case VENCIDO:
                etiqueta(f, R.string.estado_cuidado_vencido, R.color.urgencia, R.color.urgencia_suave);
                break;
            case HOY:
                etiqueta(f, R.string.estado_cuidado_hoy, R.color.naranja, R.color.naranja_suave);
                break;
            case PRONTO:
                etiqueta(f, R.string.estado_cuidado_pronto, R.color.azul, R.color.azul_suave);
                break;
            case COMPLETADO:
                etiqueta(f, R.string.estado_cuidado_completado, R.color.verde, R.color.verde_suave);
                break;
            default:
                break;
        }
        f.alTocar = () -> abrir(c);
        if (c.activo) {
            f.alMantener = () -> confirmarHecho(this, c, null);
        }
        return f;
    }

    private void etiqueta(FilaAdapter.Fila f, int texto, int color, int fondo) {
        f.etiqueta = getString(texto);
        f.colorEtiqueta = color;
        f.fondoEtiqueta = fondo;
    }

    public static int icono(Cuidado.Tipo t) {
        switch (t) {
            case VACUNA:
                return R.drawable.ic_vacuna;
            case DESPARASITANTE:
                return R.drawable.ic_desparasitante;
            case MEDICAMENTO:
                return R.drawable.ic_medicamento;
            case BANO:
                return R.drawable.ic_bano;
            case PELUQUERIA:
                return R.drawable.ic_tijeras;
            default:
                return R.drawable.ic_huella;
        }
    }

    /**
     * Pide confirmación y registra el cuidado como hecho: lo guarda en el historial,
     * calcula la siguiente fecha y reprograma la alarma.
     */
    static void confirmarHecho(Fragment f, Cuidado c, @Nullable Runnable despues) {
        Date siguiente = Periodicidad.siguienteTrasHecho(c.proxima, new Date(), c.cada, c.unidadEnum());
        String detalle = siguiente == null
                ? f.getString(R.string.cuidado_hecho_sin_repetir)
                : f.getString(R.string.cuidado_hecho_siguiente, Formato.fechaHora(siguiente));
        new MaterialAlertDialogBuilder(f.requireContext())
                .setTitle(f.getString(R.string.cuidado_hecho_pregunta, c.titulo()))
                .setMessage(detalle)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.alarma_hecho, (d, w) -> {
                    CuidadoRepo.marcarHecho(c);
                    Recordatorios.alMarcarHecho(f.requireContext(), c);
                    Toast.makeText(f.requireContext(), R.string.cuidado_registrado, Toast.LENGTH_SHORT).show();
                    if (despues != null) {
                        despues.run();
                    }
                })
                .show();
    }

    /** Aviso en el encabezado si faltan permisos para que la alarma suene a tiempo. */
    private void actualizarAviso() {
        if (b == null) {
            return;
        }
        boolean notifica = Recordatorios.puedeNotificar(requireContext());
        boolean exactas = Recordatorios.alarmasExactasPermitidas(requireContext());
        if (notifica && exactas) {
            b.tvEncabezado.setText(R.string.cuidados_encabezado);
            b.tvEncabezado.setTextColor(ContextCompat.getColor(requireContext(), R.color.texto_secundario));
            b.tvEncabezado.setOnClickListener(null);
            return;
        }
        b.tvEncabezado.setText(notifica ? R.string.aviso_alarmas_exactas : R.string.aviso_activar_notificaciones);
        b.tvEncabezado.setTextColor(ContextCompat.getColor(requireContext(), R.color.urgencia));
        b.tvEncabezado.setOnClickListener(v -> {
            if (!notifica && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                pedirNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else if (!notifica) {
                abrirAjustes(Recordatorios.ajustesNotificaciones(requireContext()));
            } else {
                abrirAjustes(Recordatorios.ajustesAlarmasExactas(requireContext()));
            }
        });
    }

    private void abrirAjustes(android.content.Intent i) {
        try {
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.sin_app_para_abrir, Toast.LENGTH_SHORT).show();
        }
    }

    private void abrir(@Nullable Cuidado c) {
        Bundle args = new Bundle();
        if (c != null) {
            args.putString(CuidadoFormFragment.ARG_ID, c.id);
        }
        NavHostFragment.findNavController(this).navigate(R.id.cuidadoFormFragment, args);
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
