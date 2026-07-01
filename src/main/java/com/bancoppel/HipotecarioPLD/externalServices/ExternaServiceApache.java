package com.bancoppel.HipotecarioPLD.externalServices;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExternaServiceApache {

    @Value("${NameMaching.disableCertificateVerification}")
    private String desactivarVerificacionCertificado;

    @Value("${NameMaching.enableProxy}")
    private String enableProxy;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ExternaServiceApache.class);
    private final com.bancoppel.HipotecarioPLD.config.configManager configManager;
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean validarRegistroNameMatchingHttpClient(Map<String, Object> registro) throws IOException {
        CloseableHttpResponse closeableHttpResponse = null;
        try {
            String wsUrl = configManager.getWsurl();
            String requestBody = mapper.writeValueAsString(registro);
            CloseableHttpClient customHttpClient;
            SSLConnectionSocketFactory sslSocketFactory;

            log.info("====Activando verificacion de certificados====");
            sslSocketFactory = new SSLConnectionSocketFactory(
            SSLContexts.custom() // no se desactiva verificacion de certificados
            .build());
                
            if(enableProxy.equalsIgnoreCase("true")) {
                String host = configManager.getProxyHost();
                int port = Integer.parseInt(configManager.getProxyPort());
                log.info("Configurando proxy para WS: " + wsUrl + " , host: " + host + ", port: " + port);

                HttpHost proxy = new HttpHost(host, port, "http");

                customHttpClient = HttpClients.custom()
                        .setSSLSocketFactory(sslSocketFactory)
                        .setProxy(proxy)
                        .build();
            }else{
                customHttpClient = HttpClients.custom()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build();
            }

            HttpPost httpPost = new HttpPost(wsUrl);
            httpPost.setEntity(new StringEntity(requestBody,
                    ContentType.APPLICATION_JSON));

            log.info("Request body a url: " + wsUrl + " , StringEntity: " + new StringEntity(requestBody).getContent());

            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Accept", "application/json");


            log.info("Enviando peticion CloseableHttpClient: {}", wsUrl);
            closeableHttpResponse = customHttpClient.execute(httpPost);
            log.info("Respuesta CloseableHttpClient Name Matching recibida");


            int statusCode = closeableHttpResponse.getStatusLine().getStatusCode(); // For HttpClient 5.x

            // Si el WS devuelve error (500,502,503,504) -> intentar de nuevo y si falla, lanzar error
            if (statusCode == 500 || statusCode == 502 ||
                    statusCode == 503 || statusCode == 504) {

                log.error("WS devolvió {}, reintentando en 10 segundos...", statusCode);
                try { Thread.sleep(10000); } catch (InterruptedException e) {}

                statusCode = closeableHttpResponse.getStatusLine().getStatusCode(); // For HttpClient 5.x


                if (statusCode != 200) {
                    throw new RuntimeException("WS sigue fallando: status=" + statusCode);
                }
            }

            if(statusCode != 200){
                String errorString = String.format("Error en llamada al WS: %s con HttpClient, codigo de estatus: %d", wsUrl, statusCode);
                log.error(errorString);
                throw new RuntimeException(errorString);
            }

            HttpEntity entity = closeableHttpResponse.getEntity();

            if (entity == null) {
                String errorString = String.format("Error en llamada al WS: %s con CloseableHttpClient, respuesta nulo", wsUrl);
                log.error(errorString);
                throw new RuntimeException(errorString);
            }
            String responseBody = EntityUtils.toString(entity);


            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = mapper.readValue(
                    responseBody,
                    Map.class
            );

            Object match = responseMap.get("name_match");
            if (match == null) {
                String errorString = String.format("Error en llamada al WS: %s con HttpClient, campo name_match nulo", wsUrl);
                log.error(errorString);
                throw new RuntimeException(errorString);
            }

            log.info("Valor de name_match: " + match.toString());
            return match instanceof Boolean && (Boolean) match;

        } catch (Exception e) {
            String errorString = String.format("Error en llamada al WS con CloseableHttpClient: %s", e.getMessage());
            log.error(errorString);
            throw new RuntimeException(errorString);
        } finally {
            if (closeableHttpResponse != null) closeableHttpResponse.close();

        }
    }





}
