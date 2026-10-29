package com.usb.edadperro.ui.perros;

import android.content.Context;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.usb.edadperro.CalculadoraEdad;
import com.usb.edadperro.R;

import java.util.List;
import java.util.Locale;

/**
 * Textos legibles para los datos de un perro.
 */
public final class TextosPerro {

    private TextosPerro() {
    }

    public static String etapa(CalculadoraEdad.Etapa e) {
        switch (e) {
            case CACHORRO:
                return "Cachorro";
            case JOVEN:
                return "Joven";
            case ADULTO:
                return "Adulto";
            default:
                return "Senior";
        }
    }

    public static String tamano(CalculadoraEdad.Tamano t) {
        switch (t) {
            case PEQUENO:
                return "Pequeño";
            case GRANDE:
                return "Grande";
            default:
                return "Mediano";
        }
    }

    /** 5 → "5 años", 1 → "1 año", 0.5 → "6 meses" */
    public static String edad(double anos) {
        if (anos < 1) {
            long meses = Math.max(1, Math.round(anos * 12));
            return meses + (meses == 1 ? " mes" : " meses");
        }
        if (anos == Math.floor(anos)) {
            return (long) anos + (anos == 1 ? " año" : " años");
        }
        return String.format(Locale.getDefault(), "%.1f años", anos);
    }

    /** Llena un contenedor con una lista de viñetas. */
    public static void llenarVinetas(Context c, LinearLayout contenedor, List<String> items, int color) {
        contenedor.removeAllViews();
        for (String s : items) {
            TextView tv = new TextView(c);
            tv.setText("•  " + s);
            tv.setTextColor(color);
            tv.setTextSize(14);
            tv.setLineSpacing(0, 1.15f);
            int pad = (int) (4 * c.getResources().getDisplayMetrics().density);
            tv.setPadding(0, pad, 0, pad);
            contenedor.addView(tv);
        }
    }

    public static int colorTexto(Context c) {
        return ContextCompat.getColor(c, R.color.texto_principal);
    }
}
