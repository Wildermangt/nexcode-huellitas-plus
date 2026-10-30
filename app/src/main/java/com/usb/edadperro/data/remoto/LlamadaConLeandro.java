package com.usb.edadperro.data.remoto;

import android.content.Context;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Llamada de voz con Leandro, el agente de Retell AI compartido por las apps de Nexcode.
 *
 * 1. Le pide la llamada al servidor leandro-voz, que es quien guarda la API key de
 *    Retell (la app nunca la tiene).
 * 2. Con el token que devuelve, se conecta por WebRTC al proxy de Retell. Desde ahí
 *    el audio va y viene solo.
 *
 * Todo el trabajo de red corre en un hilo propio; el {@link Oyente} recibe los cambios
 * de estado en el hilo principal. El permiso RECORD_AUDIO debe estar concedido antes
 * de {@link #iniciar()}.
 */
public class LlamadaConLeandro {

    public enum Estado { INACTIVA, CONECTANDO, EN_CURSO, FALLO }

    /** Recibe los cambios de estado en el hilo principal. */
    public interface Oyente {
        void alCambiar(@NonNull Estado estado, @Nullable String motivo);
    }

    private static final MediaType JSON = MediaType.get("application/json");
    private static final String APP = "huellitas";

    private final Context contexto;
    private final String urlServidor;
    private final Oyente oyente;
    private final OkHttpClient http = new OkHttpClient();
    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final Handler principal = new Handler(Looper.getMainLooper());

    private volatile Estado estado = Estado.INACTIVA;
    private PeerConnectionFactory fabrica;
    private PeerConnection conexion;
    private String urlSesiones = "";
    private String idSesion = "";
    private String token = "";

    public LlamadaConLeandro(@NonNull Context contexto, @NonNull String urlServidor, @NonNull Oyente oyente) {
        this.contexto = contexto.getApplicationContext();
        this.urlServidor = urlServidor;
        this.oyente = oyente;
    }

    public Estado estado() {
        return estado;
    }

    public void iniciar() {
        if (estado == Estado.CONECTANDO || estado == Estado.EN_CURSO) {
            return;
        }
        if (urlServidor.isEmpty()) {
            avisar(Estado.FALLO, "Falta leandro.servidorUrl en local.properties");
            return;
        }
        avisar(Estado.CONECTANDO, null);
        hilo.execute(() -> {
            try {
                conectar();
                avisar(Estado.EN_CURSO, null);
            } catch (Exception e) {
                cerrarLocal();
                avisar(Estado.FALLO, e.getMessage() != null ? e.getMessage() : "No se pudo iniciar la llamada");
            }
        });
    }

    public void colgar() {
        hilo.execute(() -> {
            String sesion = idSesion;
            cerrarLocal();
            if (!sesion.isEmpty()) {
                try {
                    enviar(urlSesiones + "/" + sesion, "DELETE", null);
                } catch (Exception ignorada) {
                    // La llamada ya se cerró localmente; Retell la termina al perder el audio.
                }
            }
            avisar(Estado.INACTIVA, null);
        });
    }

    // ------------------------------------------------------------------------

    private void conectar() throws Exception {
        // 1. Pedir la llamada al servidor
        String pedido = new JSONObject().put("app", APP).toString();
        Request peticion = new Request.Builder()
                .url(urlServidor.replaceAll("/+$", "") + "/llamada")
                .post(RequestBody.create(pedido, JSON))
                .build();
        JSONObject llamada;
        try (Response r = http.newCall(peticion).execute()) {
            if (!r.isSuccessful()) {
                throw new IllegalStateException("El servidor de voz respondió " + r.code());
            }
            llamada = new JSONObject(r.body() != null ? r.body().string() : "{}");
        }
        token = llamada.getString("access_token");
        String idLlamada = llamada.getString("call_id");

        List<PeerConnection.IceServer> ice = new ArrayList<>();
        JSONArray servidores = llamada.optJSONArray("ice_servers");
        for (int i = 0; servidores != null && i < servidores.length(); i++) {
            JSONObject s = servidores.getJSONObject(i);
            Object urls = s.get("urls");
            List<String> lista = new ArrayList<>();
            if (urls instanceof JSONArray) {
                for (int j = 0; j < ((JSONArray) urls).length(); j++) {
                    lista.add(((JSONArray) urls).getString(j));
                }
            } else {
                lista.add(urls.toString());
            }
            ice.add(PeerConnection.IceServer.builder(lista)
                    .setUsername(s.optString("username", ""))
                    .setPassword(s.optString("credential", ""))
                    .createIceServer());
        }

        // 2. Conectar el audio por WebRTC
        audio().setMode(AudioManager.MODE_IN_COMMUNICATION); // activa la cancelación de eco
        PeerConnectionFactory.initialize(
                PeerConnectionFactory.InitializationOptions.builder(contexto).createInitializationOptions());
        fabrica = PeerConnectionFactory.builder()
                .setAudioDeviceModule(JavaAudioDeviceModule.builder(contexto)
                        .setUseHardwareAcousticEchoCanceler(true)
                        .setUseHardwareNoiseSuppressor(true)
                        .createAudioDeviceModule())
                .createPeerConnectionFactory();

        PeerConnection.RTCConfiguration config = new PeerConnection.RTCConfiguration(ice);
        config.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        conexion = fabrica.createPeerConnection(config, new Observador());
        if (conexion == null) {
            throw new IllegalStateException("No se pudo crear la conexión WebRTC");
        }
        conexion.addTrack(fabrica.createAudioTrack("mic", fabrica.createAudioSource(new MediaConstraints())),
                Collections.singletonList("mic"));
        conexion.createDataChannel("control", new DataChannel.Init()); // antes de la oferta

        SessionDescription oferta = crearOferta(conexion);
        fijar(conexion, oferta, true);
        Thread.sleep(1_000); // tiempo para reunir candidatos ICE, como indica Retell

        urlSesiones = "https://api.retellai.com/webrtc-proxy/" + idLlamada + "/v1/webrtc/sessions";
        JSONObject respuesta = new JSONObject(enviar(urlSesiones, "POST", new JSONObject()
                .put("identity", "client")
                .put("sdp", conexion.getLocalDescription().description)
                .toString()));
        idSesion = respuesta.getString("session_id");
        fijar(conexion, new SessionDescription(SessionDescription.Type.ANSWER, respuesta.getString("sdp")), false);
    }

    private synchronized void cerrarLocal() {
        PeerConnection pc = conexion;
        if (pc == null) {
            return; // se limpia primero: close() puede volver a entrar
        }
        conexion = null;
        idSesion = "";
        pc.close();
        if (fabrica != null) {
            fabrica.dispose();
            fabrica = null;
        }
        audio().setMode(AudioManager.MODE_NORMAL);
    }

    private String enviar(String url, String metodo, @Nullable String json) throws Exception {
        Request peticion = new Request.Builder().url(url)
                .method(metodo, json == null ? null : RequestBody.create(json, JSON))
                .header("Authorization", "Bearer " + token)
                .build();
        try (Response r = http.newCall(peticion).execute()) {
            if (!r.isSuccessful()) {
                throw new IllegalStateException("Retell respondió " + r.code());
            }
            return r.body() != null ? r.body().string() : "";
        }
    }

    private AudioManager audio() {
        return (AudioManager) contexto.getSystemService(Context.AUDIO_SERVICE);
    }

    private void avisar(Estado nuevo, @Nullable String motivo) {
        estado = nuevo;
        principal.post(() -> oyente.alCambiar(nuevo, motivo));
    }

    // ---------------- SDP de forma síncrona (se llama desde el hilo propio) ----------------

    private static SessionDescription crearOferta(PeerConnection pc) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<SessionDescription> sdp = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        pc.createOffer(new SdpSimple() {
            @Override public void onCreateSuccess(SessionDescription d) { sdp.set(d); listo.countDown(); }
            @Override public void onCreateFailure(String e) { error.set(e); listo.countDown(); }
        }, new MediaConstraints());
        esperar(listo, error, "Oferta");
        return sdp.get();
    }

    private static void fijar(PeerConnection pc, SessionDescription sdp, boolean local) throws Exception {
        CountDownLatch listo = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        SdpSimple obs = new SdpSimple() {
            @Override public void onSetSuccess() { listo.countDown(); }
            @Override public void onSetFailure(String e) { error.set(e); listo.countDown(); }
        };
        if (local) {
            pc.setLocalDescription(obs, sdp);
        } else {
            pc.setRemoteDescription(obs, sdp);
        }
        esperar(listo, error, "SDP");
    }

    private static void esperar(CountDownLatch listo, AtomicReference<String> error, String paso) throws Exception {
        if (!listo.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException(paso + ": tiempo agotado");
        }
        if (error.get() != null) {
            throw new IllegalStateException(paso + ": " + error.get());
        }
    }

    private static class SdpSimple implements SdpObserver {
        @Override public void onCreateSuccess(SessionDescription d) { }
        @Override public void onSetSuccess() { }
        @Override public void onCreateFailure(String e) { }
        @Override public void onSetFailure(String e) { }
    }

    /** Cuando el agente cuelga o se cae la red, la pantalla se entera aquí. */
    private class Observador implements PeerConnection.Observer {
        @Override public void onConnectionChange(PeerConnection.PeerConnectionState nuevo) {
            if (nuevo == PeerConnection.PeerConnectionState.DISCONNECTED
                    || nuevo == PeerConnection.PeerConnectionState.FAILED
                    || nuevo == PeerConnection.PeerConnectionState.CLOSED) {
                // Nunca cerrar desde el hilo de WebRTC: puede bloquearse.
                hilo.execute(() -> {
                    cerrarLocal();
                    avisar(Estado.INACTIVA, null);
                });
            }
        }
        @Override public void onSignalingChange(PeerConnection.SignalingState s) { }
        @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s) { }
        @Override public void onIceConnectionReceivingChange(boolean b) { }
        @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s) { }
        @Override public void onIceCandidate(IceCandidate c) { }
        @Override public void onIceCandidatesRemoved(IceCandidate[] c) { }
        @Override public void onAddStream(MediaStream m) { }
        @Override public void onRemoveStream(MediaStream m) { }
        @Override public void onDataChannel(DataChannel d) { }
        @Override public void onRenegotiationNeeded() { }
        @Override public void onAddTrack(RtpReceiver r, MediaStream[] m) { }
    }
}
