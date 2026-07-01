package com.bancoppel.HipotecarioPLD.dto;

import lombok.Data;

@Data
public class LambdaRequestDTOAws {
	  private String rutaSftp;
	    private String rutaS3;
	    private String tipoOperacion;
	    private String origen;	
}
