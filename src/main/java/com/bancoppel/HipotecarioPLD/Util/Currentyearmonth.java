package com.bancoppel.HipotecarioPLD.Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component

public class Currentyearmonth {
    @Value("${app.timezone:UTC}")
    private String timezone;

    /**
     * Obtiene el año y mes actual en formato yyyy/MM según la zona horaria configurada.
     * @return String ej: "2026/05"
     */
    public String anioMesActual() {
        ZonedDateTime ahoraMexico = ZonedDateTime.now(ZoneId.of(timezone));
        return ahoraMexico.format(DateTimeFormatter.ofPattern("yyyy/MM"));
    }
}