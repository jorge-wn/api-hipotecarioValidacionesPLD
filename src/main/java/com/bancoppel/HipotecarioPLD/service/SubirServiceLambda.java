package com.bancoppel.HipotecarioPLD.service;


import com.bancoppel.HipotecarioPLD.config.RutasRespuestaConfigAws;
import com.bancoppel.HipotecarioPLD.dto.LambdaRequestDTOAws;
import com.bancoppel.HipotecarioPLD.dto.SubirRequestDTOAws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubirServiceLambda {

    private final RutasRespuestaConfigAws rutasRespuestaConfigAws;
    private final RestTemplate restTemplate;

    @Value("${TipoOperacion2}")
    private String tipoOperacionSubir;

    @Value("${urllambda}")
    private String lambdaUrl;

    /**
     * Se debe ejecutar DESPUÉS de haber subido los TXT al S3
     */
    public void notificarLambda(SubirRequestDTOAws request) {

        LambdaRequestDTOAws lambdaRequest = armarRequestLambda(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LambdaRequestDTOAws> entity =
                new HttpEntity<>(lambdaRequest, headers);

        log.info("Enviando JSON a Lambda: {}", lambdaRequest);

        ResponseEntity<String> response = restTemplate.exchange(
                lambdaUrl,
                HttpMethod.POST,
                entity,
                String.class
        );

        log.info("Respuesta Lambda status={} body={}",
                response.getStatusCode(),
                response.getBody()
        );
    }

    /**
     * Construye el payload que consume la Lambda
     */
    public LambdaRequestDTOAws armarRequestLambda(SubirRequestDTOAws request) {

        if (request.getProveedor() == null || request.getValidacion() == null) {
            throw new IllegalArgumentException("Proveedor y Validacion son obligatorios");
        }

        String proveedor = request.getProveedor().toUpperCase();
        String validacion = request.getValidacion().toUpperCase();

        // ===============================
        // RUTA BASE S3 DESDE CONFIG AWS
        // ===============================
        String rutaBaseS3 =
                rutasRespuestaConfigAws.obtenerRutaS3(proveedor, validacion);

        // ===============================
        // FECHA DEL SISTEMA
        // ===============================
        LocalDate now = LocalDate.now();
        String anio = String.valueOf(now.getYear());
        String mes = String.format("%02d", now.getMonthValue());

        // ===============================
        // NOMBRE ARCHIVO
        // ===============================
        String nombreArchivo = request.getArchivo() + ".txt";

        // ===============================
        // RUTA FINAL S3
        // ===============================
        String rutaS3Final = String.format(
                "%s/%s/%s/%s",
                rutaBaseS3,
                anio,
                mes,
                nombreArchivo
        );

        // ===============================
        // REQUEST A LAMBDA
        // ===============================
        LambdaRequestDTOAws lambdaRequest = new LambdaRequestDTOAws();
        lambdaRequest.setRutaS3(rutaS3Final);
        lambdaRequest.setTipoOperacion(tipoOperacionSubir);
        lambdaRequest.setOrigen(request.getOrigen());

        return lambdaRequest;
    }
}
