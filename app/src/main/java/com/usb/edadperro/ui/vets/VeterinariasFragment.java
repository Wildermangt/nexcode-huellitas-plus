package com.usb.edadperro.ui.vets;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.MainActivity;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Veterinaria;
import com.usb.edadperro.data.remoto.OsmServicio;
import com.usb.edadperro.databinding.FragmentVeterinariasBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Hilos;
import com.usb.edadperro.util.Red;
import com.usb.edadperro.util.Ui;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Marker;

import java.util.ArrayList;
import java.util.List;

/**
 * Veterinarias cercanas y servicios de urgencias (Punto 5).
 *
 * - Pide permiso de ubicación; si el usuario lo niega, permite buscar por
 *   ciudad o dirección.
 * - Las veterinarias salen de OpenStreetMap (Overpass API): son lugares reales,
 *   cada uno enlazado a su ficha pública en openstreetmap.org.
 * - Solo se marcan como urgencias las que OSM registra con emergency=yes u
 *   horario 24/7, es decir, información verificada.
 */
public class VeterinariasFragment extends Fragment implements VeterinariasAdapter.Acciones {

    private static final int RADIO_INICIAL = 5_000;
    private static final int RADIO_AMPLIADO = 15_000;
    /** Centro por defecto (Bogotá) mientras no hay ubicación. */
    private static final GeoPoint CENTRO_DEFECTO = new GeoPoint(4.6486, -74.0628);

    private FragmentVeterinariasBinding b;
    private final VeterinariasAdapter adapter = new VeterinariasAdapter(this);
    private final List<Veterinaria> todas = new ArrayList<>();
    private FusedLocationProviderClient ubicacion;
    private GeoPoint centro;
    private boolean centroEsUsuario;
    private String nombreZona;
    private int consulta = 0;

    private final ActivityResultLauncher<String[]> pedirPermiso = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), res -> {
                boolean concedido = Boolean.TRUE.equals(res.get(Manifest.permission.ACCESS_FINE_LOCATION))
                        || Boolean.TRUE.equals(res.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (concedido) {
                    obtenerUbicacion();
                } else {
                    sinPermiso();
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentVeterinariasBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        ubicacion = LocationServices.getFusedLocationProviderClient(requireContext());

        b.mapa.setTileSource(TileSourceFactory.MAPNIK);
        b.mapa.setMultiTouchControls(true);
        b.mapa.getController().setZoom(13.0);
        b.mapa.getController().setCenter(CENTRO_DEFECTO);

        b.rvVeterinarias.setLayoutManager(new LinearLayoutManager(requireContext()));
        b.rvVeterinarias.setAdapter(adapter);

        if (getArguments() != null && getArguments().getBoolean(MainActivity.ARG_SOLO_URGENCIAS)) {
            b.chipUrgencias.setChecked(true);
        }
        b.chipsFiltro.setOnCheckedStateChangeListener((g, ids) -> mostrar());

        b.btnMiUbicacion.setOnClickListener(v -> solicitarUbicacion());
        // Para lugares que aún no están en OpenStreetMap: misma zona, en Google Maps.
        b.btnGoogleMaps.setOnClickListener(v -> {
            GeoPoint p = centro != null ? centro : CENTRO_DEFECTO;
            Ui.buscarEnMapas(requireContext(), p.getLatitude(), p.getLongitude(),
                    b.chipUrgencias.isChecked() ? "veterinaria 24 horas" : "veterinaria");
        });
        b.etBuscar.setOnEditorActionListener((v, id, e) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH) {
                buscarTexto(Ui.texto(b.etBuscar));
                return true;
            }
            return false;
        });

        solicitarUbicacion();
    }

    private boolean tienePermiso() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Solicita autorización para usar la ubicación del dispositivo. */
    private void solicitarUbicacion() {
        if (tienePermiso()) {
            obtenerUbicacion();
        } else {
            pedirPermiso.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION});
        }
    }

    private void sinPermiso() {
        if (b == null) {
            return;
        }
        b.tvResumen.setText(R.string.sin_permiso_ubicacion);
        b.tilBuscar.requestFocus();
        Snackbar.make(b.getRoot(), R.string.sin_permiso_ubicacion, Snackbar.LENGTH_LONG).show();
    }

    @SuppressLint("MissingPermission") // se verifica en tienePermiso()
    private void obtenerUbicacion() {
        if (!tienePermiso()) {
            return;
        }
        cargando(true);
        b.tvResumen.setText(R.string.obteniendo_ubicacion);
        // GPS de alta precisión con un límite de 15 s; acepta una ubicación de hasta 2 min.
        CurrentLocationRequest pedido = new CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setDurationMillis(15_000)
                .setMaxUpdateAgeMillis(120_000)
                .build();
        ubicacion.getCurrentLocation(pedido, new CancellationTokenSource().getToken())
                .addOnSuccessListener(loc -> {
                    if (loc != null) {
                        usarUbicacion(loc);
                    } else {
                        ubicacion.getLastLocation().addOnSuccessListener(ultima -> {
                            if (ultima != null) {
                                usarUbicacion(ultima);
                            } else if (b != null) {
                                cargando(false);
                                b.tvResumen.setText(R.string.ubicacion_no_disponible);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> {
                    if (b != null) {
                        cargando(false);
                        b.tvResumen.setText(R.string.ubicacion_no_disponible);
                    }
                });
    }

    private void usarUbicacion(Location loc) {
        // La ubicación puede llegar cuando el usuario ya salió de esta pantalla.
        if (b == null || !isAdded()) {
            return;
        }
        nombreZona = getString(R.string.tu_ubicacion);
        buscarCerca(new GeoPoint(loc.getLatitude(), loc.getLongitude()), true);
    }

    /** Búsqueda por ciudad o dirección (cuando no se autoriza la ubicación). */
    private void buscarTexto(String texto) {
        if (texto.length() < 3) {
            b.tilBuscar.setError(getString(R.string.error_busqueda_corta));
            return;
        }
        b.tilBuscar.setError(null);
        Ui.ocultarTeclado(requireActivity());
        if (!Red.hayConexion(requireContext())) {
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
            return;
        }
        cargando(true);
        Hilos.fondo(() -> {
            try {
                OsmServicio.Lugar lugar = OsmServicio.buscarLugar(texto);
                Hilos.principal(() -> {
                    if (b == null) {
                        return;
                    }
                    if (lugar == null) {
                        cargando(false);
                        b.tvResumen.setText(getString(R.string.lugar_no_encontrado, texto));
                        return;
                    }
                    nombreZona = lugar.nombre.split(",")[0];
                    buscarCerca(new GeoPoint(lugar.lat, lugar.lon), false);
                });
            } catch (Exception e) {
                Hilos.principal(() -> error(e));
            }
        });
    }

    /** Consulta las veterinarias reales alrededor de un punto. */
    private void buscarCerca(GeoPoint punto, boolean esUsuario) {
        if (b == null) {
            return;
        }
        if (!Red.hayConexion(requireContext())) {
            cargando(false);
            b.tvResumen.setText(Errores.SIN_CONEXION);
            return;
        }
        centro = punto;
        final int miConsulta = ++consulta;
        b.mapa.getController().animateTo(punto, 14.0, 800L);
        cargando(true);
        b.tvResumen.setText(R.string.buscando_veterinarias);
        Hilos.fondo(() -> {
            try {
                int radio = RADIO_INICIAL;
                List<Veterinaria> lista = OsmServicio.veterinariasCerca(punto.getLatitude(), punto.getLongitude(), radio);
                if (lista.isEmpty()) {
                    radio = RADIO_AMPLIADO;
                    lista = OsmServicio.veterinariasCerca(punto.getLatitude(), punto.getLongitude(), radio);
                }
                final List<Veterinaria> resultado = lista;
                final int radioUsado = radio;
                Hilos.principal(() -> {
                    if (b == null || miConsulta != consulta) {
                        return;
                    }
                    cargando(false);
                    todas.clear();
                    todas.addAll(resultado);
                    centroEsUsuario = esUsuario;
                    mostrar();
                    b.tvResumen.setText(getString(R.string.resumen_veterinarias, resultado.size(),
                            radioUsado / 1000, nombreZona, contarUrgencias()));
                });
            } catch (Exception e) {
                Hilos.principal(() -> error(e));
            }
        });
    }

    private int contarUrgencias() {
        int n = 0;
        for (Veterinaria v : todas) {
            if (v.urgencias) {
                n++;
            }
        }
        return n;
    }

    /** Aplica el filtro de urgencias a la lista y a los marcadores del mapa. */
    private void mostrar() {
        boolean soloUrgencias = b.chipUrgencias.isChecked();
        List<Veterinaria> visibles = new ArrayList<>();
        for (Veterinaria v : todas) {
            if (!soloUrgencias || v.urgencias) {
                visibles.add(v);
            }
        }
        adapter.setDatos(visibles);
        pintarMarcadores(visibles);
        if (soloUrgencias && visibles.isEmpty() && !todas.isEmpty()) {
            Snackbar.make(b.getRoot(), R.string.sin_urgencias_verificadas, Snackbar.LENGTH_LONG).show();
        }
    }

    private void pintarMarcadores(List<Veterinaria> visibles) {
        b.mapa.getOverlays().clear();
        if (centro != null) {
            Marker yo = new Marker(b.mapa);
            yo.setPosition(centro);
            yo.setTitle(centroEsUsuario ? getString(R.string.tu_ubicacion) : nombreZona);
            yo.setIcon(icono(R.drawable.ic_mi_ubicacion, R.color.azul));
            yo.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            b.mapa.getOverlays().add(yo);
        }
        for (Veterinaria v : visibles) {
            Marker m = new Marker(b.mapa);
            m.setPosition(new GeoPoint(v.lat, v.lon));
            m.setTitle(v.nombre);
            m.setSnippet(v.urgencias ? getString(R.string.urgencias_24h_verificado)
                    : (v.direccion == null ? "" : v.direccion));
            m.setIcon(icono(v.urgencias ? R.drawable.ic_urgencia : R.drawable.ic_ubicacion,
                    v.urgencias ? R.color.urgencia : R.color.verde));
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            b.mapa.getOverlays().add(m);
        }
        b.mapa.invalidate();
    }

    private Drawable icono(int res, int color) {
        Drawable d = DrawableCompat.wrap(ContextCompat.getDrawable(requireContext(), res).mutate());
        DrawableCompat.setTint(d, ContextCompat.getColor(requireContext(), color));
        int px = (int) (34 * getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, px, px);
        return d;
    }

    @Override
    public void enfocar(Veterinaria v) {
        b.mapa.getController().animateTo(new GeoPoint(v.lat, v.lon), 17.0, 600L);
    }

    @Override
    public void consultarDireccion(Veterinaria v, int posicion) {
        if (!Red.hayConexion(requireContext())) {
            Snackbar.make(b.getRoot(), Errores.SIN_CONEXION, Snackbar.LENGTH_LONG).show();
            return;
        }
        Hilos.fondo(() -> {
            try {
                String dir = OsmServicio.direccionDe(v.lat, v.lon);
                Hilos.principal(() -> {
                    if (b == null) {
                        return;
                    }
                    v.direccion = dir == null ? getString(R.string.direccion_no_disponible) : dir;
                    adapter.notifyItemChanged(posicion);
                });
            } catch (Exception e) {
                Hilos.principal(() -> error(e));
            }
        });
    }

    private void error(Exception e) {
        if (b == null) {
            return;
        }
        cargando(false);
        b.tvResumen.setText(Errores.mensaje(e));
        Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG)
                .setAction(R.string.reintentar, x -> {
                    if (centro != null) {
                        buscarCerca(centro, false);
                    } else {
                        solicitarUbicacion();
                    }
                }).show();
    }

    private void cargando(boolean si) {
        if (b != null) {
            b.progreso.setVisibility(si ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        b.mapa.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        b.mapa.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        b.mapa.onDetach();
        b = null;
    }
}
