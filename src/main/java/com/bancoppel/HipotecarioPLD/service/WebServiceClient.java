package com.bancoppel.HipotecarioPLD.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.util.UriComponentsBuilder;

import com.bancoppel.HipotecarioPLD.Util.sanitizeLog;
import com.bancoppel.HipotecarioPLD.config.configManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Collections;
import java.net.http.HttpRequest;

@Service
public class WebServiceClient {
 
    private final RestTemplate restTemplate;
    private final configManager configManager; 
    private final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WebServiceClient.class);
   // private final HttpClient httpClient;
 
    @Autowired
    public WebServiceClient(configManager configManager) {
        this.configManager = configManager;
        this.restTemplate = new RestTemplate();
    }
 
    /*metodo para consumir por Name Maching*/
    public boolean validarRegistroNameMatchingRestTemplate(Map<String, Object> registro) throws RuntimeException {
        try {
            String wsUrl = configManager.getWsurl();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(registro, headers);

            ResponseEntity<Map> response = restTemplate.exchange(wsUrl, HttpMethod.POST, requestEntity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Object match = response.getBody().get("name_match");
                return match instanceof Boolean && (Boolean) match;
            }
            return false;
        }
        catch (Exception e) {
            log.error("Error en llamada al WS: " + e.getMessage());
            throw new RuntimeException("Error en llamada al WS: " + e.getMessage());
        }
    }
    
    // Nuevo metodo para consumir Name Matching usando httpClient para evitar uso de certificado
  /*  public boolean validarRegistroNameMatchingHttpClient(Map<String, Object> registro) {
        try {
            log.info("====Desactivando verificacion de certificados====");
         
            String wsUrl = configManager.getWsurl();
            String requestBody = mapper.writeValueAsString(registro);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(new URI(wsUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            log.info("Enviando peticion HttpClient: {}", wsUrl);
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            log.info("Respuesta HttpClient Name Matching recibida");

            if (response.statusCode() != 200 ) {
                log.error("Error en llamada al WS: {} con HttpClient, codigo de estado: {}",wsUrl, response.statusCode());
                return false;
            }

            if(response.body() == null) {
                log.error("Error en llamada al WS {} con HttpClient, cuerpo de respuesta nulo",wsUrl);
                return false;
            }

            Map<String, Object> responseMap = mapper.readValue(response.body(), new TypeReference<Map<String, Object>>() {});
            Object match = responseMap.get("name_match");
            if(match == null) {
                log.error("Error en llamada al WS: {} con HttpClient, campo name_match nulo",wsUrl);
                return false;
            }
            return match instanceof Boolean && (Boolean) match;

        } catch (Exception e) {
            log.error("Error en llamada al WS con HttpClient: " + e.getMessage());
            return false;
        }
    }*/
    
    
    /*metodo para consumir por listas negras actual*/
    public Map<String, Object> validarRegistroLN(Map<String, Object> datos) {
        try {
            String baseUrl = configManager.getWsurlListasNegras();
    
            // Construir la URL con parámetros
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .queryParam("pRFC", datos.getOrDefault("pRFC", ""))
                .queryParam("pNombre1", datos.getOrDefault("pNombre1", ""))
                .queryParam("pNombre2", datos.getOrDefault("pNombre2", ""))
                .queryParam("pApellPaterno", datos.getOrDefault("pApellPaterno", ""))
                .queryParam("pApellMaterno", datos.getOrDefault("pApellMaterno", ""))
                .queryParam("pFechaNac", datos.getOrDefault("pFechaNac", ""));
     
            String urlFinal = builder.toUriString();
            String urlFinalSeguro = sanitizeLog.sanitizeForLog(urlFinal);
             log.debug("URL WS Listas Negras: {}", urlFinalSeguro);  
            // Consumir el WS
            ResponseEntity<String> response = restTemplate.exchange(urlFinal, HttpMethod.GET, null, String.class);   
            if (response.getStatusCode().is2xxSuccessful()) {
                String responseBody = response.getBody();
                return mapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
            } else {
            	String status = response.getStatusCode().toString();
            	String statusSeguro = sanitizeLog.sanitizeForLog(status);
                log.warn("Respuesta no exitosa del WS LN: {}", statusSeguro);
            }     
        } catch (Exception e) {      	
            log.error("Error al validar registro en listas negras: {}", e.getMessage(), e);
        }     
        return Collections.emptyMap();
    }
 
    // --- Nuevo método para el servicio de PEPs por bus---
    public String validarPEP(Map<String, Object> pepRequest) {
        String wsPepUrl = configManager.getWsurlpep(); // Asegúrate de tener este método en configManager
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(pepRequest, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(wsPepUrl, HttpMethod.POST, requestEntity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Asumimos que "vcodret" siempre será una String
                Object vcodret = response.getBody().get("vcodret");
                return vcodret != null ? vcodret.toString() : null; // Devuelve el valor de vcodret
            }
        } catch (Exception e) {
        	log.error("Error en llamada al WS de PEPs: " + e.getMessage());
        }
        return null; // En caso de error o respuesta inesperada
    }
    
    /*Meotodo para consultar por servicio web peps dinamico*/  
    public Map<String, Object> enviarDatosPeps(Map<String, Object> datos) {
    	try {
            ResponseEntity<Map> response = restTemplate.postForEntity(configManager.getWsurlwspep(), datos, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
        } catch (Exception e) {
        	log.error("Error al enviar datos a PEPS: " + e.getMessage());
        }
        return Collections.emptyMap(); // Valor seguro para evitar NPE
    }



}