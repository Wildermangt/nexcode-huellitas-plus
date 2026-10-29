package com.usb.edadperro.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Detecta en un texto señales de una posible urgencia veterinaria.
 *
 * Es la primera capa de seguridad del chat con IA: funciona sin conexión y
 * antes de recibir la respuesta del modelo. La segunda capa es la etiqueta
 * [URGENCIA] que el asistente añade cuando identifica una emergencia.
 */
public final class DetectorUrgencias {

    /** Etiqueta que el modelo de IA incluye cuando detecta una urgencia. */
    public static final String ETIQUETA_IA = "[URGENCIA]";

    /** Señal detectada con su motivo legible. */
    private static final class Senal {
        final Pattern patron;
        final String motivo;

        Senal(String regex, String motivo) {
            this.patron = Pattern.compile(regex);
            this.motivo = motivo;
        }
    }

    // Los patrones se escriben sin tildes porque el texto se normaliza antes.
    private static final List<Senal> SENALES = new ArrayList<>();

    static {
        SENALES.add(new Senal("envenen|intoxic|veneno|raticida|matarrata|insecticida|anticongelante",
                "posible intoxicación"));
        SENALES.add(new Senal("(comio|trago|ingirio|se comio|se trago)\\b.*\\b(chocolate|xilitol|uvas?|pasas|cebolla|ajo|medicamento|pastillas?|ibuprofeno|acetaminofen|paracetamol|hueso|juguete|media|calcetin)",
                "ingesta de algo peligroso"));
        SENALES.add(new Senal("convuls|ataque epilep|temblores fuertes|se desmay|desmayo|inconsciente|no reacciona|no responde|colaps|no se mueve|no (se )?puede (levantar|parar)",
                "alteración neurológica o pérdida de conciencia"));
        SENALES.add(new Senal("no (puede )?respira|le cuesta respirar|dificultad (para|al) respirar|se ahoga|ahogando|encias (moradas|azules|blancas|palidas)",
                "dificultad respiratoria"));
        SENALES.add(new Senal("sangra mucho|sangrado (abundante|que no para)|hemorragia|vomit\\w* (con )?sangre|(heces|popo|diarrea|orina) con sangre",
                "sangrado"));
        SENALES.add(new Senal("atropell|lo golpeo un carro|accidente|fractura|hueso roto|se cayo de (un|una|el|la) (segundo|tercer|piso|balcon|altura)",
                "traumatismo"));
        SENALES.add(new Senal("abdomen (hinchado|inflamado|distendido)|barriga (hinchada|inflamada)|torsion gastrica|intenta vomitar y no puede|arcadas sin vomitar",
                "posible torsión gástrica"));
        SENALES.add(new Senal("golpe de calor|insolacion|quemadura|electrocut|mordedura de serpiente|lo mordio una serpiente|picadura de (abeja|avispa).*(hinch|respir)",
                "lesión grave"));
        SENALES.add(new Senal("no (puede|logra) orinar|no orina hace|parto (complicado|dificil)|lleva horas pujando",
                "obstrucción o parto complicado"));
    }

    private DetectorUrgencias() {
    }

    /** Pasa a minúsculas y elimina tildes para comparar de forma flexible. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase(Locale.ROOT);
    }

    /** @return true si el texto describe una posible urgencia */
    public static boolean esUrgencia(String texto) {
        return !motivos(texto).isEmpty();
    }

    /** @return motivos de urgencia encontrados (lista vacía si no hay) */
    public static List<String> motivos(String texto) {
        String t = normalizar(texto);
        if (t.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> encontrados = new ArrayList<>();
        for (Senal s : SENALES) {
            if (s.patron.matcher(t).find() && !encontrados.contains(s.motivo)) {
                encontrados.add(s.motivo);
            }
        }
        return encontrados;
    }

    /** @return true si la respuesta de la IA trae la etiqueta de urgencia */
    public static boolean respuestaMarcaUrgencia(String respuestaIa) {
        return respuestaIa != null && respuestaIa.contains(ETIQUETA_IA);
    }

    /** Quita la etiqueta interna para mostrar la respuesta al usuario. */
    public static String limpiarRespuesta(String respuestaIa) {
        if (respuestaIa == null) {
            return "";
        }
        return respuestaIa.replace(ETIQUETA_IA, "").trim();
    }
}
