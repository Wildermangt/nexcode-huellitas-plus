package com.usb.edadperro.data.modelo;

/**
 * Mensaje del chat con el asistente de IA (solo en memoria).
 */
public class MensajeIA {

    public static final int USUARIO = 0;
    public static final int ASISTENTE = 1;
    public static final int ESCRIBIENDO = 2;

    public final int tipo;
    public final String texto;
    public final boolean urgencia;

    public MensajeIA(int tipo, String texto, boolean urgencia) {
        this.tipo = tipo;
        this.texto = texto;
        this.urgencia = urgencia;
    }
}
