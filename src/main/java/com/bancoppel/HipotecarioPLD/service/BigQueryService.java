package com.bancoppel.HipotecarioPLD.service;

import com.bancoppel.HipotecarioPLD.config.configManager;
import com.bancoppel.HipotecarioPLD.externalServices.ExternaServiceApache;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.*;
import com.google.cloud.http.HttpTransportOptions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;


@Service
public class BigQueryService {
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(BigQueryService.class);
private final BigQuery bigquery;
private final String tableName;



@Autowired
public BigQueryService(BigQuery bigquery, configManager config) {
this.bigquery = bigquery;
this.tableName = String.format("%s.%s.%s", config.getBigqueryprojectid(), config.getBigquerydatasetId(),
config.getBigquerytablaId());
}



public String existeNombre(String rfc) {
String query = String.format("SELECT RespuestaCliente FROM %s WHERE UPPER(TRIM(RFC)) like @RFC", tableName);

QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query)
.addNamedParameter("RFC", QueryParameterValue.of(rfc, StandardSQLTypeName.STRING)).build();
try {
TableResult result = bigquery.query(queryConfig);
long totalFilas = result.getTotalRows();
// CASO 1: No existe el registro (0 filas)
if (totalFilas == 0) {
log.info("El cliente no existe en BigQuery.");
return "NO_EXISTE";
}



// CASO 2: Existe duplicado (2 o más filas)

if (totalFilas >= 2) {
log.warn("Se encontraron {} registros para el mismo nombre. Abortando.", totalFilas);
return "ERROR_DUPLICADOS"; // Aquí es donde "no procesas" y sales
}



// CASO 3: Registro único (Exactamente 1 fila)
FieldValueList row = result.iterateAll().iterator().next();
return row.get("RespuestaCliente").getStringValue();
} catch (BigQueryException e) {
throw new BigQueryServiceException("Error al ejecutar la consulta en BigQuery", e);
} catch (InterruptedException e) {
Thread.currentThread().interrupt();
throw new BigQueryServiceException("Consulta a BigQuery interrumpida", e);
}

}



public static class BigQueryServiceException extends RuntimeException {

public BigQueryServiceException(String message, Throwable cause) {

super(message, cause);

}

}



@Configuration
public static class BigQueryConfig {

@Value("${bigquery.credentials.path}")
private String credentialsPath;
@Value("${bigquery.project.id}")
private String projectId;
@Value("${http.proxyHost}")
private String proxyHost;
@Value("${http.proxyPort}")
private int proxyPort;
@Value("${bigquery.enableProxy}")
private String enableProxy;



@Bean
public BigQuery bigquery() throws IOException {
// Crear las credenciales SIEMPRE desde el String (asumiendo que el String ES el

// JSON)
GoogleCredentials credentials;
try (InputStream is = new ByteArrayInputStream(credentialsPath.getBytes(StandardCharsets.UTF_8))) {
credentials = GoogleCredentials.fromStream(is);
}



if (enableProxy.equalsIgnoreCase("true")) {



log.info("========Conexión a BigQuery con proxy ========");

String host = proxyHost;

int port = proxyPort;



try {

URI uri = new URI(proxyHost);

if (uri.getHost() != null) {

host = uri.getHost();

if (uri.getPort() != -1) {

port = uri.getPort();

}

} else {

host = proxyHost.replaceFirst("^https?://", "").replaceAll("/$", "");

}

} catch (URISyntaxException ex) {

host = proxyHost.replaceFirst("^https?://", "").replaceAll("/$", "");

}



if (host == null || host.isEmpty() || port <= 0) {

log.warn("Proxy no configurado correctamente, se intentará conexión directa");

} else {

log.info("========Proxy JVM configurado para GCP: " + host + ", port: " + port + " ========");

}

// Configurar PROXY

Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));

// Construir transportOptions usando una HttpTransportFactory que crea
// NetHttpTransport con proxy
HttpTransportOptions transportOptions = HttpTransportOptions.newBuilder().setHttpTransportFactory(

() -> new com.google.api.client.http.javanet.NetHttpTransport.Builder().setProxy(proxy).build())

.build();



log.info("========Proxy host for GCP: " + host + ", port: " + port + " ========");

return BigQueryOptions.newBuilder().setCredentials(credentials).setProjectId(projectId)

.setTransportOptions(transportOptions) // ← obligatorio para proxy

.build().getService();

} else {

log.info("========Conexión a BigQuery sin proxy ========");

try (InputStream is = new ByteArrayInputStream(credentialsPath.getBytes(StandardCharsets.UTF_8))) {
credentials = GoogleCredentials.fromStream(is);
}
BigQuery bigQuery = BigQueryOptions.newBuilder().setCredentials(credentials).setProjectId(projectId)
.build().getService();


BigQueryOptions options = bigQuery.getOptions();
log.info("transport options info:" + options.getTransportOptions().toString());
log.info("opciones info:" + options.toString());
return bigQuery;
}
}
}
} 

