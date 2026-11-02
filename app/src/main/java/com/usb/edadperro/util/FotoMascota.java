package com.usb.edadperro.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Prepara la foto de una mascota: la recorta a un cuadrado 1:1 centrado, la
 * reduce y la devuelve en Base64 (JPEG) para guardarla en el propio documento
 * de Firestore, sin usar Firebase Storage (que exige plan de pago).
 */
public final class FotoMascota {

    /** Lado del cuadrado final en píxeles. */
    private static final int LADO = 512;
    /** Calidad JPEG (0-100). 80 mantiene el Base64 muy por debajo de 1 MiB. */
    private static final int CALIDAD = 80;

    private FotoMascota() {
    }

    /**
     * Lee la imagen del Uri, la recorta a 1:1 centrado, la reduce a {@link #LADO}
     * y la devuelve en Base64. Devuelve {@code null} si no se pudo procesar.
     */
    public static String desdeUri(Context ctx, Uri uri) {
        try (InputStream in = ctx.getContentResolver().openInputStream(uri)) {
            Bitmap original = BitmapFactory.decodeStream(in);
            if (original == null) {
                return null;
            }
            Bitmap cuadrado = recortarCuadrado(original);
            Bitmap escalado = Bitmap.createScaledBitmap(cuadrado, LADO, LADO, true);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            escalado.compress(Bitmap.CompressFormat.JPEG, CALIDAD, out);
            return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    /** Recorta al cuadrado centrado más grande posible (relación 1:1). */
    private static Bitmap recortarCuadrado(Bitmap src) {
        int lado = Math.min(src.getWidth(), src.getHeight());
        int x = (src.getWidth() - lado) / 2;
        int y = (src.getHeight() - lado) / 2;
        return Bitmap.createBitmap(src, x, y, lado, lado);
    }

    /** Decodifica una foto en Base64 a Bitmap, o {@code null} si está vacía o es inválida. */
    public static Bitmap aBitmap(String base64) {
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        try {
            byte[] bytes = Base64.decode(base64, Base64.NO_WRAP);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }
}
