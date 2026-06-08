package com.bancoppel.HipotecarioPLD.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailNotificacionConfig {
 
    @Value("${email.notificacion.url}")
    private String url;
 
    @Value("${email.notificacion.origen}")
    private String origen;
 
    public String getUrl() {
        return url;
    }
 
    public String getOrigen() {
        return origen;
    }
} 