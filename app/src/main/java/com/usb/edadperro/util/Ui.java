package com.usb.edadperro.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputLayout;
import com.usb.edadperro.R;

/**
 * Utilidades de interfaz reutilizadas en varias pantallas.
 */
public final class Ui {

    private Ui() {
    }

    /** Texto de un campo sin espacios al inicio ni al final. */
    public static String texto(EditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    /** Quita el mensaje de error de un campo en cuanto el usuario lo corrige. */
    public static void limpiarErrorAlEscribir(TextInputLayout til) {
        if (til.getEditText() == null) {
            return;
        }
        til.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Sin acción
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                til.setError(null);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Sin acción
            }
        });
    }

    public static void ocultarTeclado(Activity a) {
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        View foco = a.getCurrentFocus();
        if (imm != null && foco != null) {
            imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
        }
    }

    /** Carga la imagen de un producto; si no hay URL muestra el ícono genérico. */
    public static void imagenProducto(ImageView iv, String url) {
        Glide.with(iv)
                .load(url == null || url.isEmpty() ? null : url)
                .placeholder(R.drawable.ic_imagen)
                .error(R.drawable.ic_imagen)
                .fitCenter()
                .into(iv);
    }

    /** Abre la app de teléfono con el número marcado (no llama automáticamente). */
    public static void marcar(Context c, String telefono) {
        abrir(c, new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + telefono.replaceAll("[^0-9+]", ""))));
    }

    /** Abre las indicaciones para llegar en la app de mapas instalada. */
    public static void comoLlegar(Context c, double lat, double lon, String nombre) {
        Uri destino = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                + lat + "," + lon + "&travelmode=driving");
        Intent i = new Intent(Intent.ACTION_VIEW, destino);
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
            abrir(c, new Intent(Intent.ACTION_VIEW,
                    Uri.parse("geo:" + lat + "," + lon + "?q=" + lat + "," + lon + "(" + Uri.encode(nombre) + ")")));
        }
    }

    /**
     * Busca lugares cerca de un punto en la app de Google Maps (o en su web si no está instalada).
     * Es un enlace normal, no la API de Google: no requiere clave ni facturación.
     */
    public static void buscarEnMapas(Context c, double lat, double lon, String consulta) {
        // El @lat,lon,zoom del enlace fija la zona; con "geo:" la app a veces la ignora.
        String url = "https://www.google.com/maps/search/" + Uri.encode(consulta)
                + "/@" + lat + "," + lon + ",15z";
        Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse(url)).setPackage("com.google.android.apps.maps");
        try {
            c.startActivity(app);
        } catch (ActivityNotFoundException e) {
            abrirUrl(c, url);
        }
    }

    public static void abrirUrl(Context c, String url) {
        abrir(c, new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private static void abrir(Context c, Intent i) {
        try {
            c.startActivity(i);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(c, R.string.sin_app_para_abrir, Toast.LENGTH_SHORT).show();
        }
    }
}
