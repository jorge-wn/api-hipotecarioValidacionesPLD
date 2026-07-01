package com.bancoppel.HipotecarioPLD.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

// Usamos Lombok para los getters, setters, constructor y toString
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data // Genera automáticamente getters, setters, toString(), equals() y hashCode()
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class FileProcessingResult implements Serializable {
  
	private static final long serialVersionUID = 1L;
    private String fileName; // Nombre del archivo procesado
    private String status; // Estado general del procesamiento (ej. "PROCESADO", "ERROR_ESTRUCTURA", "ERROR_LECTURA")
    private int totalRecords; // Número total de registros en el archivo
    private int processedOkRecords; // Número de registros procesados correctamente
    private int errorRecords; // Número de registros con algún tipo de error
    private LocalDateTime processingDate; // Fecha y hora del procesamiento
    private List<String> allRecordsContent; // Contenido de todos los registros del archivo original
    private List<String> processedOkRecordsContent; // Contenido de los registros que se procesaron sin error
    private List<String> errorRecordsContent; // Contenido de los registros que tuvieron error
    private String errorMessage; // Mensaje de error a nivel de archivo, si aplica

	public String getFileName() {
		return fileName;
	}
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public int getTotalRecords() {
		return totalRecords;
	}
	public void setTotalRecords(int totalRecords) {
		this.totalRecords = totalRecords;
	}
	public int getProcessedOkRecords() {
		return processedOkRecords;
	}
	public void setProcessedOkRecords(int processedOkRecords) {
		this.processedOkRecords = processedOkRecords;
	}
	public int getErrorRecords() {
		return errorRecords;
	}
	public void setErrorRecords(int errorRecords) {
		this.errorRecords = errorRecords;
	}
	public LocalDateTime getProcessingDate() {
		return processingDate;
	}
	public void setProcessingDate(LocalDateTime processingDate) {
		this.processingDate = processingDate;
	}
	public List<String> getAllRecordsContent() {
		return allRecordsContent;
	}
	public void setAllRecordsContent(List<String> allRecordsContent) {
		this.allRecordsContent = allRecordsContent;
	}
	public List<String> getProcessedOkRecordsContent() {
		return processedOkRecordsContent;
	}
	public void setProcessedOkRecordsContent(List<String> processedOkRecordsContent) {
		this.processedOkRecordsContent = processedOkRecordsContent;
	}
	public List<String> getErrorRecordsContent() {
		return errorRecordsContent;
	}
	public void setErrorRecordsContent(List<String> errorRecordsContent) {
		this.errorRecordsContent = errorRecordsContent;
	}
	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public static long getSerialversionuid() {
		return serialVersionUID;
	}
    
    
}
