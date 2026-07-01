package com.bancoppel.HipotecarioPLD.dto;


import lombok.Data;
import java.util.List;


@Data
public class LambdaResponseDTOAws {
	 private String timestamp;
	    private List<String> archivosProcesados;
	    private String rutaS3;
	    private String rutaSftp;
	    private String resultado;
	    private String mensaje;
}
