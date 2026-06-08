package com.bancoppel.HipotecarioPLD.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BitacoraDTO {
    private String nomArchivo;
    private String estatus;
    private Integer registros;
    private LocalDateTime fechaProcess;
    private Integer registros_procesados;
    private Integer registros_con_error;
        
   public BitacoraDTO(String nomArchivo,String estatus, Integer registros,LocalDateTime fechaProcess,Integer registros_procesados,Integer registros_con_error) {
        this.nomArchivo = nomArchivo;
        this.estatus = estatus;
        this.registros = registros;
        this.fechaProcess = fechaProcess;
        this.registros_procesados = registros_procesados;
        this.registros_con_error = registros_con_error; 
   }

public String getNomArchivo() {
	return nomArchivo;
}

public void setNomArchivo(String nomArchivo) {
	this.nomArchivo = nomArchivo;
}

public String getEstatus() {
	return estatus;
}

public void setEstatus(String estatus) {
	this.estatus = estatus;
}

public Integer getRegistros() {
	return registros;
}

public void setRegistros(Integer registros) {
	this.registros = registros;
}

public LocalDateTime getFechaProcess() {
	return fechaProcess;
}

public void setFechaProcess(LocalDateTime fechaProcess) {
	this.fechaProcess = fechaProcess;
}

public Integer getRegistros_procesados() {
	return registros_procesados;
}

public void setRegistros_procesados(Integer registros_procesados) {
	this.registros_procesados = registros_procesados;
}

public Integer registros_con_error() {
	return registros_con_error;
}

public void setRegistros_conerror(Integer registros_con_error) {
	this.registros_con_error = registros_con_error;
}   
}