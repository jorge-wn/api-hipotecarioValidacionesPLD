package com.bancoppel.HipotecarioPLD.dto;


public class EmailRequest {
    private String origen;
    private String codigoEstatus;
    private String rutaS3Adjunto;

    public EmailRequest(String origen, String codigoEstatus,String rutaS3Adjunto) {
        this.origen = origen;
        this.codigoEstatus = codigoEstatus;
        this.rutaS3Adjunto = rutaS3Adjunto;
    }
 
    public String getOrigen() {
        return origen;
    }
 
    public String getCodigoEstatus() {
        return codigoEstatus;
    }

	public String getRutaS3Adjunto() {
		return rutaS3Adjunto;
	}
    
    
}
 