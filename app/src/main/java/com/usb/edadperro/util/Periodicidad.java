package com.usb.edadperro.util;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Cálculo de fechas de los cuidados periódicos (vacunas, desparasitantes,
 * medicamentos, baños, cortes de pelo): próxima fecha, estado y textos.
 * No depende de Android, así se prueba con JUnit.
 */
public final class Periodicidad {

    public enum Unidad {
        HORAS("hora", "horas"),
        DIAS("día", "días"),
        SEMANAS("semana", "semanas"),
        MESES("mes", "meses"),
        ANIOS("año", "años");

        public final String singular;
        public final String plural;

        Unidad(String singular, String plural) {
            this.singular = singular;
            this.plural = plural;
        }
    }

    public enum Estado { VENCIDO, HOY, PRONTO, PROGRAMADO, COMPLETADO }

    /** Un cuidado se marca como «pronto» si faltan hasta estos días. */
    public static final int DIAS_PRONTO = 7;

    private Periodicidad() {
    }

    /** @return la unidad con ese nombre, o DIAS si no es válido */
    public static Unidad unidad(String nombre) {
        try {
            return Unidad.valueOf(nombre);
        } catch (Exception e) {
            return Unidad.DIAS;
        }
    }

    /** Suma «cada» unidades a una fecha (respeta meses de distinta duración). */
    public static Date sumar(Date fecha, int cada, Unidad u) {
        Calendar c = Calendar.getInstance();
        c.setTime(fecha);
        switch (u) {
            case HORAS:
                c.add(Calendar.HOUR_OF_DAY, cada);
                break;
            case SEMANAS:
                c.add(Calendar.DATE, cada * 7);
                break;
            case MESES:
                c.add(Calendar.MONTH, cada);
                break;
            case ANIOS:
                c.add(Calendar.YEAR, cada);
                break;
            default:
                c.add(Calendar.DATE, cada);
        }
        return c.getTime();
    }

    /**
     * Próxima fecha después de marcar un cuidado como hecho.
     * <ul>
     * <li>Si se hizo antes de la fecha programada, se avanza un periodo desde esa fecha.</li>
     * <li>Cada N horas (medicamentos): se conserva el horario fijo de las tomas.</li>
     * <li>Días, semanas, meses o años: se cuenta desde el día en que se hizo
     * (la siguiente vacuna se calcula desde la que se aplicó), a la misma hora programada.</li>
     * </ul>
     *
     * @return la nueva fecha, o null si el cuidado no se repite
     */
    public static Date siguienteTrasHecho(Date programada, Date hecho, int cada, Unidad u) {
        if (programada == null || cada <= 0) {
            return null;
        }
        if (hecho.before(programada)) {
            return sumar(programada, cada, u);
        }
        if (u == Unidad.HORAS) {
            long periodo = TimeUnit.HOURS.toMillis(cada);
            long pasados = (hecho.getTime() - programada.getTime()) / periodo + 1;
            return new Date(programada.getTime() + pasados * periodo);
        }
        Calendar base = Calendar.getInstance();
        base.setTime(hecho);
        Calendar p = Calendar.getInstance();
        p.setTime(programada);
        base.set(Calendar.HOUR_OF_DAY, p.get(Calendar.HOUR_OF_DAY));
        base.set(Calendar.MINUTE, p.get(Calendar.MINUTE));
        base.set(Calendar.SECOND, 0);
        base.set(Calendar.MILLISECOND, 0);
        return sumar(base.getTime(), cada, u);
    }

    public static Estado estado(Date proxima, boolean activo, Date ahora) {
        if (!activo || proxima == null) {
            return Estado.COMPLETADO;
        }
        if (proxima.before(ahora)) {
            return Estado.VENCIDO;
        }
        long dias = diasEntre(ahora, proxima);
        if (dias == 0) {
            return Estado.HOY;
        }
        return dias <= DIAS_PRONTO ? Estado.PRONTO : Estado.PROGRAMADO;
    }

    /** Días de calendario entre dos fechas (negativo si «hasta» ya pasó). */
    public static long diasEntre(Date desde, Date hasta) {
        long ms = medianoche(hasta) - medianoche(desde);
        return Math.round(ms / (double) TimeUnit.DAYS.toMillis(1));
    }

    private static long medianoche(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** "Hoy", "Mañana", "En 5 días", "En 2 meses", "Ayer", "Hace 3 días"… */
    public static String relativo(Date fecha, Date ahora) {
        long d = diasEntre(ahora, fecha);
        if (d == 0) {
            return "Hoy";
        }
        if (d == 1) {
            return "Mañana";
        }
        if (d == -1) {
            return "Ayer";
        }
        if (d < 0) {
            return "Hace " + -d + " días";
        }
        if (d < 60) {
            return "En " + d + " días";
        }
        if (d < 365) {
            return "En " + (d / 30) + " meses";
        }
        long anios = d / 365;
        return anios == 1 ? "En 1 año" : "En " + anios + " años";
    }

    /** "No se repite", "Cada día", "Cada 8 horas", "Cada 3 meses"… */
    public static String describir(int cada, Unidad u) {
        if (cada <= 0) {
            return "No se repite";
        }
        if (cada == 1) {
            return "Cada " + u.singular;
        }
        return "Cada " + cada + " " + u.plural;
    }

    /** "A la hora", "1 hora antes", "1 día antes", "30 min antes". */
    public static String describirAviso(int minutos) {
        if (minutos <= 0) {
            return "A la hora";
        }
        if (minutos % 1440 == 0) {
            int d = minutos / 1440;
            return d == 1 ? "1 día antes" : d + " días antes";
        }
        if (minutos % 60 == 0) {
            int h = minutos / 60;
            return h == 1 ? "1 hora antes" : h + " horas antes";
        }
        return minutos + " min antes";
    }
}
