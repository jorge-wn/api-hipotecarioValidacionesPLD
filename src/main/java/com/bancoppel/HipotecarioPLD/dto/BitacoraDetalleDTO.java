package com.bancoppel.HipotecarioPLD.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class BitacoraDetalleDTO {

	private String nomArchivo;
	private String estatus;
	private String registros;
	private LocalDateTime fechaProcess;

	public BitacoraDetalleDTO(String nomArchivo, String estatus, String registros, LocalDateTime fechaProcess) {
		this.nomArchivo = nomArchivo;
		this.estatus = estatus;
		this.registros = registros;
		this.fechaProcess = fechaProcess;
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

	public String getRegistros() {
		return registros;
	}

	public void setRegistros(String registros) {
		this.registros = registros;
	}

	public LocalDateTime getFechaProcess() {
		return fechaProcess;
	}

	public void setFechaProcess(LocalDateTime fechaProcess) {
		this.fechaProcess = fechaProcess;
	}
}