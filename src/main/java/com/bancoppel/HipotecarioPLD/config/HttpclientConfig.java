package com.bancoppel.HipotecarioPLD.config;



import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.http.HttpClient;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@Configuration
@Slf4j
public class HttpclientConfig {

    @Bean
    @ConditionalOnProperty(name = "crearbean.httpclient", havingValue = "true")
    HttpClient httpClient(configManager configmanager) throws Exception {

    	//Solo para desarrollo y omitir el certificado para pruebas
       /* if (!configmanager.isCertificado()) {

            log.warn("⚠️ HttpClient SIN validación SSL");
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            }, new SecureRandom());
            return HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();

            
            
        } else {

            log.info("✅ HttpClient con SSL normal (truststore)");

            return HttpClient.newBuilder().build();
        }*/
    	
    	log.info("✅ HttpClient configurado con SSL seguro nativo");
        return HttpClient.newBuilder().build();
    }

    @Bean
    public CloseableHttpClient closeableHttpClient() {
        return HttpClients.createDefault();
    }
}