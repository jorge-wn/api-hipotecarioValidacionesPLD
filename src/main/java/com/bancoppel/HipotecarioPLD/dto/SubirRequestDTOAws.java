package com.bancoppel.HipotecarioPLD.dto;


import lombok.Data;

@Data
public class SubirRequestDTOAws {
	
	    private String archivo;
	    private String proveedor;   // YAVE | KREDY | BANCOPPEL
	    private String validacion;  // PLD | SIC
	    private String origen;
}
