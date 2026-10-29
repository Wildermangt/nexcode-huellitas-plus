package com.usb.edadperro.ui.pago;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.usb.edadperro.R;
import com.usb.edadperro.data.modelo.DetallePedido;
import com.usb.edadperro.data.modelo.Pago;
import com.usb.edadperro.data.modelo.Pedido;
import com.usb.edadperro.util.Formato;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Genera el comprobante de la transacción en PDF (Punto 8) con la API nativa
 * PdfDocument de Android y lo comparte con las apps instaladas.
 */
final class ComprobantePdf {

    private static final int ANCHO = 595;   // A4 en puntos
    private static final int ALTO = 842;
    private static final int MARGEN = 48;

    private ComprobantePdf() {
    }

    static void compartir(Context c, Pedido pedido, List<DetallePedido> detalles, Pago pago) {
        try {
            File archivo = generar(c, pedido, detalles, pago);
            Uri uri = FileProvider.getUriForFile(c, c.getPackageName() + ".archivos", archivo);
            Intent i = new Intent(Intent.ACTION_SEND)
                    .setType("application/pdf")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .putExtra(Intent.EXTRA_SUBJECT, "Comprobante HuellitasPlus")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            c.startActivity(Intent.createChooser(i, c.getString(R.string.compartir_comprobante)));
        } catch (IOException e) {
            Toast.makeText(c, R.string.error_pdf, Toast.LENGTH_LONG).show();
        }
    }

    private static File generar(Context c, Pedido pedido, List<DetallePedido> detalles, Pago pago)
            throws IOException {
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page pagina = doc.startPage(new PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create());
        Canvas cv = pagina.getCanvas();

        Paint titulo = texto(20, true, Color.rgb(0x25, 0x33, 0x4A));
        Paint normal = texto(11, false, Color.rgb(0x1E, 0x28, 0x36));
        Paint gris = texto(10, false, Color.rgb(0x5B, 0x65, 0x73));
        Paint negrita = texto(11, true, Color.rgb(0x1E, 0x28, 0x36));
        Paint verde = texto(13, true, Color.rgb(0x3B, 0x87, 0x22));
        Paint linea = new Paint();
        linea.setColor(Color.rgb(0xE1, 0xE4, 0xDD));

        // Encabezado con el logotipo
        Bitmap logo = BitmapFactory.decodeResource(c.getResources(), R.drawable.marca_huellitas);
        if (logo != null) {
            cv.drawBitmap(logo, null, new Rect(MARGEN, 36, MARGEN + 64, 100), null);
        }
        cv.drawText("HuellitasPlus", MARGEN + 76, 62, titulo);
        cv.drawText("Comprobante de compra", MARGEN + 76, 82, gris);
        cv.drawText("Generado: " + Formato.fechaHora(new Date()), ANCHO - MARGEN - 170, 62, gris);

        int y = 124;
        cv.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        y += 24;
        y = par(cv, "Pedido", pedido.id.substring(0, 8).toUpperCase(), y, gris, negrita);
        y = par(cv, "Fecha del pedido", Formato.fechaHora(pedido.creado), y, gris, normal);
        y = par(cv, "Cliente", pedido.usuarioNombre, y, gris, normal);
        y = par(cv, "Entrega", pedido.direccion + ", " + pedido.ciudad, y, gris, normal);
        y = par(cv, "Teléfono", pedido.telefono, y, gris, normal);
        y = par(cv, "Estado", pedido.estadoLegible(), y, gris, negrita);

        y += 8;
        cv.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        y += 22;
        cv.drawText("Producto", MARGEN, y, negrita);
        cv.drawText("Cant.", 360, y, negrita);
        cv.drawText("Subtotal", ANCHO - MARGEN - 70, y, negrita);
        y += 18;
        for (DetallePedido d : detalles) {
            String nombre = Formato.producto(d.marca, d.nombre) + " (" + d.presentacion + ")";
            if (nombre.length() > 52) {
                nombre = nombre.substring(0, 52) + "…";
            }
            cv.drawText(nombre, MARGEN, y, normal);
            cv.drawText(String.valueOf(d.cantidad), 368, y, normal);
            cv.drawText(Formato.pesos(d.subtotal), ANCHO - MARGEN - 70, y, normal);
            y += 16;
        }
        y += 6;
        cv.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        y += 20;
        y = par(cv, "Subtotal", Formato.pesos(pedido.subtotal), y, gris, normal);
        y = par(cv, "Envío", pedido.envio == 0 ? "Gratis" : Formato.pesos(pedido.envio), y, gris, normal);
        cv.drawText("TOTAL", MARGEN, y + 4, verde);
        cv.drawText(Formato.pesos(pedido.total), 200, y + 4, verde);
        y += 30;

        cv.drawLine(MARGEN, y, ANCHO - MARGEN, y, linea);
        y += 22;
        cv.drawText("Transacción", MARGEN, y, negrita);
        y += 18;
        if (pago == null) {
            cv.drawText("Sin intentos de pago registrados.", MARGEN, y, normal);
            y += 16;
        } else {
            y = par(cv, "Pasarela", "Wompi (ambiente de pruebas / sandbox)", y, gris, normal);
            y = par(cv, "Referencia", pago.referencia, y, gris, normal);
            y = par(cv, "Id de transacción", pago.transaccionId == null ? "-" : pago.transaccionId, y, gris, normal);
            y = par(cv, "Estado del pago", pago.estado, y, gris, negrita);
            y = par(cv, "Método", pago.metodo == null ? "-" : pago.metodo, y, gris, normal);
            y = par(cv, "Monto", Formato.pesos(pago.montoPesos()), y, gris, normal);
            y = par(cv, "Fecha", Formato.fechaHora(pago.actualizado), y, gris, normal);
        }

        cv.drawText("Documento informativo generado por HuellitasPlus. Transacción de prueba: no constituye factura",
                MARGEN, ALTO - 48, gris);
        cv.drawText("ni cobro real. Desarrollado por Nexcode Corporation.", MARGEN, ALTO - 34, gris);

        doc.finishPage(pagina);
        File dir = new File(c.getCacheDir(), "comprobantes");
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("No se pudo crear la carpeta temporal");
        }
        File archivo = new File(dir, "comprobante_" + pedido.id.substring(0, 8) + ".pdf");
        try (FileOutputStream out = new FileOutputStream(archivo)) {
            doc.writeTo(out);
        } finally {
            doc.close();
        }
        return archivo;
    }

    private static int par(Canvas cv, String etiqueta, String valor, int y, Paint pe, Paint pv) {
        cv.drawText(etiqueta, MARGEN, y, pe);
        cv.drawText(valor == null ? "-" : valor, 200, y, pv);
        return y + 18;
    }

    private static Paint texto(float tam, boolean negrita, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tam);
        p.setColor(color);
        p.setTypeface(negrita ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }
}
