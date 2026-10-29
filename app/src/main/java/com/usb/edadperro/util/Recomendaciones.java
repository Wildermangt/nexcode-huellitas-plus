package com.usb.edadperro.util;

import com.usb.edadperro.CalculadoraEdad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recomendaciones generales de cuidado según la etapa de vida del perro.
 *
 * Son orientaciones informativas: la app siempre aclara que no reemplazan
 * la valoración de un médico veterinario.
 */
public final class Recomendaciones {

    private Recomendaciones() {
    }

    /**
     * @param etapa  etapa de vida calculada por {@link CalculadoraEdad#obtenerEtapa}
     * @param tamano tamaño de la raza (ajusta algunas recomendaciones)
     * @return lista de recomendaciones en español, nunca vacía
     */
    public static List<String> obtener(CalculadoraEdad.Etapa etapa, CalculadoraEdad.Tamano tamano) {
        List<String> lista = new ArrayList<>();
        switch (etapa) {
            case CACHORRO:
                lista.add("Alimento para cachorro, 3 a 4 raciones pequeñas al día.");
                lista.add("Completa el esquema de vacunas y desparasitación cada 15 días hasta los 3 meses.");
                lista.add("Socialízalo con personas y otros perros desde que el veterinario lo autorice.");
                lista.add("Juegos cortos: su sistema óseo aún está en formación.");
                break;
            case JOVEN:
                lista.add("Pasa a alimento de adulto entre los 12 y 18 meses (más tarde en razas grandes).");
                lista.add("Mucha energía: al menos 1 hora de paseo y juego diario.");
                lista.add("Buen momento para la esterilización, consúltalo con tu veterinario.");
                lista.add("Refuerza el entrenamiento básico con premios saludables.");
                break;
            case ADULTO:
                lista.add("Dos raciones al día de alimento para adulto; controla el peso.");
                lista.add("Chequeo veterinario y refuerzo de vacunas una vez al año.");
                lista.add("Higiene dental: cepillado varias veces por semana.");
                lista.add("Desparasitación interna cada 3 meses y control de pulgas mensual.");
                break;
            default:
                lista.add("Alimento senior: menos calorías y más fibra, apoyo articular.");
                lista.add("Chequeo veterinario cada 6 meses con exámenes de sangre.");
                lista.add("Paseos más cortos y frecuentes; evita el sobreesfuerzo.");
                lista.add("Vigila cambios en apetito, sed, orina o movilidad.");
                break;
        }
        if (tamano == CalculadoraEdad.Tamano.GRANDE) {
            lista.add("Raza grande: cuida las articulaciones y evita el ejercicio intenso justo después de comer.");
        } else if (tamano == CalculadoraEdad.Tamano.PEQUENO) {
            lista.add("Raza pequeña: atención a la salud dental y a la hipoglucemia en ayunos largos.");
        }
        return Collections.unmodifiableList(lista);
    }
}
