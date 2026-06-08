package com.bancoppel.HipotecarioPLD.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "resultadoExecutor")
    public Executor resultadoExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);      // Bajo consumo (AWS)
        executor.setMaxPoolSize(4);       // Límite seguro
        executor.setQueueCapacity(100);   // Back-pressure
        executor.setThreadNamePrefix("resultado-");

        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }
}
