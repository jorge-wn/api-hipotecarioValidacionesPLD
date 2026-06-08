package com.bancoppel.HipotecarioPLD.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class RutasRespuestaConfigAws {

    @Value("${s3.rutaRespuestaPLD.Kredy}")
    private String pldKredy;

    @Value("${s3.rutaRespuestaPLD.Yave}")
    private String pldYave;

    @Value("${s3.rutaRespuestaPLD.Bancoppel}")
    private String pldBancoppel;

    @Value("${s3.rutaRespuestaSIC.Kredy}")
    private String sicKredy;

    @Value("${s3.rutaRespuestaSIC.Yave}")
    private String sicYave;

    @Value("${s3.rutaRespuestaSIC.Bancoppel}")
    private String sicBancoppel;
    
    //ruta respuestas SFTP
    
    @Value("${rutaSftpPLD.S.Kredy}")
    private String sSftppldKredy;

    @Value("${rutaSftpPLD.S.Yave}")
    private String sSftppldYave;

    @Value("${rutaSftpPLD.S.Bancoppel}")
    private String sSftppldBancoppel;
//salida sic
    @Value("${rutaSftpSIC.S.Kredy}")
    private String sSftpsicKredy;

    @Value("${rutaSftpSIC.S.Yave}")
    private String sSftpsicYave;

    @Value("${rutaSftpSIC.S.Bancoppel}")
    private String sSftpsicBancoppel;

    public String obtenerRutaS3(String proveedor, String validacion) {

        proveedor = proveedor.toUpperCase(Locale.ROOT);
        validacion = validacion.toUpperCase(Locale.ROOT);

        if ("PLD".equals(validacion)) {
            return switch (proveedor) {
                case "KREDI", "KREDY" -> pldKredy;
                case "YAVE" -> pldYave;
                case "BANCOPPEL" -> pldBancoppel;
                default -> throw new IllegalArgumentException(
                        "Proveedor no soportado para PLD: " + proveedor);
            };
        }

        if ("SIC".equals(validacion)) {
            return switch (proveedor) {
                case "KREDI", "KREDY" -> sicKredy;
                case "YAVE" -> sicYave;
                case "BANCOPPEL" -> sicBancoppel;
                default -> throw new IllegalArgumentException(
                        "Proveedor no soportado para SIC: " + proveedor);
            };
        }

        throw new IllegalArgumentException("Tipo de validación inválido: " + validacion);
    }
    
    public String obtenerRutaSFTP(String proveedor, String validacion) {

        proveedor = proveedor.toUpperCase(Locale.ROOT);
        validacion = validacion.toUpperCase(Locale.ROOT);

        if ("PLD".equals(validacion)) {
            return switch (proveedor) {
                case "KREDI", "KREDY" -> sSftppldKredy;
                case "YAVE" -> sSftppldYave;
                case "BANCOPPEL" -> sSftppldBancoppel;
                default -> throw new IllegalArgumentException(
                        "Proveedor no soportado para PLD: " + proveedor);
            };
        }

        if ("SIC".equals(validacion)) {
            return switch (proveedor) {
                case "KREDI", "KREDY" -> sSftpsicKredy;
                case "YAVE" -> sSftpsicYave;
                case "BANCOPPEL" -> sSftpsicBancoppel;
                default -> throw new IllegalArgumentException(
                        "Proveedor no soportado para SIC: " + proveedor);
            };
        }

        throw new IllegalArgumentException("Tipo de validación inválido: " + validacion);
    }
    
}
