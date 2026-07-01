package com.bancoppel.HipotecarioPLD.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

 
@Component
@Getter
@Setter
public class configManager {
	 // ===== AWS  =====
	
	@Value("${email.notificacion.certificado}")
	private boolean isCertificado;

	
	@Value("${crearbean.httpclient}")
	private boolean crearbeanhttpclient;
	
	@Value("${RutaArchivo.Resumen}")
	public String RutaArchivoResumen;
	
    @Value("${TipoOperacion}")
    public String tipoOperacion;

    @Value("${Origen1}")
    public String origenPLD;

    //@Value("${Origen2}")
    //public String origenOperaciones;

    // PLD
    @Value("${rutaSftpPLD.Kredy}")
    public String sftpPLDKredy;

    @Value("${s3.rutaOrigenPLD.Kredy}")
    public String s3PLDKredy;

    @Value("${rutaSftpPLD.Yave}")
    public String sftpPLDYave;

    @Value("${s3.rutaOrigenPLD.Yave}")
    public String s3PLDYave;

    @Value("${rutaSftpPLD.Bancoppel}")
    public String sftpPLDBancoppel;

    @Value("${s3.rutaOrigenPLD.Bancoppel}")
    public String s3PLDBancoppel;

    // SIC
    @Value("${rutaSftpSIC.Kredy}")
    public String sftpSICKredy;

    @Value("${s3.rutaOrigenSIC.Kredy}")
    public String s3SICKredy;

    @Value("${rutaSftpSIC.Yave}")
    public String sftpSICYave;

    @Value("${s3.rutaOrigenSIC.Yave}")
    public String s3SICYave;

    @Value("${rutaSftpSIC.Bancoppel}")
    public String sftpSICBancoppel;

    @Value("${s3.rutaOrigenSIC.Bancoppel}")
    public String s3SICBancoppel;

    ///Salida
    ///
    ///
    ///
    // PLD
    @Value("${rutaSftpPLD.S.Kredy}")
    public String sftpPLDsKredy;

    @Value("${s3.rutaRespuestaPLD.Kredy}")
    public String s3PLDsKredy;

    @Value("${rutaSftpPLD.S.Yave}")
    public String sftpPLDsYave;

    @Value("${s3.rutaRespuestaPLD.Yave}")
    public String s3PLDsYave;

    @Value("${rutaSftpPLD.S.Bancoppel}")
    public String sftpPLDsBancoppel;

    @Value("${s3.rutaRespuestaPLD.Bancoppel}")
    public String s3PLDsBancoppel;

    // SIC
    @Value("${rutaSftpSIC.S.Kredy}")
    public String sftpSICsKredy;

    @Value("${s3.rutaRespuestaSIC.Kredy}")
    public String s3SICsKredy;

    @Value("${rutaSftpSIC.S.Yave}")
    public String sftpSICsYave;

    @Value("${s3.rutaRespuestaSIC.Yave}")
    public String s3SICsYave;

    @Value("${rutaSftpSIC.S.Bancoppel}")
    public String sftpSICsBancoppel;

    @Value("${s3.rutaRespuestaSIC.Bancoppel}")
    public String s3SICsBancoppel;
	
	// ===== AWS GENERAL =====
	//@Value("${S3.Prefix.Subida}")
    //private String S3PrefixSubida;

    @Value("${s3bucket}")
    private String S3BucketBajada;
 
    @Value("${s3bucket}")
    private String S3BucketSubida;
    
    @Value("${aws.region}")
    private String region;
 
    // ===== AWS LAMBDA =====
  //  @Value("${aws.lambda.functionName}")
   // private String awsLambdaFunctionName;
 
    @Value("${aws.lambda.timeout}")
    private int awsLambdaTimeout;
	 
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    // ===== AWS LAMBDA =====

  //  public String getAwsLambdaFunctionName() {
   //     return awsLambdaFunctionName;
   // }

   // public void setAwsLambdaFunctionName(String awsLambdaFunctionName) {
   //     this.awsLambdaFunctionName = awsLambdaFunctionName;
   // }

    public int getAwsLambdaTimeout() {
        return awsLambdaTimeout;
    }

    public void setAwsLambdaTimeout(int awsLambdaTimeout) {
        this.awsLambdaTimeout = awsLambdaTimeout;
    }

 // ===== RUTAS DE ORIGEN PLD =====
    @Value("${s3.rutaOrigenPLD.Kredy}")
    private String S3rutaOrigenPLDKredy;

    @Value("${s3.rutaOrigenPLD.Yave}")
    private String S3rutaOrigenPLDYave;

    @Value("${s3.rutaOrigenPLD.Bancoppel}")
    private String S3rutaOrigenPLDBancoppel;

    // ===== RUTAS DE RESPUESTA PLD =====
    @Value("${s3.rutaRespuestaPLD.Kredy}")
    private String S3rutaRespuestaPLDKredy;

    @Value("${s3.rutaRespuestaPLD.Yave}")
    private String S3rutaRespuestaPLDYave;

    @Value("${s3.rutaRespuestaPLD.Bancoppel}")
    private String S3rutaRespuestaPLDBancoppel;

    // ===== RUTAS DE ORIGEN SIC =====
    @Value("${s3.rutaOrigenSIC.Kredy}")
    private String S3rutaOrigenSICKredy;

    @Value("${s3.rutaOrigenSIC.Yave}")
    private String S3rutaOrigenSICYave;

    @Value("${s3.rutaOrigenSIC.Bancoppel}")
    private String S3rutaOrigenSICBancoppel;

    // ===== RUTAS DE RESPUESTA SIC =====
    @Value("${s3.rutaRespuestaSIC.Kredy}")
    private String S3rutaRespuestaSICKredy;

    @Value("${s3.rutaRespuestaSIC.Yave}")
    private String S3rutaRespuestaSICYave;

    @Value("${s3.rutaRespuestaSIC.Bancoppel}")
    private String S3rutaRespuestaSICBancoppel;

 // ===== RUTAS DE ORIGEN PLD =====
    public String getS3rutaOrigenPLDKredy() {
        return S3rutaOrigenPLDKredy;
    }

    public void setS3rutaOrigenPLDKredy(String S3rutaOrigenPLDKredy) {
        this.S3rutaOrigenPLDKredy = S3rutaOrigenPLDKredy;
    }

    public String getS3rutaOrigenPLDYave() {
        return S3rutaOrigenPLDYave;
    }

    public void setS3rutaOrigenPLDYave(String S3rutaOrigenPLDYave) {
        this.S3rutaOrigenPLDYave = S3rutaOrigenPLDYave;
    }

    public String getS3rutaOrigenPLDBancoppel() {
        return S3rutaOrigenPLDBancoppel;
    }

    public void setS3rutaOrigenPLDBancoppel(String S3rutaOrigenPLDBancoppel) {
        this.S3rutaOrigenPLDBancoppel = S3rutaOrigenPLDBancoppel;
    }

    // ===== RUTAS DE RESPUESTA PLD =====
    public String getS33rutaRespuestaPLDKredy() {
        return S3rutaRespuestaPLDKredy;
    }

    public void setS33rutaRespuestaPLDKredy(String S33rutaRespuestaPLDKredy) {
        this.S3rutaRespuestaPLDKredy = S33rutaRespuestaPLDKredy;
    }

    public String getS3rutaRespuestaPLDYave() {
        return S3rutaRespuestaPLDYave;
    }

    public void setS3rutaRespuestaPLDYave(String S3rutaRespuestaPLDYave) {
        this.S3rutaRespuestaPLDYave = S3rutaRespuestaPLDYave;
    }

    public String getS3rutaRespuestaPLDBancoppel() {
        return S3rutaRespuestaPLDBancoppel;
    }

    public void setS3rutaRespuestaPLDBancoppel(String S3rutaRespuestaPLDBancoppel) {
        this.S3rutaRespuestaPLDBancoppel = S3rutaRespuestaPLDBancoppel;
    }

    // ===== RUTAS DE ORIGEN SIC =====
    public String getS3rutaOrigenSICKredy() {
        return S3rutaOrigenSICKredy;
    }

    public void setS3rutaOrigenSICKredy(String S3rutaOrigenSICKredy) {
        this.S3rutaOrigenSICKredy = S3rutaOrigenSICKredy;
    }

    public String getS3rutaOrigenSICYave() {
        return S3rutaOrigenSICYave;
    }

    public void setS3rutaOrigenSICYave(String S3rutaOrigenSICYave) {
        this.S3rutaOrigenSICYave = S3rutaOrigenSICYave;
    }

    public String getS3rutaOrigenSICBancoppel() {
        return S3rutaOrigenSICBancoppel;
    }

    public void setS3rutaOrigenSICBancoppel(String S3rutaOrigenSICBancoppel) {
        this.S3rutaOrigenSICBancoppel = S3rutaOrigenSICBancoppel;
    }

    // ===== RUTAS DE RESPUESTA SIC =====
    public String getS3rutaRespuestaSICKredy() {
        return S3rutaRespuestaSICKredy;
    }

    public void setS3rutaRespuestaSICKredy(String S3rutaRespuestaSICKredy) {
        this.S3rutaRespuestaSICKredy = S3rutaRespuestaSICKredy;
    }

    public String getS3rutaRespuestaSICYave() {
        return S3rutaRespuestaSICYave;
    }

    public void setS3rutaRespuestaSICYave(String S3rutaRespuestaSICYave) {
        this.S3rutaRespuestaSICYave = S3rutaRespuestaSICYave;
    }

    public String getS3rutaRespuestaSICBancoppel() {
        return S3rutaRespuestaSICBancoppel;
    }

    public void setS3rutaRespuestaSICBancoppel(String S3rutaRespuestaSICBancoppel) {
        this.S3rutaRespuestaSICBancoppel = S3rutaRespuestaSICBancoppel;
    }

    
    
	//########################################################################
    
		//@Value("${ruta.archivo}")
	   // private String rutaArchivo;

	    @Value("${separador}")
	    private String separador;
	 
	    @Value("${extension}")
	    private String extension;
	 	    
	  //  @Value("${sftp.host}")
	   // private String sftpHost;
	 
	   // @Value("${sftp.port}")
	   // private int sftpPort;
	 
	    //buscar y quitar
	//   @Value("${sftp.user}")
	//  private String sftpUser;
	 
	//   @Value("${sftp.SFTP}")
	//    private String SFTP;

//		@Value("${sftp.remotePath}")
//	    private String sftpRemotePath;
		    
	    @Value("${bigquery.project.id}")
	    private String bigqueryprojectid ;

	    @Value("${bigquery.credentials.path}")
	    private String  bigquerycredentialspath;

	    @Value("${bigquery.dataset.id}")
	    private String bigquerydatasetId ;

	    @Value("${bigquery.tabla.id}")
	    private String bigquerytablaId ;
	       
	    @Value("${bigquery.location}")
	    private String bigquerylocation ;
	    
	    @Value("${urlnamematching}")
	    private String wsurl;
	    
	    @Value("${NameMaching.Activo}")
	    private String NameMachingActivo;  
	    
	    @Value("${ws.urlListasNegras}")
	    private String wsurlListasNegras ;   
	    
	    @Value("${ValidacionPEPS.Activo}")
	    private String ValidacionPEPSActivo ;  
	    
	    @Value("${ValidacionPeps.xbus}")
	    private String ValidacionPepsxbus ;  
	     
	    @Value("${ws.urlpep}")
	    private String wsurlpep;  
	    
	    @Value("${estructura.json.envio}")
	    private String estructurajsonPEP;  
	    
	    @Value("${validar.Existepep}")
	    private String validarExistepep;  

	    @Value("${ws.urlwspep}")
	    private String wsurlwspep;  
	    
	    @Value("${campo.abuscarpep}")
	    private String campoabuscarpep;  
	           
	    @Value("${NameMaching.Openshif}")
	    private String NameMachingOpenshif;
	    
	   // @Value("${sftp.proxyip}")
	   // private String sftpproxyip;
	    
	  //  @Value("${sftp.proxypuerto}")
	  //  private String sftpproxypuerto;
	    
	   // @Value("${sftp.proxytype}")
	   // private String sftpproxytype;

		@Value("${http.proxyHost}")
		private String proxyHost;

		@Value("${http.proxyPort}")
		private String proxyPort;
		
		

	}	 