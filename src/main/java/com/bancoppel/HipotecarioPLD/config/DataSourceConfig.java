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
          //  @Value("${datasourceusername}") String username,
          //  @Value("${datasourcepwd}") String password,
            @Value("${datasourceusername}") String username1,
            @Value("${datasourcepwd}") String clvdb,
            @Value("${spring.datasource.driver-class-name}") String driver,
            @Value("${aessecretkey}")char[] secretKey,            
            @Value("${aesiv}") String iv) {
    	
        try {
    	
       // AesDecryptor decryptor = new AesDecryptor(secretKey, iv);
        //char[] username1 = decryptor.decryptToCharArray(username);
        //char[] clvdb = decryptor.decryptToCharArray(password);
    	
     //   String usernameD = new String(username1);
       // String passwordD = new String(clvdb);

    	
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

        // Limpiar memoria (buena práctica)
       // Arrays.fill(username1, '\0');
      //  Arrays.fill(clvdb, '\0');
        
        return ds;
      } catch (Exception e) {
        throw new RuntimeException("Error desencriptando credenciales de BD", e);
      }
    }
}
