package com.usb.edadperro.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reglas del marketplace: solo se admiten alimentos para perros
 * (concentrados, alimentos húmedos y snacks comestibles caninos).
 *
 * Las mismas reglas se aplican en firestore.rules para que no puedan
 * saltarse desde fuera de la app.
 */
public final class ValidadorCatalogo {

    /** Única especie admitida en la tienda. */
    public static final String ESPECIE = "perro";

    /** Categorías válidas del catálogo (id del documento en Firestore). */
    public static final List<String> CATEGORIAS = Arrays.asList("concentrado", "humedo", "snacks");

    // Palabras que indican un producto para otra especie o que no es alimento.
    private static final Pattern OTRAS_ESPECIES = Pattern.compile(
            "\\b(gato|gatos|gatito|felino|felinos|cat|kitten|ave|aves|pajaro|loro|pez|peces|"
                    + "conejo|hamster|roedor|caballo|equino|cerdo|tortuga)\\b");
    private static final Pattern NO_ALIMENTO = Pattern.compile(
            "\\b(shampoo|champu|collar|correa|juguete|cama|arnes|ropa|cepillo|arena|"
                    + "pipeta|antipulgas|medicamento|transportador|comedero)\\b");

    private ValidadorCatalogo() {
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Valida los datos de un producto antes de guardarlo.
     *
     * @return lista de errores; vacía si el producto es válido
     */
    public static List<String> validar(String nombre, String marca, String descripcion,
                                       String presentacion, String categoriaId, String especie,
                                       long precio, long stock) {
        List<String> errores = new ArrayList<>();
        if (nombre == null || nombre.trim().length() < 3) {
            errores.add("El nombre debe tener al menos 3 caracteres");
        }
        if (marca == null || marca.trim().isEmpty()) {
            errores.add("La marca es obligatoria");
        }
        if (presentacion == null || presentacion.trim().isEmpty()) {
            errores.add("La presentación es obligatoria (ej. bulto 8 kg)");
        }
        if (!ESPECIE.equals(especie)) {
            errores.add("Solo se admiten productos para perros");
        }
        if (categoriaId == null || !CATEGORIAS.contains(categoriaId)) {
            errores.add("La categoría debe ser concentrado, húmedo o snacks");
        }
        if (precio <= 0) {
            errores.add("El precio debe ser mayor que cero");
        }
        if (stock < 0) {
            errores.add("El inventario no puede ser negativo");
        }
        String texto = normalizar(nombre + " " + marca + " " + descripcion + " " + presentacion);
        if (OTRAS_ESPECIES.matcher(texto).find()) {
            errores.add("El producto parece ser para otra especie; la tienda es solo para perros");
        }
        if (NO_ALIMENTO.matcher(texto).find()) {
            errores.add("La tienda solo vende alimentos (concentrados, húmedos y snacks)");
        }
        return errores;
    }
}
