package com.usb.edadperro;

/**
 * Clase que contiene toda la lógica de conversión entre la edad de un perro
 * y su equivalencia aproximada en años humanos.
 *
 * Se separa de la Activity para que la lógica pueda probarse de forma
 * independiente de la interfaz gráfica.
 *
 * Autores: Jeferson Wilderman González Tenjo - José Leandro Ocampo Camacho
 * Asignatura: Optativa II - Desarrollo Móvil
 */
public class CalculadoraEdad {

    /** Edad máxima admitida para un perro (el récord documentado es de 29 años). */
    public static final double EDAD_MAXIMA = 30.0;

    /** Tamaños de raza admitidos por el método veterinario. */
    public enum Tamano {
        PEQUENO(4.0),   // hasta 10 kg
        MEDIANO(5.0),   // entre 10 y 25 kg
        GRANDE(6.0);    // más de 25 kg

        /** Años humanos que suma cada año de perro después del segundo. */
        private final double anosPorAnoAdicional;

        Tamano(double anosPorAnoAdicional) {
            this.anosPorAnoAdicional = anosPorAnoAdicional;
        }

        public double getAnosPorAnoAdicional() {
            return anosPorAnoAdicional;
        }
    }

    /** Etapas de vida del perro según su edad en años humanos equivalentes. */
    public enum Etapa {
        CACHORRO, JOVEN, ADULTO, SENIOR
    }

    // Constantes del método veterinario
    private static final double PRIMER_ANO = 15.0;  // el 1er año equivale a 15 humanos
    private static final double SEGUNDO_ANO = 9.0;  // el 2do año suma 9 humanos más

    // Constante del método tradicional
    private static final double FACTOR_TRADICIONAL = 7.0;

    // Constantes de la fórmula epigenética (Wang et al., UCSD 2020)
    private static final double EPI_FACTOR = 16.0;
    private static final double EPI_CONSTANTE = 31.0;

    /**
     * Método veterinario: es el cálculo principal de la aplicación y el más
     * aproximado a la realidad biológica del animal.
     *
     * - El primer año de vida equivale a 15 años humanos.
     * - El segundo año suma 9 años humanos más (total 24).
     * - A partir del tercer año, cada año suma entre 4 y 6 años humanos
     *   dependiendo del tamaño de la raza (los perros grandes envejecen antes).
     *
     * @param edadPerro edad del perro en años (admite decimales, ej. 0.5)
     * @param tamano    tamaño de la raza
     * @return equivalencia en años humanos
     */
    public static double calcularVeterinario(double edadPerro, Tamano tamano) {
        if (edadPerro <= 1) {
            // Antes del primer año la equivalencia es proporcional.
            return edadPerro * PRIMER_ANO;
        }
        if (edadPerro <= 2) {
            return PRIMER_ANO + (edadPerro - 1) * SEGUNDO_ANO;
        }
        double base = PRIMER_ANO + SEGUNDO_ANO; // 24 años humanos a los 2 años
        return base + (edadPerro - 2) * tamano.getAnosPorAnoAdicional();
    }

    /**
     * Método tradicional: la regla popular de multiplicar por 7.
     * Se incluye únicamente como referencia comparativa.
     *
     * @param edadPerro edad del perro en años
     * @return equivalencia en años humanos
     */
    public static double calcularTradicional(double edadPerro) {
        return edadPerro * FACTOR_TRADICIONAL;
    }

    /**
     * Fórmula epigenética publicada por la Universidad de California en 2020,
     * basada en la metilación del ADN: edadHumana = 16 * ln(edadPerro) + 31.
     *
     * Solo tiene sentido a partir de las pocas semanas de vida, por lo que se
     * devuelve 0 cuando la edad es demasiado pequeña.
     *
     * @param edadPerro edad del perro en años
     * @return equivalencia en años humanos
     */
    public static double calcularEpigenetico(double edadPerro) {
        if (edadPerro <= 0) {
            return 0;
        }
        double resultado = EPI_FACTOR * Math.log(edadPerro) + EPI_CONSTANTE;
        return Math.max(resultado, 0);
    }

    /**
     * Determina la etapa de vida del perro a partir de su edad real y su tamaño.
     *
     * @param edadPerro edad del perro en años
     * @param tamano    tamaño de la raza
     * @return etapa de vida correspondiente
     */
    public static Etapa obtenerEtapa(double edadPerro, Tamano tamano) {
        double edadHumana = calcularVeterinario(edadPerro, tamano);
        if (edadHumana < 15) {
            return Etapa.CACHORRO;
        }
        if (edadHumana < 30) {
            return Etapa.JOVEN;
        }
        if (edadHumana < 60) {
            return Etapa.ADULTO;
        }
        return Etapa.SENIOR;
    }
}
