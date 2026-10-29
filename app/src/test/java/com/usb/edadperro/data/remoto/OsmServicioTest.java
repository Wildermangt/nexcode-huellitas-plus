package com.usb.edadperro.data.remoto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.usb.edadperro.data.modelo.Veterinaria;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Punto 11.4: el mapa muestra ubicaciones verificables.
 * Se prueba el procesamiento de una respuesta REAL de Overpass (OpenStreetMap)
 * guardada en test/resources, y la regla de urgencias verificadas.
 */
public class OsmServicioTest {

    private static final double LAT = 4.6486;
    private static final double LON = -74.0628;

    private static String leer(String recurso) {
        InputStream in = OsmServicioTest.class.getClassLoader().getResourceAsStream(recurso);
        assertNotNull("Falta el recurso " + recurso, in);
        try (Scanner s = new Scanner(in, StandardCharsets.UTF_8.name()).useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    @Test
    public void respuestaRealDeOverpassSeConvierteEnVeterinariasOrdenadas() throws Exception {
        List<Veterinaria> lista = OsmServicio.parsearOverpass(leer("overpass_bogota.json"), LAT, LON);
        assertFalse(lista.isEmpty());
        double anterior = -1;
        for (Veterinaria v : lista) {
            // Cada resultado es un lugar de OSM con identificador verificable y coordenadas reales.
            assertTrue(v.osmId.matches("(node|way)/\\d+"));
            assertTrue(v.urlOsm().startsWith("https://www.openstreetmap.org/"));
            assertTrue(v.lat > 4 && v.lat < 5 && v.lon > -75 && v.lon < -73);
            assertNotNull(v.nombre);
            // Radio de la consulta: 2 km
            assertTrue(v.distanciaKm <= 2.2);
            assertTrue(v.distanciaKm >= anterior);
            anterior = v.distanciaKm;
        }
    }

    @Test
    public void soloSeMarcaUrgenciaConDatoVerificado() throws Exception {
        String json = "{\"elements\":["
                + "{\"type\":\"node\",\"id\":1,\"lat\":4.65,\"lon\":-74.06,"
                + "\"tags\":{\"amenity\":\"veterinary\",\"name\":\"Vet 24 horas\",\"opening_hours\":\"24/7\"}},"
                + "{\"type\":\"node\",\"id\":2,\"lat\":4.651,\"lon\":-74.061,"
                + "\"tags\":{\"amenity\":\"veterinary\",\"name\":\"Clínica con urgencias\",\"emergency\":\"yes\","
                + "\"phone\":\"+57 601 555 0000\",\"addr:street\":\"Calle 93\",\"addr:housenumber\":\"15-20\","
                + "\"addr:city\":\"Bogotá\"}},"
                + "{\"type\":\"way\",\"id\":3,\"center\":{\"lat\":4.652,\"lon\":-74.062},"
                + "\"tags\":{\"amenity\":\"veterinary\",\"name\":\"Consultorio\",\"opening_hours\":\"Mo-Fr 08:00-18:00\"}}"
                + "]}";
        List<Veterinaria> lista = OsmServicio.parsearOverpass(json, LAT, LON);
        assertEquals(3, lista.size());
        int urgencias = 0;
        for (Veterinaria v : lista) {
            if (v.nombre.equals("Consultorio")) {
                assertFalse(v.urgencias);
                assertEquals("way/3", v.osmId);
                assertNull(v.telefono);
            } else {
                assertTrue(v.urgencias);
                urgencias++;
            }
            if (v.nombre.equals("Clínica con urgencias")) {
                assertEquals("Calle 93 # 15-20, Bogotá", v.direccion);
                assertEquals("+57 601 555 0000", v.telefono);
            }
        }
        assertEquals(2, urgencias);
    }

    @Test
    public void respaldoNominatimDevuelveVeterinariasDentroDelRadio() throws Exception {
        List<Veterinaria> lista = OsmServicio.parsearNominatim(leer("nominatim_bogota.json"), LAT, LON, 5.0);
        assertFalse(lista.isEmpty());
        double anterior = -1;
        for (Veterinaria v : lista) {
            assertTrue(v.osmId.matches("(node|way|relation)/\\d+"));
            assertTrue(v.lat > 4 && v.lat < 5 && v.lon > -75 && v.lon < -73);
            assertNotNull(v.nombre);
            assertTrue(v.distanciaKm <= 5.0);
            assertTrue(v.distanciaKm >= anterior);
            anterior = v.distanciaKm;
        }
        // Las etiquetas extra de OSM (teléfono, horario) se conservan.
        boolean conTelefono = false;
        for (Veterinaria v : lista) {
            conTelefono |= v.telefono != null;
        }
        assertTrue(conTelefono);
    }

    @Test
    public void respaldoNominatimDescartaLugaresQueNoSonVeterinarias() throws Exception {
        String json = "[{\"osm_type\":\"node\",\"osm_id\":1,\"lat\":\"4.65\",\"lon\":\"-74.06\","
                + "\"category\":\"shop\",\"type\":\"pet\",\"name\":\"Tienda\"},"
                + "{\"osm_type\":\"node\",\"osm_id\":2,\"lat\":\"4.651\",\"lon\":\"-74.061\","
                + "\"category\":\"amenity\",\"type\":\"veterinary\",\"name\":\"Vet\","
                + "\"extratags\":{\"emergency\":\"yes\"},"
                + "\"address\":{\"road\":\"Calle 93\",\"house_number\":\"15-20\",\"city\":\"Bogotá\"}}]";
        List<Veterinaria> lista = OsmServicio.parsearNominatim(json, LAT, LON, 2.0);
        assertEquals(1, lista.size());
        assertEquals("node/2", lista.get(0).osmId);
        assertTrue(lista.get(0).urgencias);
        assertEquals("Calle 93 # 15-20, Bogotá", lista.get(0).direccion);
    }
}
