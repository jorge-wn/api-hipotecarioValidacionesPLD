package com.bancoppel.HipotecarioPLD.service;


import com.bancoppel.HipotecarioPLD.config.RutasRespuestaConfigAws;
import com.bancoppel.HipotecarioPLD.dto.ResultadoCargaDTOAws;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter; 
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultadoServiceAws {

    @Value("${app.timezone:UTC}")
    private String timezone;
    
    private final S3ArchivoService s3ArchivoService;
    private final LectorArchivoAws lectorArchivoAws;
    private final RutasRespuestaConfigAws rutasRespuestaConfig;
        
    // Control de archivos ya procesados
    private final ConcurrentMap<String, Boolean> archivosProcesados = new ConcurrentHashMap<>();
    // Control de finalización de procesos
    public final AtomicInteger procesosTerminados = new AtomicInteger(0);
    public final AtomicBoolean todosCompletos = new AtomicBoolean(false);
    private static final int TOTAL_PROCESOS = 6;

    private static final Pattern ARCHIVO_PERMITIDO =Pattern.compile("^[A-Za-z0-9._-]+\\.txt$");
    private final AtomicBoolean tieneErrores = new AtomicBoolean(false);
    private final ConcurrentMap<String, String> erroresProcesos = new ConcurrentHashMap<>();



    @Async
    public CompletableFuture<Void> procesarDesdeS3(ResultadoCargaDTOAws request) { 
    log.info("ProcesamientoHIPO | resultado={} | mensaje={} | Archivos = {}",
                request.getResultado(), request.getMensaje(),request.getArchivosProcesados());
        
        //  CAMBIO 1: Validar si la lista viene vacía o null
        List<String> listaArchivos = request.getArchivosProcesados();
        
        if (listaArchivos == null || listaArchivos.isEmpty()) {
            log.warn("No se encontraron archivos procesados en la respuesta Lambda. Se generará archivo vacío.");
            generarArchivoVacio(request); 
                //cambio para contar si genero txt vacio
                  int terminados =   procesosTerminados.incrementAndGet(); 
              	  //contador de archivos generados  y subidos al sftp
               
                     
                log.info("Procesos terminados {}/{}",terminados,TOTAL_PROCESOS);
                if (terminados >= TOTAL_PROCESOS) {
                    todosCompletos.set(true);
                    log.info("TODOS LOS PROCESOS FINALIZARON");
                }
            return CompletableFuture.completedFuture(null);
        }

     // 1️ Obtener nombre de archivo
        String nombreArchivo = listaArchivos.get(0);
        
        if (nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new SecurityException("Nombre de archivo vacío");
        }

        if (nombreArchivo.contains("..")
                || nombreArchivo.contains("/")
                || nombreArchivo.contains("\\")) {
            throw new SecurityException(
                    "Intento de Path Traversal detectado");
        }

        if (!ARCHIVO_PERMITIDO.matcher(nombreArchivo).matches()) {
            throw new SecurityException(
                    "Nombre de archivo inválido: " + nombreArchivo);
        }
        
        // Evitar reprocesamiento
        /* if (archivosProcesados.putIfAbsent(nombreArchivo, Boolean.TRUE) != null) {
            log.info("Archivo {} ya fue procesado, se ignora", nombreArchivo);
           // return;
            return CompletableFuture.completedFuture(null);
        }*/
 
        // 2️ Normalizar ruta base (SIN slash inicial ni final)
        String rutaS3Base = normalizarRuta(request.getRutaS3());
        String rutaSftpBase = normalizarRuta(request.getRutaSftp());
        System.out.println("Ruta base S3={} | Ruta base SFTP={}"+ rutaS3Base +"|"+ rutaSftpBase);
 
        // 3️ Descargar archivo desde S3
        Path archivoLocal = s3ArchivoService.descargarArchivo(rutaS3Base,nombreArchivo);
 
        // 4️ Procesar archivo (genera respuesta y la sube a S3)
         //boolean completado =  lectorArchivoAws.procesarArchivo(archivoLocal, nombreArchivo, request);
         boolean completado =  lectorArchivoAws.procesarArchivo( nombreArchivo, request);
                   
       	if (!completado) {
    registrarError(nombreArchivo, "Fallo en procesamiento del archivo o API retornó error");
}
         if (completado) { 
         //cambio para contar si genero txt vacio
           int terminados =   procesosTerminados.incrementAndGet(); 
       	  //contador de archivos generados  y subidos al sftp
        
              
         log.info("Procesos terminados {}/{}",terminados,TOTAL_PROCESOS);
         if (terminados >= TOTAL_PROCESOS) {
             todosCompletos.set(true);
             log.info("TODOS LOS PROCESOS FINALIZARON");
         }}
        return CompletableFuture.completedFuture(null);
    }
    
 
    // Nuevo método para generar archivo vacío
    private void generarArchivoVacio(ResultadoCargaDTOAws request) {
        String anio = request.getRutaS3().split("/")[3]; 
        String mes  = request.getRutaS3().split("/")[4];  
        try {

        	String fecha = LocalDateTime
        	        .now(ZoneId.of(timezone))
        	        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        	
            String originador = obtenerOriginador(request.getRutaS3());
            String validacion = obtenerTipoValidacion(request.getRutaS3());
            String rutaBase = rutasRespuestaConfig.obtenerRutaS3(originador, validacion);
            
            String nombreArchivo = "";

            if ("YAVE".equals(originador) && "PLD".equals(validacion)) {
                nombreArchivo = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_YAVE_" + fecha + ".txt";
            }
            else if ("KREDI".equals(originador) && "PLD".equals(validacion)) {
                nombreArchivo = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_KREDI_" + fecha + ".txt";
            }
            else if ("BANCOPPEL".equals(originador) && "PLD".equals(validacion)) {
                nombreArchivo = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_BANCOPPEL_" + fecha + ".txt";
            } 
            else if ("YAVE".equals(originador) && "SIC".equals(validacion)) {
                nombreArchivo = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_YAVE_" + fecha + ".txt";
            }
            else if ("KREDI".equals(originador) && "SIC".equals(validacion)) {
                nombreArchivo = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_KREDI_" + fecha + ".txt";
            }
            else if ("BANCOPPEL".equals(originador) && "SIC".equals(validacion)) {
                nombreArchivo = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_BANCOPPEL_" + fecha + ".txt";
            }
 
            // 🔹 Crear línea vacía con pipes
            List<String> lineasFinales = new ArrayList<>();

            if ("PLD".equals(validacion)) {
                lineasFinales.add(" | | | | | | | | | ");
            } else {
                lineasFinales.add(" | | | ");
            }

            
            String rutaDestino = normalizarRuta(rutaBase);
            String prefixDestinoCompleto = rutaDestino
                    + "/" + anio
                    + "/" + mes;
            s3ArchivoService.generarYSubirRespuestaS3(
                    nombreArchivo,
                    lineasFinales,
                    "/tmp",
                    prefixDestinoCompleto,
                    originador,
                    validacion
            );
            
            log.info("Archivo vacío generado y subido correctamente: {}", nombreArchivo);
            s3ArchivoService.agregarResumenArchivo("Archivo vacío generado y subido correctamente: " + nombreArchivo, 0, 0, 0);
           
            
        } catch (Exception e) {
            log.error("Error generando archivo vacío", e);
        }
    }  

    // 🔥 Método auxiliar simple (puedes mejorarlo según tu lógica real)
    private String obtenerOriginador(String ruta) {
        if (ruta.contains("YAVE")) return "YAVE";
        if (ruta.contains("KREDI")) return "KREDI";
        if (ruta.contains("BANCOPPEL")) return "BANCOPPEL";
        return "";
    }

    private String obtenerTipoValidacion(String ruta) {
        if (ruta.contains("PLD")) return "PLD";
        if (ruta.contains("CLIENTE_COPPEL")) return "SIC";
        return "";
    }

    private String normalizarRuta(String ruta) {
        if (ruta == null) {
            return "";
        }
       return ruta
                .replaceAll("^/+", "")   // quitar slash inicial
                .replaceAll("/+$", "");  // quitar slash final
    }
    
    public void reiniciarContador() {
        procesosTerminados.set(0);
        todosCompletos.set(false);

         tieneErrores.set(false);
    erroresProcesos.clear();
    }

    public boolean todosCompletos() {
        return todosCompletos.get();
    }

    public int getProcesosTerminados() {
        return procesosTerminados.get();
    }
  
  public void registrarError(String proceso, String mensaje) {
    tieneErrores.set(true);
    erroresProcesos.put(proceso, mensaje);
    log.error("Proceso {} marcado con error: {}", proceso, mensaje);
}

public boolean tieneErrores() {
    return tieneErrores.get();
}

public Map<String, String> obtenerErrores() {
    return erroresProcesos;
}

}
 