package com.bancoppel.HipotecarioPLD.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;

import org.springframework.http.MediaType;

import org.springframework.stereotype.Service;

import org.springframework.web.reactive.function.client.WebClient;
import com.bancoppel.HipotecarioPLD.dto.LambdaRequestDTOAws;
import com.bancoppel.HipotecarioPLD.dto.LambdaResponseDTOAws;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

@Service
public class LambdaService {

    @Value("${urllambda}")
    private String lambdaUrl;
    @Value("${TipoOperacion2}")
    private String TipoOperacion2;

    //private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebClient webClient;
    private final LambdaClient lambdaClient;


    /*public LambdaService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }*/
    
//    private final WebClient webClient;

    public LambdaService(WebClient webClient) {
        this.webClient = webClient;
        this.lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_1) 
                .build();
    }
    

    // =========================================================
    // 🔹 MÉTODO NUEVO 
    // =========================================================
    public LambdaResponseDTOAws enviarResultado(String rutaSftp, String rutaS3) {

        LambdaRequestDTOAws request = new LambdaRequestDTOAws();
        request.setRutaSftp(rutaSftp);
        request.setRutaS3(rutaS3);
        request.setTipoOperacion(TipoOperacion2);
        request.setOrigen("validaciones");
        return ejecutarLambda(request);
        
    }

    // =========================================================
    // MÉTODO ORIGINAL 
    // =========================================================
    public LambdaResponseDTOAws ejecutarLambda(LambdaRequestDTOAws request) {
    	String jsonRequest = "";
        // =====================================
        // LOG DEL BODY ENVIADO A LA LAMBDA
        // =====================================
        try {
             jsonRequest = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(request);

            System.out.println("========= BODY ENVIADO A LAMBDA =========");
            System.out.println(jsonRequest);
            System.out.println("========================================");

        } catch (Exception e) {
            System.out.println("ERROR al imprimir JSON enviado a Lambda");
           // System.out.println("Ocurrió un error inesperado al ejecutar la Lambda de PLD: " + e.getMessage()+ "#####"+ e);
            //e.printStackTrace();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LambdaRequestDTOAws> entity =
                new HttpEntity<>(request, headers);

       /* ResponseEntity<LambdaResponseDTOAws> response =
                restTemplate.exchange(
                        lambdaUrl,
                        HttpMethod.POST,
                        entity,
                        LambdaResponseDTOAws.class
                );*/
        
     /*   LambdaResponseDTOAws response = webClient
                .post()
                .uri(lambdaUrl)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(LambdaResponseDTOAws.class)
                .timeout(Duration.ofSeconds(30))
                .block();*/
        
   /*     LambdaResponseDTOAws response = webClient
                .post()
                .uri(lambdaUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .onStatus(status -> status.isError(), clientResponse ->
                        clientResponse.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Error en Lambda: " + body))
                )
                .bodyToMono(LambdaResponseDTOAws.class)
                .timeout(Duration.ofSeconds(30))
                .block();*/
        
        
        try {
            // Preparar la petición invocando por Nombre de Función
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(lambdaUrl)
                    .payload(SdkBytes.fromUtf8String(jsonRequest))
                    .build();

            // Ejecutar de manera síncrona (RequestResponse)
            InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);

            // Verificar si hubo un error controlado dentro de la ejecución de la Lambda
            if (invokeResponse.functionError() != null) {
                String errorPayload = invokeResponse.payload().asUtf8String();
                throw new RuntimeException("Error en la ejecución de Lambda: " + errorPayload);
            }

            // Convertir la respuesta a tu DTO
            String jsonResponse = invokeResponse.payload().asUtf8String();
            LambdaResponseDTOAws response = objectMapper.readValue(jsonResponse, LambdaResponseDTOAws.class);

        // =====================================
        // LOG DEL BODY RESPONDIDO POR LA LAMBDA
        // =====================================
       // try {
        //    if (response != null) {
          //      String jsonResponse = objectMapper
            //            .writerWithDefaultPrettyPrinter()
              //          .writeValueAsString(response);

                System.out.println("========= BODY RESPUESTA LAMBDA =========");
                System.out.println(jsonResponse);
                System.out.println("========================================");
           // }
                return response;
        } catch (Exception e) {
            System.out.println("ERROR al imprimir JSON respuesta de Lambda");
        }  
        return null;
    }
}