package com.usb.edadperro.ui.perros;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.usb.edadperro.CalculadoraEdad;
import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.data.repo.PerroRepo;
import com.usb.edadperro.databinding.FragmentPerroFormBinding;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.FotoMascota;
import com.usb.edadperro.util.Recomendaciones;
import com.usb.edadperro.util.Ui;

import java.util.Locale;

/**
 * Registro y edición de los perros asociados al usuario.
 * Mientras se escribe la edad se muestra la equivalencia y las recomendaciones.
 */
public class PerroFormFragment extends Fragment {

    public static final String ARG_ID = "perroId";
    public static final String ARG_EDAD = "edad";
    public static final String ARG_TAMANO = "tamano";

    private FragmentPerroFormBinding b;
    private Perro perro = new Perro();
    private ActivityResultLauncher<String> selectorFoto;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Debe registrarse antes de que el fragmento llegue a RESUMED.
        selectorFoto = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                aplicarFoto(uri);
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle state) {
        b = FragmentPerroFormBinding.inflate(inflater, container, false);
        return b.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        Ui.limpiarErrorAlEscribir(b.tilNombre);
        Ui.limpiarErrorAlEscribir(b.tilEdad);
        Ui.limpiarErrorAlEscribir(b.tilPeso);

        TextWatcher vivo = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Sin acción
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                actualizarResultado();
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Sin acción
            }
        };
        b.etEdad.addTextChangedListener(vivo);
        b.chipsTamano.setOnCheckedStateChangeListener((g, ids) -> actualizarResultado());

        b.btnGuardar.setOnClickListener(v -> guardar());
        b.btnEliminar.setOnClickListener(v -> confirmarEliminar());

        View.OnClickListener elegirFoto = v -> selectorFoto.launch("image/*");
        b.btnFoto.setOnClickListener(elegirFoto);
        b.ivFoto.setOnClickListener(elegirFoto);

        Bundle args = getArguments();
        String id = args == null ? null : args.getString(ARG_ID);
        if (id != null) {
            titulo(R.string.titulo_editar_perro);
            b.btnEliminar.setVisibility(View.VISIBLE);
            PerroRepo.perro(id).get().addOnSuccessListener(d -> {
                Perro p = d.toObject(Perro.class);
                if (p != null && b != null) {
                    perro = p;
                    mostrar(p);
                }
            });
        } else if (args != null && args.containsKey(ARG_EDAD)) {
            // Viene de la calculadora con la edad y el tamaño ya definidos.
            b.etEdad.setText(CalculadoraFragment.formatearEdad(args.getDouble(ARG_EDAD)));
            perro.tamano = args.getString(ARG_TAMANO, CalculadoraEdad.Tamano.MEDIANO.name());
            marcarTamano(perro.tamanoEnum());
        }
    }

    private void titulo(int res) {
        if (getActivity() instanceof AppCompatActivity
                && ((AppCompatActivity) getActivity()).getSupportActionBar() != null) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(res);
        }
    }

    private void mostrar(Perro p) {
        b.etNombre.setText(p.nombre);
        b.etRaza.setText(p.raza);
        b.etEdad.setText(CalculadoraFragment.formatearEdad(p.edad));
        if (p.peso > 0) {
            b.etPeso.setText(CalculadoraFragment.formatearEdad(p.peso));
        }
        b.etNotas.setText(p.notas);
        pintarFoto();
        marcarTamano(p.tamanoEnum());
        if ("Macho".equals(p.sexo)) {
            b.chipMacho.setChecked(true);
        } else if ("Hembra".equals(p.sexo)) {
            b.chipHembra.setChecked(true);
        }
    }

    private void marcarTamano(CalculadoraEdad.Tamano t) {
        b.chipsTamano.check(t == CalculadoraEdad.Tamano.PEQUENO ? R.id.chipPequeno
                : t == CalculadoraEdad.Tamano.GRANDE ? R.id.chipGrande : R.id.chipMediano);
    }

    private CalculadoraEdad.Tamano tamano() {
        int id = b.chipsTamano.getCheckedChipId();
        if (id == R.id.chipPequeno) {
            return CalculadoraEdad.Tamano.PEQUENO;
        }
        if (id == R.id.chipGrande) {
            return CalculadoraEdad.Tamano.GRANDE;
        }
        return CalculadoraEdad.Tamano.MEDIANO;
    }

    /** @return el número del campo, o -1 si no es válido */
    private static double numero(String texto) {
        try {
            return Double.parseDouble(texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void actualizarResultado() {
        double edad = numero(Ui.texto(b.etEdad));
        if (edad <= 0 || edad > CalculadoraEdad.EDAD_MAXIMA) {
            b.cardResultado.setVisibility(View.GONE);
            return;
        }
        CalculadoraEdad.Tamano t = tamano();
        CalculadoraEdad.Etapa etapa = CalculadoraEdad.obtenerEtapa(edad, t);
        b.tvEquivalencia.setText(String.format(Locale.getDefault(), "Equivale a %.0f años humanos · %s",
                CalculadoraEdad.calcularVeterinario(edad, t), TextosPerro.etapa(etapa)));
        TextosPerro.llenarVinetas(requireContext(), b.listaRecomendaciones,
                Recomendaciones.obtener(etapa, t),
                ContextCompat.getColor(requireContext(), R.color.texto_principal));
        b.cardResultado.setVisibility(View.VISIBLE);
    }

    private void guardar() {
        String nombre = Ui.texto(b.etNombre);
        double edad = numero(Ui.texto(b.etEdad));
        String pesoTxt = Ui.texto(b.etPeso);
        double peso = pesoTxt.isEmpty() ? 0 : numero(pesoTxt);

        boolean ok = true;
        if (nombre.length() < 2) {
            b.tilNombre.setError(getString(R.string.error_nombre_perro));
            ok = false;
        }
        if (edad <= 0 || edad > CalculadoraEdad.EDAD_MAXIMA) {
            b.tilEdad.setError(getString(R.string.error_edad_perro, (int) CalculadoraEdad.EDAD_MAXIMA));
            ok = false;
        }
        if (peso < 0 || peso > 120) {
            b.tilPeso.setError(getString(R.string.error_peso));
            ok = false;
        }
        if (!ok) {
            return;
        }
        perro.nombre = nombre;
        perro.raza = Ui.texto(b.etRaza);
        perro.edad = edad;
        perro.peso = peso;
        perro.tamano = tamano().name();
        perro.sexo = b.chipMacho.isChecked() ? "Macho" : b.chipHembra.isChecked() ? "Hembra" : null;
        perro.notas = Ui.texto(b.etNotas);

        b.btnGuardar.setEnabled(false);
        // Firestore guarda primero en la caché local, así funciona también sin conexión.
        PerroRepo.guardar(perro).addOnFailureListener(e -> {
            if (b != null) {
                b.btnGuardar.setEnabled(true);
                Snackbar.make(b.getRoot(), Errores.mensaje(e), Snackbar.LENGTH_LONG).show();
            }
        });
        Toast.makeText(requireContext(), getString(R.string.perro_guardado, nombre), Toast.LENGTH_SHORT).show();
        NavHostFragment.findNavController(this).popBackStack();
    }

    /** Procesa la imagen elegida (recorte 1:1 + reducción) y la muestra. */
    private void aplicarFoto(Uri uri) {
        String base64 = FotoMascota.desdeUri(requireContext(), uri);
        if (base64 == null) {
            if (b != null) {
                Snackbar.make(b.getRoot(), R.string.error_foto, Snackbar.LENGTH_LONG).show();
            }
            return;
        }
        perro.fotoBase64 = base64;
        pintarFoto();
    }

    /** Muestra la foto guardada en el recuadro 1:1, o el ícono por defecto. */
    private void pintarFoto() {
        if (b == null) {
            return;
        }
        Bitmap bmp = FotoMascota.aBitmap(perro.fotoBase64);
        if (bmp != null) {
            b.ivFoto.setPadding(0, 0, 0, 0);
            ImageViewCompat.setImageTintList(b.ivFoto, null);
            b.ivFoto.setImageBitmap(bmp);
            b.btnFoto.setText(R.string.cambiar_foto);
        }
    }

    private void confirmarEliminar() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.eliminar_perro_pregunta, perro.nombre))
                .setMessage(R.string.accion_irreversible)
                .setNegativeButton(R.string.cancelar, null)
                .setPositiveButton(R.string.eliminar, (d, w) -> {
                    PerroRepo.eliminar(perro.id);
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
