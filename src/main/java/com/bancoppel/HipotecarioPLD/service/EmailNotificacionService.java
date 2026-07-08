package com.bancoppel.HipotecarioPLD.service;

import javax.net.ssl.SSLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.bancoppel.HipotecarioPLD.config.EmailNotificacionConfig;
import com.bancoppel.HipotecarioPLD.config.configManager;
import com.bancoppel.HipotecarioPLD.dto.EmailRequest;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import reactor.netty.http.client.HttpClient;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.core.publisher.Mono;
import io.netty.handler.ssl.SslContextBuilder;

@Service
public class EmailNotificacionService {

	private static final Logger log = LoggerFactory.getLogger(EmailNotificacionService.class);
    private final WebClient webClient;
    private final EmailNotificacionConfig config;
    private final configManager configmanager;

    public EmailNotificacionService(WebClient.Builder builder,
            EmailNotificacionConfig config,
            configManager configmanager) {

this.config = config;
this.configmanager = configmanager;
if (!configmanager.isCertificado()) {
    reactor.netty.http.client.HttpClient httpClient =
        reactor.netty.http.client.HttpClient.create()
            .secure(ssl -> {
                try {
                    ssl.sslContext(
                        SslContextBuilder.forClient()
                            .trustManager(InsecureTrustManagerFactory.INSTANCE)
                            .build()
                    );
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                }
            });
    this.webClient = builder
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .build();
    log.warn("⚠️ SSL desactivado");
} else {
    this.webClient = builder.build();
}
}
 
    public void enviarNotificacion(String codigoEstatus,String rutaS3Adjunto) {    
    	try {
    log.info("======================================");
    log.info("CODIGO QUE SE ENVIA AL SERVICIO: {}", codigoEstatus);
    log.info("RUTA S3: {}", rutaS3Adjunto);
    log.info("======================================");
    	EmailRequest request = new EmailRequest(config.getOrigen(),codigoEstatus,rutaS3Adjunto);
 
         webClient.post()
        .uri(config.getUrl())
        .bodyValue(request)
        .retrieve()
        .toBodilessEntity()
        .doOnSuccess(response -> {
            log.info("Notificación enviada. Status {}", response.getStatusCode());
        })
        .onErrorResume(error -> {
            log.error("Error enviando notificación email: {}", error.getMessage());
            return Mono.empty();
        })
        .block();
        
    }catch (Exception e) {

        log.error("Error enviando notificación email: {}", e.getMessage());

    }
   }    
}
 