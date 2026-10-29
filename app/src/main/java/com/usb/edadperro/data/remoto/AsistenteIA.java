package com.usb.edadperro.data.remoto;

import androidx.annotation.NonNull;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ai.FirebaseAI;
import com.google.firebase.ai.GenerativeModel;
import com.google.firebase.ai.java.ChatFutures;
import com.google.firebase.ai.java.GenerativeModelFutures;
import com.google.firebase.ai.type.Content;
import com.google.firebase.ai.type.GenerateContentResponse;
import com.google.firebase.ai.type.GenerationConfig;
import com.google.firebase.ai.type.GenerativeBackend;
import com.google.firebase.ai.type.QuotaExceededException;
import com.google.firebase.ai.type.RequestTimeoutException;
import com.google.firebase.ai.type.ServerException;
import com.usb.edadperro.BuildConfig;
import com.usb.edadperro.util.DetectorUrgencias;
import com.usb.edadperro.util.Hilos;

import java.util.concurrent.Executor;

/**
 * Asistente de cuidado canino con Gemini a través de Firebase AI Logic (Punto 4).
 *
 * Firebase AI Logic llama a la Gemini Developer API desde el proyecto de
 * Firebase, así la API key nunca se incluye en el APK. El plan gratuito
 * (Spark) permite usarlo sin tarjeta de crédito.
 */
public class AsistenteIA {

    /** Respuesta del asistente lista para mostrar. */
    public interface Callback {
        void alResponder(String texto, boolean urgencia);

        void alFallar(Throwable error);
    }

    /** Instrucciones de sistema: definen el rol, el alcance y el protocolo de urgencias. */
    private static final String INSTRUCCIONES =
            "Eres Huellitas, el asistente de cuidado canino de la app HuellitasPlus (Colombia). "
                    + "Responde SIEMPRE en español, con un tono cálido, claro y breve "
                    + "(máximo 180 palabras), usando viñetas cuando ayuden.\n"
                    + "Temas permitidos: alimentación, higiene, ejercicio, comportamiento, vacunas, "
                    + "desparasitación y cuidados generales de PERROS. Si preguntan por otra especie "
                    + "o por temas ajenos, indícalo amablemente y vuelve al cuidado de perros.\n"
                    + "Nunca des diagnósticos definitivos ni dosis de medicamentos.\n"
                    + "PROTOCOLO DE URGENCIAS: si la consulta describe una posible urgencia "
                    + "(intoxicación o ingesta de algo tóxico, convulsiones, dificultad para respirar, "
                    + "sangrado abundante, atropello o trauma, abdomen hinchado con arcadas, golpe de "
                    + "calor, desmayo, imposibilidad de orinar, parto complicado), empieza tu respuesta "
                    + "EXACTAMENTE con la etiqueta " + DetectorUrgencias.ETIQUETA_IA + ", recomienda "
                    + "acudir de inmediato a una veterinaria de urgencias y da solo primeros auxilios "
                    + "seguros mientras llega. Recuerda que la app tiene un botón para encontrar "
                    + "veterinarias cercanas.\n"
                    + "Termina recordando, en una línea corta, que tu orientación no sustituye la "
                    + "valoración de un médico veterinario.";

    private static final int REINTENTOS = 1;
    private static final long PAUSA_REINTENTO_MS = 2000;

    private final ChatFutures chat;
    private final Executor ejecutorUi;

    /**
     * @param contextoPerros descripción de los perros del usuario (puede ser vacía),
     *                       para personalizar las respuestas
     * @param ejecutorUi     ejecutor del hilo principal para entregar las respuestas
     */
    public AsistenteIA(String contextoPerros, Executor ejecutorUi) {
        this.ejecutorUi = ejecutorUi;
        String sistema = INSTRUCCIONES;
        if (contextoPerros != null && !contextoPerros.isEmpty()) {
            sistema += "\nPerros registrados por el usuario: " + contextoPerros;
        }
        GenerationConfig.Builder config = new GenerationConfig.Builder();
        config.setTemperature(0.6f);
        config.setMaxOutputTokens(2048);
        GenerativeModel modelo = FirebaseAI.getInstance(GenerativeBackend.googleAI())
                .generativeModel(BuildConfig.GEMINI_MODEL, config.build(), null, null, null,
                        new Content.Builder().addText(sistema).build());
        chat = GenerativeModelFutures.from(modelo).startChat();
    }

    /** Envía una pregunta manteniendo el historial de la conversación. */
    public void preguntar(String pregunta, Callback callback) {
        Content mensaje = new Content.Builder().setRole("user").addText(pregunta).build();
        enviar(mensaje, callback, REINTENTOS);
    }

    /**
     * El nivel gratuito de Gemini responde a veces "alta demanda" (500/503) o 429.
     * Son fallos pasajeros: se reintenta una vez tras una pausa corta. El SDK solo
     * guarda el mensaje en el historial cuando hay respuesta, así que no se duplica.
     */
    private static boolean esTemporal(Throwable t) {
        return t instanceof ServerException || t instanceof QuotaExceededException
                || t instanceof RequestTimeoutException;
    }

    private void enviar(Content mensaje, Callback callback, int reintentos) {
        ListenableFuture<GenerateContentResponse> futuro = chat.sendMessage(mensaje);
        Futures.addCallback(futuro, new FutureCallback<GenerateContentResponse>() {
            @Override
            public void onSuccess(GenerateContentResponse resultado) {
                String texto = resultado.getText();
                if (texto == null || texto.trim().isEmpty()) {
                    callback.alFallar(new IllegalStateException(
                            "El asistente no generó una respuesta. Intenta reformular la pregunta."));
                    return;
                }
                boolean urgencia = DetectorUrgencias.respuestaMarcaUrgencia(texto);
                callback.alResponder(DetectorUrgencias.limpiarRespuesta(texto), urgencia);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                if (reintentos > 0 && esTemporal(t)) {
                    Hilos.principal(() -> enviar(mensaje, callback, reintentos - 1), PAUSA_REINTENTO_MS);
                    return;
                }
                callback.alFallar(t);
            }
        }, ejecutorUi);
    }
}
