package com.usb.edadperro.data.remoto;

import androidx.annotation.Nullable;

import com.usb.edadperro.BuildConfig;
import com.usb.edadperro.util.Errores;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Cliente del backend de pagos desplegado en Vercel (carpeta backend-wompi).
 *
 * La app nunca conoce los secretos de Wompi: el backend valida la sesión
 * de Firebase, recalcula el total del pedido, firma la transacción y es el
 * único que actualiza los estados de pago en Firestore.
 *
 * Métodos bloqueantes: llamarlos desde un hilo de fondo.
 */
public final class PagosServicio {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private PagosServicio() {
    }

    /** Datos que el backend devuelve para abrir el Web Checkout de Wompi. */
    public static final class Checkout {
        public String urlCheckout;
        public String urlRetorno;
        public String referencia;
        public long montoCentavos;
    }

    /** Estado que devuelve el backend cuando el usuario salió del checkout sin pagar. */
    public static final String SIN_TRANSACCION = "SIN_TRANSACCION";

    /** Estado de una transacción verificada por el backend. */
    public static final class Verificacion {
        public String estado;
        public String estadoPedido;
        public String metodo;
        public String mensaje;
    }

    public static boolean configurado() {
        return !BuildConfig.BACKEND_URL.isEmpty();
    }

    /** Pide al backend la firma de integridad y la URL de pago para un pedido. */
    public static Checkout iniciarPago(String pedidoId, String idToken) throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("pedidoId", pedidoId);
            JSONObject r = post("/api/wompi/firma", body, idToken);
            Checkout c = new Checkout();
            c.urlCheckout = r.getString("urlCheckout");
            c.urlRetorno = r.getString("urlRetorno");
            c.referencia = r.getString("referencia");
            c.montoCentavos = r.getLong("montoCentavos");
            return c;
        } catch (JSONException e) {
            throw new Errores.Amigable("Respuesta inválida del servidor de pagos", e);
        }
    }

    /**
     * Consulta en Wompi (a través del backend) el estado real de la transacción.
     * Con transaccionId null, el backend la busca por la referencia del pedido.
     */
    public static Verificacion verificar(String pedidoId, @Nullable String transaccionId, String idToken)
            throws IOException {
        JSONObject body = new JSONObject();
        try {
            body.put("pedidoId", pedidoId);
            if (transaccionId != null) {
                body.put("transaccionId", transaccionId);
            }
            JSONObject r = post("/api/wompi/verificar", body, idToken);
            Verificacion v = new Verificacion();
            v.estado = r.optString("estado");
            v.estadoPedido = r.optString("estadoPedido");
            v.metodo = r.optString("metodo");
            v.mensaje = r.optString("mensaje");
            return v;
        } catch (JSONException e) {
            throw new Errores.Amigable("Respuesta inválida del servidor de pagos", e);
        }
    }

    private static JSONObject post(String ruta, JSONObject body, String idToken)
            throws IOException, JSONException {
        if (!configurado()) {
            throw new Errores.Amigable("El servidor de pagos no está configurado "
                    + "(falta huellita.backendUrl en local.properties).");
        }
        Request req = new Request.Builder()
                .url(BuildConfig.BACKEND_URL + ruta)
                .header("Authorization", "Bearer " + idToken)
                .post(RequestBody.create(body.toString(), JSON))
                .build();
        try (Response r = Http.cliente().newCall(req).execute()) {
            String texto = r.body() == null ? "" : r.body().string();
            JSONObject json = texto.isEmpty() ? new JSONObject() : new JSONObject(texto);
            if (!r.isSuccessful()) {
                throw new Errores.Amigable(json.optString("error",
                        "El servidor de pagos respondió con error " + r.code()));
            }
            return json;
        }
    }
}
