package com.usb.edadperro.data.remoto;

import com.usb.edadperro.data.modelo.Veterinaria;
import com.usb.edadperro.util.Distancia;
import com.usb.edadperro.util.Errores;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Consulta de veterinarias reales en OpenStreetMap (Punto 5).
 *
 * - Overpass API: busca los lugares etiquetados amenity=veterinary alrededor
 *   de una coordenada. Devuelve nombre, teléfono, dirección y horario tal como
 *   están registrados en OSM (directorio público y verificable).
 * - Nominatim: convierte una ciudad o dirección en coordenadas (cuando el
 *   usuario no autoriza la ubicación) y una coordenada en dirección. También
 *   sirve de respaldo para buscar veterinarias si ningún servidor Overpass responde.
 *
 * Ambos servicios son gratuitos y no requieren API key.
 * Todos los métodos son bloqueantes: llamarlos desde un hilo de fondo.
 */
public final class OsmServicio {

    /**
     * Servidores públicos de Overpass (mismos datos de OpenStreetMap). Si uno está
     * saturado o no responde, se intenta con el siguiente y, al final, con Nominatim.
     * Los espejos lentos (mail.ru, kumi) se omiten: tardaban hasta 50 s en fallar.
     */
    private static final String[] OVERPASS = {
            "https://overpass-api.de/api/interpreter",
    };
    private static final String NOMINATIM = "https://nominatim.openstreetmap.org";

    private OsmServicio() {
    }

    /** Resultado de geocodificar un texto. */
    public static final class Lugar {
        public final double lat;
        public final double lon;
        public final String nombre;

        Lugar(double lat, double lon, String nombre) {
            this.lat = lat;
            this.lon = lon;
            this.nombre = nombre;
        }
    }

    /**
     * Veterinarias en un radio alrededor de la coordenada, ordenadas por distancia.
     */
    public static List<Veterinaria> veterinariasCerca(double lat, double lon, int radioMetros)
            throws IOException {
        String consulta = String.format(Locale.US,
                "[out:json][timeout:25];"
                        + "(node[\"amenity\"=\"veterinary\"](around:%d,%.6f,%.6f);"
                        + "way[\"amenity\"=\"veterinary\"](around:%d,%.6f,%.6f););"
                        + "out center tags;",
                radioMetros, lat, lon, radioMetros, lat, lon);
        IOException ultimoError = null;
        for (String servidor : OVERPASS) {
            Request req = new Request.Builder()
                    .url(servidor)
                    .post(new FormBody.Builder().add("data", consulta).build())
                    .build();
            try (Response r = Http.cliente().newBuilder().callTimeout(10, TimeUnit.SECONDS).build()
                    .newCall(req).execute()) {
                if (!r.isSuccessful() || r.body() == null) {
                    ultimoError = new Errores.Amigable("El directorio de OpenStreetMap no respondió (" + r.code() + ")");
                    android.util.Log.w("OsmServicio", "Falló " + servidor + ": HTTP " + r.code());
                    continue;
                }
                return parsearOverpass(r.body().string(), lat, lon);
            } catch (org.json.JSONException e) {
                ultimoError = new Errores.Amigable("Respuesta inválida del directorio de mapas", e);
            } catch (IOException e) {
                ultimoError = e;
            }
            android.util.Log.w("OsmServicio", "Falló " + servidor + ": " + ultimoError);
        }
        try {
            return veterinariasNominatim(lat, lon, radioMetros);
        } catch (IOException e) {
            android.util.Log.w("OsmServicio", "Falló el respaldo Nominatim: " + e);
        }
        throw new Errores.Amigable("Los servidores de OpenStreetMap no están disponibles en este momento. "
                + "Inténtalo de nuevo en unos segundos.", ultimoError);
    }

    /**
     * Respaldo: los mismos lugares amenity=veterinary de OSM, buscados en Nominatim
     * dentro del cuadro que contiene el radio pedido.
     */
    private static List<Veterinaria> veterinariasNominatim(double lat, double lon, int radioMetros)
            throws IOException {
        double dLat = radioMetros / 111320.0;
        double dLon = radioMetros / (111320.0 * Math.cos(Math.toRadians(lat)));
        String caja = String.format(Locale.US, "%.6f,%.6f,%.6f,%.6f",
                lon - dLon, lat + dLat, lon + dLon, lat - dLat);
        HttpUrl url = HttpUrl.parse(NOMINATIM + "/search").newBuilder()
                .addQueryParameter("q", "veterinario")
                .addQueryParameter("format", "jsonv2")
                .addQueryParameter("viewbox", caja)
                .addQueryParameter("bounded", "1")
                .addQueryParameter("limit", "50")
                .addQueryParameter("extratags", "1")
                .addQueryParameter("addressdetails", "1")
                .addQueryParameter("accept-language", "es")
                .build();
        try (Response r = Http.cliente().newCall(new Request.Builder().url(url).build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) {
                throw new Errores.Amigable("El directorio de OpenStreetMap no respondió (" + r.code() + ")");
            }
            return parsearNominatim(r.body().string(), lat, lon, radioMetros / 1000.0);
        } catch (org.json.JSONException | NumberFormatException e) {
            throw new Errores.Amigable("Respuesta inválida del directorio de mapas", e);
        }
    }

    /** Convierte la respuesta JSON de Nominatim (jsonv2 + extratags) en objetos Veterinaria. */
    static List<Veterinaria> parsearNominatim(String json, double latRef, double lonRef, double radioKm)
            throws org.json.JSONException {
        JSONArray arr = new JSONArray(json);
        List<Veterinaria> lista = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.getJSONObject(i);
            if (!"amenity".equals(e.optString("category")) || !"veterinary".equals(e.optString("type"))) {
                continue;
            }
            Veterinaria v = new Veterinaria();
            v.osmId = e.optString("osm_type") + "/" + e.optLong("osm_id");
            v.lat = Double.parseDouble(e.getString("lat"));
            v.lon = Double.parseDouble(e.getString("lon"));
            v.distanciaKm = Distancia.km(latRef, lonRef, v.lat, v.lon);
            if (v.distanciaKm > radioKm) {
                continue;
            }
            JSONObject tags = e.optJSONObject("extratags");
            if (tags == null) {
                tags = new JSONObject();
            }
            String nombre = e.optString("name", "").trim();
            v.nombre = nombre.isEmpty() ? "Veterinaria sin nombre registrado" : nombre;
            v.telefono = primero(tags, "phone", "contact:phone", "contact:mobile", "mobile");
            v.sitioWeb = primero(tags, "website", "contact:website");
            v.horario = primero(tags, "opening_hours");
            v.direccion = direccionNominatim(e.optJSONObject("address"));
            v.urgencias = "yes".equalsIgnoreCase(tags.optString("emergency", ""))
                    || (v.horario != null && v.horario.contains("24/7"));
            lista.add(v);
        }
        Collections.sort(lista, (a, b) -> Double.compare(a.distanciaKm, b.distanciaKm));
        return lista;
    }

    private static String direccionNominatim(JSONObject dir) {
        if (dir == null) {
            return null;
        }
        String calle = primero(dir, "road");
        String numero = primero(dir, "house_number");
        String ciudad = primero(dir, "city", "town", "village");
        StringBuilder sb = new StringBuilder();
        if (calle != null) {
            sb.append(calle);
            if (numero != null) {
                sb.append(" # ").append(numero);
            }
        }
        if (ciudad != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ciudad);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Convierte la respuesta JSON de Overpass en objetos Veterinaria. */
    static List<Veterinaria> parsearOverpass(String json, double latRef, double lonRef)
            throws org.json.JSONException {
        JSONArray elementos = new JSONObject(json).optJSONArray("elements");
        List<Veterinaria> lista = new ArrayList<>();
        if (elementos == null) {
            return lista;
        }
        for (int i = 0; i < elementos.length(); i++) {
            JSONObject e = elementos.getJSONObject(i);
            JSONObject tags = e.optJSONObject("tags");
            if (tags == null) {
                continue;
            }
            Veterinaria v = new Veterinaria();
            v.osmId = e.optString("type") + "/" + e.optLong("id");
            if (e.has("lat")) {
                v.lat = e.optDouble("lat");
                v.lon = e.optDouble("lon");
            } else if (e.has("center")) {
                JSONObject c = e.getJSONObject("center");
                v.lat = c.optDouble("lat");
                v.lon = c.optDouble("lon");
            } else {
                continue;
            }
            v.nombre = tags.optString("name", "Veterinaria sin nombre registrado");
            v.telefono = primero(tags, "phone", "contact:phone", "contact:mobile", "mobile");
            v.sitioWeb = primero(tags, "website", "contact:website");
            v.horario = primero(tags, "opening_hours");
            v.direccion = direccionDeTags(tags);
            String emergencia = tags.optString("emergency", "");
            v.urgencias = "yes".equalsIgnoreCase(emergencia)
                    || (v.horario != null && v.horario.contains("24/7"));
            v.distanciaKm = Distancia.km(latRef, lonRef, v.lat, v.lon);
            lista.add(v);
        }
        Collections.sort(lista, (a, b) -> Double.compare(a.distanciaKm, b.distanciaKm));
        return lista;
    }

    private static String primero(JSONObject tags, String... claves) {
        for (String k : claves) {
            String v = tags.optString(k, "").trim();
            if (!v.isEmpty()) {
                return v;
            }
        }
        return null;
    }

    private static String direccionDeTags(JSONObject tags) {
        String completa = primero(tags, "addr:full");
        if (completa != null) {
            return completa;
        }
        String calle = primero(tags, "addr:street");
        String numero = primero(tags, "addr:housenumber");
        String ciudad = primero(tags, "addr:city");
        StringBuilder sb = new StringBuilder();
        if (calle != null) {
            sb.append(calle);
            if (numero != null) {
                sb.append(" # ").append(numero);
            }
        }
        if (ciudad != null) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ciudad);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** Busca una ciudad o dirección (priorizando Colombia). Devuelve null si no hay resultados. */
    public static Lugar buscarLugar(String texto) throws IOException {
        HttpUrl url = HttpUrl.parse(NOMINATIM + "/search").newBuilder()
                .addQueryParameter("q", texto)
                .addQueryParameter("format", "json")
                .addQueryParameter("limit", "1")
                .addQueryParameter("countrycodes", "co")
                .addQueryParameter("accept-language", "es")
                .build();
        try (Response r = Http.cliente().newCall(new Request.Builder().url(url).build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) {
                throw new Errores.Amigable("El buscador de direcciones no respondió (" + r.code() + ")");
            }
            JSONArray arr = new JSONArray(r.body().string());
            if (arr.length() == 0) {
                return null;
            }
            JSONObject o = arr.getJSONObject(0);
            return new Lugar(Double.parseDouble(o.getString("lat")),
                    Double.parseDouble(o.getString("lon")),
                    o.optString("display_name"));
        } catch (org.json.JSONException | NumberFormatException e) {
            throw new Errores.Amigable("Respuesta inválida del buscador de direcciones", e);
        }
    }

    /** Dirección legible de una coordenada (geocodificación inversa). */
    public static String direccionDe(double lat, double lon) throws IOException {
        HttpUrl url = HttpUrl.parse(NOMINATIM + "/reverse").newBuilder()
                .addQueryParameter("lat", String.valueOf(lat))
                .addQueryParameter("lon", String.valueOf(lon))
                .addQueryParameter("format", "json")
                .addQueryParameter("accept-language", "es")
                .addQueryParameter("zoom", "18")
                .build();
        try (Response r = Http.cliente().newCall(new Request.Builder().url(url).build()).execute()) {
            if (!r.isSuccessful() || r.body() == null) {
                throw new Errores.Amigable("El buscador de direcciones no respondió (" + r.code() + ")");
            }
            return new JSONObject(r.body().string()).optString("display_name", null);
        } catch (org.json.JSONException e) {
            throw new Errores.Amigable("Respuesta inválida del buscador de direcciones", e);
        }
    }
}
