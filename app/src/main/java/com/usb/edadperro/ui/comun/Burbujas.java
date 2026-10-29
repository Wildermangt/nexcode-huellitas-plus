package com.usb.edadperro.ui.comun;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.usb.edadperro.R;
import com.usb.edadperro.databinding.ItemBurbujaBinding;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configura una burbuja de chat como propia (derecha) o ajena (izquierda),
 * y convierte el formato Markdown básico de las respuestas de la IA.
 */
public final class Burbujas {

    /** Grupo 1: "**negrita**"; grupo 2: "*cursiva*" (sin espacios junto a los asteriscos). */
    private static final Pattern ENFASIS =
            Pattern.compile("\\*\\*(.+?)\\*\\*|(?<![*\\w])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![*\\w])");

    private Burbujas() {
    }

    public static void configurar(ItemBurbujaBinding b, boolean mia, boolean conAvatar) {
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) b.burbuja.getLayoutParams();
        int ancho = (int) (b.getRoot().getResources().getDisplayMetrics().widthPixels * 0.78);
        b.tvTexto.setMaxWidth(ancho);
        b.contenedor.setGravity(mia ? Gravity.END : Gravity.START);
        b.burbuja.setBackgroundResource(mia ? R.drawable.burbuja_mia : R.drawable.burbuja_otro);
        b.burbuja.setLayoutParams(lp);
        b.ivAvatar.setVisibility(conAvatar && !mia ? View.VISIBLE : View.GONE);
    }

    /** "**texto**" → negrita, "*texto*" → cursiva y "* item" → "• item". */
    public static CharSequence markdown(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto
                .replaceAll("(?m)^\\s*[*-]\\s+", "•  ")
                .replaceAll("(?m)^#{1,6}\\s*", "");
        SpannableStringBuilder sb = new SpannableStringBuilder();
        Matcher m = ENFASIS.matcher(limpio);
        int ultimo = 0;
        while (m.find()) {
            sb.append(limpio, ultimo, m.start());
            int inicio = sb.length();
            boolean negrita = m.group(1) != null;
            sb.append(negrita ? m.group(1) : m.group(2));
            sb.setSpan(new StyleSpan(negrita ? Typeface.BOLD : Typeface.ITALIC), inicio, sb.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            ultimo = m.end();
        }
        sb.append(limpio.substring(ultimo));
        return sb;
    }
}
