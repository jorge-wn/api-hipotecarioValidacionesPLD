package com.bancoppel.HipotecarioPLD.service;

import com.bancoppel.HipotecarioPLD.externalServices.ExternaServiceApache;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.http.HttpTransportOptions;
import jakarta.persistence.PersistenceException;

import com.bancoppel.HipotecarioPLD.dto.SubirRequestDTOAws;
import com.bancoppel.HipotecarioPLD.Util.sanitizeLog;
import com.bancoppel.HipotecarioPLD.config.configManager;
import com.bancoppel.HipotecarioPLD.constants.MessagesError;
import com.bancoppel.HipotecarioPLD.dto.BitacoraDTO;
import com.bancoppel.HipotecarioPLD.dto.BitacoraDetalleDTO;
import com.bancoppel.HipotecarioPLD.dto.LambdaRequestDTOAws;
import com.bancoppel.HipotecarioPLD.dto.LambdaResponseDTOAws;
import com.bancoppel.HipotecarioPLD.dto.ResultadoCargaDTOAws;
import com.bancoppel.HipotecarioPLD.entity.BitacoraDetalleEntity;
import com.bancoppel.HipotecarioPLD.entity.BitacoraEntity;
import com.bancoppel.HipotecarioPLD.exceptions.FileProcessingException;
import com.bancoppel.HipotecarioPLD.repository.BitacoraDetalleRepository;
import com.bancoppel.HipotecarioPLD.repository.BitacoraRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.slf4j.MDC;
import java.io.File;

import com.bancoppel.HipotecarioPLD.Util.StringCleaner;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import com.bancoppel.HipotecarioPLD.config.RutasRespuestaConfigAws;
import com.bancoppel.HipotecarioPLD.service.S3ArchivoService;

@Service
//@Slf4j
@RequiredArgsConstructor
public class LectorArchivoAws {

	@Value("${app.timezone:UTC}")
    private String timezone;
	
    private final RutasRespuestaConfigAws rutasRespuestaConfig;
    private final S3ArchivoService s3archivoservice;
    private final configManager config;
    private final ObjectMapper mapper = new ObjectMapper();
    private final BitacoraRepository bitacoraRepository;
    private final BitacoraDetalleRepository bitacoraDetalleRepository;
    private final WebServiceClient webServiceClient;
    private final BigQueryService bigQueryService;
    private final ExternaServiceApache externaServiceApache;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LectorArchivoAws.class);
    private static final List<String> resumenArchivos = Collections.synchronizedList(new ArrayList<>());

    // Usar AtomicInteger para contadores de registros en un entorno multihilo
    // Hacemos estos a nivel de instancia, pero los reseteamos por cada archivo procesado
    private final AtomicInteger totalRegistrosProcesados = new AtomicInteger(0);
    private final AtomicInteger registrosProcesadosOk = new AtomicInteger(0);
    private final AtomicInteger registrosConErrorEstructura = new AtomicInteger(0);
    private final List<String> currentFileErrorLines = Collections.synchronizedList(new ArrayList<>()); // Lista de errores para el archivo actual
   
    // Definir el tamaño del pool de hilos para el procesamiento de líneas
    private final ExecutorService lineProcessingExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
   	private static final Path BASE_DIR = Paths.get("/tmp/hipotecario").toAbsolutePath().normalize();

  
    

    public String getProcessingSummary(String fileName) {   	
    	int registrosEror = totalRegistrosProcesados.get() -  registrosProcesadosOk.get();    	
    	return "Nombre Archivo: " + fileName +
    	           " Total de Registros: " + totalRegistrosProcesados.get() +
    	           " Registros Procesados : " + registrosProcesadosOk.get() +
    	           " Registros Con error: "+ registrosEror ;
    }
    /////aws
    //public void procesarArchivo(Path archivoLocal1,String nombreArchivo,ResultadoCargaDTOAws request) {
    //public boolean procesarArchivo(Path archivoLocal1,String nombreArchivo,ResultadoCargaDTOAws request) {
        public boolean procesarArchivo(String nombreArchivo,ResultadoCargaDTOAws request) {
       	
    	log.info("Iniciando el procesamiento del archivo: {}", nombreArchivo);
        
    	long startTime = System.currentTimeMillis();
      	
        String fecha = LocalDateTime
    	        .now(ZoneId.of(timezone))
    	        .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String ipAddress = "";
        MDC.put("sourceIP", ipAddress);
        MDC.put("logType", "SYSTEM");
        MDC.put("status", "SUCCESS");
        MDC.put("tracingId", "poner");
        MDC.put("eventType", "FileRead");
        MDC.put("method", "procesarArchivo");
        MDC.put("destinationIP", ipAddress);
        MDC.put("aditionalInfo", "");
        String originador ="NA";
        String Proceso ="NA";
        String nombreArchivor ="";
        
        List<String> lineasFinales = new ArrayList<>();
        
        Path archivoLocal = null;
        
        try {
     // ======================================================================
        	// 1. Construir la ruta inicial
        	archivoLocal = BASE_DIR.resolve(nombreArchivo).normalize();

        	// 2. Sobreescribir "archivoLocal" con la ruta canónica segura
        	String canonicalPathStr = archivoLocal.toFile().getCanonicalPath();
        	archivoLocal = Paths.get(canonicalPathStr);

        	// 3. Validar que siga dentro del directorio permitido (Usando la ruta canónica)
        	if (!archivoLocal.startsWith(BASE_DIR.toRealPath())) {
        	    throw new SecurityException("Path Traversal detectado");
        	}

        	// 4. Validar que exista y sea un archivo regular
        	if (!Files.exists(archivoLocal)) {
        	    throw new SecurityException("El archivo no existe");
        	}

        	if (!Files.isRegularFile(archivoLocal)) {
        	    throw new SecurityException("Archivo inválido");
        	}

        	// 5. Leer el archivo usando la variable unificada
        	List<String> registrosTotales = Files.readAllLines(archivoLocal)
        	        .stream()
        	        .filter(linea -> linea != null && !linea.trim().isEmpty())
        	        .collect(Collectors.toList());
       
            // Validación de estructura y procesamiento según tipo de archivo
            if (nombreArchivo.startsWith("CHIPO_PLD_LAYOUT_KREDI")) {
            	originador ="KREDI";
            	Proceso = "PLD";
                registrosConErrorEstructura.set(validarEstructuraPLD_KREDI(archivoLocal, nombreArchivo));
                lineasFinales.addAll(procesarPLDFile(nombreArchivo, registrosTotales));
            } else if (nombreArchivo.startsWith("CHIPO_PLD_LAYOUT_BANCOPPEL")) {
            	originador ="BANCOPPEL";
            	Proceso = "PLD";
                registrosConErrorEstructura.set(validarEstructuraPLD_bancoppel(archivoLocal, nombreArchivo));
                lineasFinales.addAll(procesarPLDFile(nombreArchivo, registrosTotales));
            } else if (nombreArchivo.startsWith("CHIPO_PLD_LAYOUT_YAVE")) {
            	originador ="YAVE";
            	Proceso = "PLD";
                registrosConErrorEstructura.set(validarEstructuraPLD_YAVE(archivoLocal, nombreArchivo));
                lineasFinales.addAll(procesarPLDFile(nombreArchivo, registrosTotales));
            } else if (nombreArchivo.startsWith("CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_KREDI")) {
            	originador ="KREDI";
            	Proceso = "SIC";
                registrosConErrorEstructura.set(validarEstructuraSIC_KREDI(archivoLocal, nombreArchivo));
                lineasFinales.addAll(procesarSICFile(nombreArchivo, registrosTotales));
            } else if (nombreArchivo.startsWith("CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_BANCOPPEL")) {
            	originador ="BANCOPPEL";
            	Proceso = "SIC";
                registrosConErrorEstructura.set(validarEstructuraSIC_bancoppel(archivoLocal, nombreArchivo));
                lineasFinales.addAll(procesarSICFile(nombreArchivo, registrosTotales));
            } else if (nombreArchivo.startsWith("CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_YAVE")) {
            	originador ="YAVE";
            	Proceso = "SIC";
                registrosConErrorEstructura.set(validarEstructuraSIC_YAVE(archivoLocal, nombreArchivo));
                lineasFinales.addAll(procesarSICFile(nombreArchivo, registrosTotales));
            } else {
                log.warn("LOG: Nombre de archivo no reconocido: {}", nombreArchivo);
                SaveBitacora(nombreArchivo, "Error archivo no reconocido", registrosTotales.size(), LocalDateTime.now(), 0, 0);
            }
            ///Logica para poner nombre de los archivos de respuesta///
            if ("YAVE".equals(originador) && "PLD".equals(Proceso)) {
                nombreArchivor = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_YAVE_" + fecha + ".txt";
            }
            else if ("KREDI".equals(originador) && "PLD".equals(Proceso)) {
                nombreArchivor = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_KREDI_" + fecha + ".txt";
            }
            else if ("BANCOPPEL".equals(originador) && "PLD".equals(Proceso)) {
                nombreArchivor = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_BANCOPPEL_" + fecha + ".txt";
            } 
            else if ("YAVE".equals(originador) && "SIC".equals(Proceso)) {
                nombreArchivor = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_YAVE_" + fecha + ".txt";
            }
            else if ("KREDI".equals(originador) && "SIC".equals(Proceso)) {
                nombreArchivor = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_KREDI_" + fecha + ".txt";
            }
            else if ("BANCOPPEL".equals(originador) && "SIC".equals(Proceso)) {
                nombreArchivor = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_BANCOPPEL_" + fecha + ".txt";
            }
         // 1️⃣ Obtener ruta base desde properties
            System.out.println("originador"+ originador);
            System.out.println("Proceso"+ Proceso);     
            String rutaBase = rutasRespuestaConfig.obtenerRutaS3(originador, Proceso);
            // 2️⃣ Limpiar posibles slashes
            System.out.println("rutaBase: "+ rutaBase);  
            rutaBase = rutaBase.replaceAll("^/+", "").replaceAll("/+$", "");
            System.out.println("rutaBase: "+ rutaBase);
            // 3️⃣ Extraer año y mes desde el archivo original o request
            String anio = request.getRutaS3().split("/")[3];  // ejemplo 
            String mes  = request.getRutaS3().split("/")[4];  // ejemplo
               // 4️⃣ Construir ruta final correcta
            String prefixDestinoCompleto = rutaBase+ "/" + anio + "/" + mes;
            
            System.out.println("PrefixDestino FINAL = {}"+ prefixDestinoCompleto);
            System.out.println("año "+ "'"+anio+ "'");
            System.out.println("Mes "+ "'"+ mes+"'");
             
            // 5️⃣ Subir a S3 con ruta completa
			s3archivoservice.generarYSubirRespuestaS3(
                    nombreArchivor,
                    lineasFinales,
                    "/tmp",
                    prefixDestinoCompleto,
                    originador,
                    Proceso
            );
           
            long endTime = System.currentTimeMillis();
            MDC.put("elapsedTime", String.valueOf(endTime - startTime));
            log.info("Procesamiento completo del archivo {} en {} ms", nombreArchivor, (endTime - startTime));

            return true;
        } catch (Exception e) {
        	return false;
        	//System.out.println("Error procesando archivo" + nombreArchivo + e);
          //  throw new RuntimeException("Error en procesamiento de archivo PLD/SIC", e);
        } finally {
            try {
                if (archivoLocal != null) {
                    Files.deleteIfExists(archivoLocal);
                }
            } catch (IOException e) {
                log.warn("No fue posible eliminar el archivo temporal: {}", archivoLocal);
            }

            MDC.clear();
        }
    }
    
    
    //private String[] procesarPLDFile(String rutaArchivo, List<String> registrosTotales) {
    	private List<String> procesarPLDFile(String rutaArchivo, List<String> registrosTotales) {
        MDC.put("method", "procesarPLDFile");
        long startTime = System.currentTimeMillis();
        List<Future<String>> futures = new ArrayList<>();
        List<String> lineasProcesadasSincronizadas = Collections.synchronizedList(new ArrayList<>());
        String nombreArchivo = Paths.get(rutaArchivo).getFileName().toString();
        
        String[] partes = nombreArchivo.replace(config.getExtension(), "").split("_");
        String originador = partes[partes.length - 2];
        String fechaArchivo = partes[partes.length - 1];
        String nuevoNombreArchivo = "CHIPO_PLD_LAYOUT_LISTASNEGRAS_" + originador + "_" + fechaArchivo + config.getExtension();
        
     // Contadores locales
        AtomicInteger totalProcesados = new AtomicInteger(0);
        AtomicInteger procesadosOk = new AtomicInteger(0);
        AtomicInteger erroresEstructura = new AtomicInteger(0);
        
    for (int i = 0; i < registrosTotales.size(); i++) {
    final int numeroLinea = i + 1;
    final String linea1 = registrosTotales.get(i);

    futures.add(lineProcessingExecutor.submit(() -> {
      //  for (String linea1 : registrosTotales) {
        //    futures.add(lineProcessingExecutor.submit(() -> {                      
            	String lineaConRespuesta = "";
                try {
                    if (linea1 == null || linea1.trim().isEmpty()) {
                       return "";
                    }
                    
                  String linea  =  StringCleaner.cleanText(linea1);
                    
                    /*valida que la linea no tenga caracteres extraños si tiene lo regresa en el txt de respuesta "|9|9|9|I" */
                    if (contieneCaracteresNoPermitidos(linea)) {
                    	String lineaSeguro = sanitizeLog.sanitizeForLog(linea);
                        log.warn("LOG: {}: {}",MessagesError.LINEACONERROR, lineaSeguro);
                        lineaConRespuesta = linea + "|9|9|9|I";
                        try {
                        	SaveBitacoraDetalle(nombreArchivo, "Error Caracteres Especiales",   String.valueOf(numeroLinea), LocalDateTime.now());
                        }
                        catch (Exception e) {
							log.error("Error al guardar bitacora detalle:"+ e.getMessage());					
							}                        
                        erroresEstructura.incrementAndGet();
                        lineasProcesadasSincronizadas.add(lineaConRespuesta);
                       return lineaConRespuesta;                   
                    }
                    /*******************************************************************************************/
                    /*valida si se procesa por el servicio de nameching*/
                    if ("SI".equalsIgnoreCase(config.getNameMachingActivo())) {
                    	/*valida si se procesa por el servicio de nameching por openshift */
                        if ("SI".equalsIgnoreCase(config.getNameMachingOpenshif())) {
                           
                    	// Modo Name Matching openshitf
                        Map<String, Object> jsonMap = convertirLineaAJsonNameMaching(linea);   
                        /*valida si se va realizar la consulta a peps de lo contrario se envia 9 en el txt de respuesta*/
                        String pepResult = "9";
                        if ("SI".equalsIgnoreCase(config.getValidacionPEPSActivo())) {
                        	/*valida si se va realizar la consulta a peps por el bus*/
                        	if ("SI".equalsIgnoreCase(config.getValidacionPepsxbus())) {
                            String vcodretPep = conversionjsonPeps(jsonMap);
                            pepResult = ("000".equals(vcodretPep)) ? "1" : "0";
                        	  }
                        	/*valida si se va realizar la consulta por ws dinamico envia en properties los valores de json entrada, el valor a buscar y campo a buscar para luego llenar la respuesta en el txt */
                        	  else {
                        		  Map<String, Object> json = construirJsonDinami(linea, config.getEstructurajsonPEP());
                                  Map<String, Object> respuesta = webServiceClient.enviarDatosPeps(json);
                                  if (respuesta != null && respuesta.containsKey(config.getCampoabuscarpep())) {
                                      Object valorPep = respuesta.get(config.getCampoabuscarpep());
                                      if ("true".equalsIgnoreCase(String.valueOf(valorPep)) || "1".equals(String.valueOf(valorPep))) {
                                          pepResult = "1";
                                      } else {
                                          pepResult = "0";
                                      }
                                  } 
                                  else {
                                      pepResult = "9";
                                  }
                        	  }
                        	
                        }
                        String jsonString = mapper.writeValueAsString(jsonMap);
                        String jsonStringSeguro = sanitizeLog.sanitizeForLog(jsonString);


                        boolean match = false;
                        try {
                      	match = externaServiceApache.validarRegistroNameMatchingHttpClient(jsonMap);//version final
                      	  }
                      	  catch (Exception ex) {
                      		  lineaConRespuesta = linea + "|9|" + pepResult + "|9|I";
                                lineasProcesadasSincronizadas.add(lineaConRespuesta);
                                erroresEstructura.incrementAndGet();
                                SaveBitacoraDetalle(nombreArchivo, "Error servicio name matching",   String.valueOf(numeroLinea), LocalDateTime.now());
                                return lineaConRespuesta;  
                      	  }

                        lineaConRespuesta = linea + "|" + (match ? "1" : "0") + "|" + pepResult + "|9|I";
                        }
                        /*******************************************************************************************/
                        else {
                        	   
                        	// Modo Name Matching consumo por el bus
                            Map<String, Object> jsonMap = convertirLineaAJsonNameMachingPorBus(linea);   
                       String jsonpinrt = mapper.writeValueAsString(jsonMap);
                            System.out.println("JSON: " + jsonpinrt);
                            /*valida si se va realizar la consulta a peps de lo contrario se envia 9 en el txt de respuesta*/
                            String pepResult = "9";
                            if ("SI".equalsIgnoreCase(config.getValidacionPEPSActivo())) {
                            	/*valida si se va realizar la consulta a peps por el bus*/
                            	if ("SI".equalsIgnoreCase(config.getValidacionPepsxbus())) {
                                String vcodretPep = conversionjsonPeps(jsonMap);
                                pepResult = ("000".equals(vcodretPep)) ? "1" : "0";
                            	  }
                            	/*valida si se va realizar la consulta por ws dinamico envia en properties los valores de json entrada, el valor a buscar y campo a buscar para luego llenar la respuesta en el txt */
                            	  else {
                            		  Map<String, Object> json = construirJsonDinami(linea, config.getEstructurajsonPEP());
                                      Map<String, Object> respuesta = webServiceClient.enviarDatosPeps(json);
                                      if (respuesta != null && respuesta.containsKey(config.getCampoabuscarpep())) {
                                          Object valorPep = respuesta.get(config.getCampoabuscarpep());
                                          if ("true".equalsIgnoreCase(String.valueOf(valorPep)) || "1".equals(String.valueOf(valorPep))) {
                                              pepResult = "1";
                                          } else {
                                              pepResult = "0";
                                          }
                                      } 
                                      else {
                                          pepResult = "9";
                                      }
                            	  }	
                        	
                        }

                            String jsonString = mapper.writeValueAsString(jsonMap);
                                                 
                         //   boolean match = webServiceClient.validarRegistro(jsonMap);
                            boolean match = false;
                            try {
                            	String jsonMapSeguro = sanitizeLog.sanitizeForLog(jsonString);


                            	  try {
                            	match = externaServiceApache.validarRegistroNameMatchingHttpClient(jsonMap);//version final
                            	  }
                            	  catch (Exception ex) {
                            		  lineaConRespuesta = linea + "|9|" + pepResult + "|9|I";
                                      lineasProcesadasSincronizadas.add(lineaConRespuesta);
                                      erroresEstructura.incrementAndGet();
                                      SaveBitacoraDetalle(nombreArchivo, "Error servicio name matching",   String.valueOf(numeroLinea), LocalDateTime.now());
                                      return lineaConRespuesta;  
                            	  }
                            	
                            } catch (Exception ex) {
                                log.error("Error de conexión al WS de Name Matching: {}", ex.getMessage());
                               
                                try {
                                SaveBitacoraDetalle(nombreArchivo, "Fallo conexión WS NameMatching",  String.valueOf(numeroLinea), LocalDateTime.now());
                                }
                                catch (Exception e) {
        							log.error("Error al guardar bitacora detalle:"+ e.getMessage());					
        							}           
                                // Aquí puedes asignar una marca clara para fallos de conexión
                                lineaConRespuesta = linea + "|Error WS|" + pepResult + "|9|I";
                                lineasProcesadasSincronizadas.add(lineaConRespuesta);
                                erroresEstructura.incrementAndGet();
                                return lineaConRespuesta;
                            } 
                            lineaConRespuesta = linea + "|" + (match ? "1" : "0") + "|" + pepResult + "|9|I";
                        }
                    }
                    
            /*********************************************************************************************************************/
                    /*validacion de listas negras sevicio actual cuando name maching no esta activo*/
                    else {

                        // Modo Listas Negras
                        Map<String, Object> jsonMapLN = convertirLineaAJsonListasNegras(linea);
                        Map<String, Object> respuestaLN = webServiceClient.validarRegistroLN(jsonMapLN);
                        String valorListaNegra = "0";
                        String pepResult = "9";    
                        if (respuestaLN != null && respuestaLN.containsKey("datos")) {
                            Map<String, Object> datos = (Map<String, Object>) respuestaLN.get("datos");
                            if (datos != null && "1".equals(String.valueOf(datos.get("listaNegra")))) {
                                valorListaNegra = "1";
                            }
                        }
                        /*valida si se va realizar la consulta a peps de lo contrario se envia 9 en el txt de respuesta*/
                        if ("SI".equalsIgnoreCase(config.getValidacionPEPSActivo())) {
                        	/*valida si se va realizar la consulta a peps por el bus*/
                        	if ("SI".equalsIgnoreCase(config.getValidacionPepsxbus())) {
                                String vcodretPep = conversionjsonPeps(jsonMapLN);
                                pepResult = ("000".equals(vcodretPep)) ? "1" : "0";
                            }/*valida si se va realizar la consulta por ws dinamico envia en properties los valores de json entrada, el valor a buscar y campo a buscar para luego llenar la respuesta en el txt */
                            else {
                      		  Map<String, Object> json = construirJsonDinami(linea, config.getEstructurajsonPEP());                               
                                Map<String, Object> respuesta = webServiceClient.enviarDatosPeps(json);
                                if (respuesta != null && respuesta.containsKey(config.getCampoabuscarpep())) {
                                    Object valorPep = respuesta.get(config.getCampoabuscarpep());
                                    if ("true".equalsIgnoreCase(String.valueOf(valorPep)) || "1".equals(String.valueOf(valorPep))) {
                                        pepResult = "1";
                                    } else {
                                        pepResult = "0";
                                    }
                                } 
                                else {
                                    pepResult = "9";
                                }
                      	  }
                        }    
                        lineaConRespuesta = linea + "|" + valorListaNegra + "|" + pepResult + "|9|I";
                    }
     
                    lineasProcesadasSincronizadas.add(lineaConRespuesta);
                    String lineaConRespuestaSeguro = sanitizeLog.sanitizeForLog(lineaConRespuesta);
                  
                       
                    totalProcesados.incrementAndGet();
                    procesadosOk.incrementAndGet();
                    return lineaConRespuesta;
                    
                	} catch (Exception e) {
                    log.error("Error al procesar línea PLD: {}", e, e);
                    currentFileErrorLines.add(linea1 + " | Error: " + e);
                    lineaConRespuesta = linea1 + "|9|9|9|I";
                    erroresEstructura.incrementAndGet();
                    throw new RuntimeException("Error procesando línea PLD: " + linea1, e);
                }
            }));
        }    
        for (Future<String> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error al obtener resultado de hilo PLD: {}", e);
            }
        }
         
     // Construir resumen texto plano para este archivo
        int total = registrosTotales.size();
        int ok = procesadosOk.get();
        int errores = erroresEstructura.get();
        // Guardar bitácora final
try {
        SaveBitacora(nombreArchivo, "Procesado", total, LocalDateTime.now(), ok, errores);
                }
   catch (Exception e) {
	log.error("Error al guardar bitacora :"+ e.getMessage());					
             }

        long endTime = System.currentTimeMillis();
        MDC.put("elapsedTime", String.valueOf(endTime - startTime));
        MDC.remove("method");
     
    
        s3archivoservice.agregarResumenArchivo(
                nuevoNombreArchivo,
                total,
                ok,
                errores
        );
      
        return new ArrayList<>(lineasProcesadasSincronizadas);
    }
      
    	
/*procesa los archivos de puntualidad coppel*/
    	private List<String> procesarSICFile(String rutaArchivo, List<String> registrosTotales) throws IOException {
        MDC.put("method", "procesarSICFile");
        long startTime = System.currentTimeMillis();
        AtomicInteger registrosProcesadosOkLocal = new AtomicInteger(0);
        AtomicInteger registrosConErrorLocal = new AtomicInteger(0);   
        List<Future<String>> futures = new ArrayList<>();
        List<String> lineasProcesadasSincronizadas = Collections.synchronizedList(new ArrayList<>());       
        String nombreArchivo = Paths.get(rutaArchivo).getFileName().toString();
        
        // CAMBIO: aquí se renombra el archivo de salida
        String[] partes = nombreArchivo.replace(config.getExtension(), "").split("_");
        String originador = partes[partes.length - 2];
        String fechaArchivo = partes[partes.length - 1];
        String nuevoNombreArchivo = "CHIPO_VALIDACIONCLIENTE_LAYOUT_SIC_RESPUESTA_" + originador + "_" + fechaArchivo + config.getExtension();
        
        for (int i = 0; i < registrosTotales.size(); i++) {
    final int numeroLinea = i + 1;   // Línea real del TXT (empieza en 1)
    final String linea = registrosTotales.get(i);
        //for (String linea : registrosTotales) {
            futures.add(lineProcessingExecutor.submit(() -> {
           
                String lineaConRespuesta = "";
                try {               	
                	
                	/*valida que la linea no tenga caracteres extraños si tiene lo regresa en el txt de respuesta "|9" */
               String linea1 =	 StringCleaner.cleanText(linea);
               	
                	if (contieneCaracteresNoPermitidosSIC(linea1)) {
                    	String lineaSeguro = sanitizeLog.sanitizeForLog(linea1);
                    	 
                        log.warn("LOG: {}: {}",MessagesError.LINEACONERRORSIC, lineaSeguro);
                        lineaConRespuesta = linea1 + "|9";
                        try {
                        SaveBitacoraDetalle(nombreArchivo, "Error Caracteres Especiales",  String.valueOf(numeroLinea), LocalDateTime.now());
                        }
                        catch (Exception e) {
							log.error("Error al guardar bitacora detalle:"+ e.getMessage());					
					    }           
                        registrosConErrorLocal.incrementAndGet();
                        lineasProcesadasSincronizadas.add(lineaConRespuesta);
                        return lineaConRespuesta;
                    }               	
                    String[] valores = linea1.split("\\|");
                    if (valores.length > 1) {
                    	String RFC = valores[0].trim();
                    	String nombreCompleto = valores[1].trim();
                        log.info("Realizando peticion a BigQuery para RFC: {}, Nombre: {}", RFC, nombreCompleto);
                        String resultadoBigquery = bigQueryService.existeNombre(nombreCompleto); 
                        
                        log.info(nombreCompleto+": Resultado de la puntualidad: "+ resultadoBigquery  );
                        String valorAnexar = "";
                       
                     /*  if("A".equals(resultadoBigquery) || "B".equals(resultadoBigquery)) {
                    	   valorAnexar = "|0|";
                       }else if ("C".equals(resultadoBigquery)|| "D".equals(resultadoBigquery) || "Z".equals(resultadoBigquery)){
                    	   valorAnexar = "|1|";
                       }else if ("N".equals(resultadoBigquery) ){
                    	   valorAnexar = "|2|";
                       else if (resultadoBigquery = ""){
                        	   valorAnexar = "|2|";
                       }else { 
                    	   valorAnexar = "|9|";
                       }*/
                       if("1".equals(resultadoBigquery) || "2".equals(resultadoBigquery)) {
                     	   valorAnexar = "|0|";
                        }else if ("0".equals(resultadoBigquery)){
                     	   valorAnexar = "|1|";
                        }else if ("null".equals(resultadoBigquery) || "".equals(resultadoBigquery)  || " ".equals(resultadoBigquery) || "NO_EXISTE".equals(resultadoBigquery) ){
                     	   valorAnexar = "|2|";
                        }else { 
                     	   valorAnexar = "|9|";
                        }
                       
                      
                       
                       lineaConRespuesta = linea1 + valorAnexar;
                       registrosProcesadosOkLocal.incrementAndGet();
                    }
                    else {
                        lineaConRespuesta = linea1 + "|-1|"; //indica error
                        String lineaSeguro = sanitizeLog.sanitizeForLog(linea1);
                        log.debug("LOG: Línea SIC procesada (sin nombre válido): {}", lineaSeguro);
                        currentFileErrorLines.add(linea1 + " | Error: Nombre no encontrado");
                        try {
                        SaveBitacoraDetalle(nombreArchivo, "Error Nombre SIC",String.valueOf(numeroLinea), LocalDateTime.now());
                        }
                        catch (Exception e) {
							log.error("Error al guardar bitacora detalle:"+ e.getMessage());					
					    }           
                        registrosConErrorLocal.incrementAndGet();
                    }
                    
                    lineasProcesadasSincronizadas.add(lineaConRespuesta);
                    String lineaConRespuestaSeguro = sanitizeLog.sanitizeForLog(lineaConRespuesta);
                    log.debug("LOG: Línea SIC procesada: {}", lineaConRespuestaSeguro);
                    return lineaConRespuesta;

                } catch (BigQueryException  e) {
                    int status = e.getCode();  // ← aquí capturas el 403
                    log.error("Error BigQuery status: {}", status);
                    log.error("Error al procesar línea SIC: {}", e, e);
                    currentFileErrorLines.add(linea + " | Error: " + e);
                    String lineaError = linea + "|9|";
                    currentFileErrorLines.add(linea + " | Error: " + e.getMessage());                   
                    try {
                    SaveBitacoraDetalle(nombreArchivo, "Error Procesamiento SIC",String.valueOf(numeroLinea), LocalDateTime.now());
                    }
                    catch (Exception q) {
						log.error("Error al guardar bitacora detalle:"+ q.getMessage());					
				    }           
                    registrosConErrorLocal.incrementAndGet();
                    lineasProcesadasSincronizadas.add(lineaError);

                    return lineaError;
                  //  return linea + "|-1|"; 
                }
            }));
        }
        for (Future<String> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error al obtener resultado de hilo SIC: {}", e);
            }
        }

        
        
        long endTime = System.currentTimeMillis();
        MDC.put("elapsedTime", String.valueOf(endTime - startTime));
        MDC.remove("method");
        int total = registrosTotales.size();
        int ok = registrosProcesadosOkLocal.get();
        int errores = registrosConErrorLocal.get();
        
     try {
        // Guardar bitácora final
        SaveBitacora(nombreArchivo, "Procesado", total, LocalDateTime.now(), ok, errores);
     }
     catch (Exception e) {
			log.error("Error al guardar bitacora :"+ e.getMessage());					
	    }        
       
     //guarda resumen de archivo 
     s3archivoservice.agregarResumenArchivo(nuevoNombreArchivo,total,ok,errores);
  
       // return new String [] {nuevoNombreArchivo,resumen};
        return new ArrayList<>(lineasProcesadasSincronizadas);
    }
    
    private int validarEstructuraPLD_KREDI(Path archivoPath, String nombreArchivo) throws IOException {
        Pattern pattern = Pattern.compile("^\\d+\\|[^|]+\\|\\d{2}/\\d{2}/\\d{4}\\|[A-Z0-9]{13}\\|KREDI$");
        return contarErroresEstructura(archivoPath, pattern, nombreArchivo);
    }

    private int validarEstructuraPLD_bancoppel(Path archivoPath, String nombreArchivo) throws IOException {
        Pattern pattern = Pattern.compile("^\\d+\\|[^|]+\\|\\d{2}/\\d{2}/\\d{4}\\|[A-Z0-9]{13}\\|BANCOPPEL$");
        return contarErroresEstructura(archivoPath, pattern, nombreArchivo);
    }

    private int validarEstructuraPLD_YAVE(Path archivoPath, String nombreArchivo) throws IOException {
        Pattern pattern = Pattern.compile("^\\d+\\|[^|]+\\|\\d{2}/\\d{2}/\\d{4}\\|[A-Z0-9]{13}\\|YAVE$");
        return contarErroresEstructura(archivoPath, pattern, nombreArchivo);
    }

    private int validarEstructuraSIC_KREDI(Path archivoPath, String nombreArchivo) throws IOException {
        Pattern pattern = Pattern.compile("^[A-Z0-9]{13}\\|[^|]+\\|\\d{4}-\\d{2}-\\d{2}\\|KREDI$");
    	return contarErroresEstructura(archivoPath, pattern, nombreArchivo);
    }

    private int validarEstructuraSIC_bancoppel(Path archivoPath, String nombreArchivo) throws IOException {
        Pattern pattern = Pattern.compile("^[A-Z0-9]{13}\\|[^|]+\\|\\d{4}-\\d{2}-\\d{2}\\|BANCOPPEL$");
        return contarErroresEstructura(archivoPath, pattern, nombreArchivo);
    }

    private int validarEstructuraSIC_YAVE(Path archivoPath, String nombreArchivo) throws IOException {
        Pattern pattern = Pattern.compile("^[A-Z0-9]{13}\\|[^|]+\\|\\d{4}-\\d{2}-\\d{2}\\|YAVE$");
        return contarErroresEstructura(archivoPath, pattern, nombreArchivo);
    }

    private int contarErroresEstructura(Path archivoPath, Pattern pattern, String nombreArchivo) throws IOException {
        AtomicInteger errores = new AtomicInteger(0);
        int numeroLinea = 0;
        
        File inputFile = new File(archivoPath.toString());
        String canonicalPath = inputFile.getCanonicalPath();

        List<String> lineas = Files.readAllLines(Paths.get(canonicalPath))
                .stream()
                .filter(linea -> linea != null && !linea.trim().isEmpty())
                .collect(Collectors.toList());
        
        List<Future<?>> futures = new ArrayList<>();
        for (String linea : lineas) {
            numeroLinea = +1;
            String numLinea= String.valueOf(numeroLinea);
            futures.add(lineProcessingExecutor.submit(() -> {
                Matcher matcher = pattern.matcher(linea);
                if (!matcher.matches()) {
                    errores.incrementAndGet();
                    try {
                    SaveBitacoraDetalle(nombreArchivo, "Error de Estructura", numLinea, LocalDateTime.now());
                    }
                    catch (Exception e) {
						log.error("Error al guardar bitacora detalle:"+ e.getMessage());					
				    }           
                }
            }
            ));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.error("Error en hilo de validación de estructura: {}", e);
            }
        }
        return errores.get();
    }
  
    /*crea el json a enviar al servicio de name maching */
    private Map<String, Object> convertirLineaAJsonNameMaching(String linea) {
        String[] valores = linea.split("\\|");
        String fechaOriginal = valores.length > 2 ? valores[2] : "";
        String fechanac = fechaOriginal.replaceAll("/", "");// se formatea la fecha en forma DDmmAAAAA

        String nombreCompleto = valores.length > 1 ? valores[1].trim() : "";
        Map<String, String> nombreDesglosado = NombreParser.desglosarNombre(nombreCompleto);
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("fecha_nac", fechanac);
        jsonMap.put("nombre_1", nombreDesglosado.get("nombre_1"));
        jsonMap.put("nombre_2", nombreDesglosado.get("nombre_2"));
        jsonMap.put("apellido_paterno", nombreDesglosado.get("apellido_paterno"));
        jsonMap.put("apellido_materno", nombreDesglosado.get("apellido_materno"));

        jsonMap.put("num_cte", valores.length > 0 ? tryParseInt(valores[0]) : 0);
        jsonMap.put("num_cte_sol", valores.length > 6 ? tryParseInt(valores[6]) : 0);

        jsonMap.put("sucursal", valores.length > 7 ? valores[7] : "HIPO_PLD");
        jsonMap.put("usuario", valores.length > 8 ? valores[8] : "HIPO");
        jsonMap.put("tipo_sol", valores.length > 9 ? valores[9] : "PLD");
        jsonMap.put("canal", valores.length > 10 ? tryParseInt(valores[10]) : 1);
        return jsonMap;
    }

    private Map<String, Object> convertirLineaAJsonNameMachingPorBus(String linea) throws Exception {
        String[] valores = linea.split("\\|");
        String fechaOriginal = valores.length > 2 ? valores[2] : "";
        String fechanac = fechaOriginal.replaceAll("/", "");// formato de fecha en DDmmAAAA
        String nombreCompleto = valores.length > 1 ? valores[1].trim() : "";
        Map<String, String> nombreDesglosado = NombreParser.desglosarNombre(nombreCompleto);    
        Map<String, Object> jsonMap = new LinkedHashMap<>(); // Mantiene el orden
        Map<String, String> cabecera = new LinkedHashMap<>();   
        cabecera.put("idTrxGlobal", generateIdTrxGlobal());
        cabecera.put("sistemaOrigen", "HIPOTECARIOPLD");   
        jsonMap.put("cabecera", cabecera);
        jsonMap.put("fecha_nac",fechanac);
        jsonMap.put("nombre_1", nombreDesglosado.getOrDefault("nombre_1", ""));
        jsonMap.put("nombre_2", nombreDesglosado.getOrDefault("nombre_2", ""));
        jsonMap.put("apellido_paterno", nombreDesglosado.getOrDefault("apellido_paterno", ""));
        jsonMap.put("apellido_materno", nombreDesglosado.getOrDefault("apellido_materno", ""));
        jsonMap.put("num_cte", valores.length > 0 ? tryParseInt(valores[0]) : 0);
        jsonMap.put("num_cte_sol", valores.length > 6 ? tryParseInt(valores[6]) : 0);
        jsonMap.put("sucursal", valores.length > 7 ? valores[7] : "");
        jsonMap.put("usuario", valores.length > 8 ? valores[8] : "");
        jsonMap.put("tipo_sol", valores.length > 9 ? valores[9] : "");
        jsonMap.put("canal", valores.length > 10 ? tryParseInt(valores[10]) : 1);
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT); // Para que salga bonito     
        return  jsonMap;
    }
    /*crea el json de envio a listas negras*/
    private Map<String, Object> convertirLineaAJsonListasNegras(String linea) {
        String[] valores = linea.split("\\|");
        String nombreCompleto = valores.length > 1 ? valores[1].trim() : "";
        Map<String, String> nombreDesglosado = NombreParser.desglosarNombre(nombreCompleto);
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put("pRFC", valores.length > 2 ? valores[4] : "");
        jsonMap.put("pNombre1", nombreDesglosado.get("nombre_1"));
        jsonMap.put("pNombre2", nombreDesglosado.get("nombre_2"));
        jsonMap.put("pApellPaterno", nombreDesglosado.get("apellido_paterno"));
        jsonMap.put("pApellMaterno", nombreDesglosado.get("apellido_materno"));
        jsonMap.put("pFechaNac", valores.length > 2 ? valores[2] : "");
        return jsonMap;
    }
    
   /*Metodo para construir json para Peps pero consumo por bus*/
    public String conversionjsonPeps(Map<String, Object> jsonMap) throws Exception {
        // Extrae los datos necesarios de jsonMap
        // Ajusta las claves según la estructura real de tu JSON de entrada
        String pempresa = (String) jsonMap.get("pempresa");
        String pnumcte = (String) jsonMap.get("pnumcte");
        String pnumDirec = (String) jsonMap.get("pnum_direc");

        // Construir el JSON de solicitud para PEPs
        Map<String, Object> pepRequest = new HashMap<>();
        Map<String, String> cabecera = new HashMap<>();
        // Puedes generar un ID único o dejarlo vacío si aplica
        String ididTrx = generateIdTrxGlobal();
        cabecera.put("idTrxGlobal", ididTrx); 
        cabecera.put("sistemaOrigen", "HIPOTECARIOIPLD");
        pepRequest.put("cabecera", cabecera);
        pepRequest.put("cabecera", cabecera);
        pepRequest.put("pempresa", pempresa);
        pepRequest.put("pnumcte", pnumcte);
        pepRequest.put("pnum_direc", pnumDirec);

      
        String vcodretPep = webServiceClient.validarPEP(pepRequest);
        log.debug("LOG: Respuesta del WS de PEPs: {}", vcodretPep);

        return vcodretPep;
    }

    /*metodo para construir json dimanico para peps*/
    public Map<String, Object> construirJsonDinami(String lineaTxt, String estructura) {
        String[] valoresTxt = lineaTxt.split("\\|");
        String[] campos = estructura.split(",");
        Map<String, Object> jsonMap = new LinkedHashMap<>();
        int indexTxt = 0;
        for (String campo : campos) {
            campo = campo.trim();
            if (campo.contains("=")) {
                String[] partes = campo.split("=", 2);
                String clv = partes[0].trim();
                String valor = partes[1].replaceAll("^\"|\"$", "");
                jsonMap.put(clv, valor);
            } else {
                if (indexTxt < valoresTxt.length) {
                    jsonMap.put(campo, valoresTxt[indexTxt]);
                    indexTxt++;
                }
            }
        }
        return jsonMap;
    }
 
    private Integer tryParseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
        	String textSeguro = sanitizeLog.sanitizeForLog(text);        	 
            log.warn("Error al parsear entero: '{}'. Se usará 0.", textSeguro);
            return 0;
        }
    }

    private boolean contieneCaracteresNoPermitidos(String texto) {
        if (texto == null || texto.isEmpty()) {
            return false; // Considera si un texto vacío o nulo debe ser un error.
        }
        return !texto.matches("^[A-Z0-9|/ ]+$"); // Si encuentra alguna coincidencia, significa que hay caracteres no permitidos.
    }
    
    private boolean contieneCaracteresNoPermitidosSIC(String linea) {
        if (linea == null || linea.isEmpty()) return true;    
        String regex = "^[A-Z0-9]{13}\\|[A-Z ]+\\|\\d{4}-\\d{2}-\\d{2}\\|[A-Z]+$"; 
        return !linea.matches(regex);
    }
    
    /*metodo para guardar en  la tabla bitacora los archivos procesados*/
	public void SaveBitacora(String nomArchivo, String estatus, Integer registros, LocalDateTime fechaProcess,
		Integer registrosProcesadosOk, Integer registrosConError) {
		  try {

                BitacoraDTO bitacoraDTO = new BitacoraDTO(nomArchivo, estatus, registros, LocalDateTime.now(), registrosProcesadosOk,
                        registrosConError);
                BitacoraEntity bitacoraEntity = new BitacoraEntity(bitacoraDTO);
//                System.out.println("=====================================debug bitacora ok======================================");
              bitacoraRepository.guardarEnBitacora(nomArchivo,estatus,registros,registrosProcesadosOk,registrosConError,fechaProcess);

              log.info("LOG: Bitácora guardada para el archivo {}: Estatus={}, Registros={}", nomArchivo, estatus, registros);
		    }  catch (JDBCConnectionException e) {
		        log.error("No se pudo guardar detalle de bitácora por error de conexión a BD: ", e.getMessage());
		    }
			  catch ( PersistenceException | DataAccessException e) {
		        log.error("No se pudo guardar detalle de bitácora por error de conexión a BD: {}", e.getMessage());
	    } catch (Exception e) {
		        log.error("Error inesperado al guardar detalle de bitácora: {}", e.getMessage());
		    }		 
		}

	/*metodo para guardar en  la tabla bitacora detalle los registros con error*/
	public void SaveBitacoraDetalle(String nomArchivo, String estatus, String registros, LocalDateTime fechaProcess) {
		  try {	
		BitacoraDetalleDTO bitacoraDetalleDTO = new BitacoraDetalleDTO(nomArchivo, estatus, registros, fechaProcess);
		BitacoraDetalleEntity bitacoraDetalleEntity = new BitacoraDetalleEntity(bitacoraDetalleDTO);
//    	System.out.println("=====================================debug bitacoradetalle======================================");
        bitacoraDetalleRepository.guardarEnBitacoraDetalle(nomArchivo, estatus, registros, fechaProcess);
//        System.out.println("=====================================debug bitacoradetalle ok======================================");
		String nomArchivoSeguro = sanitizeLog.sanitizeForLog(nomArchivo);
		String registrosSeguro = sanitizeLog.sanitizeForLog(registros);
		log.info("LOG: Bitácora- detallada guardada para el archivo {}: Registro con error={}: Fecha:{} ", nomArchivoSeguro, registrosSeguro, fechaProcess);
	 }      catch (JDBCConnectionException e) {
	        log.error("No se pudo guardar detalle de bitácora por error de conexión a BD: ", e.getMessage());
	    }
		  catch ( PersistenceException | DataAccessException e) {
	        log.error("No se pudo guardar detalle de bitácora por error de conexión a BD: {}", e.getMessage());

	    } catch (Exception e) {

	        log.error("Error inesperado al guardar detalle de bitácora: {}", e.getMessage());

	    }
	}
	
	public String generateIdTrxGlobal() throws Exception {
	       LocalDateTime now = LocalDateTime.now(); 
	       DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
	        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM");
	        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
	        DateTimeFormatter hourFormatter = DateTimeFormatter.ofPattern("HH");
	        DateTimeFormatter minuteFormatter = DateTimeFormatter.ofPattern("mm");
	        DateTimeFormatter secondFormatter = DateTimeFormatter.ofPattern("ss");
	        DateTimeFormatter millisecondFormatter = DateTimeFormatter.ofPattern("SSS");

	        String idTrxGlobal = "0001" +
                    now.format(dayFormatter) +
                    now.format(monthFormatter) +
                    now.format(yearFormatter) +
                    now.format(hourFormatter) +
                    now.format(minuteFormatter) +
                    now.format(secondFormatter) +
                    now.format(millisecondFormatter);

	       return idTrxGlobal;
	    }
	
	/*Desglosa nombre*/
    public static class NombreParser {
        public static Map<String, String> desglosarNombre(String nombreCompleto) {
            String[] partes = nombreCompleto.split("\\s+");
            int totalPartes = partes.length;
            String primernombre = "";
            String segundonombre = "";
            String apellidoPaterno = "";
            String apellidoMaterno = "";

            if (totalPartes == 2) {
                primernombre = partes[0];
                apellidoPaterno = partes[1];
            } else if (totalPartes == 3) {
                primernombre = partes[0];
                apellidoPaterno = partes[1];

                apellidoMaterno = partes[2];
            } else if (totalPartes > 3) {
                primernombre = partes[0];
                apellidoPaterno = partes[totalPartes - 2];
                apellidoMaterno = partes[totalPartes - 1];
                if (totalPartes > 4) {
                    segundonombre = String.join(" ", Arrays.copyOfRange(partes, 1, totalPartes - 2));
                } else {
                    segundonombre = partes[1];
                }
            }

            Map<String, String> resultado = new HashMap<>();
            resultado.put("nombre_1", primernombre);
            resultado.put("nombre_2", segundonombre);
            resultado.put("apellido_paterno", apellidoPaterno);
            resultado.put("apellido_materno", apellidoMaterno);
            return resultado;
        }
    }
    
    public void guardarResumenTXT(String ruta) {
        try {
            Path path = Paths.get(ruta);
            Files.write(
                    path,
                    resumenArchivos,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Resumen de archivos guardado en: {}", ruta);
        } catch (IOException e) {
            log.error("Error guardando resumen", e);
        }
    }
    
    
   
   
}
