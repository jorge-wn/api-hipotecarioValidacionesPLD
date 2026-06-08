package com.bancoppel.HipotecarioPLD.controller;
 
import com.ulisesbocchio.jasyptspringboot.annotation.EnableEncryptableProperties;
import com.bancoppel.HipotecarioPLD.dto.LambdaRequestDTOAws;
import com.bancoppel.HipotecarioPLD.dto.LambdaResponseDTOAws;
import com.bancoppel.HipotecarioPLD.dto.ResultadoCargaDTOAws;
import com.bancoppel.HipotecarioPLD.config.*;
import com.bancoppel.HipotecarioPLD.service.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.time.*;
import lombok.RequiredArgsConstructor;
import com.bancoppel.HipotecarioPLD.Util.Currentyearmonth;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("api")
@EnableEncryptableProperties
@RequiredArgsConstructor
public class FileController {
    //#####AWS
    private final ResultadoServiceAws resultadoServiceAws;
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LectorArchivoAws.class);
    private final configManager config;
    private final LambdaService lambdaService;
    private final EmailNotificacionService emailNotificacionService;
    private final S3ArchivoService s3archivoservice;
    private final Currentyearmonth currentyearmonth;
    
   // @Value("${app.timezone:UTC}")
   // private String timezone;
       
    @Autowired
    public FileController(
    		Currentyearmonth currentyearmonth,
    		LambdaService lambdaService,
    		ResultadoServiceAws resultadoServiceAws,
            configManager config,
            EmailNotificacionService emailNotificacionService,
            S3ArchivoService s3archivoservice,
        //    @Value("${sftp.user}") String sftpUserEncrypted,
         //   @Value("${sftp.SFTP}") String sftpEncrypted,
            @Value("${aessecretkey}") char[] base64Key,
            @Value("${aesiv}") String base64Iv
    ) throws Exception {
    	this.currentyearmonth = currentyearmonth;
    	this.resultadoServiceAws = resultadoServiceAws;
        this.config = config;
        this.lambdaService = lambdaService;
        this.emailNotificacionService = emailNotificacionService;
        this.s3archivoservice = s3archivoservice;
    }
 
    //########################CAMBIOS AWS##################################
    @GetMapping("/PLDescarga")
    public ResponseEntity<String> ejecutarPLD() {
  /*  List<LambdaResponseDTOAws> respuestas = new ArrayList<>();
     respuestas.add(ejecutar(config.sftpPLDKredy, config.s3PLDKredy));
     respuestas.add(ejecutar(config.sftpSICKredy, config.s3SICKredy));
     respuestas.add(ejecutar(config.sftpPLDYave, config.s3PLDYave));
     respuestas.add(ejecutar(config.sftpSICYave, config.s3SICYave));
     respuestas.add(ejecutar(config.sftpPLDBancoppel, config.s3PLDBancoppel));
     respuestas.add(ejecutar(config.sftpSICBancoppel, config.s3SICBancoppel));
          
     try {
         Thread.sleep(15000);
     } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
     }*/
    	s3archivoservice.limpiarResumen();
        List<CompletableFuture<Void>> procesos = new ArrayList<>();

        procesos.add(ejecutar(config.sftpPLDKredy,config.s3PLDKredy));
        procesos.add(ejecutar(config.sftpSICKredy,config.s3SICKredy));
        procesos.add(ejecutar(config.sftpPLDYave,config.s3PLDYave));
        procesos.add(ejecutar(config.sftpSICYave,config.s3SICYave));
        procesos.add(ejecutar(config.sftpPLDBancoppel,config.s3PLDBancoppel));
        procesos.add(ejecutar(config.sftpSICBancoppel,config.s3SICBancoppel));

        CompletableFuture.allOf(procesos.toArray(new CompletableFuture[0])).join();
    	
     
     String keyS3 =  config.getRutaArchivoResumen()
             + "/resumen_procesamientoPLD_"
             + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"))
             + ".txt";    
      String resumen = s3archivoservice.guardarResumenTXT(keyS3);
      
      //return ResponseEntity.ok(respuestas);*/
     // boolean todoOk = respuestas.stream().allMatch(r -> "Success".equalsIgnoreCase(r.getResultado()));
     
      //HttpStatus status = todoOk ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
     // System.out.println("resultados status: "+status);
      // enviar email con el codigo real
    //  emailNotificacionService.enviarNotificacion(String.valueOf(status.value()),keyS3 );
      emailNotificacionService.enviarNotificacion(String.valueOf(HttpStatus.OK.value()),keyS3);
      log.info("Culmino el proceso de validación PLD/Puntualidad Coppel");
      return ResponseEntity.ok(resumen);
    }
    
  //  private LambdaResponseDTOAws ejecutar(String sftp, String s3 ) {
    private CompletableFuture<Void> ejecutar(String sftp, String s3){
    	String anioMes = currentyearmonth.anioMesActual();
         LambdaRequestDTOAws request = new LambdaRequestDTOAws();
        request.setRutaSftp(sftp + "/" + anioMes);
        request.setRutaS3(s3 + "/" + anioMes);
        request.setTipoOperacion(config.tipoOperacion);
        request.setOrigen(config.origenPLD);
        LambdaResponseDTOAws response = lambdaService.ejecutarLambda(request);
        // 🔹 Convertir LambdaResponse → ResultadoCargaDTOAws
        log.info("Enviando a carga dto aws");
        ResultadoCargaDTOAws resultado = mapearResponse(response);
        // 🔹 Llamada directa a método de clase
        recibirResultadoAws(resultado);
        //return response;
        return recibirResultadoAws(resultado);
    }
    
       private ResultadoCargaDTOAws mapearResponse(LambdaResponseDTOAws response) {
    	log.info("INICIANDO mapearResponse");
        ResultadoCargaDTOAws dto = new ResultadoCargaDTOAws();
        dto.setArchivosProcesados(response.getArchivosProcesados());
        dto.setMensaje(response.getMensaje());
        dto.setResultado(response.getResultado());
        dto.setRutaS3(response.getRutaS3());
        
        return dto;
    }
     
   /* public void recibirResultadoAws(ResultadoCargaDTOAws request) {
        log.info("Procesando resultado AWS desde Lambda");
        resultadoServiceAws.procesarDesdeS3(request);
    }*/
    
    public CompletableFuture<Void> recibirResultadoAws(ResultadoCargaDTOAws request) {
        log.info("Procesando resultado AWS desde Lambda");
        return resultadoServiceAws.procesarDesdeS3(request);
    }
    
  /*  private String anioMesActual() {
        ZonedDateTime ahoraMexico = ZonedDateTime.now(ZoneId.of(timezone));
        return ahoraMexico.format(DateTimeFormatter.ofPattern("yyyy/MM"));
    }*/
   
}