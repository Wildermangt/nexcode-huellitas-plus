package com.usb.edadperro.data.repo;

/**
 * Catálogo inicial de la tienda: 14 alimentos para perros de marcas reales.
 *
 * Las imágenes provienen de Open Pet Food Facts (base de datos abierta,
 * licencia CC BY-SA) y corresponden al código de barras de cada producto.
 * Los precios son de referencia para el entorno de pruebas (sandbox).
 */
final class CatalogoInicial {

    static final String URL_IMAGENES = "https://images.openpetfoodfacts.org/images/products/";

    private CatalogoInicial() {
    }

    /** {id, categoría, nombre, marca, presentación, precio, stock, descripción, imagen} */
    static final Object[][] PRODUCTOS = {
            {"rc-medium-adult", "concentrado", "Medium Adult", "Royal Canin", "Bulto 12 kg",
                    369_900L, 8L,
                    "Alimento seco para perros adultos de raza mediana (11 a 25 kg) de 12 meses a 7 años. "
                            + "Favorece las defensas naturales y la digestión.",
                    "318/255/106/5367/front_fr.3.400.jpg"},
            {"rc-mini-adult", "concentrado", "Mini Adult", "Royal Canin", "Bolsa 4 kg",
                    159_900L, 12L,
                    "Croquetas pequeñas para perros adultos de raza pequeña (hasta 10 kg). "
                            + "Ayuda a mantener el peso ideal.",
                    "318/255/105/5719/front_fr.3.400.jpg"},
            {"dogchow-large-adult", "concentrado", "Dog Chow Adultos Raza Grande", "Purina", "Bulto 14 kg",
                    189_900L, 15L,
                    "Alimento completo para perros adultos de raza grande, con proteína de calidad "
                            + "para músculos fuertes y articulaciones sanas.",
                    "761/303/448/7926/front_fr.3.400.jpg"},
            {"proplan-small-mini", "concentrado", "Pro Plan Small & Mini Adult", "Purina", "Bolsa 3 kg",
                    139_900L, 10L,
                    "Nutrición avanzada para perros adultos pequeños y miniatura, con pollo como "
                            + "primer ingrediente.",
                    "761/303/512/0778/front_fr.8.400.jpg"},
            {"one-mini-adult", "concentrado", "Purina One Mini Adult", "Purina", "Bolsa 1,5 kg",
                    52_900L, 20L,
                    "Alimento para perros adultos de menos de 10 kg, con fibras naturales para una "
                            + "buena digestión.",
                    "761/303/414/6656/front_fr.27.400.jpg"},
            {"pedigree-puppy", "concentrado", "Pedigree Cachorros", "Pedigree", "Bolsa 2,8 kg",
                    49_900L, 18L,
                    "Alimento para cachorros con calcio y DHA para el desarrollo de huesos y cerebro.",
                    "890/600/248/3006/front_en.3.400.jpg"},
            {"pedigree-pouch-pollo", "humedo", "Sobre Pollo a la Parrilla en Salsa", "Pedigree", "Sobre 100 g",
                    4_900L, 60L,
                    "Alimento húmedo completo para perros adultos, trozos de pollo en salsa.",
                    "002/310/011/7973/front_en.5.400.jpg"},
            {"pedigree-pouch-filet", "humedo", "Sobre Filet Mignon", "Pedigree", "Sobre 100 g",
                    4_900L, 45L,
                    "Alimento húmedo para perros adultos sabor filet mignon, ideal para mezclar con "
                            + "el concentrado.",
                    "002/310/011/9007/front_en.7.400.jpg"},
            {"rc-mini-pouch", "humedo", "Mini Adult en Salsa", "Royal Canin", "Sobre 85 g",
                    7_900L, 40L,
                    "Trozos en salsa para perros adultos de raza pequeña, complemento del alimento seco.",
                    "900/357/900/8256/front_es.16.400.jpg"},
            {"cesar-clasicos", "humedo", "Cesar Clásicos", "Cesar", "Bandeja 100 g",
                    6_500L, 35L,
                    "Paté húmedo para perros adultos de raza pequeña, en práctica bandeja de una porción.",
                    "400/842/909/5429/front_es.3.400.jpg"},
            {"dentastix-medianos", "snacks", "Dentastix Razas Medianas", "Pedigree", "Paquete 3 barras",
                    7_900L, 50L,
                    "Snack dental que ayuda a reducir el sarro con su forma en X. Para perros de 10 a 25 kg.",
                    "599/874/910/4392/front_fr.25.400.jpg"},
            {"milkbone-medium", "snacks", "Galletas Milk-Bone Medianas", "Milk-Bone", "Caja 482 g",
                    24_900L, 25L,
                    "Galletas crujientes para perros que ayudan a limpiar los dientes.",
                    "007/910/051/3529/front_en.14.400.jpg"},
            {"beggin-original", "snacks", "Beggin' Strips Original", "Purina", "Bolsa 85 g",
                    15_900L, 30L,
                    "Tiras suaves sabor tocino para premiar a tu perro durante el entrenamiento.",
                    "003/810/000/1306/front_en.8.400.jpg"},
            {"markies-original", "snacks", "Markies Original", "Pedigree", "Caja 500 g",
                    21_900L, 22L,
                    "Galletas rellenas con médula, con calcio y vitaminas. Premio para perros adultos.",
                    "501/039/499/6587/front_fr.50.400.jpg"},
    };
}
