package com.bancoppel.HipotecarioPLD.config;

import com.bancoppel.HipotecarioPLD.Util.AesDecryptor;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;
import java.util.Arrays;


@Configuration
public class DataSourceConfig {
   
    @Bean
    public DataSource dataSource(
            @Value("${urldatasource}") String url,
            @Value("${datasourceusername}") String username1,
            @Value("${datasourcepd}") String clvdb,
            @Value("${spring.datasource.driver-class-name}") String driver,
            @Value("${aessecretkey}")char[] secretKey,            
            @Value("${aesiv}") String iv) {
    	
        try {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(url);
        ds.setUsername(username1);
        ds.setPassword(clvdb);
        ds.setDriverClassName(driver);
        // ⚡ Optimizado para AWS (bajo consumo)
        ds.setMaximumPoolSize(5);
        ds.setMinimumIdle(1);
        ds.setIdleTimeout(30000);
        ds.setConnectionTimeout(30000);
        ds.setMaxLifetime(600000);
        return ds;
      } catch (Exception e) {
        throw new RuntimeException("Error desencriptando credenciales de BD", e);
      }
    }
}
