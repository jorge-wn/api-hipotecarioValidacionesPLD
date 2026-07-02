package com.bancoppel.HipotecarioPLD.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bancoppel.HipotecarioPLD.config.RutasRespuestaConfigAws;
import com.bancoppel.HipotecarioPLD.dto.LambdaResponseDTOAws;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.bancoppel.HipotecarioPLD.Util.Currentyearmonth;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import org.owasp.encoder.Encode;
import com.bancoppel.HipotecarioPLD.Util.StringCleaner;
import java.util.regex.Pattern;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;


@Service
@Slf4j
public class S3ArchivoService {

    private final S3Client s3Client;
    private final String tempDir;
    private final String bucketBajada;
    private final String bucketSubida;
    private final LambdaService lambdaservice;
    private final RutasRespuestaConfigAws rutasRespuestaConfig;
    private final Currentyearmonth  currentyearmonth;
    
    private static final Set<String> resumenArchivos  =  Collections.synchronizedSet(new LinkedHashSet<>());
    private static final Pattern ARCHIVO_PERMITIDO = Pattern.compile("^[A-Za-z0-9._-]+\\.txt$");
  private static final Map<String, List<String>> detalleErrores =
        new ConcurrentHashMap<>();
  
    
    
        public S3ArchivoService(
            @Value("${aws.region}") String awsRegion,
            @Value("${app.temp.dir:/tmp/hipotecario}") String tempDir,
            @Value("${s3bucket}") String bucketBajada,
            @Value("${s3bucket}") String bucketSubida,
       
            LambdaService lambdaservice,
            RutasRespuestaConfigAws rutasRespuestaConfig,
            Currentyearmonth  currentyearmonth
            ) {
    	this.currentyearmonth = currentyearmonth;
    
        // ==========================================
        this.s3Client = S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();

        this.tempDir = tempDir;
        this.bucketBajada = bucketBajada;
        this.bucketSubida = bucketSubida;
        this.lambdaservice =lambdaservice;
       this.rutasRespuestaConfig = rutasRespuestaConfig;
        log.info("S3 inicializado | region={} | bucketBajada={} | bucketSubida={}",
                awsRegion, bucketBajada, bucketSubida);
    }

  /*  public Path descargarArchivo(String prefix, String nombreArchivo) {
        try {
            // Crear directorio temporal si no existe
            Files.createDirectories(Paths.get(tempDir));

            if (prefix == null || prefix.isEmpty()) {
                throw new IllegalArgumentException("El prefix S3 está vacío");
            }
            
            

            // 🔹 Usar el prefix tal cual viene y concatenar el nombre del archivo
            String key = prefix.endsWith("/") ? prefix + nombreArchivo : prefix + "/" + nombreArchivo;

            Path destino = Paths.get(tempDir, nombreArchivo);

            log.info("Descargando archivo desde S3 | bucket={} | key={}", bucketBajada, key);

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketBajada)
                    .key(key)
                    .build();

            try (InputStream in = s3Client.getObject(request)) {
                Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("Archivo descargado correctamente en {}", destino);
            return destino;

        } catch (Exception e) {
            log.error("Error descargando archivo desde S3", e);
            throw new RuntimeException("Error descargando archivo desde S3", e);
        }
    }
    */
        
        public Path descargarArchivo(String prefix, String nombreArchivo) {
            try {
                Path baseDir = Paths.get(tempDir)
                        .toAbsolutePath()
                        .normalize();

                Files.createDirectories(baseDir);

                if (prefix == null || prefix.isBlank()) {
                    throw new IllegalArgumentException("El prefix S3 está vacío");
                }

                if (nombreArchivo == null || nombreArchivo.isBlank()) {
                    throw new SecurityException("Nombre de archivo vacío");
                }

                if (!ARCHIVO_PERMITIDO.matcher(nombreArchivo).matches()) {
                    throw new SecurityException(
                            "Nombre de archivo inválido");
                }

                String nombreSeguro = Paths.get(nombreArchivo)
                        .getFileName()
                        .toString();

                Path destino = baseDir
                        .resolve(nombreSeguro)
                        .normalize();

                if (!destino.startsWith(baseDir)) {
                    throw new SecurityException(
                            "Intento de Path Traversal detectado");
                }

                String key = prefix.endsWith("/")
                        ? prefix + nombreSeguro
                        : prefix + "/" + nombreSeguro;

                log.info("Descargando archivo desde S3 | bucket={} | key={}",
                        bucketBajada,
                        key);

                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(bucketBajada)
                        .key(key)
                        .build();

                try (InputStream in = s3Client.getObject(request)) {
                    Files.copy(in,destino,StandardCopyOption.REPLACE_EXISTING);
                }

                log.info("Archivo descargado correctamente en {}",destino);

                return destino;

            } catch (Exception e) {
                log.error("Error descargando archivo desde S3",e);
                throw new RuntimeException("Error descargando archivo desde S3",e);
            }
        }
        
        
    // =========================================================
    // MÉTODO EXISTENTE – STREAMING (NO SE TOCA)
    // =========================================================
    public List<Path> descargarArchivosStreaming(String prefix) {

        log.info("Iniciando descarga desde S3 | bucket={} | prefix={}", bucketBajada, prefix);

        List<Path> archivos = new ArrayList<>();
        String continuationToken = null;

        try {
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio temporal: " + tempDir, e);
        }

        do {
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucketBajada)
                    .prefix(prefix)
                    .continuationToken(continuationToken)
                    .build();

            ListObjectsV2Response listResp = s3Client.listObjectsV2(listReq);

            for (S3Object obj : listResp.contents()) {

                if (!obj.key().endsWith(".txt")) {
                    continue;
                }

                try {
                    Path tempFile = Files.createTempFile(
                            Paths.get(tempDir),
                            "s3-stream-",
                            ".txt"
                    );

                    GetObjectRequest getReq = GetObjectRequest.builder()
                            .bucket(bucketBajada)
                            .key(obj.key())
                            .build();

                    try (InputStream in = s3Client.getObject(getReq)) {
                        Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                    }

                    archivos.add(tempFile);
                    log.info("Archivo descargado: s3://{}/{}", bucketBajada, obj.key());

                } catch (Exception e) {
                    log.error("Error descargando archivo S3: s3://{}/{}", bucketBajada, obj.key(), e);
                }
            }

            continuationToken = listResp.nextContinuationToken();

        } while (continuationToken != null);

        log.info("Descarga finalizada. Total archivos: {}", archivos.size());
        return archivos;
    }

    // =========================================================
    // SUBIDA DE ARCHIVOS 
    // =========================================================
    public void subirArchivo(Path archivoLocal, String key) {

        try {
                PutObjectRequest putReq = PutObjectRequest.builder()
                    .bucket(bucketSubida) 
                    .key(key)
                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putReq, archivoLocal);

            log.info("Archivo subido a S3: s3://{}/{}", bucketSubida, key);
            

        } catch (Exception e) {
            log.error("Error subiendo archivo a S3: s3://{}/{}", bucketSubida, key, e);
            throw new RuntimeException("Error subiendo archivo a S3", e);
        }
    }

    // =========================================================
    // RESPUESTA PLD / SIC 
    // =========================================================
    public void generarYSubirRespuestaS3(String nombreArchivoSalida,List<String> lineas,
            String rutaTemporalLocal, String prefixDestino,String originador, String Proceso) {

        Path archivoTmp = Paths.get(rutaTemporalLocal, nombreArchivoSalida);

        try {
            Files.createDirectories(archivoTmp.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(
                    archivoTmp,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            )) {
                for (String linea : lineas) {
                    writer.write(linea);
                    writer.newLine();
                }
            }

            log.info("Archivo TXT generado: {}", archivoTmp.toAbsolutePath());


         // 🔥 FORZAMOS que el key NUNCA contenga el bucket
            String cleanPrefix = prefixDestino;

         // 1️⃣ Si viene con bucket, eliminarlo
         if (cleanPrefix != null && cleanPrefix.startsWith(bucketSubida)) {
             cleanPrefix = cleanPrefix.substring(bucketSubida.length());
         }

         // 2️⃣ Quitar slashes sobrantes
         cleanPrefix = cleanPrefix.replaceAll("^/+", "")
                                  .replaceAll("/+$", "");
       
         
            String keyFinal = cleanPrefix.isEmpty()
                    ? nombreArchivoSalida
                    : cleanPrefix + "/" + nombreArchivoSalida;
             
            log.info("Subiendo con keyFinal={}", keyFinal);
             
        
      
         
            ///aqui sube al s3 revisar los path 
            subirArchivo(archivoTmp, keyFinal);
     
            log.info("archivoTmp"+ archivoTmp +":"+ keyFinal);    
       
            // aqui va el cambio del path para la salida del SFTP
            String rutaBase = rutasRespuestaConfig.obtenerRutaSFTP(originador, Proceso);

       //     log.info("ruta base: "+ rutaBase ); 
            String rutaobsoluta = rutaBase +"/"+ currentyearmonth.anioMesActual() +"/" + nombreArchivoSalida;
            
           
            LambdaResponseDTOAws lambdaResponse = lambdaservice.enviarResultado(rutaobsoluta, keyFinal);
                     
            
        } catch (Exception e) {
          //  log.error("Error al generar o subir archivo TXT a S3", e);        	
            throw new RuntimeException("Error en el flujo de subir txt o enviar email", e);
        } 
        finally {

            try {
                Files.deleteIfExists(archivoTmp);
              
            } catch (IOException ex) {
                log.warn("No se pudo eliminar archivo temporal {}", archivoTmp);
            }
        }
    }
    
    
   public void agregarResumenArchivo(String nombreArchivo,
                                  int total,
                                  int ok,
                                  int errores) {
    StringBuilder resumen = new StringBuilder();

    resumen.append("Archivo: ").append(nombreArchivo).append("\n");
    resumen.append("Total registros: ").append(total).append("\n");
    resumen.append("Procesados OK: ").append(ok).append("\n");
    resumen.append("Con error: ").append(errores).append("\n");
   List<String> erroresArchivo = detalleErrores.remove(nombreArchivo);

   if (erroresArchivo != null) {
    for (String error : erroresArchivo) {
        resumen.append(error).append("\n");
    }
}
    resumen.append("=====================================\n");
    resumenArchivos.add(resumen.toString());
}
    
    public String guardarResumenTXT(String keyS3) {

        try {
            Path tempFile = Files.createTempFile("resumenPLD_", ".txt");
            StringBuilder contenido = new StringBuilder();
            contenido.append("========== RESUMEN ARCHIVOS ==========\n");
            for (String r : resumenArchivos) {
                contenido.append(r);
                if (!r.endsWith("\n")) {
                    contenido.append("\n");
                }
            }

            Files.writeString(
                    tempFile,
                    contenido.toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            log.info("Archivo resumen generado: {}", tempFile);

            // reutilizamos tu método existente
            subirArchivo(tempFile, keyS3);
            log.info("Resumen subido a S3 en key={}", keyS3);
         
            
            String resultado  = Files.readString(tempFile);
            
            
            log.info("Contenido del resumen:\n{}", resultado );
           Files.deleteIfExists(tempFile);
           
           return Encode.forHtml(resultado);
            //return resultado ;

        }
        catch (Exception e) {

            log.error("Error generando resumen PLD", e);
            return "ERROR";
        }
    }
    
    public void agregarLineaResumen(String linea) {
        resumenArchivos.add(linea);
    }
    
    public void limpiarResumen() {
        resumenArchivos.clear();
         detalleErrores.clear();
    }

 public void agregarDetalleError(String archivo,int linea,String descripcion) {
    detalleErrores.computeIfAbsent(archivo,k -> Collections.synchronizedList(new ArrayList<>()))
            .add("Linea: " + linea + " - " + descripcion);
} 
  
}