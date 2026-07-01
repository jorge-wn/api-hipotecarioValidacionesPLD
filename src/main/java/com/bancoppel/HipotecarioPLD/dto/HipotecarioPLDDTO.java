package com.bancoppel.HipotecarioPLD.dto;

import java.util.List;
import lombok.Data;

@Data
public class HipotecarioPLDDTO {
	   private List<String> archivosAprocesar;
	    private String rutaS3;
	    private String tipoOperacion;
}
