package com.usb.edadperro.ui.ia;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.usb.edadperro.data.modelo.MensajeIA;
import com.usb.edadperro.data.modelo.Perro;
import com.usb.edadperro.data.remoto.AsistenteIA;
import com.usb.edadperro.data.repo.PerroRepo;
import com.usb.edadperro.ui.perros.TextosPerro;
import com.usb.edadperro.util.DetectorUrgencias;
import com.usb.edadperro.util.Errores;
import com.usb.edadperro.util.Hilos;

import java.util.ArrayList;
import java.util.List;

/**
 * Estado del chat con IA. Vive mientras la app esté abierta, así la
 * conversación no se pierde al girar la pantalla o cambiar de pestaña.
 */
public class AsistenteViewModel extends ViewModel {

    /** Evento de urgencia: motivo detectado (o null si se cerró el banner). */
    private final MutableLiveData<String> urgencia = new MutableLiveData<>();
    private final MutableLiveData<List<MensajeIA>> mensajes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> esperando = new MutableLiveData<>(false);
    private AsistenteIA asistente;

    public AsistenteViewModel() {
        List<MensajeIA> inicial = new ArrayList<>();
        inicial.add(new MensajeIA(MensajeIA.ASISTENTE,
                "¡Hola! Soy **Huellitas**, tu asistente de cuidado canino. Pregúntame sobre "
                        + "alimentación, higiene, vacunas, comportamiento o cuidados generales de tu perro.",
                false));
        mensajes.setValue(inicial);
        prepararAsistente();
    }

    LiveData<List<MensajeIA>> mensajes() {
        return mensajes;
    }

    LiveData<String> urgencia() {
        return urgencia;
    }

    LiveData<Boolean> esperando() {
        return esperando;
    }

    void cerrarUrgencia() {
        urgencia.setValue(null);
    }

    /** Crea el asistente con el contexto de los perros del usuario. */
    private void prepararAsistente() {
        PerroRepo.misPerros().get().addOnCompleteListener(t -> {
            StringBuilder sb = new StringBuilder();
            if (t.isSuccessful()) {
                for (DocumentSnapshot d : t.getResult().getDocuments()) {
                    Perro p = d.toObject(Perro.class);
                    if (p == null) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(p.nombre).append(" (")
                            .append(p.raza == null || p.raza.isEmpty() ? "raza no indicada" : p.raza)
                            .append(", ").append(TextosPerro.edad(p.edad))
                            .append(", tamaño ").append(TextosPerro.tamano(p.tamanoEnum()).toLowerCase())
                            .append(", etapa ").append(TextosPerro.etapa(p.etapa()).toLowerCase())
                            .append(")");
                }
            }
            try {
                asistente = new AsistenteIA(sb.toString(), Hilos::principal);
            } catch (Exception e) {
                asistente = null;
            }
        });
    }

    void enviar(String pregunta) {
        List<MensajeIA> lista = new ArrayList<>(mensajes.getValue());
        // Capa 1: detección local de urgencias, inmediata y sin conexión.
        List<String> motivos = DetectorUrgencias.motivos(pregunta);
        boolean urgente = !motivos.isEmpty();
        lista.add(new MensajeIA(MensajeIA.USUARIO, pregunta, urgente));
        if (urgente) {
            urgencia.setValue("Posible " + motivos.get(0) + ". Busca atención veterinaria de inmediato.");
        }
        lista.add(new MensajeIA(MensajeIA.ESCRIBIENDO, "", false));
        mensajes.setValue(lista);
        esperando.setValue(true);

        if (asistente == null) {
            responderError("El asistente aún se está preparando o la IA no está habilitada en el "
                    + "proyecto de Firebase. Intenta de nuevo en unos segundos.");
            prepararAsistente();
            return;
        }
        asistente.preguntar(pregunta, new AsistenteIA.Callback() {
            @Override
            public void alResponder(String texto, boolean urgenciaIa) {
                // Capa 2: el modelo marcó la respuesta como urgencia.
                if (urgenciaIa && urgencia.getValue() == null) {
                    urgencia.setValue("El asistente identificó una posible urgencia. "
                            + "Busca atención veterinaria de inmediato.");
                }
                reemplazarEscribiendo(new MensajeIA(MensajeIA.ASISTENTE, texto, urgenciaIa));
            }

            @Override
            public void alFallar(Throwable error) {
                responderError(Errores.mensaje(error));
            }
        });
    }

    private void responderError(String detalle) {
        reemplazarEscribiendo(new MensajeIA(MensajeIA.ASISTENTE,
                "No pude responder en este momento. " + detalle, false));
    }

    private void reemplazarEscribiendo(MensajeIA nuevo) {
        List<MensajeIA> lista = new ArrayList<>(mensajes.getValue());
        if (!lista.isEmpty() && lista.get(lista.size() - 1).tipo == MensajeIA.ESCRIBIENDO) {
            lista.remove(lista.size() - 1);
        }
        lista.add(nuevo);
        mensajes.setValue(lista);
        esperando.setValue(false);
    }
}
